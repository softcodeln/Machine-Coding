package com.lucky.parkinglot.service;

import com.lucky.parkinglot.enums.GateType;
import com.lucky.parkinglot.enums.SpotStatus;
import com.lucky.parkinglot.enums.TicketStatus;
import com.lucky.parkinglot.enums.VehicleType;
import com.lucky.parkinglot.exception.ActiveTicketExistsException;
import com.lucky.parkinglot.exception.NoSpotAvailableException;
import com.lucky.parkinglot.exception.TicketNotFoundException;
import com.lucky.parkinglot.model.Customer;
import com.lucky.parkinglot.model.Floor;
import com.lucky.parkinglot.model.Gate;
import com.lucky.parkinglot.model.Spot;
import com.lucky.parkinglot.model.Ticket;
import com.lucky.parkinglot.model.Vehicle;
import com.lucky.parkinglot.payment.Payment;
import com.lucky.parkinglot.repository.FloorRepository;
import com.lucky.parkinglot.repository.SpotRepository;
import com.lucky.parkinglot.repository.TicketRepository;
import com.lucky.parkinglot.strategy.HourlyPricing;
import com.lucky.parkinglot.strategy.NearestSpotAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParkingServiceTest {

    private static final int CAR_RATE = 50;
    private static final int BIKE_RATE = 20;
    private static final int PENALTY = 200;
    private static final Payment PAYMENT_OK = (customer, amount) -> true;
    private static final Payment PAYMENT_FAILS = (customer, amount) -> false;

    private final Customer customer = new Customer("Ravi");
    private final Gate entryGate = new Gate("Entry-1", GateType.ENTRY);
    private final Gate exitGate = new Gate("Exit-1", GateType.EXIT);
    private final Vehicle car = new Vehicle("CAR-1", VehicleType.CAR);

    private final FloorRepository floorRepository = new FloorRepository();
    private MutableClock clock;
    private Spot bikeSpot;
    private Spot carSpot1;
    private Spot carSpot2;
    private ParkingService service;

    @BeforeEach
    void setUp() {
        SpotRepository spotRepository = new SpotRepository();
        Floor floor = floorRepository.save(new Floor("Ground"));
        bikeSpot = spotRepository.save(floor.addSpot("G-1", VehicleType.BIKE));
        carSpot1 = spotRepository.save(floor.addSpot("G-2", VehicleType.CAR));
        carSpot2 = spotRepository.save(floor.addSpot("G-3", VehicleType.CAR));
        clock = new MutableClock();
        service = newService(spotRepository);
    }

    private ParkingService newService(SpotRepository spotRepository) {
        HourlyPricing pricing = new HourlyPricing(Map.of(VehicleType.CAR, CAR_RATE, VehicleType.BIKE, BIKE_RATE));
        return new ParkingService(new NearestSpotAssignment(spotRepository), pricing, new TicketRepository(),
                spotRepository, clock, PENALTY);
    }

    @Test
    void entryAssignsNearestSpotOfMatchingTypeAndGeneratesActiveTicket() {
        Ticket ticket = service.parkVehicle(car, customer, entryGate);

        assertThat(ticket.getId()).isNotNull();
        assertThat(ticket.getSpot()).isSameAs(carSpot1);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(ticket.getEntryGate()).isSameAs(entryGate);
        assertThat(carSpot1.getStatus()).isEqualTo(SpotStatus.OCCUPIED);
        assertThat(carSpot1.getVehicle()).isSameAs(car);
        assertThat(bikeSpot.getStatus()).isEqualTo(SpotStatus.FREE);
    }

    @Test
    void lowerFloorsAreFilledFirst() {
        SpotRepository spotRepository = new SpotRepository();
        Floor first = floorRepository.save(new Floor("First"));
        Floor second = floorRepository.save(new Floor("Second"));
        spotRepository.save(second.addSpot("S-1", VehicleType.CAR));
        Spot firstFloorSpot = spotRepository.save(first.addSpot("F-1", VehicleType.CAR));

        assertThat(newService(spotRepository).parkVehicle(car, customer, entryGate).getSpot())
                .isSameAs(firstFloorSpot);
    }

    @Test
    void entryIsRejectedAtExitGate() {
        assertThatThrownBy(() -> service.parkVehicle(car, customer, exitGate))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(carSpot1.getStatus()).isEqualTo(SpotStatus.FREE);
    }

    @Test
    void noMatchingSpotMeansNoTicket() {
        service.parkVehicle(new Vehicle("BIKE-1", VehicleType.BIKE), customer, entryGate);

        assertThatThrownBy(() -> service.parkVehicle(new Vehicle("BIKE-2", VehicleType.BIKE), customer, entryGate))
                .isInstanceOf(NoSpotAvailableException.class);
        assertThatThrownBy(() -> service.findActiveTicket("BIKE-2")).isInstanceOf(TicketNotFoundException.class);
        assertThat(carSpot1.getStatus()).isEqualTo(SpotStatus.FREE);
    }

    @Test
    void spotUnderMaintenanceIsNeverAssigned() {
        assertThat(carSpot1.startMaintenance()).isTrue();

        assertThat(service.parkVehicle(car, customer, entryGate).getSpot()).isSameAs(carSpot2);
    }

    @Test
    void occupiedSpotCannotGoIntoMaintenance() {
        service.parkVehicle(car, customer, entryGate);

        assertThat(carSpot1.startMaintenance()).isFalse();
        assertThat(carSpot1.getStatus()).isEqualTo(SpotStatus.OCCUPIED);
    }

    @Test
    void vehicleCannotHoldTwoActiveTickets() {
        service.parkVehicle(car, customer, entryGate);

        assertThatThrownBy(() -> service.parkVehicle(car, customer, entryGate))
                .isInstanceOf(ActiveTicketExistsException.class);
        assertThat(carSpot2.getStatus()).isEqualTo(SpotStatus.FREE);
    }

    @Test
    void successfulExitChargesRoundedUpHoursAndFreesSpot() {
        Ticket ticket = service.parkVehicle(car, customer, entryGate);
        clock.advance(Duration.ofMinutes(61));

        assertThat(service.exitVehicle(ticket, PAYMENT_OK)).isTrue();

        assertThat(ticket.getAmount()).isEqualTo(2 * CAR_RATE);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(ticket.getExitTime()).isEqualTo(ticket.getEntryTime().plusMinutes(61));
        assertThat(carSpot1.getStatus()).isEqualTo(SpotStatus.FREE);
        assertThat(carSpot1.getVehicle()).isNull();
    }

    @Test
    void exactHoursAreNotRoundedUpAndRatesDependOnVehicleType() {
        Vehicle bike = new Vehicle("BIKE-1", VehicleType.BIKE);
        Ticket ticket = service.parkVehicle(bike, customer, entryGate);
        clock.advance(Duration.ofHours(3));

        service.exitVehicle(ticket, PAYMENT_OK);

        assertThat(ticket.getAmount()).isEqualTo(3 * BIKE_RATE);
    }

    @Test
    void oneSecondOverAnHourIsChargedAsNextHour() {
        Ticket ticket = service.parkVehicle(car, customer, entryGate);
        clock.advance(Duration.ofHours(1).plusSeconds(1));

        service.exitVehicle(ticket, PAYMENT_OK);

        assertThat(ticket.getAmount()).isEqualTo(2 * CAR_RATE);
    }

    @Test
    void failedPaymentKeepsTicketActiveAndSpotOccupiedUntilRetrySucceeds() {
        Ticket ticket = service.parkVehicle(car, customer, entryGate);
        clock.advance(Duration.ofMinutes(30));

        assertThat(service.exitVehicle(ticket, PAYMENT_FAILS)).isFalse();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(carSpot1.getStatus()).isEqualTo(SpotStatus.OCCUPIED);
        assertThat(service.findActiveTicket(car.getNumber())).isSameAs(ticket);

        clock.advance(Duration.ofHours(1));
        assertThat(service.exitVehicle(ticket, PAYMENT_OK)).isTrue();

        assertThat(ticket.getAmount()).isEqualTo(2 * CAR_RATE);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(carSpot1.getStatus()).isEqualTo(SpotStatus.FREE);
    }

    @Test
    void paidTicketCannotBeUsedAgain() {
        Ticket ticket = service.parkVehicle(car, customer, entryGate);
        service.exitVehicle(ticket, PAYMENT_OK);

        assertThatThrownBy(() -> service.exitVehicle(ticket, PAYMENT_OK)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void vehicleCanParkAgainAfterExit() {
        service.exitVehicle(service.parkVehicle(car, customer, entryGate), PAYMENT_OK);

        Ticket second = service.parkVehicle(car, customer, entryGate);

        assertThat(second.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(second.getSpot()).isSameAs(carSpot1);
    }

    @Test
    void lostTicketIsFoundByVehicleNumberAndChargedPenalty() {
        Ticket ticket = service.parkVehicle(car, customer, entryGate);
        clock.advance(Duration.ofMinutes(10));

        assertThat(service.exitVehicleWithLostTicket(car.getNumber(), PAYMENT_OK)).isTrue();

        assertThat(ticket.getAmount()).isEqualTo(CAR_RATE + PENALTY);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(carSpot1.getStatus()).isEqualTo(SpotStatus.FREE);
    }

    @Test
    void lostTicketForUnknownVehicleIsRejected() {
        assertThatThrownBy(() -> service.exitVehicleWithLostTicket("NOPE", PAYMENT_OK))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void concurrentGatesNeverAssignTheSameSpot() throws Exception {
        SpotRepository spotRepository = new SpotRepository();
        Floor floor = floorRepository.save(new Floor("Busy"));
        int spotCount = 5;
        for (int i = 0; i < spotCount; i++) {
            spotRepository.save(floor.addSpot("S-" + i, VehicleType.CAR));
        }
        ParkingService lot = newService(spotRepository);

        List<Callable<Ticket>> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Vehicle vehicle = new Vehicle("V-" + i, VehicleType.CAR);
            tasks.add(() -> lot.parkVehicle(vehicle, customer, entryGate));
        }

        List<Ticket> tickets = runTogether(tasks);

        Set<Spot> spots = new HashSet<>();
        tickets.forEach(ticket -> spots.add(ticket.getSpot()));
        assertThat(tickets).hasSize(spotCount);
        assertThat(spots).hasSize(spotCount);
    }

    @Test
    void concurrentEntriesForOneVehicleCreateOneTicketAndHoldOneSpot() throws Exception {
        List<Callable<Ticket>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> service.parkVehicle(car, customer, entryGate));
        }

        List<Ticket> tickets = runTogether(tasks);

        assertThat(tickets).hasSize(1);
        assertThat(List.of(carSpot1, carSpot2)).filteredOn(spot -> spot.getStatus() == SpotStatus.OCCUPIED)
                .containsExactly(tickets.getFirst().getSpot());
    }

    @Test
    void concurrentExitsChargeCustomerOnce() throws Exception {
        Ticket ticket = service.parkVehicle(car, customer, entryGate);
        CountingPayment payment = new CountingPayment();

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> service.exitVehicle(ticket, payment));
        }

        assertThat(runTogether(tasks)).containsExactly(true);
        assertThat(payment.calls).isEqualTo(1);
    }

    // Runs the tasks at the same moment and returns the results of those that did not throw.
    private <T> List<T> runTogether(List<Callable<T>> tasks) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        List<T> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            for (Future<T> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException expectedForLosers) {
                    // The task lost the race and was rejected.
                }
            }
        }
        return results;
    }

    private static class CountingPayment implements Payment {

        private int calls;

        @Override
        public synchronized boolean pay(Customer customer, Integer amount) {
            calls++;
            return true;
        }
    }

    private static class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

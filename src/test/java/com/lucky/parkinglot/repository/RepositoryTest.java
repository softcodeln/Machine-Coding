package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.enums.GateType;
import com.lucky.parkinglot.enums.SpotStatus;
import com.lucky.parkinglot.enums.VehicleType;
import com.lucky.parkinglot.model.Customer;
import com.lucky.parkinglot.model.Floor;
import com.lucky.parkinglot.model.Gate;
import com.lucky.parkinglot.model.Spot;
import com.lucky.parkinglot.model.Ticket;
import com.lucky.parkinglot.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryTest {

    private final FloorRepository floorRepository = new FloorRepository();
    private final SpotRepository spotRepository = new SpotRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final TicketRepository ticketRepository = new TicketRepository();

    @Test
    void saveAssignsIdOnInsertAndKeepsItOnUpdate() {
        CustomerRepository repository = new CustomerRepository();

        Customer first = repository.save(new Customer("Ravi"));
        Customer second = repository.save(new Customer("Meera"));
        repository.save(first);

        assertThat(first.getId()).isEqualTo(1);
        assertThat(second.getId()).isEqualTo(2);
        assertThat(repository.findAll()).containsExactly(first, second);
        assertThat(repository.findById(2)).containsSame(second);
        assertThat(repository.findById(99)).isEmpty();
    }

    @Test
    void vehicleNumberMustBeUnique() {
        Vehicle vehicle = vehicleRepository.save(new Vehicle("KA-01", VehicleType.CAR));

        vehicleRepository.save(vehicle);

        assertThat(vehicleRepository.findByNumber("KA-01")).containsSame(vehicle);
        assertThatThrownBy(() -> vehicleRepository.save(new Vehicle("KA-01", VehicleType.BIKE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void spotNumberMustBeUniquePerFloor() {
        Floor first = floorRepository.save(new Floor("First"));
        Floor second = floorRepository.save(new Floor("Second"));
        spotRepository.save(first.addSpot("A-1", VehicleType.CAR));

        spotRepository.save(second.addSpot("A-1", VehicleType.CAR));

        assertThatThrownBy(() -> spotRepository.save(first.addSpot("A-1", VehicleType.CAR)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findFreeByTypeFiltersAndOrdersByFloorThenSpot() {
        Floor first = floorRepository.save(new Floor("First"));
        Floor second = floorRepository.save(new Floor("Second"));
        Spot upstairs = spotRepository.save(second.addSpot("S-1", VehicleType.CAR));
        Spot downstairsA = spotRepository.save(first.addSpot("F-1", VehicleType.CAR));
        Spot downstairsB = spotRepository.save(first.addSpot("F-2", VehicleType.CAR));
        spotRepository.save(first.addSpot("F-3", VehicleType.BIKE));
        Spot maintenance = spotRepository.save(first.addSpot("F-4", VehicleType.CAR));
        maintenance.startMaintenance();

        assertThat(spotRepository.findFreeByType(VehicleType.CAR)).containsExactly(downstairsA, downstairsB, upstairs);
    }

    @Test
    void occupyIfFreeSucceedsOnlyOnce() {
        Floor floor = floorRepository.save(new Floor("Ground"));
        Spot spot = spotRepository.save(floor.addSpot("G-1", VehicleType.CAR));

        assertThat(spotRepository.occupyIfFree(spot.getId())).containsSame(spot);
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.OCCUPIED);
        assertThat(spotRepository.occupyIfFree(spot.getId())).isEmpty();
        assertThat(spotRepository.occupyIfFree(99)).isEmpty();
    }

    @Test
    void secondActiveTicketForVehicleIsNotSavedUntilFirstIsPaid() {
        Floor floor = floorRepository.save(new Floor("Ground"));
        Spot spot = spotRepository.save(floor.addSpot("G-1", VehicleType.CAR));
        Vehicle vehicle = vehicleRepository.save(new Vehicle("KA-01", VehicleType.CAR));
        Ticket first = newTicket(vehicle, spot);
        Ticket second = newTicket(vehicle, spot);

        assertThat(ticketRepository.saveIfVehicleHasNoActiveTicket(first)).isTrue();
        assertThat(ticketRepository.findActiveByVehicleNumber("KA-01")).containsSame(first);
        assertThat(ticketRepository.saveIfVehicleHasNoActiveTicket(second)).isFalse();
        assertThat(second.getId()).isNull();

        first.markPaid();
        ticketRepository.save(first);

        assertThat(ticketRepository.findActiveByVehicleNumber("KA-01")).isEmpty();
        assertThat(ticketRepository.saveIfVehicleHasNoActiveTicket(second)).isTrue();
    }

    private Ticket newTicket(Vehicle vehicle, Spot spot) {
        return new Ticket(LocalDateTime.now(), new Gate("Entry-1", GateType.ENTRY), vehicle, new Customer("Ravi"), spot);
    }
}

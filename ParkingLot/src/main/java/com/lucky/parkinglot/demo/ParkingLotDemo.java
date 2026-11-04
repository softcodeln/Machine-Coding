package com.lucky.parkinglot.demo;

import com.lucky.parkinglot.enums.GateType;
import com.lucky.parkinglot.enums.VehicleType;
import com.lucky.parkinglot.exception.NoSpotAvailableException;
import com.lucky.parkinglot.gateway.RazorpayGatewayAdapter;
import com.lucky.parkinglot.gateway.RazorpayPaymentGateway;
import com.lucky.parkinglot.model.Customer;
import com.lucky.parkinglot.model.Employee;
import com.lucky.parkinglot.model.Floor;
import com.lucky.parkinglot.model.Gate;
import com.lucky.parkinglot.model.ParkingLot;
import com.lucky.parkinglot.model.Ticket;
import com.lucky.parkinglot.model.Vehicle;
import com.lucky.parkinglot.payment.CashPayment;
import com.lucky.parkinglot.payment.Payment;
import com.lucky.parkinglot.payment.UpiPayment;
import com.lucky.parkinglot.repository.CustomerRepository;
import com.lucky.parkinglot.repository.EmployeeRepository;
import com.lucky.parkinglot.repository.FloorRepository;
import com.lucky.parkinglot.repository.GateRepository;
import com.lucky.parkinglot.repository.ParkingLotRepository;
import com.lucky.parkinglot.repository.SpotRepository;
import com.lucky.parkinglot.repository.TicketRepository;
import com.lucky.parkinglot.repository.VehicleRepository;
import com.lucky.parkinglot.service.ParkingService;
import com.lucky.parkinglot.strategy.HourlyPricing;
import com.lucky.parkinglot.strategy.NearestSpotAssignment;

import java.time.Clock;
import java.util.List;
import java.util.Map;

public class ParkingLotDemo {

    public static void main(String[] args) {
        ParkingLotRepository parkingLotRepository = new ParkingLotRepository();
        FloorRepository floorRepository = new FloorRepository();
        SpotRepository spotRepository = new SpotRepository();
        GateRepository gateRepository = new GateRepository();
        EmployeeRepository employeeRepository = new EmployeeRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        VehicleRepository vehicleRepository = new VehicleRepository();
        TicketRepository ticketRepository = new TicketRepository();

        Floor ground = floorRepository.save(new Floor("Ground"));
        spotRepository.save(ground.addSpot("G-1", VehicleType.CAR));
        spotRepository.save(ground.addSpot("G-2", VehicleType.BIKE));

        Gate entryGate = gateRepository.save(new Gate("Entry-1", GateType.ENTRY));
        Gate exitGate = gateRepository.save(new Gate("Exit-1", GateType.EXIT));
        Employee employee = employeeRepository.save(new Employee("Aashi", "9000000000", entryGate));
        parkingLotRepository.save(new ParkingLot("City Centre", List.of(ground), List.of(entryGate),
                List.of(exitGate), List.of(employee)));

        ParkingService service = new ParkingService(
                new NearestSpotAssignment(spotRepository),
                new HourlyPricing(Map.of(VehicleType.CAR, 50, VehicleType.BIKE, 20)),
                ticketRepository,
                spotRepository,
                Clock.systemDefaultZone(),
                200);

        Customer ravi = customerRepository.save(new Customer("Ravi"));
        Customer meera = customerRepository.save(new Customer("Meera"));
        Vehicle car = vehicleRepository.save(new Vehicle("KA-01-1234", VehicleType.CAR));
        Vehicle secondCar = vehicleRepository.save(new Vehicle("KA-01-5678", VehicleType.CAR));
        Vehicle bike = vehicleRepository.save(new Vehicle("KA-02-9999", VehicleType.BIKE));

        Ticket ticket = service.parkVehicle(car, ravi, entryGate);
        System.out.printf("Ticket %d: %s parked at %s, spot %s (%s)%n", ticket.getId(), car.getNumber(),
                ticket.getSpot().getFloor().getName(), ticket.getSpot().getNumber(), ticket.getSpot().getStatus());

        try {
            service.parkVehicle(secondCar, meera, entryGate);
        } catch (NoSpotAvailableException e) {
            System.out.println("Second car turned away: " + e.getMessage());
        }

        Payment failedPayment = (payer, amount) -> false;
        boolean paid = service.exitVehicle(ticket, failedPayment);
        System.out.printf("First payment succeeded: %b, ticket %s, spot %s%n", paid, ticket.getStatus(),
                ticket.getSpot().getStatus());

        Payment upi = new UpiPayment(new RazorpayGatewayAdapter(new RazorpayPaymentGateway()), "IN");
        paid = service.exitVehicle(ticket, upi);
        System.out.printf("UPI payment succeeded: %b, amount %d, ticket %s, spot %s%n", paid, ticket.getAmount(),
                ticket.getStatus(), ticket.getSpot().getStatus());

        service.parkVehicle(bike, ravi, entryGate);
        paid = service.exitVehicleWithLostTicket(bike.getNumber(), new CashPayment());
        System.out.printf("Lost-ticket exit by cash succeeded: %b%n", paid);

        System.out.printf("Tickets stored: %d, vehicles stored: %d%n", ticketRepository.findAll().size(),
                vehicleRepository.findAll().size());
    }
}

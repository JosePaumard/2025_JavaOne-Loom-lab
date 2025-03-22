package org.paumard.server.company.model.company;

import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import org.paumard.server.company.model.CompanyServer;
import org.paumard.server.company.model.city.City;
import org.paumard.server.company.model.flight.*;

import java.util.*;
import java.util.stream.Collectors;

public record Company(String name, int pricingStrategy, Set<City> servedCities, String tag, int average,
                      int dispersion) {

    public Company {
        Objects.requireNonNull(name);
        Objects.requireNonNull(tag);
        Objects.requireNonNull(servedCities);
        if (pricingStrategy < 0) {
            throw new IllegalArgumentException("Price strategy should be greater than 0");
        }
        if (dispersion <= 0) {
            throw new IllegalArgumentException("Dispersion should be greater than 0");
        }
        if (servedCities.isEmpty()) {
            throw new IllegalArgumentException("The company " + name + " has no served Cities");
        }
        servedCities = Collections.unmodifiableSet(new HashSet<>(servedCities));
    }

    public record ErrorMessage(String message) {
        public ErrorMessage {
            Objects.requireNonNull(message);
        }
    }

    public record CompanyDTO(String name, String tag) {

        public CompanyDTO {
            Objects.requireNonNull(name);
            Objects.requireNonNull(tag);
        }

        public CompanyDTO(Company company) {
            this(company.name(), company.tag());
        }
    }

    private static Random random = new Random();

    public boolean serves(City city) {
        return servedCities.contains(city);
    }

    public Handler handler() {
        return (request, response) -> {
            CompanyServer.sleepFor(average, dispersion);
            try {
                Flight flight = request.content().as(Flight.FlightDTO.class).flight();
                if (Flights.flightExists(flight) &&
                    servedCities.contains(flight.from()) &&
                    servedCities.contains(flight.to())) {
                    response.send(price(flight).dto());
                } else if (servedCities.contains(flight.from()) &&
                           servedCities.contains(flight.to())) {
                    var multilegFlights = servedCities.stream()
                            .filter(city -> !city.equals(flight.from()) & !city.equals(flight.to()))
                            .filter(city -> Flights.flightExists(Flight.from(flight.from()).to(city)) &&
                                            Flights.flightExists(Flight.from(city).to(flight.to())))
                            .map(city -> new MultilegFlight(flight.from(), city, flight.to()))
                            .toList();
                    if (multilegFlights.isEmpty()) {
                        response.status(Status.NOT_FOUND_404);
                        response.send(new ErrorMessage(
                                name + " does not serve " + flight.from().name() + " to " + flight.to().name()));
                    } else {
                        var bestFlight = multilegFlights.stream()
                                .map(this::price)
                                .min(Comparator.comparingInt(PricedMultilegFlight::price))
                                .orElseThrow();
                        response.send(bestFlight.dto());
                    }
                } else {
                    response.status(Status.NOT_FOUND_404);
                    response.send(new ErrorMessage(
                            name + " does not serve " + flight.from().name() + " to " + flight.to().name()));
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                response.status(Status.NOT_FOUND_404);
                response.send(new ErrorMessage(e.getMessage()));
            }
        };
    }

    private PricedFlight price(Flight flight) {
        int price = (Flights.priceForFlight(flight) *
                     (pricingStrategy + random.nextInt(-10, +10))) / 100;
        return new PricedFlight(flight, price);
    }

    private PricedMultilegFlight price(MultilegFlight flight) {
        int price = (Flights.priceForFlight(flight.firstLeg()) + Flights.priceForFlight(flight.secondLeg())) *
                    (pricingStrategy + random.nextInt(-10, +10)) / 125;
        return new PricedMultilegFlight(flight, price);
    }

    public static Company of(String name, int pricingStrategy, String tag, int average, int dispersion, City... servedCities) {
        return new Company(name, pricingStrategy,
                Arrays.stream(servedCities).collect(Collectors.toSet()),
                tag, average, dispersion);
    }

}

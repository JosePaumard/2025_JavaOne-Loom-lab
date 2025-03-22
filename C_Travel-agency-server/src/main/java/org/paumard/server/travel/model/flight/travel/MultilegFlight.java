package org.paumard.server.travel.model.flight.travel;

import org.paumard.server.travel.model.city.Cities;

import java.util.Objects;

public record MultilegFlight(Flight flight1, Flight flight2) implements Travel {

    public MultilegFlight {
        Objects.requireNonNull(flight1);
        Objects.requireNonNull(flight2);
        if (!flight1.to().equals(flight2.from())) {
            throw new IllegalArgumentException("Flights need to connect");
        }
    }

    public MultilegFlight(String from, String via, String to) {
        var fromCity = Cities.byName(from);
        var viaCity = Cities.byName(via);
        var toCity = Cities.byName(to);
        this(new Flight(fromCity, viaCity), new Flight(viaCity, toCity));
    }
}

package org.paumard.server.travel.model.flight.priced;

import org.paumard.server.travel.model.flight.travel.Flight;

import java.util.Objects;

public record PricedFlight(Flight flight, int price) implements PricedTravel {

    public PricedFlight {
        Objects.requireNonNull(flight);
    }
}

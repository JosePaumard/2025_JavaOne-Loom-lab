package org.paumard.server.company.model.flight;

import java.util.Objects;

public record PricedFlight(Flight flight, int price) {

    public PricedFlight {
        Objects.requireNonNull(flight);
    }

    public record PricedFlightDTO(Flight.FlightDTO flight, int price) {
        public PricedFlightDTO {
            Objects.requireNonNull(flight);
        }
    }

    public PricedFlightDTO dto() {
        return new PricedFlightDTO(flight.dto(), price);
    }
}
package org.paumard.server.company.model.flight;

import java.util.Objects;

public record PricedMultilegFlight(MultilegFlight multilegFlight, int price) {

    public PricedMultilegFlight {
        Objects.requireNonNull(multilegFlight);
    }

    public record PricedMultilegFlightDTO(MultilegFlight.MultilegFlightDTO multilegFlight, int price) {
        public PricedMultilegFlightDTO {
            Objects.requireNonNull(multilegFlight);
        }
    }

    public PricedMultilegFlightDTO dto() {
        return new PricedMultilegFlightDTO(multilegFlight.dto(), price);
    }
}

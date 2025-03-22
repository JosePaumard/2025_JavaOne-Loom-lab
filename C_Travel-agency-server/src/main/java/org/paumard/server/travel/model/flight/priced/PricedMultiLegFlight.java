package org.paumard.server.travel.model.flight.priced;

import org.paumard.server.travel.model.flight.travel.MultilegFlight;

import java.util.Objects;

public record PricedMultiLegFlight(MultilegFlight multiLegFlight, int price) implements PricedTravel {

    public PricedMultiLegFlight {
        Objects.requireNonNull(multiLegFlight);
    }

    public static PricedMultiLegFlight of(PricedFlight pricedFlight1, PricedFlight pricedFlight2) {
        var multiLegFlight = new MultilegFlight(pricedFlight1.flight(), pricedFlight2.flight());
        return new PricedMultiLegFlight(multiLegFlight, pricedFlight1.price() + pricedFlight2.price());
    }
}

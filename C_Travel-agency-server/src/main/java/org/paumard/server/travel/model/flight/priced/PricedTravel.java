package org.paumard.server.travel.model.flight.priced;

public sealed interface PricedTravel permits PricedFlight, PricedMultiLegFlight {
}

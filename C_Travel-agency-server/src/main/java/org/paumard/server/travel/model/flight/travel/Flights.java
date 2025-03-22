package org.paumard.server.travel.model.flight.travel;

import org.paumard.server.travel.model.flight.priced.PricedFlight;
import org.paumard.server.travel.model.flight.priced.PricedMultiLegFlight;
import org.paumard.server.travel.model.flight.priced.PricedTravel;

public class Flights {

    public static Travel flightFor(PricedTravel pricedTravel) {
        return switch(pricedTravel) {
            case PricedFlight(Flight flight, _) -> flight;
            case PricedMultiLegFlight(MultilegFlight flight, _) -> flight;
        };
    }

    public static int priceFor(PricedTravel pricedTravel) {
        return switch(pricedTravel) {
            case PricedFlight(_, int price) -> price;
            case PricedMultiLegFlight(_, int price) -> price;
        };
    }
}

package org.paumard.server.travel.model.dto;

import org.paumard.server.travel.model.company.Company;
import org.paumard.server.travel.model.flight.priced.PricedTravel;
import org.paumard.server.travel.model.flight.travel.Flights;
import org.paumard.server.travel.model.flight.travel.Travel;

public record CompanyPricedTravelDTO(Company company, Travel travel, int price) {

    public CompanyPricedTravelDTO(Company company, PricedTravel pricedFlight) {
        this(company, Flights.flightFor(pricedFlight), Flights.priceFor(pricedFlight));
    }
}

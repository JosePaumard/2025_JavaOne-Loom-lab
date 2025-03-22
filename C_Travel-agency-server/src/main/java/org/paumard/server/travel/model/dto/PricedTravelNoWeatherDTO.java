package org.paumard.server.travel.model.dto;

import org.paumard.server.travel.model.weather.exception.WeatherErrorMessage;

import java.util.Objects;

public record PricedTravelNoWeatherDTO(CompanyPricedTravelDTO companyPricedTravel, WeatherErrorMessage error) {

    public PricedTravelNoWeatherDTO {
        Objects.requireNonNull(companyPricedTravel);
        Objects.requireNonNull(error);
    }
}

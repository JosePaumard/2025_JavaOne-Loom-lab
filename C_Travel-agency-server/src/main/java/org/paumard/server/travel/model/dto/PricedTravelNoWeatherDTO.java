package org.paumard.server.travel.model.dto;

import org.paumard.server.travel.model.weather.exception.WeatherErrorMessage;
import org.paumard.server.travel.response.CompanyResponse;

import java.util.Objects;

public record PricedTravelNoWeatherDTO(CompanyResponse.Priced companyPricedTravel, WeatherErrorMessage error) {

    public PricedTravelNoWeatherDTO {
        Objects.requireNonNull(companyPricedTravel);
        Objects.requireNonNull(error);
    }
}

package org.paumard.server.travel.model.dto;

import org.paumard.server.travel.model.weather.Weather;

import java.util.Objects;

public record PricedTravelWithWeatherDTO(CompanyPricedTravelDTO travel, Weather weather) {

    public PricedTravelWithWeatherDTO {
        Objects.requireNonNull(travel);
        Objects.requireNonNull(weather);
    }
}

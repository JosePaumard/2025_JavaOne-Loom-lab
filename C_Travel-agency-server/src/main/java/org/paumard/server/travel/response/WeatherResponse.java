package org.paumard.server.travel.response;

import org.paumard.server.travel.model.weather.WeatherAgency;

import java.util.Objects;

public sealed interface WeatherResponse
        extends CompanyWeatherResponse {

    record Weather(WeatherAgency agency, String weather)
            implements WeatherResponse {
        public Weather {
            Objects.requireNonNull(agency);
            Objects.requireNonNull(weather);
        }
    }

    record NoWeather(WeatherAgency agency)
            implements WeatherResponse {
        public NoWeather {
            Objects.requireNonNull(agency);
        }
    }

    record WeatherTimeout(String message)
            implements WeatherResponse {
        public WeatherTimeout {
            Objects.requireNonNull(message);
        }
    }
}
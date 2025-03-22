package org.paumard.server.travel.response;

import org.paumard.server.travel.model.company.Company;
import org.paumard.server.travel.model.flight.Flight;
import org.paumard.server.travel.model.weather.Weather;

import java.util.Objects;

public sealed interface TravelResponse {

    record SimpleFlightWeather(Company company, Flight.SimpleFlight simpleFlight, int price, Weather weather)
            implements TravelResponse {
        public SimpleFlightWeather {
            Objects.requireNonNull(company);
            Objects.requireNonNull(simpleFlight);
            Objects.requireNonNull(weather);
        }
    }

    record MultilegFlightWeather(Company company, Flight.MultilegFlight multilegFlight, int price, Weather weather)
            implements TravelResponse {
        public MultilegFlightWeather {
            Objects.requireNonNull(company);
            Objects.requireNonNull(multilegFlight);
            Objects.requireNonNull(weather);
        }
    }

    record SimpleFlightNoWeather(Company company, Flight.SimpleFlight simpleFlight, int price)
            implements TravelResponse {
        public SimpleFlightNoWeather {
            Objects.requireNonNull(company);
            Objects.requireNonNull(simpleFlight);
        }
    }

    record MultilegFlightNoWeather(Company company, Flight.MultilegFlight multilegFlight, int price)
            implements TravelResponse {
        public MultilegFlightNoWeather {
            Objects.requireNonNull(company);
            Objects.requireNonNull(multilegFlight);
        }
    }

    record NoFlight(String message)
            implements TravelResponse {
        public NoFlight {
            Objects.requireNonNull(message);
        }
    }
}
package org.paumard.server.weather.model;

import io.helidon.webserver.http.Handler;
import org.paumard.server.weather.WeatherServer;

import java.util.Objects;
import java.util.regex.Pattern;

public record WeatherAgency(String name, String tag, int average, int dispersion) {


    public record WeatherAgencyDTO(String name, String tag) {
        public WeatherAgencyDTO {
            Objects.requireNonNull(name);
            Objects.requireNonNull(tag);
        }

        public WeatherAgencyDTO(WeatherAgency weatherAgency) {
            this(weatherAgency.name(), weatherAgency.tag());
        }
    }

    public WeatherAgency {
        Objects.requireNonNull(name);
        Objects.requireNonNull(tag);
    }

    public Handler handler() {
        return (request, response) -> {
            var cityDTO = request.content().as(City.CityDTO.class);
            // var city = Cities.cityByName(cityDTO.name());
            WeatherServer.sleepFor(average, dispersion);
            response.send(Weather.randomFor(name));
        };
    }

    public static WeatherAgency of(String line) {
        var elements = Pattern.compile(",").splitAsStream(line).toArray(String[]::new);
        var name = elements[0].trim();
        var tag =  elements[1].trim();
        int average = Integer.parseInt(elements[2].trim());
        int dispersion = Integer.parseInt(elements[3].trim());
        return new WeatherAgency(name, tag, average, dispersion);
    }
}

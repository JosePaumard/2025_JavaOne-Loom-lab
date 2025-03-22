package org.paumard.server.weather.model;

import io.helidon.webserver.http.HttpRouting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WeatherAgencies {

    public static List<WeatherAgency> weatherAgencies;

    public static List<WeatherAgency> readAgenciesFrom(String fileName) {
        Path agencies = Path.of("files", fileName);
        try (var lines = Files.lines(agencies)) {
            weatherAgencies = lines.filter(line -> !line.startsWith("#"))
                    .map(WeatherAgency::of)
                    .toList();
            return weatherAgencies;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerWeatherAgencies(HttpRouting.Builder routingBuilder) {
        routingBuilder.get("/weather-agencies", (_, response) -> {
            response.send(weatherAgencies.stream()
                    .map(WeatherAgency.WeatherAgencyDTO::new)
                    .toArray(WeatherAgency.WeatherAgencyDTO[]::new));
        });
    }

    public static void registerWeatherHandlers(HttpRouting.Builder routingBuilder) {
        weatherAgencies.forEach(
                agency -> routingBuilder.post("/weather/" + agency.tag(), agency.handler()));
    }
}

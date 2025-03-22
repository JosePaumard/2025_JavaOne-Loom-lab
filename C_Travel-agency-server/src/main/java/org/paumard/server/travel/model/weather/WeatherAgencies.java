package org.paumard.server.travel.model.weather;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WeatherAgencies {

    private static List<WeatherAgency> weatherAgencies;

    public static List<WeatherAgency> readWeatherAgenciesFrom(String fileName) {
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

    public static List<WeatherAgency> weatherAgencies() {
        return weatherAgencies;
    }
}

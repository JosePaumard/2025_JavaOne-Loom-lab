package org.paumard.server.travel.model.city;

import io.helidon.webserver.http.HttpRouting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Cities {
    public static Map<String, City> cityByName;

    public static List<City> readCitiesFrom(String fileName) {
        try (var lines = Files.lines(Path.of("files", fileName));) {

            var cities = lines
                    .filter(line -> !line.startsWith("#"))
                    .filter(line -> !line.isEmpty())
                    .map(Cities::ofCity).toList();
            cityByName = cities.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(City::name, Function.identity()),
                            Collections::unmodifiableMap)
                    );
            return cities;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static City ofCity(String line) {
        line = line.trim();
        var indexOfFirstSpace = line.indexOf(' ');
        var name = line.substring(indexOfFirstSpace + 1);
        return new City(name);
    }

    public static Set<City> cities() {
        return new HashSet<>(cityByName.values());
    }

    public static City byName(String cityName) {
        return cityByName.get(cityName);
    }

    public static void registerCities(HttpRouting.Builder routingBuilder) {
        routingBuilder.get("/cities", (_, response) -> {
            response.send(cities().stream().map(City.CityDTO::of).sorted().toArray(City.CityDTO[]::new));
        });
    }
}
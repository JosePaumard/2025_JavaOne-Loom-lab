package org.paumard.server.weather.model;

import io.helidon.webserver.http.HttpRouting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Cities {
    public static Map<Integer, City> cityById;
    public static Map<String, City> cityByName;

    public static List<City> readCitiesFrom(String fileName) {
        try (var lines = Files.lines(Path.of("files", fileName));) {

            var cities = lines
                    .filter(line -> !line.startsWith("#"))
                    .filter(line -> !line.isEmpty())
                    .map(Cities::ofCity).toList();
            cityById = cities.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(City::id, Function.identity()),
                            Collections::unmodifiableMap)
                    );
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
        var id = Integer.parseInt(line.substring(0, indexOfFirstSpace));
        var name = line.substring(indexOfFirstSpace + 1);
        return new City(id, name);
    }

    public static City cityByName(String name) {
        return cityByName.get(name);
    }

    public static Set<City> cities() {
        return new HashSet<>(cityByName.values());
    }

    public static void registerCities(HttpRouting.Builder routingBuilder) {
        routingBuilder.get("/cities", (_, response) -> {
            response.send(cities().stream().map(City.CityDTO::of).sorted().toArray(City.CityDTO[]::new));
        });
    }
}
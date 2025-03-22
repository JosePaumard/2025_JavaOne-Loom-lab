package org.paumard.server.company.model.flight;

import org.paumard.server.company.model.city.Cities;
import org.paumard.server.company.model.city.City;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public class Flights {
    public static Set<Flight> directFlights = new HashSet<>();
    public static Map<Flight, Integer> pricePerFlight = new HashMap<>();

    public static Set<Flight> readFlightsFrom(String fileName) {
        try (var lines = Files.lines(Path.of("files", fileName));) {
            Set<Flight> directFlights = new HashSet<>();
            Map<Flight, Integer> pricePerFlight = new HashMap<>();
            var table = lines.skip(2).toList();
            int fromId = 1;
            for (var line : table) {
                var elements = line.trim().split(" ");
                var prices = Arrays.stream(elements)
                        .skip(1)
                        .filter(Predicate.not(String::isBlank))
                        .toList();
                int toId = 1;
                while (toId < prices.size()) {
                    if (!prices.get(toId - 1).equals("-")) {
                        int price = Integer.parseInt(prices.get(toId - 1));
                        City from = Cities.cityById.get(fromId);
                        City to = Cities.cityById.get(toId);
                        Flight directFlight = Flight.from(from).to(to);
                        directFlights.add(directFlight);
                        pricePerFlight.put(directFlight, price);
                        Flight reversedDirectFlight = Flight.from(to).to(from);
                        directFlights.add(reversedDirectFlight);
                        pricePerFlight.put(reversedDirectFlight, price);
                    }
                    toId++;
                }
                fromId++;
            }
            Flights.directFlights = Collections.unmodifiableSet(directFlights);
            Flights.pricePerFlight = Collections.unmodifiableMap(pricePerFlight);
            return directFlights;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean flightExists(Flight directFlight) {
        return directFlights.contains(directFlight);
    }

    public static int priceForFlight(Flight directFlight) {
        return pricePerFlight.get(directFlight);
    }
}

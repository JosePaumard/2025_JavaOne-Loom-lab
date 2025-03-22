package org.paumard.server.travel.model.flight.travel;

import org.paumard.server.travel.model.city.Cities;
import org.paumard.server.travel.model.city.City;

import java.util.Objects;
import java.util.function.Supplier;

public record Flight(City from, City to) implements Travel {

    public Flight {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (from.equals(to)) {
            throw new IllegalArgumentException("To and from should be different");
        }
    }

    public Flight(String from, String to) {
        var fromCity = Cities.byName(from);
        var toCity = Cities.byName(to);
        this(fromCity, toCity);
    }

    public static CitySupplier from(City from) {
        return () -> from;
    }

    public interface CitySupplier extends Supplier<City> {

        default City from() {
            return get();
        }

        default Flight to(City to) {
            return new Flight(from(), to);
        }
    }

    public String toString() {
        return "Flight from " + from.name() + " to " + to.name();
    }
}

package org.paumard.server.company.model.flight;

import org.paumard.server.company.model.city.City;

import java.util.Objects;
import java.util.function.Supplier;

public record Flight(City from, City to) {

    public Flight {
        Objects.requireNonNull(from, "The departure city is not defined");
        Objects.requireNonNull(to, "The destination city is not defined");
        if (to.equals(from)) {
            throw new IllegalArgumentException("To and from are the same city");
        }
    }

    public record FlightDTO(City.CityDTO from, City.CityDTO to) {
        public Flight flight() {
            return new Flight(from.city(), to.city());
        }
    }

    public static CitySupplier from(City from) {
        return () -> from;
    }

    public static CitySupplier from(String fromCity) {
        return from(City.byName(fromCity));
    }

    public FlightDTO dto() {
        return new FlightDTO(City.CityDTO.of(from), City.CityDTO.of(to));
    }

    public interface CitySupplier extends Supplier<City> {
        default City from() {
            return get();
        }

        default Flight to(City to) {
            return new Flight(from(), to);
        }

        default Flight to(String toCity) {
            return new Flight(from(), City.byName(toCity));
        }
    }

    public String toString() {
        return "Flight from " + from.name() + " to " + to.name();
    }
}

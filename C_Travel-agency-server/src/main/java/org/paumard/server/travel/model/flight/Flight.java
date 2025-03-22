package org.paumard.server.travel.model.flight;

import org.paumard.server.travel.model.city.Cities;
import org.paumard.server.travel.model.city.City;

import java.util.Objects;
import java.util.function.Supplier;

public sealed interface Flight {

    record SimpleFlight(City from, City to)
            implements Flight {

        public SimpleFlight {
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);
            if (from.equals(to)) {
                throw new IllegalArgumentException("To and from should be different");
            }
        }

        public SimpleFlight(String from, String to) {
            var fromCity = Cities.byName(from);
            var toCity = Cities.byName(to);
            this(fromCity, toCity);
        }

        public static SimpleFlight.CitySupplier from(City from) {
            return () -> from;
        }

        public interface CitySupplier extends Supplier<City> {

            default City from() {
                return get();
            }

            default SimpleFlight to(City to) {
                return new SimpleFlight(from(), to);
            }
        }

        public String toString() {
            return "Flight from " + from.name() + " to " + to.name();
        }
    }

    record MultilegFlight(SimpleFlight flight1, SimpleFlight flight2)
            implements Flight {

        public MultilegFlight {
            Objects.requireNonNull(flight1);
            Objects.requireNonNull(flight2);
            if (!flight1.to().equals(flight2.from())) {
                throw new IllegalArgumentException("Flights need to connect");
            }
        }

        public MultilegFlight(String from, String via, String to) {
            var fromCity = Cities.byName(from);
            var viaCity = Cities.byName(via);
            var toCity = Cities.byName(to);
            this(new SimpleFlight(fromCity, viaCity), new SimpleFlight(viaCity, toCity));
        }
    }
}

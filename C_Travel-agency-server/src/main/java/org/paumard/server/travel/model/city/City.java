package org.paumard.server.travel.model.city;

import java.util.Objects;

public record City(String name) {

    public City {
        Objects.requireNonNull(name);
    }

    public record CityDTO(String name) implements Comparable<CityDTO> {

        public CityDTO {
            Objects.requireNonNull(name);
        }

        public static CityDTO of(City city) {
            return new CityDTO(city.name());
        }

        @Override
        public int compareTo(CityDTO other) {
            return this.name.compareTo(other.name);
        }
    }
}
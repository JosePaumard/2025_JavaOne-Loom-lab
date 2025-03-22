package org.paumard.server.weather.model;

import java.util.Objects;

public record City(int id, String name) {

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
package org.paumard.server.company.model.city;

import java.util.Objects;

public record City(int id, String name) {

    public City {
        Objects.requireNonNull(name);
    }

    public static City byName(String name) {
        return Cities.getCityByName(name);
    }

    public CityDTO dto() {
        return new CityDTO(name);
    }

    public record CityDTO(String name) implements Comparable<CityDTO> {

        public CityDTO {
            Objects.requireNonNull(name);
        }

        public static CityDTO of(City city) {
            return new CityDTO(city.name());
        }

        public City city() {
            return Cities.getCityByName(name);
        }

        @Override
        public int compareTo(CityDTO other) {
            return this.name.compareTo(other.name);
        }
    }
}
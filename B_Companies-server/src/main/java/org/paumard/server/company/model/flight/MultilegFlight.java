package org.paumard.server.company.model.flight;

import org.paumard.server.company.model.city.City;

import java.util.Objects;

public record MultilegFlight(City from, City via, City to) {

    public MultilegFlight {
        Objects.requireNonNull(from);
        Objects.requireNonNull(via);
        Objects.requireNonNull(to);
        if (from.equals(via)) {
            throw new IllegalArgumentException("From and Via are the same city");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("From and To are the same city");
        }
        if (via.equals(to)) {
            throw new IllegalArgumentException("Via and To are the same city");
        }
    }

    public Flight firstLeg() {
        return Flight.from(from).to(via);
    }

    public Flight secondLeg() {
        return Flight.from(via).to(to);
    }

    public MultilegFlightDTO dto() {
        return new MultilegFlightDTO(from.dto(), via.dto(), to.dto());
    }

    public record MultilegFlightDTO(City.CityDTO from, City.CityDTO via, City.CityDTO to) {}
}

package org.paumard.server.travel.model.company;

import java.util.Objects;

public record Company(String name, String tag) {

    public Company {
        Objects.requireNonNull(name);
        Objects.requireNonNull(tag);
    }

    public record CompanyDTO(String name, String tag) implements Comparable<CompanyDTO> {

        public CompanyDTO {
            Objects.requireNonNull(name);
            Objects.requireNonNull(tag);
        }

        public static CompanyDTO of(Company company) {
            return new CompanyDTO(company.name(), company.tag());
        }

        @Override
        public int compareTo(CompanyDTO other) {
            return this.name().compareTo(other.name());
        }
    }
}

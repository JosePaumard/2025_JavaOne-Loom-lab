package org.paumard.server.travel.model.company;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class Companies {
    private static List<Company> companies;

    public static List<Company> readCompaniesFrom(String fileName) {
        try (var lines = Files.lines(Path.of("files", fileName));) {

            companies = lines
                    .filter(line -> !line.startsWith("#"))
                    .filter(line -> !line.isEmpty())
                    .map(Companies::ofCompany)
                    .toList();
            return companies;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Company ofCompany(String line) {
        Pattern pattern = Pattern.compile("""
                (?<name>[ a-zA-Z]+) \
                (?<pricingStrategy>\\d+) \
                (?<flightAvailabilityRate>\\d+) \
                (?<tag>[a-z\\-]+) \
                (?<average>\\d+) \
                (?<dispersion>\\d+)$""");

        var matcher = pattern.matcher(line);
        if (matcher.matches()) {
            var name = matcher.group("name");
            var tag = matcher.group("tag");
            var company = new Company(name, tag);
            return company;
        }
        throw new AssertionError("Line [" + line + "] does not match");
    }

    public static List<Company> companies() {
        return companies;
    }
}

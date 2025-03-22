package org.paumard.server.company.model.company;

import io.helidon.webserver.http.HttpRouting;
import org.paumard.server.company.model.CompanyServer;
import org.paumard.server.company.model.city.Cities;
import org.paumard.server.company.model.city.City;

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

    public static void registerCompanies(HttpRouting.Builder routing) {
        routing.get("/companies", (_, response) -> {
            response.send(companies.stream().map(Company.CompanyDTO::new).toArray(Company.CompanyDTO[]::new));
        });
    }

    public static void registerCompanyHandlers(HttpRouting.Builder routing) {
        companies.forEach(
                company -> routing.post("/company/" + company.tag(), company.handler()));
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
            int pricingStrategy = Integer.parseInt(matcher.group("pricingStrategy"));
            int flightAvailabilityRate = Integer.parseInt(matcher.group("flightAvailabilityRate"));
            var tag = matcher.group("tag");
            int average = Integer.parseInt(matcher.group("average"));
            int dispersion = Integer.parseInt(matcher.group("dispersion"));
            var servedCities = Cities.cities().stream()
                    .filter(_ -> CompanyServer.random.nextInt(0, 100) <= flightAvailabilityRate)
                    .toArray(City[]::new);
            var company = Company.of(name, pricingStrategy, tag, average, dispersion, servedCities);
            return company;
        }
        throw new AssertionError("Line [" + line + "] does not match");
    }
}

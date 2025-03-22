package org.paumard.server.company.model;

import io.helidon.common.config.Config;
import io.helidon.http.media.MediaContext;
import io.helidon.http.media.jsonb.JsonbSupport;
import io.helidon.http.media.jsonp.JsonpSupport;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentFeature;
import org.paumard.server.company.model.city.Cities;
import org.paumard.server.company.model.company.Companies;
import org.paumard.server.company.model.flight.Flights;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Random;

public class CompanyServer {

    public static Random random = new Random(314L);

    void main() throws UnknownHostException {

        Cities.readCitiesFrom("us-cities.txt");
        Companies.readCompaniesFrom("companies.txt");
        Flights.readFlightsFrom("flights-and-prices.txt");

        var properties = readPropertiesFrom("server.properties");
        var COMPANIES_SERVER_HOST = properties.getProperty("companies.host");
        var COMPANIES_SERVER_PORT = Integer.parseInt(properties.getProperty("companies.port"));

        var routingBuilder = HttpRouting.builder();

        routingBuilder.get("/whoami", (_, res) -> {
            res.send("Current thread: " + Thread.currentThread());
        });

        Cities.registerCities(routingBuilder);

        Companies.registerCompanies(routingBuilder);
        Companies.registerCompanyHandlers(routingBuilder);

        WebServer webServer = WebServer.builder()
                .address(InetAddress.getLocalHost())
                .port(COMPANIES_SERVER_PORT).host(COMPANIES_SERVER_HOST)
                .addFeature(
                        StaticContentFeature.builder()
                                .addClasspath(b -> b.location("/static-content").welcome("index.html").context("/"))
                                .build())
                .routing(routingBuilder)
                .mediaContext(MediaContext.builder()
                        .mediaSupportsDiscoverServices(false)
                        .addMediaSupport(JsonpSupport.create())
                        .addMediaSupport(JsonbSupport.create(Config.empty()))
                        .build())
                .build();

        webServer.start();

        while (true) {
        }
    }

    private static Properties readPropertiesFrom(String fileName) {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(fileName))) {
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            System.out.println("Error reading properties file " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void sleepFor(int average, int dispersion) {
        try {
            Thread.sleep(random.nextInt(average - dispersion, average + dispersion));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
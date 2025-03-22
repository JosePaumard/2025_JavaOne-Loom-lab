package org.paumard.server.travel;

import io.helidon.common.config.Config;
import io.helidon.common.uri.UriInfo;
import io.helidon.http.Status;
import io.helidon.http.media.MediaContext;
import io.helidon.http.media.jsonb.JsonbSupport;
import io.helidon.http.media.jsonp.JsonpSupport;
import io.helidon.webclient.api.ClientUri;
import io.helidon.webclient.api.WebClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.staticcontent.StaticContentFeature;
import org.paumard.server.travel.model.city.Cities;
import org.paumard.server.travel.model.city.City;
import org.paumard.server.travel.model.company.Companies;
import org.paumard.server.travel.model.company.Company;
import org.paumard.server.travel.model.company.exception.CompanyErrorMessage;
import org.paumard.server.travel.model.dto.PricedTravelNoWeatherDTO;
import org.paumard.server.travel.model.dto.PricedTravelWithWeatherDTO;
import org.paumard.server.travel.model.flight.Flight;
import org.paumard.server.travel.model.weather.Weather;
import org.paumard.server.travel.model.weather.WeatherAgencies;
import org.paumard.server.travel.model.weather.WeatherAgency;
import org.paumard.server.travel.model.weather.exception.WeatherErrorMessage;
import org.paumard.server.travel.response.CompanyResponse;
import org.paumard.server.travel.response.CompanyServerResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

public class TravelAgencyServer {

    public record TravelRequest(String from, String to) {}
    public record QueryFlight(City from, City to) {}

    private static QueryFlight queriedFlightFrom(ServerRequest request) {
        var travelRequest = request.content().as(TravelRequest.class);
        var cityFrom = Cities.byName(travelRequest.from());
        var destinationCity = Cities.byName(travelRequest.to());
        var flight = new QueryFlight(cityFrom, destinationCity);
        return flight;
    }

    static Callable<CompanyResponse>
    companyQuery(ClientUri clientURI, Company company, QueryFlight flight) {
        return () -> {
            try (var response = WebClient.builder()
                    .baseUri(clientURI).build()
                    .post("/company/" + company.tag())
                    .submit(flight)) {

                var companyServerResponse = CompanyServerResponse.of(response);
                return switch (companyServerResponse) {
                    case CompanyServerResponse.SimpleFlight(
                            Flight.SimpleFlight simpleFlight, int price) ->
                            new CompanyResponse.PricedSimpleFlight(
                                    company, simpleFlight, price);
                    case CompanyServerResponse.MultilegFlight(
                            Flight.MultilegFlight multiLegFlight, int price) ->
                            new CompanyResponse.PricedMultilegFlight(
                                    company, multiLegFlight, price);
                    case CompanyServerResponse.NoFlight(String message) ->
                            new CompanyResponse.NoFlight(company, message);
                    case CompanyServerResponse.Error(String message) ->
                            new CompanyResponse.Error(company, message);
                };
            }
        };
    }

    void main() throws UnknownHostException {

        Cities.readCitiesFrom("us-cities.txt");
        Companies.readCompaniesFrom("companies.txt");
        WeatherAgencies.readWeatherAgenciesFrom("weather-agencies.txt");

        var properties = readPropertiesFrom("server.properties");
        var WEATHER_SERVER_URI = createWeatherServerURI(properties);
        var COMPANY_SERVER_URI = createCompanyServerURI(properties);

        var TRAVEL_AGENCY_HOST = properties.getProperty("travel-agency.host");
        var TRAVEL_AGENCY_PORT = Integer.parseInt(properties.getProperty("travel-agency.port"));

        var routingBuilder = HttpRouting.builder();

        routingBuilder.get("/whoami", (req, res) -> {
            res.send("Current thread: " + Thread.currentThread());
        });

        Cities.registerCities(routingBuilder);

        // FIXME: Start of the spaghetti code
        routingBuilder.post("/travel", (req, res) -> {
            // FIXME: Extracting the requested flight
            var queryFlight = queriedFlightFrom(req);
            var destinationCity = queryFlight.to();
            // FIXME: Company for loop
            try (var companyScope = StructuredTaskScope.<CompanyResponse>open()) {
                record CompanyTask(Company company, StructuredTaskScope.Subtask<CompanyResponse> task) {}

                var companySubtasks = Companies.companies()
                        .stream()
                        .map(company -> new CompanyTask(
                                company,
                                companyScope.fork(companyQuery(COMPANY_SERVER_URI, company, queryFlight))))
                        .toList();

                companyScope.join();

                var map = companySubtasks.stream()
                        .collect(
                                Collectors.partitioningBy(
                                        e -> e.task().state() == StructuredTaskScope.Subtask.State.SUCCESS &&
                                             e.task().get() instanceof CompanyResponse.Priced
                                )
                        );

                var companyPricedTravels =
                        map.get(true).stream()
                                .map(CompanyTask::task)
                                .map(StructuredTaskScope.Subtask::get)
                                .map(CompanyResponse.Priced.class::cast)
                                .toList();

                var errorCompanies =
                        map.get(false).stream()
                                .map(CompanyTask::company)
                                .toList();
                // FIXME: best flight
                var bestFlightOpt = companyPricedTravels.stream()
                        .min(Comparator.comparingInt(CompanyResponse.Priced::price));
                if (bestFlightOpt.isPresent()) {
                    var bestFlight = bestFlightOpt.orElseThrow();
                    var weathers = new ArrayList<Weather>();
                    var errorWeatherAgencies = new ArrayList<WeatherAgency>();
                    // FIXME: weather agencies for loop
                    for (var weatherAgency : WeatherAgencies.weatherAgencies()) {
                        try (var response = WebClient.builder()
                                .baseUri(WEATHER_SERVER_URI).build()
                                .post("/weather/" + weatherAgency.tag())
                                .submit(destinationCity);) {

                            if (response.status() == Status.OK_200) {
                                var weather = response.as(Weather.class);
                                weathers.add(weather);
                            } else {
                                errorWeatherAgencies.add(weatherAgency);
                            }
                        }
                    }
                    if (!weathers.isEmpty()) {
                        var weather = weathers.getFirst();
                        var pricedTravelWithWeather = new PricedTravelWithWeatherDTO(bestFlight, weather);
                        res.status(Status.OK_200).send(pricedTravelWithWeather);
                    } else {
                        var errorMessage = new WeatherErrorMessage("Weather not available");
                        var pricedTravelNoWeather = new PricedTravelNoWeatherDTO(bestFlight, errorMessage);
                        res.status(Status.NOT_FOUND_404).send(pricedTravelNoWeather);
                    }
                } else {
                    var errorMessage = new CompanyErrorMessage("No flight available", errorCompanies.toArray(Company[]::new));
                    res.status(Status.NOT_FOUND_404).send(errorMessage);
                }
            }
            // FIXME: Scope end
        });
        // FIXME: Handler end

        WebServer webServer = WebServer.builder()
                .address(InetAddress.getLocalHost())
                .host(TRAVEL_AGENCY_HOST).port(TRAVEL_AGENCY_PORT)
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

    private static ClientUri createCompanyServerURI(Properties properties) {
        var COMPANY_SERVER_HOST = properties.getProperty("companies.host");
        var COMPANY_SERVER_PORT = Integer.parseInt(properties.getProperty("companies.port"));
        var companyServerUriInfo = UriInfo.builder()
                .host(COMPANY_SERVER_HOST)
                .port(COMPANY_SERVER_PORT)
                .build();
        var COMPANY_SERVER_URI = ClientUri.create(companyServerUriInfo);
        return COMPANY_SERVER_URI;
    }

    private static ClientUri createWeatherServerURI(Properties properties) {
        var WEATHER_SERVER_HOST = properties.getProperty("weather-agencies.host");
        var WEATHER_SERVER_PORT = Integer.parseInt(properties.getProperty("weather-agencies.port"));
        var weatherServerUriInfo = UriInfo.builder()
                .host(WEATHER_SERVER_HOST)
                .port(WEATHER_SERVER_PORT)
                .build();
        var WEATHER_SERVER_URI = ClientUri.create(weatherServerUriInfo);
        return WEATHER_SERVER_URI;
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
}

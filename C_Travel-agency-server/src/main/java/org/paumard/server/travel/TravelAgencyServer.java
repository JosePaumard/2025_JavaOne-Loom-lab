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
import io.helidon.webserver.staticcontent.StaticContentFeature;
import jakarta.json.Json;
import org.paumard.server.travel.model.city.Cities;
import org.paumard.server.travel.model.company.Companies;
import org.paumard.server.travel.model.company.Company;
import org.paumard.server.travel.model.company.exception.CompanyErrorMessage;
import org.paumard.server.travel.model.dto.CompanyPricedTravelDTO;
import org.paumard.server.travel.model.dto.PricedTravelNoWeatherDTO;
import org.paumard.server.travel.model.dto.PricedTravelWithWeatherDTO;
import org.paumard.server.travel.model.dto.TravelRequestDTO;
import org.paumard.server.travel.model.flight.priced.PricedFlight;
import org.paumard.server.travel.model.flight.priced.PricedMultiLegFlight;
import org.paumard.server.travel.model.flight.travel.Flight;
import org.paumard.server.travel.model.flight.travel.MultilegFlight;
import org.paumard.server.travel.model.weather.Weather;
import org.paumard.server.travel.model.weather.WeatherAgencies;
import org.paumard.server.travel.model.weather.WeatherAgency;
import org.paumard.server.travel.model.weather.exception.WeatherErrorMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;

public class TravelAgencyServer {

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
            var travelRequest = req.content().as(TravelRequestDTO.class);
            var cityFrom = Cities.byName(travelRequest.from());
            var destinationCity = Cities.byName(travelRequest.to());
            var flight = Flight.from(cityFrom).to(destinationCity);
            var companies = Companies.companies();
            var companyPricedTravels = new ArrayList<CompanyPricedTravelDTO>();
            var errorCompanies = new ArrayList<Company>();
            // FIXME: Company for loop
            for (var company : companies) {
                try (var response = WebClient.builder()
                        .baseUri(COMPANY_SERVER_URI).build()
                        .post("/company/" + company.tag())
                        .submit(flight)) {
                    // FIXME: Company Server Response Analysis
                    if (response.status() == Status.OK_200) {
                        var reader = Json.createReader(response.entity().inputStream());
                        var jsonObject = reader.readObject();
                        if (jsonObject.containsKey("multilegFlight")) {
                            var jsonFlight = jsonObject.getJsonObject("multilegFlight");
                            var from = jsonFlight.getJsonObject("from").getString("name");
                            var to = jsonFlight.getJsonObject("to").getString("name");
                            var via = jsonFlight.getJsonObject("via").getString("name");
                            var price = jsonObject.getInt("price");
                            var multilegFlight = new MultilegFlight(from, via, to);
                            var pricedFlight = new PricedMultiLegFlight(multilegFlight, price);
                            var companyPricedFlight = new CompanyPricedTravelDTO(company, pricedFlight);
                            companyPricedTravels.add(companyPricedFlight);
                        } else if (jsonObject.containsKey("flight")) {
                            var jsonFlight = jsonObject.getJsonObject("flight");
                            var from = jsonFlight.getJsonObject("from").getString("name");
                            var to = jsonFlight.getJsonObject("to").getString("name");
                            var price = jsonObject.getInt("price");
                            var simpleFlight = new Flight(from, to);
                            var pricedFlight = new PricedFlight(simpleFlight, price);
                            var companyPricedFlight = new CompanyPricedTravelDTO(company, pricedFlight);
                            companyPricedTravels.add(companyPricedFlight);
                        }
                    } else {
                        var message = response.as(String.class);
                        errorCompanies.add(company);
                    }
                }
            }
            // FIXME: best flight
            var bestFlightOpt = companyPricedTravels.stream()
                    .min(Comparator.comparingInt(CompanyPricedTravelDTO::price));
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
                // FIXME: Weather scope end
            } else {
                var errorMessage = new CompanyErrorMessage("No flight available", errorCompanies.toArray(Company[]::new));
                res.status(Status.NOT_FOUND_404).send(errorMessage);
            }
            // FIXME: Company scope end
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

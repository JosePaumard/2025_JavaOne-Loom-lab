package org.paumard.server.weather;

import io.helidon.common.config.Config;
import io.helidon.http.media.MediaContext;
import io.helidon.http.media.jsonb.JsonbSupport;
import io.helidon.http.media.jsonp.JsonpSupport;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentFeature;
import org.paumard.server.weather.model.Cities;
import org.paumard.server.weather.model.WeatherAgencies;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Random;

public class WeatherServer {

    public static Random random = new Random();

    void main() throws IOException {

        Cities.readCitiesFrom("us-cities.txt");
        WeatherAgencies.readAgenciesFrom("weather-agencies.txt");

        var properties = readPropertiesFrom("server.properties");
        var WEATHER_SERVER_PORT = Integer.parseInt(properties.getProperty("weather-agencies.port"));
        var WEATHER_SERVER_HOST = properties.getProperty("weather-agencies.host");

        var routingBuilder = HttpRouting.builder();

        routingBuilder.get("/whoami", (_, res) -> {
            res.send("Current thread: " + Thread.currentThread());
        });

        Cities.registerCities(routingBuilder);

        WeatherAgencies.registerWeatherAgencies(routingBuilder);
        WeatherAgencies.registerWeatherHandlers(routingBuilder);

        WebServer webServer = WebServer.builder()
                .address(InetAddress.getLocalHost())
                .port(WEATHER_SERVER_PORT).host(WEATHER_SERVER_HOST)
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
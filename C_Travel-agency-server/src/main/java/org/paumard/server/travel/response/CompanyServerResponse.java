package org.paumard.server.travel.response;

import io.helidon.http.Status;
import io.helidon.webclient.api.HttpClientResponse;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.paumard.server.travel.model.flight.Flight;

import java.util.Objects;

public sealed interface CompanyServerResponse {

    record SimpleFlight(Flight.SimpleFlight simpleFlight, int price)
            implements CompanyServerResponse {

        public SimpleFlight {
            Objects.requireNonNull(simpleFlight);
        }

        // Utility constructor
        public SimpleFlight(String from, String to, int price) {
            var simpleFlight = new Flight.SimpleFlight(from, to);
            this(simpleFlight, price);
        }

        public static CompanyServerResponse.SimpleFlight of(JsonObject jsonObject) {
            var jsonFlight = jsonObject.getJsonObject("flight");
            var from = jsonFlight.getJsonObject("from").getString("name");
            var to = jsonFlight.getJsonObject("to").getString("name");
            var price = jsonObject.getInt("price");
            var simpleFlight = new CompanyServerResponse.SimpleFlight(from, to, price);
            return simpleFlight;
        }
    }

    record MultilegFlight(Flight.MultilegFlight multilegFlight, int price)
            implements CompanyServerResponse {

        public MultilegFlight {
            Objects.requireNonNull(multilegFlight);
        }

        public MultilegFlight(String from, String via, String to, int price) {
            var multiLegFlight = new Flight.MultilegFlight(from, via, to);
            this (multiLegFlight, price);
        }

        public static CompanyServerResponse.MultilegFlight of(JsonObject jsonObject) {
            var jsonFlight = jsonObject.getJsonObject("multilegFlight");
            var from = jsonFlight.getJsonObject("from").getString("name");
            var to = jsonFlight.getJsonObject("to").getString("name");
            var via = jsonFlight.getJsonObject("via").getString("name");
            var price = jsonObject.getInt("price");
            var multiLegFlight = new CompanyServerResponse.MultilegFlight(from, via, to, price);
            return multiLegFlight;
        }
    }

    record NoFlight(String message)
            implements CompanyServerResponse {

        public NoFlight {
            Objects.requireNonNull(message);
        }
    }

    record Error(String message)
            implements CompanyServerResponse {

        public Error {
            Objects.requireNonNull(message);
        }
    }

    static CompanyServerResponse of(HttpClientResponse response) {
        if (response.status() == Status.OK_200) {
            var reader = Json.createReader(response.entity().inputStream());
            var jsonObject = reader.readObject();
            if (jsonObject.containsKey("multilegFlight")) {
                return MultilegFlight.of(jsonObject);
            } else if (jsonObject.containsKey("flight")) {
                return CompanyServerResponse.SimpleFlight.of(jsonObject);
            } else {
                return new NoFlight("No Flight available");
            }
        } else {
            var message = response.as(String.class);
            return new Error(message);
        }
    }
}

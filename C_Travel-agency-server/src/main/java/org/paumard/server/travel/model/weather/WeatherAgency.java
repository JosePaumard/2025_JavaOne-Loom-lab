package org.paumard.server.travel.model.weather;

import java.util.regex.Pattern;

public record WeatherAgency(String name, String tag) {

    public static WeatherAgency of(String line) {
        var elements = Pattern.compile(",").splitAsStream(line).toArray(String[]::new);
        var name = elements[0].trim();
        var tag =  elements[1].trim();
        return new WeatherAgency(name, tag);
    }
}

package org.paumard.server.weather.model;


import static org.paumard.server.weather.WeatherServer.random;

public record Weather(String weather, String agency) {

    enum Type {
        Sunny, Cloudy, Rainy;
    }

    public static Weather randomFor(String agency) {
        Type weatherType = Type.values()[random.nextInt(0, Type.values().length)];
        return new Weather(weatherType.name(), agency);
    }
}

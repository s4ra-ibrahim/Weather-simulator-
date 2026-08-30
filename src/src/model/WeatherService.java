package model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

///  Class knows the lists of cities (with coordinates) and fetches weather for a city

public class WeatherService {

    private final ExecutorService threadPool;

    public WeatherService(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final LinkedHashMap<String, double[]> CITIES = new LinkedHashMap<>();
    static {
        CITIES.put("New York",   new double[]{ 40.7128,  -74.0060 });
        CITIES.put("London",     new double[]{ 51.5074,   -0.1278 });
        CITIES.put("Tokyo",      new double[]{ 35.6762,  139.6503 });
        CITIES.put("Sydney",     new double[]{ -33.8688, 151.2093 });
        CITIES.put("Paris",      new double[]{ 48.8566,    2.3522 });
        CITIES.put("Stockholm",  new double[]{ 59.3293,   18.0686 });
        CITIES.put("Lund",       new double[]{ 55.7047,   13.1910 });
        CITIES.put("Atlantis",   new double[]{ 999.0,     999.0   });
    }

    private static String describeCode(int code) {
        return switch (code) {
            case 0         -> "Clear sky";
            case 1, 2, 3   -> "Partly cloudy";
            case 45, 48    -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 95        -> "Thunderstorm";
            default        -> "Code " + code;
        };
    }

    public List<String> getCityNames(){

        List<String> cityNames= new ArrayList<>(CITIES.keySet());
        return cityNames;
    }

    public WeatherResult fetchWeatherSync(String city) {

        double[] coords = CITIES.get(city); /// Values of the city added inside array. [0] for latitude, [1] for longitude

        long start = System.currentTimeMillis(); /// starts a timer

        String url = "https://api.open-meteo.com/v1/forecast?latitude="
                + coords[0]
                + "&longitude="
                + coords[1]
                + "&current_weather=true";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()); /// waits for response
            String body = response.body(); /// anweres request

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonObject cw = json.getAsJsonObject("current_weather");
            double temperature = cw.get("temperature").getAsDouble();
            double windSpeed = cw.get("windspeed").getAsDouble();
            int code = cw.get("weathercode").getAsInt();
            String condition = describeCode(code);
            long fetchMillis = System.currentTimeMillis() - start;
            return new WeatherResult(city, temperature, windSpeed, condition, fetchMillis, null);

        } catch (Exception e) {
            return new WeatherResult(city, null, null, null, System.currentTimeMillis() - start, e.getMessage());
        }
    }

    public CompletableFuture<WeatherResult> fetchWeatherAsync(String city) {

        double[] coords = CITIES.get(city); /// Values of the city added inside array. [0] for latitude, [1] for longitude

        long start = System.currentTimeMillis(); /// starts a timer

        String url = "https://api.open-meteo.com/v1/forecast?latitude="
                + coords[0]
                + "&longitude="
                + coords[1]
                + "&current_weather=true";

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()) /// doesnt wait . goes to next line immedietly.
                .thenApplyAsync(response -> { /// the callback. When reply recieved -> respond.

                    String body = response.body();

                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    JsonObject cw = json.getAsJsonObject("current_weather");
                    double temperature = cw.get("temperature").getAsDouble();
                    double windSpeed = cw.get("windspeed").getAsDouble();
                    int code = cw.get("weathercode").getAsInt();
                    String condition = describeCode(code);
                    long fetchMillis = System.currentTimeMillis() - start;
                    return new WeatherResult(city, temperature, windSpeed, condition, fetchMillis, null);


                }, threadPool)
                .exceptionally(e -> {
                    return new WeatherResult(city, null, null, null, System.currentTimeMillis() - start, e.getMessage());
                });

    }


}

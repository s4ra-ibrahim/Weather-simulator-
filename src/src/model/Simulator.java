package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/// Class creates the thread pool

public class Simulator {

    private final ExecutorService threadPool;
    private final WeatherService service;

    public Simulator() {
        this.threadPool = Executors.newFixedThreadPool(8); /// creates a pool of 8 thread. One for each city.
        this.service = new WeatherService(threadPool);
    }

    public void runSync(){

        long start = System.currentTimeMillis();
        List<String> cityNames = service.getCityNames();
        for(String city : cityNames){
            WeatherResult result = service.fetchWeatherSync(city);

            System.out.println(result.city() + " | " + result.temperature() + "°C | " + result.windSpeed() +
                    " km/h | " + result.condition() + " | " + result.fetchMillis() + "ms | " +
                    (result.isSuccess() ? "OK" : result.errorMessage()));
        }

        System.out.println("Total time [Sync]: " + (System.currentTimeMillis() - start) + "ms");
    }

    public void runAsync() {
        long start = System.currentTimeMillis();
        List<String> cities = service.getCityNames();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String city : cities) {
            CompletableFuture<Void> future = service.fetchWeatherAsync(city)
                    .thenAccept(result -> { /// callback for displaying results
                        System.out.println(result.city() + " | " + result.temperature() + "°C | " + result.windSpeed() +
                                " km/h | " + result.condition() + " | " + result.fetchMillis() + "ms | " +
                                (result.isSuccess() ? "OK" : result.errorMessage()));
                    });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> System.out.println("Total time [Async]: " + (System.currentTimeMillis() - start) + "ms"))
                .join();
    }

}

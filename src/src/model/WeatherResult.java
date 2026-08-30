package model;

///  Class holds results for fetching weather for one city

public record WeatherResult(
        String city,
        Double temperature,
        Double windSpeed,
        String condition,
        long fetchMillis,
        String errorMessage) {

        public boolean isSuccess(){
            return errorMessage==null;
        }

}

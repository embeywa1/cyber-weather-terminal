package src;

import java.util.List;

/**
 * Model class encapsulating weather metrics for current conditions and short-term forecast.
 */
public class WeatherData {
    private String cityName;
    private String country;
    private double latitude;
    private double longitude;
    private double temperatureC;
    private double windSpeedKmH;
    private int humidity;
    private int weatherCode;
    private boolean isDaytime;
    private List<ForecastDay> forecastList;

    public static class ForecastDay {
        private String date;
        private double maxTempC;
        private double minTempC;
        private int weatherCode;

        public ForecastDay(String date, double maxTempC, double minTempC, int weatherCode) {
            this.date = date;
            this.maxTempC = maxTempC;
            this.minTempC = minTempC;
            this.weatherCode = weatherCode;
        }

        public String getDate() { return date; }
        public double getMaxTempC() { return maxTempC; }
        public double getMinTempC() { return minTempC; }
        public int getWeatherCode() { return weatherCode; }
    }

    public WeatherData(String cityName, String country, double latitude, double longitude,
                       double temperatureC, double windSpeedKmH, int humidity,
                       int weatherCode, boolean isDaytime, List<ForecastDay> forecastList) {
        this.cityName = cityName;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperatureC = temperatureC;
        this.windSpeedKmH = windSpeedKmH;
        this.humidity = humidity;
        this.weatherCode = weatherCode;
        this.isDaytime = isDaytime;
        this.forecastList = forecastList;
    }

    public String getCityName() { return cityName; }
    public String getCountry() { return country; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getTemperatureC() { return temperatureC; }
    public double getTemperatureF() { return (temperatureC * 9 / 5) + 32; }
    public double getWindSpeedKmH() { return windSpeedKmH; }
    public double getWindSpeedMph() { return windSpeedKmH * 0.621371; }
    public int getHumidity() { return humidity; }
    public int getWeatherCode() { return weatherCode; }
    public boolean isDaytime() { return isDaytime; }
    public List<ForecastDay> getForecastList() { return forecastList; }

    public String getWeatherDescription() {
        switch (weatherCode) {
            case 0: return "Clear Sky";
            case 1: case 2: return "Partly Cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Foggy";
            case 51: case 53: case 55: return "Light Drizzle";
            case 61: case 63: case 65: return "Rainy";
            case 71: case 73: case 75: return "Snowy";
            case 80: case 81: case 82: return "Rain Showers";
            case 95: case 96: case 99: return "Thunderstorm";
            default: return "Variable Weather";
        }
    }

    public String getWeatherEmoji() {
        switch (weatherCode) {
            case 0: return isDaytime ? "☀️" : "🌙";
            case 1: case 2: return isDaytime ? "🌤️" : "🌙";
            case 3: return "☁️";
            case 45: case 48: return "🌫️";
            case 51: case 53: case 55: case 61: case 63: case 65: return "🌧️";
            case 71: case 73: case 75: return "❄️";
            case 80: case 81: case 82: return "🌦️";
            case 95: case 96: case 99: return "🌩️";
            default: return "🌡️";
        }
    }
}

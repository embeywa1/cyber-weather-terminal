package src;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP Client integrating with keyless Open-Meteo Geocoding & Weather API.
 */
public class WeatherService {

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public WeatherData fetchWeather(String locationQuery) throws Exception {
        double lat, lon;
        String cityName, country;

        // Check if query is raw coordinates (e.g., "-1.2864, 36.8172")
        if (locationQuery.matches("^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$")) {
            String[] parts = locationQuery.split(",");
            lat = Double.parseDouble(parts[0].trim());
            lon = Double.parseDouble(parts[1].trim());
            cityName = "Coordinates (" + String.format("%.2f", lat) + ", " + String.format("%.2f", lon) + ")";
            country = "Custom Location";
        } else {
            // Perform Geocoding lookup
            String encodedQuery = URLEncoder.encode(locationQuery.trim(), StandardCharsets.UTF_8);
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedQuery + "&count=1&language=en&format=json";

            String geoResponse = makeHttpGet(geoUrl);
            String resultsJson = extractJsonObject(geoResponse, "results");

            if (resultsJson == null || resultsJson.trim().equals("[]")) {
                throw new IllegalArgumentException("Location '" + locationQuery + "' not found. Please verify spelling.");
            }

            cityName = extractJsonValue(resultsJson, "name");
            country = extractJsonValue(resultsJson, "country");
            if (country == null || country.isEmpty()) country = "Global Region";

            lat = Double.parseDouble(extractJsonValue(resultsJson, "latitude"));
            lon = Double.parseDouble(extractJsonValue(resultsJson, "longitude"));
        }

        // Fetch Weather Metrics + 5-day Forecast
        String weatherUrl = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true&hourly=relativehumidity_2m&daily=weathercode,temperature_2m_max,temperature_2m_min&timezone=auto",
            lat, lon
        );

        String weatherResponse = makeHttpGet(weatherUrl);
        String currentWeatherJson = extractJsonObject(weatherResponse, "current_weather");

        if (currentWeatherJson == null) {
            throw new RuntimeException("Failed to parse weather metrics from network response.");
        }

        double tempC = Double.parseDouble(extractJsonValue(currentWeatherJson, "temperature"));
        double windSpeed = Double.parseDouble(extractJsonValue(currentWeatherJson, "windspeed"));
        int weatherCode = Integer.parseInt(extractJsonValue(currentWeatherJson, "weathercode"));
        int isDay = Integer.parseInt(extractJsonValue(currentWeatherJson, "is_day"));

        // Extract Humidity (approximate current hour)
        int humidity = 65; // Fallback default
        String hourlyJson = extractJsonObject(weatherResponse, "hourly");
        if (hourlyJson != null) {
            String humidityArray = extractJsonObject(hourlyJson, "relativehumidity_2m");
            if (humidityArray != null) {
                String[] values = humidityArray.replace("[", "").replace("]", "").split(",");
                if (values.length > 0 && !values[0].trim().isEmpty()) {
                    humidity = (int) Double.parseDouble(values[0].trim());
                }
            }
        }

        // Extract Forecast Data
        List<WeatherData.ForecastDay> forecastList = new ArrayList<>();
        String dailyJson = extractJsonObject(weatherResponse, "daily");
        if (dailyJson != null) {
            List<String> dates = extractJsonArrayValues(dailyJson, "time");
            List<String> maxTemps = extractJsonArrayValues(dailyJson, "temperature_2m_max");
            List<String> minTemps = extractJsonArrayValues(dailyJson, "temperature_2m_min");
            List<String> codes = extractJsonArrayValues(dailyJson, "weathercode");

            for (int i = 0; i < Math.min(5, dates.size()); i++) {
                forecastList.add(new WeatherData.ForecastDay(
                    dates.get(i),
                    Double.parseDouble(maxTemps.get(i)),
                    Double.parseDouble(minTemps.get(i)),
                    Integer.parseInt(codes.get(i))
                ));
            }
        }

        return new WeatherData(cityName, country, lat, lon, tempC, windSpeed, humidity, weatherCode, isDay == 1, forecastList);
    }

    private String makeHttpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP Server Error (Status: " + response.statusCode() + ")");
        }
        return response.body();
    }

    // Utility JSON Extractors (Zero-Dependency Lightweight Regex Parsing)
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^\",\\}\\]]+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String extractJsonObject(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return null;
        int start = json.indexOf(":", keyIndex) + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

        char openChar = json.charAt(start);
        if (openChar != '{' && openChar != '[') {
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }

        char closeChar = (openChar == '{') ? '}' : ']';
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == openChar) depth++;
            else if (json.charAt(i) == closeChar) depth--;
            if (depth == 0) return json.substring(start, i + 1);
        }
        return null;
    }

    private List<String> extractJsonArrayValues(String json, String key) {
        List<String> list = new ArrayList<>();
        String arrayStr = extractJsonObject(json, key);
        if (arrayStr != null) {
            String clean = arrayStr.replace("[", "").replace("]", "").replace("\"", "");
            for (String val : clean.split(",")) {
                if (!val.trim().isEmpty()) list.add(val.trim());
            }
        }
        return list;
    }
}

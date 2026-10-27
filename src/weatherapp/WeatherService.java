package weatherapp;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import weatherapp.WeatherException.Type;

/**
 * Talks to the Open-Meteo web services (https://open-meteo.com).
 *
 * <p>Two services are used: the <b>geocoding API</b> turns a place name into coordinates, and
 * the <b>forecast API</b> returns the current weather and a daily forecast for coordinates.
 * Both are free for non-commercial use and need no API key. Every call is a plain HTTPS GET
 * that returns JSON; one search costs at most two calls, and changing the display units
 * never calls the service again because values are converted locally.
 *
 * <p>The methods block while they wait for the network, so the GUI calls them from a
 * background thread. Every failure is reported as a {@link WeatherException} with a message
 * that is safe to show to the user.
 */
public class WeatherService {

    public static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    public static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    /** How many days the forecast covers, today included. */
    public static final int FORECAST_DAYS = 5;

    private static final String CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,"
            + "apparent_temperature,is_day,weather_code,surface_pressure,"
            + "wind_speed_10m,wind_direction_10m";
    private static final String DAILY_FIELDS = "weather_code,temperature_2m_max,"
            + "temperature_2m_min,precipitation_probability_max,sunrise,sunset";
    private static final String USER_AGENT = "WeatherInformationApp/1.0 (student project)";

    private final HttpClient client;
    private final String geocodingUrl;
    private final String forecastUrl;
    private final Duration requestTimeout;

    /** A service that uses the real Open-Meteo servers. */
    public WeatherService() {
        this(GEOCODING_URL, FORECAST_URL, Duration.ofSeconds(10));
    }

    /** A service with custom addresses and timeout; the tests use this with a local server. */
    public WeatherService(String geocodingUrl, String forecastUrl, Duration requestTimeout) {
        this.geocodingUrl = geocodingUrl;
        this.forecastUrl = forecastUrl;
        this.requestTimeout = requestTimeout;
        this.client = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
    }

    /**
     * Resolves what the user typed to a place. Coordinates need no lookup; a city name is
     * sent to the geocoding API, which returns the best match first.
     *
     * @throws WeatherException NOT_FOUND if no place matches, NETWORK or SERVICE on failures
     */
    public Location findLocation(LocationQuery query) throws WeatherException {
        if (query instanceof LocationQuery.Coordinates coordinates) {
            return Location.ofCoordinates(coordinates.latitude(), coordinates.longitude());
        }
        String name = ((LocationQuery.City) query).name();
        Json response = getJson(geocodingUrl + "?name=" + encode(name)
                + "&count=1&language=en&format=json");

        Json best = response.get("results").get(0);
        if (best.isMissing()) {
            throw new WeatherException(Type.NOT_FOUND, "No place called \"" + name
                    + "\" was found. Check the spelling, or add the country: \"Paris, France\".");
        }
        double latitude = best.get("latitude").asDouble(Double.NaN);
        double longitude = best.get("longitude").asDouble(Double.NaN);
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            throw incompleteData();
        }
        return new Location(best.get("name").asString(name), best.get("admin1").asString(""),
                best.get("country").asString(""), latitude, longitude);
    }

    /**
     * Downloads the current weather and the daily forecast for a place.
     *
     * @throws WeatherException NETWORK or SERVICE if the data cannot be retrieved or read
     */
    public WeatherReport fetchWeather(Location location) throws WeatherException {
        String url = String.format(Locale.ROOT,
                "%s?latitude=%.4f&longitude=%.4f&current=%s&daily=%s&timezone=auto"
                        + "&forecast_days=%d",
                forecastUrl, location.latitude(), location.longitude(),
                CURRENT_FIELDS, DAILY_FIELDS, FORECAST_DAYS);
        Json root = getJson(url);
        try {
            return new WeatherReport(location, root.get("timezone").asString(""),
                    readCurrent(root.get("current")), readForecast(root.get("daily")));
        } catch (DateTimeParseException e) {
            throw new WeatherException(Type.SERVICE,
                    "The weather service sent a date the app could not read.", e);
        }
    }

    private static CurrentConditions readCurrent(Json current) throws WeatherException {
        return new CurrentConditions(
                LocalDateTime.parse(text(current, "time")),
                number(current, "temperature_2m"),
                number(current, "apparent_temperature"),
                (int) Math.round(number(current, "relative_humidity_2m")),
                number(current, "wind_speed_10m"),
                (int) Math.round(number(current, "wind_direction_10m")),
                number(current, "surface_pressure"),
                (int) Math.round(number(current, "weather_code")),
                number(current, "is_day") == 1);
    }

    private static List<DailyForecast> readForecast(Json daily) throws WeatherException {
        int days = daily.get("time").size();
        if (days == 0) {
            throw incompleteData();
        }
        List<DailyForecast> forecast = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String sunrise = daily.get("sunrise").get(i).asString(null);
            String sunset = daily.get("sunset").get(i).asString(null);
            forecast.add(new DailyForecast(
                    LocalDate.parse(daily.get("time").get(i).asString("")),
                    (int) Math.round(number(daily.get("weather_code"), i)),
                    number(daily.get("temperature_2m_max"), i),
                    number(daily.get("temperature_2m_min"), i),
                    daily.get("precipitation_probability_max").get(i)
                            .asInt(DailyForecast.UNKNOWN_CHANCE),
                    sunrise == null ? null : LocalDateTime.parse(sunrise),
                    sunset == null ? null : LocalDateTime.parse(sunset)));
        }
        return forecast;
    }

    /** A required numeric property; throws if the service left it out. */
    private static double number(Json object, String property) throws WeatherException {
        Json value = object.get(property);
        if (!value.isNumber()) {
            throw incompleteData();
        }
        return value.asDouble(0);
    }

    /** A required numeric array element; throws if the service left it out. */
    private static double number(Json array, int index) throws WeatherException {
        Json value = array.get(index);
        if (!value.isNumber()) {
            throw incompleteData();
        }
        return value.asDouble(0);
    }

    private static String text(Json object, String property) throws WeatherException {
        String value = object.get(property).asString(null);
        if (value == null) {
            throw incompleteData();
        }
        return value;
    }

    /** Sends a GET request and returns the parsed JSON reply, or throws a friendly error. */
    private Json getJson(String url) throws WeatherException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(requestTimeout)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new WeatherException(Type.NETWORK,
                    "The weather service took too long to answer. Please try again.", e);
        } catch (IOException e) {
            throw new WeatherException(Type.NETWORK, "Could not reach the weather service. "
                    + "Check your internet connection and try again.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherException(Type.NETWORK, "The request was interrupted.", e);
        }

        int status = response.statusCode();
        Json body;
        try {
            body = Json.parse(response.body());
        } catch (IllegalArgumentException e) {
            if (status >= 400) {
                throw httpError(status, Json.missing());
            }
            throw new WeatherException(Type.SERVICE,
                    "The weather service sent a reply the app could not read.", e);
        }
        if (status >= 400 || body.get("error").asBoolean(false)) {
            throw httpError(status, body);
        }
        return body;
    }

    private static WeatherException httpError(int status, Json body) {
        if (status == 429) {
            return new WeatherException(Type.SERVICE, "The weather service is receiving too "
                    + "many requests. Wait a minute and try again.");
        }
        String reason = body.get("reason").asString("HTTP status " + status);
        return new WeatherException(Type.SERVICE,
                "The weather service could not answer the request: " + reason);
    }

    private static WeatherException incompleteData() {
        return new WeatherException(Type.SERVICE,
                "The weather service returned incomplete data. Please try again later.");
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}

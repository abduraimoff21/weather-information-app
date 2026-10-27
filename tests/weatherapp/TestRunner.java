package weatherapp;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.sun.net.httpserver.HttpServer;

/**
 * Automated checks for the Weather Information App. It needs no test library: run it with
 * {@code java -cp out weatherapp.TestRunner}. The checks cover the pure logic (units,
 * formatting, input validation, JSON, time of day, history), the service against a local mock
 * HTTP server (including every error case), the real Open-Meteo API, and the window itself.
 */
public final class TestRunner {

    private static final List<String> RESULTS = new ArrayList<>();
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        unitConversions();
        displayFormatting();
        weatherCodes();
        timeOfDay();
        json();
        inputValidation();
        searchHistory();
        MockServer server = new MockServer();
        try {
            serviceAgainstMockServer(server);
            networkFailures(server);
            if (GraphicsEnvironment.isHeadless()) {
                note("SKIP window checks (no display available)");
            } else {
                windowAgainstMockServer(server);
            }
        } finally {
            server.stop();
        }
        liveApi();

        String summary = passed + "/" + (passed + failed) + " checks passed";
        RESULTS.add("");
        RESULTS.add(summary);
        System.out.println();
        System.out.println(summary);
        Path output = Path.of(args.length > 0 ? args[0] : "tests/output/test_results.txt");
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output, RESULTS, StandardCharsets.UTF_8);
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---- tiny assertion helpers ---------------------------------------------------------

    private static void check(String name, boolean condition) {
        String line = (condition ? "PASS " : "FAIL ") + name;
        System.out.println(line);
        RESULTS.add(line);
        if (condition) {
            passed++;
        } else {
            failed++;
        }
    }

    private static void checkEquals(String name, Object expected, Object actual) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        check(name + (same ? "" : "  (expected " + expected + ", got " + actual + ")"), same);
    }

    private static void note(String text) {
        System.out.println(text);
        RESULTS.add(text);
    }

    private interface Action {
        void run() throws Exception;
    }

    /** Checks that the action fails with a WeatherException of the given type. */
    private static WeatherException expectFailure(String name, WeatherException.Type type,
                                                  Action action) {
        try {
            action.run();
            check(name + "  (no error was thrown)", false);
        } catch (WeatherException e) {
            checkEquals(name, type, e.type());
            return e;
        } catch (Exception e) {
            check(name + "  (unexpected " + e + ")", false);
        }
        return null;
    }

    // ---- pure logic ---------------------------------------------------------------------

    private static void unitConversions() {
        note("# Unit conversion");
        TemperatureUnit f = TemperatureUnit.FAHRENHEIT;
        checkEquals("0 C is 32 F", 32.0, f.fromCelsius(0));
        checkEquals("100 C is 212 F", 212.0, f.fromCelsius(100));
        checkEquals("-40 C is -40 F", -40.0, f.fromCelsius(-40));
        check("36.6 C is 97.88 F", Math.abs(f.fromCelsius(36.6) - 97.88) < 1e-9);
        checkEquals("Celsius stays unchanged", 21.5, TemperatureUnit.CELSIUS.fromCelsius(21.5));
        check("36 km/h is 10 m/s",
                Math.abs(WindSpeedUnit.METRES_PER_SECOND.fromKilometresPerHour(36) - 10) < 1e-9);
        check("100 km/h is 62.1371 mph", Math.abs(
                WindSpeedUnit.MILES_PER_HOUR.fromKilometresPerHour(100) - 62.1371) < 1e-9);
        check("100 km/h is 53.9957 knots", Math.abs(
                WindSpeedUnit.KNOTS.fromKilometresPerHour(100) - 53.9957) < 1e-9);
        checkEquals("km/h stays unchanged", 17.0,
                WindSpeedUnit.KILOMETRES_PER_HOUR.fromKilometresPerHour(17));
    }

    private static void displayFormatting() {
        note("# Display formatting");
        checkEquals("temperature in Celsius", "13.7°C",
                DisplayFormat.temperature(13.7, TemperatureUnit.CELSIUS, 1));
        checkEquals("temperature in Fahrenheit", "56.7°F",
                DisplayFormat.temperature(13.7, TemperatureUnit.FAHRENHEIT, 1));
        checkEquals("rounded temperature", "17°C",
                DisplayFormat.temperature(16.7, TemperatureUnit.CELSIUS, 0));
        checkEquals("negative zero is shown as 0.0", "0.0°C",
                DisplayFormat.temperature(-0.04, TemperatureUnit.CELSIUS, 1));
        checkEquals("negative temperature", "-12.5°C",
                DisplayFormat.temperature(-12.5, TemperatureUnit.CELSIUS, 1));
        checkEquals("wind speed in mph", "22.4 mph",
                DisplayFormat.windSpeed(36, WindSpeedUnit.MILES_PER_HOUR));
        checkEquals("compass: 0 degrees is N", "N", DisplayFormat.compassPoint(0));
        checkEquals("compass: 90 degrees is E", "E", DisplayFormat.compassPoint(90));
        checkEquals("compass: 261 degrees is W", "W", DisplayFormat.compassPoint(261));
        checkEquals("compass: 350 degrees is N", "N", DisplayFormat.compassPoint(350));
        checkEquals("compass: 360 degrees is N", "N", DisplayFormat.compassPoint(360));
        checkEquals("compass: -90 degrees is W", "W", DisplayFormat.compassPoint(-90));
        checkEquals("pressure is rounded", "1012 hPa", DisplayFormat.pressure(1012.4));
        checkEquals("date and time", "Tue, 27 Oct 2026 · 16:45",
                DisplayFormat.dateTime(LocalDateTime.of(2026, 10, 27, 16, 45)));
        checkEquals("unknown clock time", "--:--", DisplayFormat.clockTime(null));
        checkEquals("history timestamp", "27 Oct 2026, 21:35:12",
                DisplayFormat.historyTime(LocalDateTime.of(2026, 10, 27, 21, 35, 12)));
        LocalDate today = LocalDate.of(2026, 10, 27);
        checkEquals("day name for today", "Today", DisplayFormat.dayName(today, today));
        checkEquals("day name for tomorrow", "Wed",
                DisplayFormat.dayName(today.plusDays(1), today));
    }

    private static void weatherCodes() {
        note("# Weather codes and icons");
        checkEquals("code 0 text", "Clear sky", WeatherCode.describe(0));
        checkEquals("code 0 icon", IconType.CLEAR, WeatherCode.iconFor(0));
        checkEquals("code 2 icon", IconType.PARTLY_CLOUDY, WeatherCode.iconFor(2));
        checkEquals("code 3 icon", IconType.CLOUDY, WeatherCode.iconFor(3));
        checkEquals("code 45 icon", IconType.FOG, WeatherCode.iconFor(45));
        checkEquals("code 53 icon", IconType.DRIZZLE, WeatherCode.iconFor(53));
        checkEquals("code 61 text", "Slight rain", WeatherCode.describe(61));
        checkEquals("code 61 icon", IconType.RAIN, WeatherCode.iconFor(61));
        checkEquals("code 75 icon", IconType.SNOW, WeatherCode.iconFor(75));
        checkEquals("code 95 icon", IconType.THUNDERSTORM, WeatherCode.iconFor(95));
        checkEquals("unknown code text", "Unknown conditions", WeatherCode.describe(999));
        checkEquals("unknown code icon", IconType.CLOUDY, WeatherCode.iconFor(999));
        boolean allDraw = true;
        for (IconType type : IconType.values()) {
            for (boolean night : new boolean[] {false, true}) {
                allDraw &= new WeatherIcon(type, night, 64).toImage().getWidth() == 64;
            }
        }
        check("every icon draws in day and night variants", allDraw);
    }

    private static void timeOfDay() {
        note("# Time of day (dynamic background)");
        LocalDateTime sunrise = LocalDateTime.of(2026, 10, 27, 6, 45);
        LocalDateTime sunset = LocalDateTime.of(2026, 10, 27, 16, 42);
        checkEquals("03:00 is night", TimeOfDay.NIGHT, phase(3, 0, sunrise, sunset));
        checkEquals("05:59 is still night", TimeOfDay.NIGHT, phase(5, 59, sunrise, sunset));
        checkEquals("06:00 is sunrise", TimeOfDay.SUNRISE, phase(6, 0, sunrise, sunset));
        checkEquals("07:30 is sunrise", TimeOfDay.SUNRISE, phase(7, 30, sunrise, sunset));
        checkEquals("07:46 is day", TimeOfDay.DAY, phase(7, 46, sunrise, sunset));
        checkEquals("12:00 is day", TimeOfDay.DAY, phase(12, 0, sunrise, sunset));
        checkEquals("15:26 is day", TimeOfDay.DAY, phase(15, 26, sunrise, sunset));
        checkEquals("15:27 is sunset", TimeOfDay.SUNSET, phase(15, 27, sunrise, sunset));
        checkEquals("16:45 (after sunset) is sunset", TimeOfDay.SUNSET,
                phase(16, 45, sunrise, sunset));
        checkEquals("17:26 is sunset", TimeOfDay.SUNSET, phase(17, 26, sunrise, sunset));
        checkEquals("17:27 is night", TimeOfDay.NIGHT, phase(17, 27, sunrise, sunset));
        checkEquals("23:30 is night", TimeOfDay.NIGHT, phase(23, 30, sunrise, sunset));
        checkEquals("no sun times: 06:00 uses the clock", TimeOfDay.SUNRISE,
                phase(6, 0, null, null));
        checkEquals("no sun times: 12:00 uses the clock", TimeOfDay.DAY, phase(12, 0, null, null));
        checkEquals("no sun times: 18:00 uses the clock", TimeOfDay.SUNSET,
                phase(18, 0, null, null));
        checkEquals("no sun times: 22:00 uses the clock", TimeOfDay.NIGHT,
                phase(22, 0, null, null));
        checkEquals("impossible sun times use the clock", TimeOfDay.DAY,
                phase(12, 0, sunset, sunrise));
        checkEquals("clock 04:59 is night", TimeOfDay.NIGHT,
                TimeOfDay.fromClock(LocalTime.of(4, 59)));
        checkEquals("clock 19:29 is sunset", TimeOfDay.SUNSET,
                TimeOfDay.fromClock(LocalTime.of(19, 29)));
    }

    private static TimeOfDay phase(int hour, int minute, LocalDateTime rise, LocalDateTime set) {
        return TimeOfDay.at(LocalDateTime.of(2026, 10, 27, hour, minute), rise, set);
    }

    private static void json() {
        note("# JSON reader");
        Json doc = Json.parse("{\"name\":\"Zürich\",\"n\":-12.5,\"big\":1.5e3,\"ok\":true,"
                + "\"none\":null,\"list\":[1,2,3],\"nested\":{\"a\":{\"b\":[\"x\",\"y\"]}}}");
        checkEquals("string", "Zürich", doc.get("name").asString(""));
        checkEquals("negative number", -12.5, doc.get("n").asDouble(0));
        checkEquals("exponent", 1500.0, doc.get("big").asDouble(0));
        checkEquals("boolean", true, doc.get("ok").asBoolean(false));
        check("JSON null counts as missing", doc.get("none").isMissing());
        checkEquals("array size", 3, doc.get("list").size());
        checkEquals("array element", 2.0, doc.get("list").get(1).asDouble(0));
        checkEquals("deep lookup", "y", doc.get("nested").get("a").get("b").get(1).asString(""));
        check("absent property is missing", doc.get("nothing").isMissing());
        check("absent deep lookup never throws",
                doc.get("nothing").get("a").get(3).get("b").isMissing());
        checkEquals("fallback for the wrong type", 7.0, doc.get("name").asDouble(7));
        checkEquals("escapes", "a\"b\\c/d\ne\tf",
                Json.parse("\"a\\\"b\\\\c\\/d\\ne\\tf\"").asString(""));
        checkEquals("unicode escape", "é", Json.parse("\"\\u00e9\"").asString(""));
        checkEquals("whitespace is ignored", 1.0, Json.parse("  [ 1 , 2 ]\n").get(0).asDouble(0));
        checkEquals("empty object", 0, Json.parse("{}").size());
        for (String bad : new String[] {"", "{", "[1,]", "{\"a\":}", "abc", "{\"a\":1} x",
                "\"open", "{a:1}", "[1 2]", "tru", "\"\\q\"", "-", "[".repeat(100)}) {
            boolean rejected;
            try {
                Json.parse(bad);
                rejected = false;
            } catch (IllegalArgumentException e) {
                rejected = true;
            }
            check("rejects malformed JSON: " + (bad.length() > 12 ? "[[[..." : bad), rejected);
        }
    }

    private static void inputValidation() {
        note("# Input validation");
        for (String city : new String[] {"Paris", "Paris, France", "São Paulo", "Zürich",
                "St. John's", "Winston-Salem", "  new   york  ", "東京", "Tashkent"}) {
            check("accepts the city \"" + city + "\"", parsesAs(city, LocationQuery.City.class));
        }
        checkEquals("extra spaces are tidied", new LocationQuery.City("new york"),
                parseOrNull("  new   york  "));
        checkEquals("coordinates with comma", new LocationQuery.Coordinates(48.85, 2.35),
                parseOrNull("48.85, 2.35"));
        checkEquals("coordinates with a space", new LocationQuery.Coordinates(-33.87, 151.21),
                parseOrNull("-33.87 151.21"));
        checkEquals("coordinates with a semicolon", new LocationQuery.Coordinates(0, 0),
                parseOrNull("0;0"));
        checkEquals("coordinates at the limits", new LocationQuery.Coordinates(-90, 180),
                parseOrNull("-90,180"));
        for (String bad : new String[] {"", "   ", "!!!", "<script>alert(1)</script>", "a",
                "12345", "48.85", "91, 0", "0, 181", "-90.5, 10", "Paris;DROP TABLE",
                "x".repeat(101)}) {
            boolean rejected = parseOrNull(bad) == null;
            check("rejects invalid input: \"" + (bad.length() > 14 ? bad.substring(0, 14) + "..."
                    : bad) + "\"", rejected);
        }
        expectFailure("null input is invalid", WeatherException.Type.INVALID_INPUT,
                () -> InputParser.parse(null));
        WeatherException range = expectFailure("latitude range error",
                WeatherException.Type.INVALID_INPUT, () -> InputParser.parse("95, 10"));
        check("the message names the problem", range != null
                && range.getMessage().contains("Latitude must be between -90 and 90"));
    }

    private static LocationQuery parseOrNull(String text) {
        try {
            return InputParser.parse(text);
        } catch (WeatherException e) {
            return null;
        }
    }

    private static boolean parsesAs(String text, Class<? extends LocationQuery> type) {
        LocationQuery query = parseOrNull(text);
        return query != null && type.isInstance(query);
    }

    private static void searchHistory() throws IOException {
        note("# Search history");
        Path file = Files.createTempDirectory("weather-history").resolve("history.tsv");
        SearchHistory history = new SearchHistory(file, 3);
        check("starts empty", history.entries().isEmpty());
        LocalDateTime t0 = LocalDateTime.of(2026, 10, 27, 9, 0, 0);
        history.record(new Location("Paris", "Île-de-France", "France", 48.85, 2.35), t0);
        history.record(new Location("Tokyo", "Tokyo", "Japan", 35.68, 139.69), t0.plusMinutes(1));
        history.record(Location.ofCoordinates(-33.87, 151.21), t0.plusMinutes(2));
        checkEquals("three entries", 3, history.entries().size());
        checkEquals("newest first", "33.87°S, 151.21°E",
                history.entries().get(0).location().name());
        checkEquals("oldest last", "Paris", history.entries().get(2).location().name());
        checkEquals("timestamp is kept", t0.plusMinutes(1), history.entries().get(1).time());
        history.record(new Location("Cairo", "", "Egypt", 30.04, 31.24), t0.plusMinutes(3));
        checkEquals("capacity is respected", 3, history.entries().size());
        check("the oldest entry was dropped",
                history.entries().stream().noneMatch(e -> e.location().name().equals("Paris")));

        SearchHistory reloaded = new SearchHistory(file, 3);
        checkEquals("history survives a restart", history.entries(), reloaded.entries());
        checkEquals("accented text survives a restart", "Cairo",
                reloaded.entries().get(0).location().name());

        List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        lines.add("this line is damaged");
        lines.add("2026-10-27T09:00\tX\t\t\tnot-a-number\t1.0");
        Files.write(file, lines, StandardCharsets.UTF_8);
        checkEquals("damaged lines are skipped", 3, new SearchHistory(file, 3).entries().size());

        SearchHistory tabs = new SearchHistory(file, 5);
        tabs.record(new Location("Odd\tName\nHere", "", "", 1, 2), t0);
        checkEquals("tabs and line breaks cannot corrupt the file", 1,
                countFirstEntries(file, "Odd Name Here"));

        reloaded.clear();
        check("clear empties the list", reloaded.entries().isEmpty());
        check("clear empties the file", new SearchHistory(file, 3).entries().isEmpty());
    }

    private static int countFirstEntries(Path file, String name) {
        SearchHistory reloaded = new SearchHistory(file, 5);
        return reloaded.entries().get(0).location().name().equals(name) ? 1 : 0;
    }

    // ---- the service against a local mock server ----------------------------------------

    private static void serviceAgainstMockServer(MockServer server) throws Exception {
        note("# Weather service (local mock server)");
        WeatherService service = server.service(Duration.ofSeconds(5));
        server.reset();

        LocationQuery.City city = new LocationQuery.City("Testville");
        Location place = service.findLocation(city);
        checkEquals("geocoding: name", "Testville", place.name());
        checkEquals("geocoding: region", "Testshire", place.region());
        checkEquals("geocoding: country", "Testland", place.country());
        checkEquals("geocoding: latitude", 12.34, place.latitude());
        checkEquals("geocoding: longitude", 56.78, place.longitude());
        checkEquals("geocoding: label", "Testville, Testshire, Testland", place.label());
        check("geocoding request names the city",
                server.lastGeocodingQuery.contains("name=Testville"));
        check("geocoding asks for one result", server.lastGeocodingQuery.contains("count=1"));
        check("the request identifies the app",
                server.lastUserAgent != null
                        && server.lastUserAgent.startsWith("WeatherInformationApp"));

        service.findLocation(new LocationQuery.City("São Paulo, Brazil"));
        check("city names are URL-encoded",
                server.lastGeocodingQuery.contains("name=S%C3%A3o+Paulo%2C+Brazil"));

        Location byCoordinates = service.findLocation(new LocationQuery.Coordinates(48.85, 2.35));
        checkEquals("coordinates need no geocoding call", 2, server.geocodingCalls.get());
        checkEquals("coordinate label", "48.85°N, 2.35°E", byCoordinates.label());

        WeatherReport report = service.fetchWeather(place);
        CurrentConditions now = report.current();
        checkEquals("forecast request: latitude", true,
                server.lastForecastQuery.contains("latitude=12.3400"));
        checkEquals("forecast request: longitude", true,
                server.lastForecastQuery.contains("longitude=56.7800"));
        check("forecast request: timezone is automatic",
                server.lastForecastQuery.contains("timezone=auto"));
        check("forecast request: five days", server.lastForecastQuery.contains("forecast_days=5"));
        check("forecast request: humidity and wind are requested",
                server.lastForecastQuery.contains("relative_humidity_2m")
                        && server.lastForecastQuery.contains("wind_speed_10m"));
        checkEquals("time zone", "Europe/Testville", report.timezone());
        checkEquals("observation time", LocalDateTime.of(2026, 10, 27, 12, 0), now.time());
        checkEquals("temperature", 20.0, now.temperatureC());
        checkEquals("feels like", 18.5, now.feelsLikeC());
        checkEquals("humidity", 60, now.humidityPercent());
        checkEquals("wind speed", 36.0, now.windSpeedKmh());
        checkEquals("wind direction", 270, now.windDirectionDegrees());
        checkEquals("pressure", 1013.2, now.pressureHpa());
        checkEquals("weather code", 2, now.weatherCode());
        checkEquals("daytime flag", true, now.daytime());
        checkEquals("five forecast days", 5, report.forecast().size());
        DailyForecast first = report.today();
        checkEquals("first day", LocalDate.of(2026, 10, 27), first.date());
        checkEquals("first day high", 22.5, first.highC());
        checkEquals("first day low", 11.0, first.lowC());
        checkEquals("first day precipitation", 30, first.precipitationChancePercent());
        checkEquals("first day sunrise", LocalDateTime.of(2026, 10, 27, 6, 45), first.sunrise());
        checkEquals("first day sunset", LocalDateTime.of(2026, 10, 27, 16, 42), first.sunset());
        checkEquals("missing precipitation is marked unknown", DailyForecast.UNKNOWN_CHANCE,
                report.forecast().get(1).precipitationChancePercent());
        checkEquals("last day", LocalDate.of(2026, 10, 31), report.forecast().get(4).date());

        // Error handling
        server.reset();
        WeatherException notFound = expectFailure("unknown place is reported as not found",
                WeatherException.Type.NOT_FOUND,
                () -> service.findLocation(new LocationQuery.City("Nowhereville")));
        check("not-found message names the input",
                notFound != null && notFound.getMessage().contains("Nowhereville"));

        server.forecastStatus = 500;
        server.forecastBody = "<html>Internal Server Error</html>";
        expectFailure("HTTP 500 is a service problem", WeatherException.Type.SERVICE,
                () -> service.fetchWeather(place));
        server.forecastStatus = 429;
        server.forecastBody = "{\"error\":true,\"reason\":\"rate limit\"}";
        WeatherException tooMany = expectFailure("HTTP 429 is a service problem",
                WeatherException.Type.SERVICE, () -> service.fetchWeather(place));
        check("429 message asks the user to wait",
                tooMany != null && tooMany.getMessage().contains("too many requests"));
        server.forecastStatus = 400;
        server.forecastBody =
                "{\"error\":true,\"reason\":\"Latitude must be in range of -90 to 90\"}";
        WeatherException badRequest = expectFailure("HTTP 400 is a service problem",
                WeatherException.Type.SERVICE, () -> service.fetchWeather(place));
        check("400 message repeats the reason from the service", badRequest != null
                && badRequest.getMessage().contains("Latitude must be in range"));
        server.forecastStatus = 200;
        server.forecastBody = "this is not json";
        expectFailure("unreadable reply is a service problem", WeatherException.Type.SERVICE,
                () -> service.fetchWeather(place));
        server.forecastBody = "{\"error\":true,\"reason\":\"Something broke\"}";
        expectFailure("error flag in a 200 reply is a service problem",
                WeatherException.Type.SERVICE, () -> service.fetchWeather(place));
        server.forecastBody = "{\"timezone\":\"UTC\"}";
        expectFailure("reply without weather data is a service problem",
                WeatherException.Type.SERVICE, () -> service.fetchWeather(place));
        server.forecastBody = MockServer.FORECAST.replace("\"temperature_2m\":20.0,", "");
        expectFailure("a missing temperature is a service problem",
                WeatherException.Type.SERVICE, () -> service.fetchWeather(place));
        server.forecastBody = MockServer.FORECAST.replace("2026-10-27T12:00", "yesterday");
        expectFailure("an unreadable date is a service problem",
                WeatherException.Type.SERVICE, () -> service.fetchWeather(place));
    }

    private static void networkFailures(MockServer server) throws Exception {
        note("# Network failures");
        server.reset();
        server.forecastDelayMillis = 3000;
        WeatherService impatient = server.service(Duration.ofMillis(800));
        WeatherException slow = expectFailure("a slow service is a network problem",
                WeatherException.Type.NETWORK, () -> impatient.fetchWeather(
                        new Location("Testville", "", "", 12.34, 56.78)));
        check("timeout message says it took too long",
                slow != null && slow.getMessage().contains("too long"));
        server.forecastDelayMillis = 0;

        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        WeatherService offline = new WeatherService("http://localhost:" + closedPort + "/geo",
                "http://localhost:" + closedPort + "/forecast", Duration.ofSeconds(3));
        WeatherException refused = expectFailure("no connection is a network problem",
                WeatherException.Type.NETWORK,
                () -> offline.findLocation(new LocationQuery.City("Paris")));
        check("connection message mentions the internet",
                refused != null && refused.getMessage().contains("internet connection"));
        expectFailure("no connection while fetching the forecast", WeatherException.Type.NETWORK,
                () -> offline.fetchWeather(new Location("X", "", "", 1, 2)));
        WeatherService unknownHost = new WeatherService("http://no-such-host.invalid/geo",
                "http://no-such-host.invalid/forecast", Duration.ofSeconds(3));
        expectFailure("an unknown host is a network problem", WeatherException.Type.NETWORK,
                () -> unknownHost.findLocation(new LocationQuery.City("Paris")));
    }

    // ---- the window ---------------------------------------------------------------------

    private static void windowAgainstMockServer(MockServer server) throws Exception {
        note("# Window (against the mock server)");
        server.reset();
        Path file = Files.createTempDirectory("weather-gui").resolve("history.tsv");
        SearchHistory history = new SearchHistory(file, 20);
        WeatherFrame frame = onEdt(() -> {
            WeatherFrame created = new WeatherFrame(server.service(Duration.ofSeconds(5)), history);
            created.setVisible(true);
            return created;
        });
        try {
            checkEquals("starts with an empty history", 0, onEdt(frame::historySize));
            check("starts with a hint in the status bar",
                    onEdt(frame::statusText).contains("Enter a city name"));

            search(frame, "");
            check("empty input is rejected with a message",
                    onEdt(frame::statusText).startsWith("Invalid input:"));
            search(frame, "!!!");
            check("unusable characters are rejected",
                    onEdt(frame::statusText).contains("Use only letters"));
            search(frame, "95, 10");
            check("out-of-range coordinates are rejected",
                    onEdt(frame::statusText).contains("Latitude must be between"));
            checkEquals("invalid input never reaches the service", 0,
                    server.geocodingCalls.get() + server.forecastCalls.get());

            search(frame, "Nowhereville");
            check("an unknown place shows 'Location not found'",
                    onEdt(frame::statusText).startsWith("Location not found:"));
            checkEquals("a failed search is not added to the history", 0,
                    onEdt(frame::historySize));
            checkEquals("the window is usable again after an error", false, onEdt(frame::isBusy));

            search(frame, "Testville");
            check("a successful search is confirmed",
                    onEdt(frame::statusText).startsWith("Showing the weather for Testville"));
            checkEquals("the search is added to the history", 1, onEdt(frame::historySize));
            check("the current temperature is shown", showsText(frame, "20.0°C"));
            check("the conditions are shown", showsText(frame, "Partly cloudy"));
            check("the humidity is shown", showsText(frame, "60%"));
            check("the wind speed and direction are shown",
                    showsText(frame, "36.0 km/h") && showsText(frame, "Wind (W)"));
            check("the pressure is shown", showsText(frame, "1013 hPa"));
            check("sunrise and sunset are shown",
                    showsText(frame, "06:45") && showsText(frame, "16:42"));
            check("the place and local time are shown",
                    showsText(frame, "Testville, Testshire, Testland")
                            && showsText(frame,
                            "Local time: Tue, 27 Oct 2026 · 12:00  (Europe/Testville)"));
            check("five forecast cards are shown",
                    showsText(frame, "Today") && showsText(frame, "Wed")
                            && showsText(frame, "Sat"));
            check("a forecast high and low are shown", showsText(frame, "23°C / 11°C"));
            check("a precipitation chance is shown", showsText(frame, "Precipitation 30%"));
            checkEquals("midday gives the day background", TimeOfDay.DAY, onEdt(frame::timeOfDay));

            int callsBefore = server.geocodingCalls.get() + server.forecastCalls.get();
            onEdt(() -> {
                frame.chooseUnits(TemperatureUnit.FAHRENHEIT, WindSpeedUnit.MILES_PER_HOUR);
                return null;
            });
            check("Fahrenheit is applied", showsText(frame, "68.0°F"));
            check("miles per hour are applied", showsText(frame, "22.4 mph"));
            check("the forecast changes unit too", showsText(frame, "73°F / 52°F"));
            onEdt(() -> {
                frame.chooseUnits(TemperatureUnit.CELSIUS, WindSpeedUnit.METRES_PER_SECOND);
                return null;
            });
            check("metres per second are applied", showsText(frame, "10.0 m/s"));
            onEdt(() -> {
                frame.chooseUnits(TemperatureUnit.CELSIUS, WindSpeedUnit.KNOTS);
                return null;
            });
            check("knots are applied", showsText(frame, "19.4 kn"));
            checkEquals("changing units does not call the service again", callsBefore,
                    server.geocodingCalls.get() + server.forecastCalls.get());

            server.forecastBody = MockServer.FORECAST
                    .replace("2026-10-27T12:00", "2026-10-27T20:30")
                    .replace("\"is_day\":1", "\"is_day\":0");
            search(frame, "Testville");
            checkEquals("20:30 gives the night background", TimeOfDay.NIGHT,
                    onEdt(frame::timeOfDay));
            server.forecastBody = MockServer.FORECAST
                    .replace("2026-10-27T12:00", "2026-10-27T16:50")
                    .replace("\"is_day\":1", "\"is_day\":0");
            search(frame, "Testville");
            checkEquals("16:50 (after sunset) gives the sunset background", TimeOfDay.SUNSET,
                    onEdt(frame::timeOfDay));
            server.forecastBody = MockServer.FORECAST
                    .replace("2026-10-27T12:00", "2026-10-27T06:30");
            search(frame, "Testville");
            checkEquals("06:30 gives the sunrise background", TimeOfDay.SUNRISE,
                    onEdt(frame::timeOfDay));
            checkEquals("every successful search is in the history", 4, onEdt(frame::historySize));

            server.forecastStatus = 500;
            search(frame, "Testville");
            check("a failing service shows 'Weather service problem'",
                    onEdt(frame::statusText).startsWith("Weather service problem:"));
            checkEquals("the earlier result stays on screen after a failure", true,
                    onEdt(() -> frame.currentReport() != null));
            checkEquals("the failed request is not in the history", 4, onEdt(frame::historySize));
            server.forecastStatus = 200;

            search(frame, "48.85, 2.35");
            check("a coordinate search works",
                    onEdt(frame::statusText).contains("48.85°N, 2.35°E"));
            check("the history is stored in a file", Files.size(file) > 0);
        } finally {
            onEdt(() -> {
                frame.dispose();
                return null;
            });
        }
    }

    private static void search(WeatherFrame frame, String text) throws Exception {
        onEdt(() -> {
            frame.setSearchText(text);
            frame.submitSearch();
            return null;
        });
        long deadline = System.currentTimeMillis() + 15_000;
        while (onEdt(frame::isBusy) && System.currentTimeMillis() < deadline) {
            Thread.sleep(40);
        }
        onEdt(() -> null);                           // let the finished result be painted
    }

    private static boolean showsText(Component root, String text) throws Exception {
        return onEdt(() -> containsLabel(root, text));
    }

    private static boolean containsLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return true;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (containsLabel(child, text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <T> T onEdt(Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.call();
        }
        Object[] box = new Object[2];
        SwingUtilities.invokeAndWait(() -> {
            try {
                box[0] = task.call();
            } catch (Exception e) {
                box[1] = e;
            }
        });
        if (box[1] != null) {
            throw (Exception) box[1];
        }
        @SuppressWarnings("unchecked")
        T result = (T) box[0];
        return result;
    }

    // ---- the real service ---------------------------------------------------------------

    private static void liveApi() {
        note("# Live Open-Meteo API (needs an internet connection)");
        WeatherService service = new WeatherService();
        try {
            Location london = service.findLocation(new LocationQuery.City("London"));
            checkEquals("live: London is found", "London", london.name());
            checkEquals("live: London is in the United Kingdom", "United Kingdom",
                    london.country());
            check("live: London's latitude is plausible", Math.abs(london.latitude() - 51.5) < 0.5);

            WeatherReport report = service.fetchWeather(london);
            CurrentConditions now = report.current();
            check("live: temperature is plausible",
                    now.temperatureC() > -60 && now.temperatureC() < 60);
            check("live: humidity is 0-100",
                    now.humidityPercent() >= 0 && now.humidityPercent() <= 100);
            check("live: wind speed is not negative", now.windSpeedKmh() >= 0);
            check("live: wind direction is 0-360",
                    now.windDirectionDegrees() >= 0 && now.windDirectionDegrees() <= 360);
            check("live: pressure is plausible",
                    now.pressureHpa() > 850 && now.pressureHpa() < 1100);
            check("live: the conditions have a description",
                    !WeatherCode.describe(now.weatherCode()).startsWith("Unknown"));
            checkEquals("live: five forecast days", WeatherService.FORECAST_DAYS,
                    report.forecast().size());
            check("live: highs are not below lows",
                    report.forecast().stream().allMatch(d -> d.highC() >= d.lowC()));
            check("live: sunrise comes before sunset",
                    report.today().sunrise().isBefore(report.today().sunset()));
            checkEquals("live: time zone", "Europe/London", report.timezone());
            check("live: the report is current (within a day of today)",
                    Math.abs(Duration.between(now.time(), LocalDateTime.now()).toHours()) <= 24);

            Location texas = service.findLocation(new LocationQuery.City("Paris, Texas"));
            checkEquals("live: 'Paris, Texas' finds the Texas Paris", "Texas", texas.region());
            Location france = service.findLocation(new LocationQuery.City("Paris, France"));
            checkEquals("live: 'Paris, France' finds the French Paris", "France", france.country());

            Location tashkent = service.findLocation(new LocationQuery.City("Tashkent"));
            checkEquals("live: Tashkent is found", "Uzbekistan", tashkent.country());
            WeatherReport tashkentReport = service.fetchWeather(tashkent);
            checkEquals("live: Tashkent's time zone", "Asia/Tashkent", tashkentReport.timezone());

            WeatherReport byCoordinates =
                    service.fetchWeather(Location.ofCoordinates(-33.87, 151.21));
            checkEquals("live: Sydney coordinates give the Sydney time zone", "Australia/Sydney",
                    byCoordinates.timezone());
        } catch (WeatherException e) {
            check("live: no failure expected, but got " + e.type() + ": " + e.getMessage(), false);
        }
        expectFailure("live: a made-up place is not found", WeatherException.Type.NOT_FOUND,
                () -> service.findLocation(new LocationQuery.City("Qxzvwkjhgfd")));
    }

    // ---- a local stand-in for the Open-Meteo servers ------------------------------------

    /** Serves canned geocoding and forecast replies on a free local port. */
    private static final class MockServer {

        static final String GEOCODING = "{\"results\":[{\"id\":1,\"name\":\"Testville\","
                + "\"latitude\":12.34,\"longitude\":56.78,\"country\":\"Testland\","
                + "\"admin1\":\"Testshire\",\"timezone\":\"Europe/Testville\"}]}";
        static final String NO_RESULTS = "{\"generationtime_ms\":0.4}";

        static final String FORECAST = "{\"latitude\":12.34,\"longitude\":56.78,"
                + "\"timezone\":\"Europe/Testville\",\"current\":{\"time\":\"2026-10-27T12:00\","
                + "\"interval\":900,\"temperature_2m\":20.0,\"relative_humidity_2m\":60,"
                + "\"apparent_temperature\":18.5,\"is_day\":1,\"weather_code\":2,"
                + "\"surface_pressure\":1013.2,\"wind_speed_10m\":36.0,"
                + "\"wind_direction_10m\":270},\"daily\":{\"time\":[\"2026-10-27\",\"2026-10-28\","
                + "\"2026-10-29\",\"2026-10-30\",\"2026-10-31\"],\"weather_code\":[2,61,3,0,95],"
                + "\"temperature_2m_max\":[22.5,19.0,17.5,21.0,18.0],"
                + "\"temperature_2m_min\":[11.0,12.5,9.0,8.5,10.0],"
                + "\"precipitation_probability_max\":[30,null,10,0,80],"
                + "\"sunrise\":[\"2026-10-27T06:45\",\"2026-10-28T06:46\",\"2026-10-29T06:48\","
                + "\"2026-10-30T06:50\",\"2026-10-31T06:52\"],"
                + "\"sunset\":[\"2026-10-27T16:42\",\"2026-10-28T16:40\",\"2026-10-29T16:39\","
                + "\"2026-10-30T16:37\",\"2026-10-31T16:35\"]}}";

        final AtomicInteger geocodingCalls = new AtomicInteger();
        final AtomicInteger forecastCalls = new AtomicInteger();
        volatile String lastGeocodingQuery = "";
        volatile String lastForecastQuery = "";
        volatile String lastUserAgent;
        volatile int forecastStatus;
        volatile String forecastBody;
        volatile long forecastDelayMillis;

        private final HttpServer server;

        MockServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/geo", exchange -> {
                geocodingCalls.incrementAndGet();
                String query = exchange.getRequestURI().getRawQuery();
                lastGeocodingQuery = query;
                lastUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
                String name = URLDecoder.decode(query.replaceAll(".*name=([^&]*).*", "$1"),
                        StandardCharsets.UTF_8);
                reply(exchange, 200, name.startsWith("Nowhere") ? NO_RESULTS : GEOCODING, 0);
            });
            server.createContext("/forecast", exchange -> {
                forecastCalls.incrementAndGet();
                lastForecastQuery = exchange.getRequestURI().getRawQuery();
                reply(exchange, forecastStatus, forecastBody, forecastDelayMillis);
            });
            server.start();
            reset();
        }

        /** Back to a healthy server and fresh counters. */
        void reset() {
            forecastStatus = 200;
            forecastBody = FORECAST;
            forecastDelayMillis = 0;
            geocodingCalls.set(0);
            forecastCalls.set(0);
        }

        WeatherService service(Duration timeout) {
            String base = "http://localhost:" + server.getAddress().getPort();
            return new WeatherService(base + "/geo", base + "/forecast", timeout);
        }

        void stop() {
            server.stop(0);
        }

        private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status,
                                  String body, long delayMillis) throws IOException {
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}

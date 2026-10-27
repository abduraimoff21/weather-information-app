package weatherapp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Turns numbers and dates into the text shown in the window. All formatting uses English
 * conventions (a decimal point, English day names) so the display looks the same on every
 * computer.
 */
public final class DisplayFormat {

    private static final String[] COMPASS = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy '·' HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter HISTORY_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_MONTH =
            DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private DisplayFormat() {
    }

    /** A temperature converted from Celsius, such as "57.7°F". */
    public static String temperature(double celsius, TemperatureUnit unit, int decimals) {
        return number(unit.fromCelsius(celsius), decimals) + unit.symbol();
    }

    /** A wind speed converted from km/h, such as "10.5 mph". */
    public static String windSpeed(double kmh, WindSpeedUnit unit) {
        return number(unit.fromKilometresPerHour(kmh), 1) + " " + unit.symbol();
    }

    /** The compass point for a wind direction in degrees, such as 261 gives "W". */
    public static String compassPoint(int degrees) {
        int normalised = ((degrees % 360) + 360) % 360;
        return COMPASS[(int) Math.round(normalised / 22.5) % COMPASS.length];
    }

    public static String percent(int value) {
        return value + "%";
    }

    public static String pressure(double hectopascals) {
        return Math.round(hectopascals) + " hPa";
    }

    /** Date and time at the searched place, such as "Tue, 27 Oct 2026 · 16:45". */
    public static String dateTime(LocalDateTime time) {
        return DATE_TIME.format(time);
    }

    /** The clock time, such as "06:45"; "--:--" when unknown. */
    public static String clockTime(LocalDateTime time) {
        return time == null ? "--:--" : TIME.format(time);
    }

    /** The timestamp shown in the history, such as "27 Oct 2026, 21:35:12". */
    public static String historyTime(LocalDateTime time) {
        return HISTORY_TIME.format(time);
    }

    /** "Today" for the current day, otherwise the weekday, such as "Wed". */
    public static String dayName(LocalDate date, LocalDate today) {
        return date.equals(today) ? "Today" : WEEKDAY.format(date);
    }

    public static String dayAndMonth(LocalDate date) {
        return DAY_MONTH.format(date);
    }

    /** Formats with a fixed number of decimals and avoids the odd result "-0". */
    private static String number(double value, int decimals) {
        String text = String.format(Locale.ENGLISH, "%." + decimals + "f", value);
        return text.matches("-0(\\.0+)?") ? text.substring(1) : text;
    }
}

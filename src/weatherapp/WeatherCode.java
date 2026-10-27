package weatherapp;

import java.util.Map;

/**
 * Translates the numeric weather codes of the API (WMO "weather interpretation codes")
 * into text and into the icon that represents them.
 */
public final class WeatherCode {

    private record Info(String description, IconType icon) { }

    private static final Info UNKNOWN = new Info("Unknown conditions", IconType.CLOUDY);

    private static final Map<Integer, Info> CODES = Map.ofEntries(
            entry(0, "Clear sky", IconType.CLEAR),
            entry(1, "Mainly clear", IconType.CLEAR),
            entry(2, "Partly cloudy", IconType.PARTLY_CLOUDY),
            entry(3, "Overcast", IconType.CLOUDY),
            entry(45, "Fog", IconType.FOG),
            entry(48, "Rime fog", IconType.FOG),
            entry(51, "Light drizzle", IconType.DRIZZLE),
            entry(53, "Moderate drizzle", IconType.DRIZZLE),
            entry(55, "Dense drizzle", IconType.DRIZZLE),
            entry(56, "Icy drizzle", IconType.DRIZZLE),
            entry(57, "Heavy icy drizzle", IconType.DRIZZLE),
            entry(61, "Slight rain", IconType.RAIN),
            entry(63, "Moderate rain", IconType.RAIN),
            entry(65, "Heavy rain", IconType.RAIN),
            entry(66, "Light freezing rain", IconType.RAIN),
            entry(67, "Heavy freezing rain", IconType.RAIN),
            entry(71, "Slight snowfall", IconType.SNOW),
            entry(73, "Moderate snowfall", IconType.SNOW),
            entry(75, "Heavy snowfall", IconType.SNOW),
            entry(77, "Snow grains", IconType.SNOW),
            entry(80, "Light showers", IconType.RAIN),
            entry(81, "Showers", IconType.RAIN),
            entry(82, "Heavy showers", IconType.RAIN),
            entry(85, "Slight snow showers", IconType.SNOW),
            entry(86, "Heavy snow showers", IconType.SNOW),
            entry(95, "Thunderstorm", IconType.THUNDERSTORM),
            entry(96, "Thunderstorm, hail", IconType.THUNDERSTORM),
            entry(99, "Thunder, heavy hail", IconType.THUNDERSTORM));

    private WeatherCode() {
    }

    private static Map.Entry<Integer, Info> entry(int code, String description, IconType icon) {
        return Map.entry(code, new Info(description, icon));
    }

    /** A short English description such as "Slight rain". */
    public static String describe(int code) {
        return CODES.getOrDefault(code, UNKNOWN).description();
    }

    /** The icon that represents the code. */
    public static IconType iconFor(int code) {
        return CODES.getOrDefault(code, UNKNOWN).icon();
    }
}

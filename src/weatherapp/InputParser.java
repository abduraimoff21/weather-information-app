package weatherapp;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks what the user typed in the search box and turns it into a {@link LocationQuery}.
 *
 * <p>Validating here means that obviously wrong input is rejected immediately, with a helpful
 * message, instead of being sent to the weather service.
 */
public final class InputParser {

    private static final int MAX_LENGTH = 100;

    /** Two decimal numbers separated by a comma, a semicolon or a space: "48.85, 2.35". */
    private static final Pattern COORDINATES = Pattern.compile(
            "^([+-]?\\d{1,3}(?:\\.\\d+)?)\\s*(?:[,;]|\\s)\\s*([+-]?\\d{1,3}(?:\\.\\d+)?)$");

    /** A single number such as "48.85", which is neither a place nor a coordinate pair. */
    private static final Pattern SINGLE_NUMBER = Pattern.compile("^[+-]?\\d+(?:[.,]\\d+)?$");

    /** Letters (any alphabet), digits, spaces and the punctuation used in place names. */
    private static final Pattern PLACE_NAME =
            Pattern.compile("^[\\p{L}\\p{M}\\d][\\p{L}\\p{M}\\d\\s'’.,()\\-]*$");

    private static final String COORDINATE_HINT =
            "For coordinates, enter latitude and longitude, for example 48.85, 2.35.";

    private InputParser() {
    }

    /**
     * Validates the text and classifies it.
     *
     * @throws WeatherException of type INVALID_INPUT if the text cannot be a location
     */
    public static LocationQuery parse(String input) throws WeatherException {
        String text = input == null ? "" : input.trim().replaceAll("\\s+", " ");
        if (text.isEmpty()) {
            throw invalid("Please enter a city name or coordinates first.");
        }
        if (text.length() > MAX_LENGTH) {
            throw invalid("That is too long for a location. Use at most " + MAX_LENGTH
                    + " characters.");
        }

        Matcher coordinates = COORDINATES.matcher(text);
        if (coordinates.matches()) {
            return parseCoordinates(coordinates);
        }
        if (SINGLE_NUMBER.matcher(text).matches()) {
            throw invalid("A single number is not a place. " + COORDINATE_HINT);
        }
        if (!PLACE_NAME.matcher(text).matches()) {
            throw invalid("Use only letters, numbers, spaces and hyphens in a city name, "
                    + "or coordinates like 48.85, 2.35.");
        }
        if (text.codePoints().filter(Character::isLetterOrDigit).count() < 2) {
            throw invalid("A city name needs at least two letters.");
        }
        return new LocationQuery.City(text);
    }

    private static LocationQuery parseCoordinates(Matcher matcher) throws WeatherException {
        double latitude = Double.parseDouble(matcher.group(1));
        double longitude = Double.parseDouble(matcher.group(2));
        if (latitude < -90 || latitude > 90) {
            throw invalid(String.format(Locale.ENGLISH,
                    "Latitude must be between -90 and 90 degrees (you entered %s).",
                    matcher.group(1)));
        }
        if (longitude < -180 || longitude > 180) {
            throw invalid(String.format(Locale.ENGLISH,
                    "Longitude must be between -180 and 180 degrees (you entered %s).",
                    matcher.group(2)));
        }
        return new LocationQuery.Coordinates(latitude, longitude);
    }

    private static WeatherException invalid(String message) {
        return new WeatherException(WeatherException.Type.INVALID_INPUT, message);
    }
}

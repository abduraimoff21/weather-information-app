package weatherapp;

/**
 * A problem that can be explained to the user in plain language.
 *
 * <p>Every failure the app expects (bad input, an unknown place, no internet connection,
 * a misbehaving weather service) is reported with this one checked exception, so the
 * GUI has a single place where it turns errors into messages.
 */
public class WeatherException extends Exception {

    /** The kind of problem; the GUI uses it to choose a short title for the message. */
    public enum Type {
        INVALID_INPUT("Invalid input"),
        NOT_FOUND("Location not found"),
        NETWORK("Connection problem"),
        SERVICE("Weather service problem");

        private final String title;

        Type(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private final Type type;

    public WeatherException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public WeatherException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public Type type() {
        return type;
    }
}

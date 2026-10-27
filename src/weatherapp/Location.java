package weatherapp;

import java.util.Locale;
import java.util.Objects;

/**
 * A place on Earth: where the weather is asked for.
 *
 * @param name      the place name, or the formatted coordinates for a coordinate search
 * @param region    the state or province (may be empty)
 * @param country   the country (may be empty)
 * @param latitude  degrees north (negative is south)
 * @param longitude degrees east (negative is west)
 */
public record Location(String name, String region, String country,
                       double latitude, double longitude) {

    public Location {
        name = Objects.requireNonNullElse(name, "");
        region = Objects.requireNonNullElse(region, "");
        country = Objects.requireNonNullElse(country, "");
    }

    /** A location known only by its coordinates, named after them. */
    public static Location ofCoordinates(double latitude, double longitude) {
        String name = String.format(Locale.ENGLISH, "%.2f°%s, %.2f°%s",
                Math.abs(latitude), latitude >= 0 ? "N" : "S",
                Math.abs(longitude), longitude >= 0 ? "E" : "W");
        return new Location(name, "", "", latitude, longitude);
    }

    /** Text for the screen and the history, such as "Paris, Île-de-France, France". */
    public String label() {
        StringBuilder label = new StringBuilder(name);
        if (!region.isBlank() && !region.equalsIgnoreCase(name)) {
            label.append(", ").append(region);
        }
        if (!country.isBlank()) {
            label.append(", ").append(country);
        }
        return label.toString();
    }
}

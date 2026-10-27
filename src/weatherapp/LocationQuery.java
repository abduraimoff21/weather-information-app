package weatherapp;

/** What the user asked for: either a place name or a pair of coordinates. */
public sealed interface LocationQuery {

    /** A place name such as "Paris" or "Paris, France". */
    record City(String name) implements LocationQuery { }

    /** A latitude and longitude in decimal degrees. */
    record Coordinates(double latitude, double longitude) implements LocationQuery { }
}

package weatherapp;

/**
 * Units the wind speed can be shown in. Speeds are stored in kilometres per hour (the unit
 * the weather service returns) and converted only when they are displayed.
 */
public enum WindSpeedUnit {
    KILOMETRES_PER_HOUR("km/h", 1.0),
    MILES_PER_HOUR("mph", 0.621371),
    METRES_PER_SECOND("m/s", 1.0 / 3.6),
    KNOTS("kn", 0.539957);

    private final String symbol;
    private final double perKilometrePerHour;

    WindSpeedUnit(String symbol, double perKilometrePerHour) {
        this.symbol = symbol;
        this.perKilometrePerHour = perKilometrePerHour;
    }

    /** Converts a speed given in kilometres per hour to this unit. */
    public double fromKilometresPerHour(double kmh) {
        return kmh * perKilometrePerHour;
    }

    public String symbol() {
        return symbol;
    }
}

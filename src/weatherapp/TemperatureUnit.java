package weatherapp;

/**
 * Units the temperature can be shown in. The app stores every temperature in Celsius
 * (the unit the weather service returns) and converts only when it is displayed.
 */
public enum TemperatureUnit {
    CELSIUS("°C") {
        @Override
        public double fromCelsius(double celsius) {
            return celsius;
        }
    },
    FAHRENHEIT("°F") {
        @Override
        public double fromCelsius(double celsius) {
            return celsius * 9.0 / 5.0 + 32.0;
        }
    };

    private final String symbol;

    TemperatureUnit(String symbol) {
        this.symbol = symbol;
    }

    /** Converts a temperature given in degrees Celsius to this unit. */
    public abstract double fromCelsius(double celsius);

    public String symbol() {
        return symbol;
    }
}

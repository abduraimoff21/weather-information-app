package weatherapp;

import java.util.List;

/**
 * Everything the app shows for one search: the place, the weather now and the forecast.
 *
 * @param location the place the report is about
 * @param timezone the place's time zone name, such as "Europe/London"
 * @param current  the weather right now
 * @param forecast the daily forecast; the first entry is today
 */
public record WeatherReport(Location location, String timezone, CurrentConditions current,
                            List<DailyForecast> forecast) {

    public WeatherReport {
        forecast = List.copyOf(forecast);
    }

    /** Today's forecast, which holds today's sunrise and sunset. */
    public DailyForecast today() {
        return forecast.get(0);
    }
}

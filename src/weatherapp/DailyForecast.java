package weatherapp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The forecast for one day, in the location's own time zone.
 *
 * @param date                       the day
 * @param weatherCode                the API's weather code, see {@link WeatherCode}
 * @param highC                      the highest temperature, in degrees Celsius
 * @param lowC                       the lowest temperature, in degrees Celsius
 * @param precipitationChancePercent the chance of rain or snow, or -1 if unknown
 * @param sunrise                    the time of sunrise, or null if unknown
 * @param sunset                     the time of sunset, or null if unknown
 */
public record DailyForecast(LocalDate date, int weatherCode, double highC, double lowC,
                            int precipitationChancePercent,
                            LocalDateTime sunrise, LocalDateTime sunset) {

    /** Marks a precipitation chance that the service did not provide. */
    public static final int UNKNOWN_CHANCE = -1;
}

package weatherapp;

import java.time.LocalDateTime;

/**
 * The weather right now at a location. Temperatures are in degrees Celsius and the wind
 * speed in km/h; {@link TemperatureUnit} and {@link WindSpeedUnit} convert them for display.
 *
 * @param time                 the observation time, in the location's own time zone
 * @param temperatureC         air temperature
 * @param feelsLikeC           apparent ("feels like") temperature
 * @param humidityPercent      relative humidity, 0 to 100
 * @param windSpeedKmh         wind speed
 * @param windDirectionDegrees where the wind comes from, 0 to 360 (0 is north)
 * @param pressureHpa          air pressure at the surface
 * @param weatherCode          the API's weather code, see {@link WeatherCode}
 * @param daytime              true if the sun is up, which chooses the sun or moon icon
 */
public record CurrentConditions(LocalDateTime time, double temperatureC, double feelsLikeC,
                                int humidityPercent, double windSpeedKmh,
                                int windDirectionDegrees, double pressureHpa,
                                int weatherCode, boolean daytime) {
}

package weatherapp;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * The part of the day, which decides the background of the window.
 *
 * <p>When the sunrise and sunset of the searched place are known, the phase follows the sun:
 * the sky already glows before the sun rises and stays coloured for a while after it sets.
 * Without them (before the first search) the computer's clock is used.
 */
public enum TimeOfDay {
    SUNRISE, DAY, SUNSET, NIGHT;

    private static final Duration BEFORE_SUNRISE = Duration.ofMinutes(45);
    private static final Duration AFTER_SUNRISE = Duration.ofMinutes(60);
    private static final Duration BEFORE_SUNSET = Duration.ofMinutes(75);
    private static final Duration AFTER_SUNSET = Duration.ofMinutes(45);

    /**
     * The phase at a moment, given that day's sunrise and sunset in the same time zone.
     * If either is unknown (or they make no sense) the clock time is used instead.
     */
    public static TimeOfDay at(LocalDateTime now, LocalDateTime sunrise, LocalDateTime sunset) {
        if (sunrise == null || sunset == null || !sunrise.isBefore(sunset)) {
            return fromClock(now.toLocalTime());
        }
        if (within(now, sunrise.minus(BEFORE_SUNRISE), sunrise.plus(AFTER_SUNRISE))) {
            return SUNRISE;
        }
        if (within(now, sunset.minus(BEFORE_SUNSET), sunset.plus(AFTER_SUNSET))) {
            return SUNSET;
        }
        return now.isAfter(sunrise) && now.isBefore(sunset) ? DAY : NIGHT;
    }

    /** A rough phase from the hour alone: 05:00-07:00 sunrise, 17:00-19:30 sunset. */
    public static TimeOfDay fromClock(LocalTime time) {
        int minutes = time.getHour() * 60 + time.getMinute();
        if (minutes >= 5 * 60 && minutes < 7 * 60) {
            return SUNRISE;
        }
        if (minutes >= 7 * 60 && minutes < 17 * 60) {
            return DAY;
        }
        if (minutes >= 17 * 60 && minutes < 19 * 60 + 30) {
            return SUNSET;
        }
        return NIGHT;
    }

    /** True if start is not after the moment and the moment is before end. */
    private static boolean within(LocalDateTime moment, LocalDateTime start, LocalDateTime end) {
        return !moment.isBefore(start) && moment.isBefore(end);
    }
}

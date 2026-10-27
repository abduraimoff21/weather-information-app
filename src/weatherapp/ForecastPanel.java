package weatherapp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** The row of day cards that shows the short-term forecast. */
class ForecastPanel extends RoundedPanel {

    private final JPanel row = new JPanel(new GridLayout(1, WeatherService.FORECAST_DAYS, 10, 0));

    ForecastPanel() {
        super(new BorderLayout(0, 8), Ui.CARD);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        row.setOpaque(false);
        add(Ui.label("5-day forecast", Font.BOLD, 16, Ui.TEXT), BorderLayout.NORTH);
        add(row, BorderLayout.CENTER);
        row.add(placeholder());
    }

    private static JLabel placeholder() {
        JLabel label = Ui.label("The forecast appears here after your first search.",
                Font.PLAIN, 14, Ui.TEXT_MUTED);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    /** Replaces the cards with the given forecast, in the chosen temperature unit. */
    void show(List<DailyForecast> forecast, TemperatureUnit unit) {
        row.removeAll();
        row.setLayout(new GridLayout(1, Math.max(1, forecast.size()), 10, 0));
        LocalDate today = forecast.get(0).date();
        for (DailyForecast day : forecast) {
            row.add(new DayCard(day, today, unit));
        }
        row.revalidate();
        row.repaint();
    }

    /** One day: weekday, date, icon, high and low, and the chance of precipitation. */
    private static final class DayCard extends RoundedPanel {

        DayCard(DailyForecast day, LocalDate today, TemperatureUnit unit) {
            super(new BorderLayout(), new Color(255, 255, 255, 30));
            setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));

            JPanel heading = new JPanel();
            heading.setOpaque(false);
            heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
            heading.add(centered(DisplayFormat.dayName(day.date(), today), Font.BOLD, 16,
                    Ui.TEXT));
            heading.add(centered(DisplayFormat.dayAndMonth(day.date()), Font.PLAIN, 12,
                    Ui.TEXT_MUTED));

            JLabel picture = new JLabel(
                    new WeatherIcon(WeatherCode.iconFor(day.weatherCode()), false, 50));
            picture.setHorizontalAlignment(SwingConstants.CENTER);
            picture.setToolTipText(WeatherCode.describe(day.weatherCode()));

            JPanel numbers = new JPanel();
            numbers.setOpaque(false);
            numbers.setLayout(new BoxLayout(numbers, BoxLayout.Y_AXIS));
            numbers.add(centered(DisplayFormat.temperature(day.highC(), unit, 0) + " / "
                    + DisplayFormat.temperature(day.lowC(), unit, 0), Font.BOLD, 14, Ui.TEXT));
            numbers.add(centered(WeatherCode.describe(day.weatherCode()), Font.PLAIN, 12,
                    Ui.TEXT_MUTED));
            numbers.add(centered(day.precipitationChancePercent() == DailyForecast.UNKNOWN_CHANCE
                    ? " " : "Precipitation " + DisplayFormat.percent(
                            day.precipitationChancePercent()), Font.PLAIN, 12, Ui.TEXT_MUTED));

            add(heading, BorderLayout.NORTH);
            add(picture, BorderLayout.CENTER);
            add(numbers, BorderLayout.SOUTH);
        }

        private static JLabel centered(String text, int style, int size, Color color) {
            JLabel label = Ui.label(text, style, size, color);
            label.setAlignmentX(CENTER_ALIGNMENT);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            return label;
        }
    }
}

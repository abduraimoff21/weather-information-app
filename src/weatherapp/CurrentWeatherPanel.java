package weatherapp;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The large card with the weather right now: place, local time, icon, temperature,
 * conditions and a grid of details. Before the first search it shows a short welcome note.
 */
class CurrentWeatherPanel extends RoundedPanel {

    private static final String WELCOME = "welcome";
    private static final String WEATHER = "weather";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    private final JLabel place = Ui.label(" ", Font.BOLD, 24, Ui.TEXT);
    private final JLabel localTime = Ui.label(" ", Font.PLAIN, 14, Ui.TEXT_MUTED);
    private final JLabel icon = new JLabel();
    private final JLabel temperature = Ui.label(" ", Font.BOLD, 48, Ui.TEXT);
    private final JLabel conditions = Ui.label(" ", Font.PLAIN, 18, Ui.TEXT);

    private final DetailTile feelsLike = new DetailTile("Feels like");
    private final DetailTile humidity = new DetailTile("Humidity");
    private final DetailTile wind = new DetailTile("Wind");
    private final DetailTile pressure = new DetailTile("Pressure");
    private final DetailTile sunrise = new DetailTile("Sunrise");
    private final DetailTile sunset = new DetailTile("Sunset");

    CurrentWeatherPanel() {
        super(new BorderLayout(), Ui.CARD);
        setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));
        content.setOpaque(false);
        content.add(buildWelcome(), WELCOME);
        content.add(buildWeather(), WEATHER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildWelcome() {
        JPanel welcome = new JPanel();
        welcome.setOpaque(false);
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));

        JLabel picture = new JLabel(new WeatherIcon(IconType.PARTLY_CLOUDY, false, 96));
        picture.setAlignmentX(CENTER_ALIGNMENT);

        JLabel text = new JLabel("<html><div style='text-align:center'>"
                + "<span style='font-size:20px'><b>Welcome to the Weather Information App"
                + "</b></span><br><br>Type a city (for example <b>Paris, France</b>) or "
                + "coordinates (for example <b>48.85, 2.35</b>)<br>and press <b>Search</b> to "
                + "see the current weather and the 5-day forecast.</div></html>");
        text.setFont(Ui.font(Font.PLAIN, 15));
        text.setForeground(Ui.TEXT);
        text.setAlignmentX(CENTER_ALIGNMENT);

        welcome.add(Box.createVerticalGlue());
        welcome.add(picture);
        welcome.add(Box.createVerticalStrut(12));
        welcome.add(text);
        welcome.add(Box.createVerticalGlue());
        return welcome;
    }

    private JPanel buildWeather() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(place);
        header.add(localTime);

        // Icon and temperature on the left, the six detail tiles on the right.
        JPanel readings = new JPanel(new BorderLayout(14, 0));
        readings.setOpaque(false);
        JPanel temperatureColumn = new JPanel();
        temperatureColumn.setOpaque(false);
        temperatureColumn.setLayout(new BoxLayout(temperatureColumn, BoxLayout.Y_AXIS));
        temperatureColumn.add(Box.createVerticalGlue());
        temperatureColumn.add(temperature);
        temperatureColumn.add(conditions);
        temperatureColumn.add(Box.createVerticalGlue());
        readings.add(icon, BorderLayout.WEST);
        readings.add(temperatureColumn, BorderLayout.CENTER);

        JPanel details = new JPanel(new GridLayout(2, 3, 10, 10));
        details.setOpaque(false);
        for (DetailTile tile : new DetailTile[] {feelsLike, humidity, wind,
                pressure, sunrise, sunset}) {
            details.add(tile);
        }

        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        body.add(readings, BorderLayout.WEST);
        body.add(details, BorderLayout.CENTER);

        JPanel weather = new JPanel(new BorderLayout(0, 10));
        weather.setOpaque(false);
        weather.add(header, BorderLayout.NORTH);
        weather.add(body, BorderLayout.CENTER);
        return weather;
    }

    /** Shows a report in the chosen units. */
    void show(WeatherReport report, TemperatureUnit temperatureUnit, WindSpeedUnit windUnit) {
        CurrentConditions now = report.current();
        DailyForecast today = report.today();

        place.setText(report.location().label());
        localTime.setText("Local time: " + DisplayFormat.dateTime(now.time())
                + (report.timezone().isEmpty() ? "" : "  (" + report.timezone() + ")"));
        icon.setIcon(new WeatherIcon(WeatherCode.iconFor(now.weatherCode()),
                !now.daytime(), 96));
        temperature.setText(DisplayFormat.temperature(now.temperatureC(), temperatureUnit, 1));
        conditions.setText(WeatherCode.describe(now.weatherCode()));

        feelsLike.setValue(DisplayFormat.temperature(now.feelsLikeC(), temperatureUnit, 1));
        humidity.setValue(DisplayFormat.percent(now.humidityPercent()));
        wind.setCaption("Wind (" + DisplayFormat.compassPoint(now.windDirectionDegrees()) + ")");
        wind.setValue(DisplayFormat.windSpeed(now.windSpeedKmh(), windUnit));
        pressure.setValue(DisplayFormat.pressure(now.pressureHpa()));
        sunrise.setValue(DisplayFormat.clockTime(today.sunrise()));
        sunset.setValue(DisplayFormat.clockTime(today.sunset()));
        cards.show(content, WEATHER);
    }

    /** One labelled value in the details grid. */
    private static final class DetailTile extends RoundedPanel {

        private final JLabel caption;
        private final JLabel value = Ui.label(" ", Font.BOLD, 17, Ui.TEXT);

        DetailTile(String captionText) {
            super(new BorderLayout(), new Color(255, 255, 255, 28));
            caption = Ui.label(captionText, Font.PLAIN, 13, Ui.TEXT_MUTED);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            add(caption, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        void setCaption(String text) {
            caption.setText(text);
        }

        void setValue(String text) {
            value.setText(text);
        }
    }
}

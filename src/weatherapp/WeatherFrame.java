package weatherapp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * The main window. It owns the controls, starts the searches and passes the results on to
 * the panels that display them.
 *
 * <p>Searches run on a background thread ({@link SwingWorker}) so the window never freezes
 * while the network is slow; the result is shown back on the event thread. The latest
 * report is kept in memory, so switching units just redraws it without asking the service
 * again.
 */
public final class WeatherFrame extends JFrame {

    private static final String ATTRIBUTION_URL = "https://open-meteo.com/";

    private final WeatherService service;
    private final SearchHistory history;

    private final BackgroundPanel background = new BackgroundPanel();
    private final HintTextField searchField =
            new HintTextField("City or coordinates, e.g. Paris, France  or  48.85, 2.35", 30);
    private final AccentButton searchButton = new AccentButton("Search");
    private final StatusBar statusBar = new StatusBar();
    private final CurrentWeatherPanel currentPanel = new CurrentWeatherPanel();
    private final ForecastPanel forecastPanel = new ForecastPanel();
    private final HistoryPanel historyPanel =
            new HistoryPanel(this::lookUpAgain, this::clearHistory);

    private TemperatureUnit temperatureUnit = TemperatureUnit.CELSIUS;
    private WindSpeedUnit windUnit = WindSpeedUnit.KILOMETRES_PER_HOUR;

    private final SegmentedControl<TemperatureUnit> temperatureControl = new SegmentedControl<>(
            List.of(TemperatureUnit.values()), TemperatureUnit::symbol, unit -> {
                temperatureUnit = unit;
                redraw();
            });
    private final SegmentedControl<WindSpeedUnit> windControl = new SegmentedControl<>(
            List.of(WindSpeedUnit.values()), WindSpeedUnit::symbol, unit -> {
                windUnit = unit;
                redraw();
            });

    /** The latest successful result; null until the first search succeeds. */
    private WeatherReport report;
    private boolean busy;

    public WeatherFrame(WeatherService service, SearchHistory history) {
        super("Weather Information App");
        this.service = service;
        this.history = history;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(new WeatherIcon(IconType.PARTLY_CLOUDY, false, 64).toImage());
        setContentPane(background);
        background.setLayout(new BorderLayout(0, 12));
        background.setBorder(BorderFactory.createEmptyBorder(16, 18, 10, 18));

        background.add(buildControls(), BorderLayout.NORTH);
        background.add(buildCenter(), BorderLayout.CENTER);
        background.add(buildFooter(), BorderLayout.SOUTH);

        searchButton.setMnemonic('S');
        searchButton.setToolTipText("Look up the weather (Enter)");
        searchField.setToolTipText("Type a city name, or latitude and longitude");
        searchField.addActionListener(e -> submitSearch());
        searchButton.addActionListener(e -> submitSearch());
        getRootPane().setDefaultButton(searchButton);

        background.setTimeOfDay(TimeOfDay.fromClock(LocalDateTime.now().toLocalTime()));
        historyPanel.refresh(history.entries());
        statusBar.showInfo("Enter a city name or coordinates and press Search.");

        setMinimumSize(new Dimension(960, 740));
        setSize(1000, 770);
        setLocationRelativeTo(null);
    }

    // ---- building the window ------------------------------------------------------------

    private JPanel buildControls() {
        JPanel controls = new RoundedPanel(new GridBagLayout(), Ui.CARD);
        controls.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 10);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 10);
        controls.add(searchField, c);

        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 10, 0);
        controls.add(searchButton, c);

        JPanel units = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        units.setOpaque(false);
        units.add(Ui.label("Temperature", Font.PLAIN, 14, Ui.TEXT_MUTED));
        units.add(temperatureControl);
        units.add(Box.createHorizontalStrut(14));
        units.add(Ui.label("Wind speed", Font.PLAIN, 14, Ui.TEXT_MUTED));
        units.add(windControl);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 0, 0);
        controls.add(units, c);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(Ui.label("Weather Information", Font.BOLD, 28, Ui.TEXT));
        heading.add(Ui.label("Current conditions and a 5-day forecast for any place",
                Font.PLAIN, 14, new Color(232, 238, 248)));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        JPanel stack = new JPanel(new BorderLayout(0, 8));
        stack.setOpaque(false);
        stack.add(controls, BorderLayout.NORTH);
        stack.add(statusBar, BorderLayout.SOUTH);
        top.add(heading, BorderLayout.NORTH);
        top.add(stack, BorderLayout.CENTER);
        return top;
    }

    private JPanel buildCenter() {
        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;

        c.gridy = 0;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 12, 0);
        main.add(currentPanel, c);

        c.gridy = 1;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 0, 0);
        main.add(forecastPanel, c);

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(main, BorderLayout.CENTER);
        center.add(historyPanel, BorderLayout.EAST);
        return center;
    }

    private JPanel buildFooter() {
        JLabel credit = Ui.label("Weather data by Open-Meteo.com (CC BY 4.0)", Font.PLAIN, 12,
                Ui.TEXT);
        credit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        credit.setToolTipText("Open " + ATTRIBUTION_URL);
        credit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openInBrowser(ATTRIBUTION_URL);
            }
        });
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(credit, BorderLayout.EAST);
        return footer;
    }

    private static void openInBrowser(String url) {
        boolean canBrowse = Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        if (canBrowse) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException e) {
                System.err.println("Could not open the browser: " + e.getMessage());
            }
        }
    }

    // ---- searching ----------------------------------------------------------------------

    /** Validates the text in the search box and, if it is a usable location, looks it up. */
    public void submitSearch() {
        if (busy) {
            return;
        }
        LocationQuery query;
        try {
            query = InputParser.parse(searchField.getText());
        } catch (WeatherException e) {
            statusBar.showError(e);
            return;
        }
        String label = query instanceof LocationQuery.City city
                ? "\"" + city.name() + "\"" : "the coordinates";
        lookUp("Looking up " + label + "…",
                () -> service.fetchWeather(service.findLocation(query)));
    }

    /** Shows a past search again; its coordinates are already known, so no geocoding is needed. */
    private void lookUpAgain(SearchHistory.Entry entry) {
        if (busy) {
            return;
        }
        searchField.setText(entry.location().name());
        lookUp("Looking up " + entry.location().label() + " again…",
                () -> service.fetchWeather(entry.location()));
    }

    private void clearHistory() {
        history.clear();
        historyPanel.refresh(history.entries());
    }

    /** Runs a weather request off the event thread and shows its outcome when it finishes. */
    private void lookUp(String progressMessage, Callable<WeatherReport> request) {
        setBusy(true);
        statusBar.showInfo(progressMessage);
        new SwingWorker<WeatherReport, Void>() {
            @Override
            protected WeatherReport doInBackground() throws Exception {
                return request.call();
            }

            @Override
            protected void done() {
                try {
                    show(get());
                } catch (ExecutionException e) {
                    showFailure(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        searchButton.setEnabled(!busy);
        searchButton.setText(busy ? "Searching…" : "Search");
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    // ---- showing results ----------------------------------------------------------------

    private void show(WeatherReport newReport) {
        report = newReport;
        history.record(newReport.location(), LocalDateTime.now());
        historyPanel.refresh(history.entries());
        redraw();
        statusBar.showInfo("Showing the weather for " + newReport.location().label()
                + ". Updated at " + DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)
                        .format(LocalDateTime.now()) + ".");
    }

    private void showFailure(Throwable cause) {
        if (cause instanceof WeatherException problem) {
            statusBar.showError(problem);
        } else {
            cause.printStackTrace();            // a bug: keep the details for the developer
            statusBar.showUnexpectedError();
        }
    }

    /** Draws the latest report in the selected units and picks the matching background. */
    private void redraw() {
        if (report == null) {
            return;
        }
        currentPanel.show(report, temperatureUnit, windUnit);
        forecastPanel.show(report.forecast(), temperatureUnit);
        background.setTimeOfDay(TimeOfDay.at(report.current().time(),
                report.today().sunrise(), report.today().sunset()));
    }

    // ---- hooks used by the tests and the screenshot tool --------------------------------

    public void setSearchText(String text) {
        searchField.setText(text);
    }

    public void chooseUnits(TemperatureUnit temperature, WindSpeedUnit wind) {
        temperatureControl.select(temperature);
        windControl.select(wind);
    }

    public boolean isBusy() {
        return busy;
    }

    public String statusText() {
        return statusBar.text();
    }

    public TimeOfDay timeOfDay() {
        return background.timeOfDay();
    }

    public int historySize() {
        return historyPanel.entryCount();
    }

    public WeatherReport currentReport() {
        return report;
    }
}

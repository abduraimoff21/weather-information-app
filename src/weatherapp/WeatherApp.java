package weatherapp;

import java.nio.file.Path;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of the Weather Information App.
 *
 * <p>Usage: {@code java -cp out weatherapp.WeatherApp}. The search history is stored in
 * {@code .weather-information-app/history.tsv} in the user's home folder; the system property
 * {@code weatherapp.history} can point to a different file.
 */
public final class WeatherApp {

    private static final String HISTORY_PROPERTY = "weatherapp.history";
    private static final int HISTORY_CAPACITY = 20;

    private WeatherApp() {
    }

    public static void main(String[] args) {
        // Swing components must be created and used on the event dispatch thread.
        SwingUtilities.invokeLater(() -> {
            useNativeLookAndFeel();
            SearchHistory history = new SearchHistory(historyFile(), HISTORY_CAPACITY);
            new WeatherFrame(new WeatherService(), history).setVisible(true);
        });
    }

    private static Path historyFile() {
        String custom = System.getProperty(HISTORY_PROPERTY);
        if (custom != null && !custom.isBlank()) {
            return Path.of(custom);
        }
        return Path.of(System.getProperty("user.home"), ".weather-information-app",
                "history.tsv");
    }

    static void useNativeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // The default look and feel works too, so a failure here is harmless.
        }
    }
}

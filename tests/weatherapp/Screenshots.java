package weatherapp;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * Starts the real application window, drives it like a user would and saves screenshots of
 * the screen. It talks to the real Open-Meteo service, so it needs an internet connection.
 *
 * <p>Usage: {@code java -cp out weatherapp.Screenshots <output folder>}
 */
public final class Screenshots {

    /** Cities spread around the world, so that every part of the day exists somewhere. */
    private static final List<String> CITIES = List.of("Honolulu", "Anchorage", "Los Angeles",
            "Mexico City", "New York", "Reykjavik", "London", "Cairo", "Moscow", "Nairobi",
            "Dubai", "Tashkent", "Mumbai", "Dhaka", "Bangkok", "Tokyo", "Sydney", "Auckland",
            "Suva", "Lisbon", "Singapore", "Johannesburg", "Santiago", "Vancouver");

    private static Path outDir;

    public static void main(String[] args) throws Exception {
        outDir = Path.of(args.length > 0 ? args[0] : "docs/screenshots");
        Files.createDirectories(outDir);
        Files.deleteIfExists(outDir.resolve("manifest.tsv"));
        Path history = Files.createTempDirectory("weather-shots").resolve("history.tsv");

        WeatherApp.useNativeLookAndFeel();
        WeatherFrame frame = edt(() -> {
            WeatherFrame created = new WeatherFrame(new WeatherService(),
                    new SearchHistory(history, 20));
            created.setLocation(40, 20);
            created.setAlwaysOnTop(true);
            created.setVisible(true);
            return created;
        });
        try {
            Thread.sleep(1200);
            save(frame, "01_welcome");

            // Search around the world until each part of the day has been seen once.
            Map<TimeOfDay, String> seen = new EnumMap<>(TimeOfDay.class);
            for (String city : CITIES) {
                search(frame, city);
                TimeOfDay phase = edt(frame::timeOfDay);
                if (!seen.containsKey(phase) && edt(frame::currentReport) != null
                        && edt(frame::currentReport).location().name().length() > 0) {
                    seen.put(phase, city);
                    save(frame, "phase_" + phase.name().toLowerCase());
                    System.out.println(phase + " -> " + city);
                }
                if (seen.size() == TimeOfDay.values().length) {
                    break;
                }
            }

            // The daytime city makes the main screenshots; fall back to any city seen.
            String main = seen.getOrDefault(TimeOfDay.DAY,
                    seen.values().stream().findFirst().orElse("London"));
            search(frame, main);
            save(frame, "02_result_celsius");

            edt(() -> {
                frame.chooseUnits(TemperatureUnit.FAHRENHEIT, WindSpeedUnit.MILES_PER_HOUR);
                return null;
            });
            Thread.sleep(400);
            save(frame, "03_result_fahrenheit");
            edt(() -> {
                frame.chooseUnits(TemperatureUnit.CELSIUS, WindSpeedUnit.KNOTS);
                return null;
            });

            search(frame, "48.8566, 2.3522");
            save(frame, "04_coordinates");

            search(frame, "Qxzvwkjhgfd");
            save(frame, "05_error_not_found");
            search(frame, "!!!");
            save(frame, "06_error_invalid_input");
            search(frame, "95, 10");
            save(frame, "07_error_invalid_coordinates");
            System.out.println("history entries: " + edt(frame::historySize));
        } finally {
            edt(() -> {
                frame.dispose();
                return null;
            });
        }

        // A second window whose service cannot connect shows the network error.
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        WeatherService offline = new WeatherService("http://localhost:" + closedPort + "/geo",
                "http://localhost:" + closedPort + "/forecast", Duration.ofSeconds(3));
        WeatherFrame offlineFrame = edt(() -> {
            WeatherFrame created = new WeatherFrame(offline, new SearchHistory(
                    Files.createTempDirectory("weather-offline").resolve("h.tsv"), 20));
            created.setLocation(40, 20);
            created.setAlwaysOnTop(true);
            created.setVisible(true);
            return created;
        });
        try {
            Thread.sleep(800);
            search(offlineFrame, "Paris, France");
            save(offlineFrame, "08_error_no_connection");
        } finally {
            edt(() -> {
                offlineFrame.dispose();
                return null;
            });
        }
        System.exit(0);
    }

    private static void search(WeatherFrame frame, String text) throws Exception {
        edt(() -> {
            frame.setSearchText(text);
            frame.submitSearch();
            return null;
        });
        long deadline = System.currentTimeMillis() + 20_000;
        while (edt(frame::isBusy) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        Thread.sleep(500);                           // let the result be painted
    }

    /** Copies the window's pixels from the screen (full resolution on scaled displays). */
    private static void save(WeatherFrame frame, String name) throws Exception {
        edt(() -> {
            frame.toFront();
            frame.requestFocus();
            return null;
        });
        Thread.sleep(350);
        Rectangle bounds = edt(frame::getBounds);
        MultiResolutionImage screen = new Robot().createMultiResolutionScreenCapture(bounds);
        List<Image> variants = screen.getResolutionVariants();
        Image best = variants.get(variants.size() - 1);
        BufferedImage full = new BufferedImage(best.getWidth(null), best.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        full.getGraphics().drawImage(best, 0, 0, null);

        // Windows 11 adds an invisible resize border to the window bounds; cut it off so the
        // picture shows only the window itself.
        int border = (int) Math.round(7.0 * full.getWidth() / bounds.width);
        BufferedImage image = full.getSubimage(border, 1, full.getWidth() - 2 * border,
                full.getHeight() - border - 1);
        ImageIO.write(image, "png", outDir.resolve(name + ".png").toFile());
        recordInManifest(frame, name);
        System.out.println("saved " + name + ".png (" + image.getWidth() + "x"
                + image.getHeight() + ")");
    }

    /** Notes which place and local time a screenshot shows, for the report's captions. */
    private static void recordInManifest(WeatherFrame frame, String name) throws Exception {
        WeatherReport report = edt(frame::currentReport);
        String line = name + ".png	" + (report == null ? "-" : report.location().label())
                + "	" + (report == null ? "-" : report.current().time().toString())
                + "	" + (report == null ? "-" : report.timezone()) + System.lineSeparator();
        Files.writeString(outDir.resolve("manifest.tsv"), line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static <T> T edt(Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.call();
        }
        Object[] box = new Object[2];
        SwingUtilities.invokeAndWait(() -> {
            try {
                box[0] = task.call();
            } catch (Exception e) {
                box[1] = e;
            }
        });
        if (box[1] != null) {
            throw (Exception) box[1];
        }
        @SuppressWarnings("unchecked")
        T result = (T) box[0];
        return result;
    }

    private Screenshots() {
    }
}

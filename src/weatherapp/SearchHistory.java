package weatherapp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * The list of recent searches, newest first, each with the time it was made.
 *
 * <p>The list is kept in a small tab-separated text file so that it survives restarting the
 * app. Saving is a convenience: if the file cannot be written the history still works for
 * the current session, and unreadable lines in an existing file are skipped.
 */
public final class SearchHistory {

    /** One past search. */
    public record Entry(LocalDateTime time, Location location) { }

    private final Path file;
    private final int capacity;
    private final List<Entry> entries = new ArrayList<>();

    /**
     * @param file     where the history is stored (created on first save)
     * @param capacity how many searches to keep; older ones are dropped
     */
    public SearchHistory(Path file, int capacity) {
        this.file = file;
        this.capacity = capacity;
        load();
    }

    /** The entries, newest first. */
    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** Adds a search at the top of the list. */
    public void record(Location location, LocalDateTime when) {
        entries.add(0, new Entry(when, location));
        while (entries.size() > capacity) {
            entries.remove(entries.size() - 1);
        }
        save();
    }

    public void clear() {
        entries.clear();
        save();
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Entry entry = parseLine(line);
                if (entry != null && entries.size() < capacity) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read the search history: " + e.getMessage());
        }
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        for (Entry entry : entries) {
            Location place = entry.location();
            lines.add(String.join("\t", entry.time().toString(), clean(place.name()),
                    clean(place.region()), clean(place.country()),
                    Double.toString(place.latitude()), Double.toString(place.longitude())));
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save the search history: " + e.getMessage());
        }
    }

    /** Returns null for a line that is not a valid history entry. */
    private static Entry parseLine(String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length != 6) {
            return null;
        }
        try {
            Location place = new Location(parts[1], parts[2], parts[3],
                    Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
            return new Entry(LocalDateTime.parse(parts[0]), place);
        } catch (NumberFormatException | DateTimeParseException e) {
            return null;
        }
    }

    /** Tabs and line breaks would corrupt the file format. */
    private static String clean(String text) {
        return text.replaceAll("[\\t\\r\\n]+", " ");
    }
}

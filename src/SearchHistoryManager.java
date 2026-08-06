package src;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages search history records with precise timestamps.
 */
public class SearchHistoryManager {
    private final List<HistoryEntry> historyList = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    public static class HistoryEntry {
        private final String query;
        private final String timestamp;

        public HistoryEntry(String query) {
            this.query = query;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd"));
        }

        public String getQuery() { return query; }
        public String getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return query + " (" + timestamp + ")";
        }
    }

    public synchronized void addEntry(String query) {
        // Avoid duplicate consecutive entries
        if (!historyList.isEmpty() && historyList.get(0).getQuery().equalsIgnoreCase(query)) {
            return;
        }
        historyList.add(0, new HistoryEntry(query));
        if (historyList.size() > MAX_HISTORY) {
            historyList.remove(historyList.size() - 1);
        }
    }

    public synchronized List<HistoryEntry> getHistory() {
        return new ArrayList<>(historyList);
    }
}

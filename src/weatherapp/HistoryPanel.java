package weatherapp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

/** The side card listing recent searches with their timestamps; click one to show it again. */
class HistoryPanel extends RoundedPanel {

    private static final int WIDTH = 270;
    private static final int CELL_WIDTH = 220;

    private final DefaultListModel<SearchHistory.Entry> model = new DefaultListModel<>();
    private final JList<SearchHistory.Entry> list = new JList<>(model);
    private final JLabel emptyNote = Ui.label("No searches yet.", Font.PLAIN, 14, Ui.TEXT_MUTED);

    /**
     * @param onSelect called with the entry the user clicks (or presses Enter on)
     * @param onClear  called when the user presses "Clear history"
     */
    HistoryPanel(Consumer<SearchHistory.Entry> onSelect, Runnable onClear) {
        super(new BorderLayout(0, 8), Ui.CARD);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        setPreferredSize(new Dimension(WIDTH, 0));

        list.setOpaque(false);
        list.setFixedCellWidth(CELL_WIDTH);        // long names are cut with "..."
        list.setCellRenderer(new EntryRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        list.setToolTipText("Click a search to look it up again");
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && list.getCellBounds(index, index).contains(e.getPoint())) {
                    onSelect.accept(model.get(index));
                }
            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && list.getSelectedValue() != null) {
                    onSelect.accept(list.getSelectedValue());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);

        AccentButton clear = new AccentButton("Clear history");
        clear.addActionListener(e -> onClear.run());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.label("Recent searches", Font.BOLD, 16, Ui.TEXT), BorderLayout.NORTH);
        top.add(emptyNote, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(clear, BorderLayout.SOUTH);
    }

    /** Replaces the list with the given entries (newest first). */
    void refresh(List<SearchHistory.Entry> entries) {
        model.clear();
        entries.forEach(model::addElement);
        emptyNote.setVisible(entries.isEmpty());
    }

    int entryCount() {
        return model.size();
    }

    /** Draws one entry as a place name above a smaller timestamp. */
    private static final class EntryRenderer extends JPanel
            implements ListCellRenderer<SearchHistory.Entry> {

        private final JLabel place = Ui.label(" ", Font.BOLD, 14, Ui.TEXT);
        private final JLabel time = Ui.label(" ", Font.PLAIN, 12, Ui.TEXT_MUTED);

        EntryRenderer() {
            super(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            add(place, BorderLayout.NORTH);
            add(time, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SearchHistory.Entry> list,
                SearchHistory.Entry entry, int index, boolean selected, boolean focused) {
            place.setText(entry.location().label());
            time.setText(DisplayFormat.historyTime(entry.time()));
            setOpaque(selected);                       // only the selected row has a fill
            setBackground(new Color(55, 85, 140));
            return this;
        }
    }
}

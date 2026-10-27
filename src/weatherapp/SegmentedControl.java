package weatherapp;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * A row of mutually exclusive options drawn as one pill, for choosing a unit.
 *
 * @param <T> the type of the options (here an enum of units)
 */
class SegmentedControl<T> extends JPanel {

    private final Map<T, JToggleButton> buttons = new HashMap<>();
    private final Consumer<T> onChange;

    /**
     * @param options  the choices, in display order; the first is selected at the start
     * @param caption  turns an option into its label
     * @param onChange called whenever the selection changes
     */
    SegmentedControl(List<T> options, Function<T, String> caption, Consumer<T> onChange) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.onChange = onChange;
        setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        for (T option : options) {
            JToggleButton button = new Segment(caption.apply(option));
            button.addActionListener(e -> onChange.accept(option));
            group.add(button);
            buttons.put(option, button);
            add(button);
        }
        buttons.get(options.get(0)).setSelected(true);
    }

    /** Selects an option as if the user had clicked it (the change listener is called). */
    void select(T option) {
        JToggleButton button = buttons.get(option);
        if (!button.isSelected()) {
            button.setSelected(true);
            onChange.accept(option);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth((Graphics2D) g.create());
        try {
            g2.setColor(new Color(255, 255, 255, 38));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    /** One choice of the control: highlighted when selected. */
    private static final class Segment extends JToggleButton {

        Segment(String text) {
            super(text);
            setFont(Ui.font(Font.BOLD, 14));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.smooth((Graphics2D) g.create());
            try {
                if (isSelected()) {
                    g2.setColor(Ui.ACCENT);
                    g2.fill(new RoundRectangle2D.Double(
                            2, 2, getWidth() - 4, getHeight() - 4, 14, 14));
                }
                g2.setFont(getFont());
                g2.setColor(isSelected() ? Color.WHITE : Ui.TEXT_MUTED);
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
                        (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}

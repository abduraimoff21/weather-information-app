package weatherapp;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;

/** Colours, fonts and small helpers shared by all the window's components. */
final class Ui {

    /** The translucent dark "glass" behind every group of controls. */
    static final Color CARD = new Color(8, 18, 42, 150);
    static final Color CARD_BORDER = new Color(255, 255, 255, 50);
    static final Color TEXT = Color.WHITE;
    static final Color TEXT_MUTED = new Color(203, 213, 225);
    static final Color ACCENT = new Color(37, 99, 235);
    static final Color ERROR = new Color(185, 28, 28, 230);

    private static final String FONT_FAMILY = "Segoe UI";

    private Ui() {
    }

    static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    /** A transparent label with the given look. */
    static JLabel label(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    /** Switches on smooth edges and text for custom painting. */
    static Graphics2D smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    /** The same colour with a different opacity (0 to 255). */
    static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}

package weatherapp;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;

/** A flat, rounded button in the app's accent colour. */
class AccentButton extends JButton {

    private static final Color DISABLED = new Color(110, 120, 140);

    AccentButton(String text) {
        super(text);
        setFont(Ui.font(Font.BOLD, 15));
        setForeground(Color.WHITE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(9, 26, 9, 26));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth((Graphics2D) g.create());
        try {
            Color fill = Ui.ACCENT;
            if (!isEnabled()) {
                fill = DISABLED;
            } else if (getModel().isPressed()) {
                fill = Ui.ACCENT.darker();
            } else if (getModel().isRollover()) {
                fill = Ui.ACCENT.brighter();
            }
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));

            g2.setFont(getFont());
            g2.setColor(Color.WHITE);
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        } finally {
            g2.dispose();
        }
    }
}

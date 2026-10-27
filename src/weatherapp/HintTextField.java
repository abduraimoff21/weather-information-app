package weatherapp;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JTextField;

/** A text field that shows a grey hint while it is empty. */
class HintTextField extends JTextField {

    private static final Color HINT_COLOR = new Color(120, 130, 145);

    private final String hint;

    HintTextField(String hint, int columns) {
        super(columns);
        this.hint = hint;
        setFont(Ui.font(Font.PLAIN, 16));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 120)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty()) {
            Graphics2D g2 = Ui.smooth((Graphics2D) g.create());
            try {
                Insets insets = getInsets();
                FontMetrics metrics = g2.getFontMetrics(getFont());
                g2.setColor(HINT_COLOR);
                g2.setFont(getFont());
                g2.drawString(hint, insets.left,
                        (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}

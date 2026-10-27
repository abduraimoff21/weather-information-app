package weatherapp;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

/** A panel with rounded corners and a translucent fill, so the background shows through. */
class RoundedPanel extends JPanel {

    private static final int ARC = 22;

    private Color fill;

    RoundedPanel(LayoutManager layout, Color fill) {
        super(layout);
        this.fill = fill;
        setOpaque(false);
    }

    /** Changes the fill colour (used by the status bar to show errors in red). */
    void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth((Graphics2D) g.create());
        try {
            RoundRectangle2D shape = new RoundRectangle2D.Double(
                    0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0, ARC, ARC);
            g2.setColor(fill);
            g2.fill(shape);
            g2.setColor(Ui.CARD_BORDER);
            g2.draw(shape);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

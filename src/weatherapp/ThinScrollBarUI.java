package weatherapp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** A slim, translucent scroll bar that suits the window's glass look. */
final class ThinScrollBarUI extends BasicScrollBarUI {

    private static final int WIDTH = 8;

    @Override
    protected void configureScrollBarColors() {
        super.configureScrollBarColors();
        thumbColor = new Color(255, 255, 255, 120);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(WIDTH + 4, super.getPreferredSize(c).height);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return invisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return invisibleButton();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        // The track stays transparent.
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        Graphics2D g2 = Ui.smooth((Graphics2D) g.create());
        try {
            g2.setColor(thumbColor);
            g2.fill(new RoundRectangle2D.Double(
                    bounds.x + 2, bounds.y, WIDTH, bounds.height, WIDTH, WIDTH));
        } finally {
            g2.dispose();
        }
    }

    /** The arrow buttons are not needed; wheel and dragging are enough. */
    private static JButton invisibleButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        return button;
    }
}

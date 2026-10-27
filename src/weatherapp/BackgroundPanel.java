package weatherapp;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Random;

import javax.swing.JPanel;

/**
 * The window's background: a sky whose colours follow the {@link TimeOfDay}, with the sun
 * low in the sky at sunrise and sunset, a high sun by day, and a moon and stars at night.
 */
class BackgroundPanel extends JPanel {

    private static final int STAR_COUNT = 90;

    private static final float[] TWO_STOPS = {0f, 1f};
    private static final float[] THREE_STOPS = {0f, 0.55f, 1f};

    /** Star positions as fractions of the window, fixed so the sky does not flicker. */
    private final double[][] stars = new double[STAR_COUNT][3];

    private TimeOfDay timeOfDay = TimeOfDay.DAY;

    BackgroundPanel() {
        Random random = new Random(7);
        for (double[] star : stars) {
            star[0] = random.nextDouble();            // x
            star[1] = random.nextDouble() * 0.75;     // y (stars stay in the upper sky)
            star[2] = 0.8 + random.nextDouble() * 1.6; // radius in pixels
        }
    }

    TimeOfDay timeOfDay() {
        return timeOfDay;
    }

    void setTimeOfDay(TimeOfDay timeOfDay) {
        if (this.timeOfDay != timeOfDay) {
            this.timeOfDay = timeOfDay;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth((Graphics2D) g.create());
        try {
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(sky(h));
            g2.fillRect(0, 0, w, h);

            switch (timeOfDay) {
                case DAY -> glowingSun(g2, w - 80, 44, 24, new Color(255, 244, 170));
                case SUNRISE -> {
                    horizonGlow(g2, w, h, new Color(255, 190, 120));
                    glowingSun(g2, w - 80, 44, 24, new Color(255, 214, 140));
                }
                case SUNSET -> {
                    horizonGlow(g2, w, h, new Color(255, 120, 70));
                    glowingSun(g2, w - 80, 44, 24, new Color(255, 170, 90));
                }
                case NIGHT -> {
                    stars(g2, w, h);
                    moon(g2, w - 80, 44, 22);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    private LinearGradientPaint sky(int height) {
        return switch (timeOfDay) {
            case DAY -> new LinearGradientPaint(0, 0, 0, height, TWO_STOPS,
                    new Color[] {new Color(28, 110, 200), new Color(140, 200, 242)});
            case SUNRISE -> new LinearGradientPaint(0, 0, 0, height, THREE_STOPS,
                    new Color[] {new Color(58, 74, 150), new Color(226, 140, 154),
                        new Color(255, 205, 130)});
            case SUNSET -> new LinearGradientPaint(0, 0, 0, height, THREE_STOPS,
                    new Color[] {new Color(40, 38, 98), new Color(200, 74, 108),
                        new Color(255, 158, 74)});
            case NIGHT -> new LinearGradientPaint(0, 0, 0, height, TWO_STOPS,
                    new Color[] {new Color(6, 10, 30), new Color(28, 56, 104)});
        };
    }

    /** A sun disc surrounded by a soft glow. */
    private static void glowingSun(Graphics2D g2, double cx, double cy, double radius,
                                   Color color) {
        double glow = radius * 3.2;
        g2.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) glow, TWO_STOPS,
                new Color[] {Ui.withAlpha(color, 170), Ui.withAlpha(color, 0)}));
        g2.fill(new Ellipse2D.Double(cx - glow, cy - glow, glow * 2, glow * 2));
        g2.setColor(Ui.withAlpha(color, 245));
        g2.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    /** A warm glow along the bottom edge, where the sun is rising or setting. */
    private static void horizonGlow(Graphics2D g2, int w, int h, Color color) {
        float radius = w * 0.75f;
        g2.setPaint(new RadialGradientPaint(w / 2f, h * 1.05f, radius, TWO_STOPS,
                new Color[] {Ui.withAlpha(color, 190), Ui.withAlpha(color, 0)}));
        g2.fillRect(0, 0, w, h);
    }

    private void stars(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(255, 255, 255, 190));
        for (double[] star : stars) {
            double r = star[2];
            g2.fill(new Ellipse2D.Double(star[0] * w - r, star[1] * h - r, r * 2, r * 2));
        }
    }

    /** A crescent moon with a faint halo. */
    private static void moon(Graphics2D g2, double cx, double cy, double radius) {
        double halo = radius * 3;
        g2.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) halo, TWO_STOPS,
                new Color[] {new Color(200, 215, 255, 90), new Color(200, 215, 255, 0)}));
        g2.fill(new Ellipse2D.Double(cx - halo, cy - halo, halo * 2, halo * 2));

        Area crescent = new Area(new Ellipse2D.Double(
                cx - radius, cy - radius, radius * 2, radius * 2));
        double cut = radius * 0.85;
        crescent.subtract(new Area(new Ellipse2D.Double(
                cx + radius * 0.45 - cut, cy - radius * 0.25 - cut, cut * 2, cut * 2)));
        g2.setColor(new Color(250, 240, 190));
        g2.fill(crescent);
    }
}

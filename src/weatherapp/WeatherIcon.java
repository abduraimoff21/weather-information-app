package weatherapp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

/**
 * A weather picture drawn with Java 2D shapes (no image files needed), so it stays sharp at
 * every size. The artwork is designed on a 100 x 100 grid and scaled to the requested size.
 */
public final class WeatherIcon implements Icon {

    private static final Color SUN = new Color(255, 190, 40);
    private static final Color SUN_LIGHT = new Color(255, 232, 110);
    private static final Color MOON = new Color(250, 236, 170);
    private static final Color DROP = new Color(90, 170, 255);
    private static final Color SNOWFLAKE = new Color(225, 240, 255);
    private static final Color BOLT = new Color(255, 214, 64);

    // Cloud colours as {top, bottom} of a vertical gradient.
    private static final Color[] CLOUD_LIGHT = {new Color(255, 255, 255), new Color(205, 218, 232)};
    private static final Color[] CLOUD_GREY = {new Color(205, 214, 225), new Color(140, 154, 172)};
    private static final Color[] CLOUD_DARK = {new Color(150, 162, 178), new Color(88, 100, 118)};

    /** The cloud is drawn in a 100 x 64 box and then moved and scaled. */
    private static final double CLOUD_HEIGHT = 64;

    private final IconType type;
    private final boolean night;
    private final int size;

    /**
     * @param type  which weather to draw
     * @param night draw the moon instead of the sun where the picture has one
     * @param size  width and height in pixels
     */
    public WeatherIcon(IconType type, boolean night, int size) {
        this.type = type;
        this.night = night;
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = Ui.smooth((Graphics2D) g.create());
        try {
            g2.translate(x, y);
            g2.scale(size / 100.0, size / 100.0);
            switch (type) {
                case CLEAR -> sunOrMoon(g2, 50, 50, 24);
                case PARTLY_CLOUDY -> {
                    sunOrMoon(g2, 38, 38, 19);
                    cloud(g2, 16, 38, 0.85, CLOUD_LIGHT);
                }
                case CLOUDY -> {
                    cloud(g2, 36, 14, 0.62, CLOUD_GREY);
                    cloud(g2, 6, 30, 0.9, CLOUD_LIGHT);
                }
                case FOG -> {
                    cloud(g2, 14, 10, 0.72, CLOUD_LIGHT);
                    g2.setColor(new Color(225, 234, 244));
                    g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND));
                    g2.draw(new Line2D.Double(14, 66, 84, 66));
                    g2.draw(new Line2D.Double(26, 78, 92, 78));
                    g2.draw(new Line2D.Double(14, 90, 74, 90));
                }
                case DRIZZLE -> {
                    cloud(g2, 8, 8, 0.84, CLOUD_GREY);
                    drops(g2, 3, 5f, 0.6);
                }
                case RAIN -> {
                    cloud(g2, 8, 8, 0.84, CLOUD_GREY);
                    drops(g2, 4, 6f, 1.0);
                }
                case SNOW -> {
                    cloud(g2, 8, 8, 0.84, CLOUD_GREY);
                    snowflake(g2, 30, 78);
                    snowflake(g2, 50, 88);
                    snowflake(g2, 70, 78);
                }
                case THUNDERSTORM -> {
                    cloud(g2, 8, 6, 0.84, CLOUD_DARK);
                    lightningBolt(g2);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    /** Renders the icon into an image (used for the window's title-bar icon). */
    public BufferedImage toImage() {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            paintIcon(null, g2, 0, 0);
        } finally {
            g2.dispose();
        }
        return image;
    }

    private void sunOrMoon(Graphics2D g2, double cx, double cy, double radius) {
        if (night) {
            moon(g2, cx, cy, radius);
        } else {
            sun(g2, cx, cy, radius);
        }
    }

    private static void sun(Graphics2D g2, double cx, double cy, double radius) {
        g2.setColor(SUN);
        g2.setStroke(new BasicStroke((float) (radius * 0.22), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 4 * i;
            g2.draw(new Line2D.Double(
                    cx + Math.cos(angle) * radius * 1.38, cy + Math.sin(angle) * radius * 1.38,
                    cx + Math.cos(angle) * radius * 1.75, cy + Math.sin(angle) * radius * 1.75));
        }
        g2.setPaint(new GradientPaint(0, (float) (cy - radius), SUN_LIGHT,
                0, (float) (cy + radius), SUN));
        g2.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    /** A crescent: a disc with a second, slightly smaller disc cut out of it. */
    private static void moon(Graphics2D g2, double cx, double cy, double radius) {
        Area crescent = new Area(new Ellipse2D.Double(
                cx - radius, cy - radius, radius * 2, radius * 2));
        double cutRadius = radius * 0.82;
        crescent.subtract(new Area(new Ellipse2D.Double(cx + radius * 0.45 - cutRadius,
                cy - radius * 0.3 - cutRadius, cutRadius * 2, cutRadius * 2)));
        g2.setColor(MOON);
        g2.fill(crescent);
    }

    /** A cloud made of a rounded base and two bumps; (x, y) is its top-left corner. */
    private static void cloud(Graphics2D g2, double x, double y, double scale, Color[] colors) {
        Area shape = new Area(new RoundRectangle2D.Double(0, 26, 100, 38, 38, 38));
        shape.add(new Area(new Ellipse2D.Double(14, 12, 40, 40)));
        shape.add(new Area(new Ellipse2D.Double(36, 0, 52, 52)));
        AffineTransform place = new AffineTransform();
        place.translate(x, y);
        place.scale(scale, scale);
        shape.transform(place);
        g2.setPaint(new GradientPaint(0, (float) y, colors[0],
                0, (float) (y + CLOUD_HEIGHT * scale), colors[1]));
        g2.fill(shape);
    }

    /** Slanted raindrops under a cloud. */
    private static void drops(Graphics2D g2, int count, float width, double length) {
        g2.setColor(DROP);
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double spacing = 56.0 / (count - 1);
        for (int i = 0; i < count; i++) {
            double x = 28 + i * spacing;
            g2.draw(new Line2D.Double(x, 70, x - 5 * length, 70 + 20 * length));
        }
    }

    /** A six-armed snowflake centred on (cx, cy). */
    private static void snowflake(Graphics2D g2, double cx, double cy) {
        g2.setColor(SNOWFLAKE);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI / 3 * i;
            double dx = Math.cos(angle) * 7;
            double dy = Math.sin(angle) * 7;
            g2.draw(new Line2D.Double(cx - dx, cy - dy, cx + dx, cy + dy));
        }
    }

    private static void lightningBolt(Graphics2D g2) {
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(56, 52);
        bolt.lineTo(38, 78);
        bolt.lineTo(50, 78);
        bolt.lineTo(44, 98);
        bolt.lineTo(68, 68);
        bolt.lineTo(55, 68);
        bolt.lineTo(63, 52);
        bolt.closePath();
        g2.setColor(BOLT);
        g2.fill(bolt);
    }
}

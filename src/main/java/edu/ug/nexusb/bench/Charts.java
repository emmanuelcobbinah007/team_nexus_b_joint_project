package edu.ug.nexusb.bench;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Shared self-contained SVG line-chart renderer for the Week 4 experiments
 * (T070-T073) -- no external charting library, so the chart is exactly as
 * "from scratch" as everything else in this project. Reads a CSV with a
 * header row, groups rows into one series per distinct value in {@code
 * seriesColumn}, and plots {@code xColumn} against {@code yColumn}.
 */
public final class Charts {

    private static final String[] COLORS = {
        "#4c6ef5", "#e8590c", "#2f9e44", "#e03131", "#7048e6", "#0c8599"
    };

    private Charts() {
    }

    /** Chart configuration: which axes to use, whether each is log-scaled. */
    public record Config(
            String csvPath, int seriesColumn, int xColumn, int yColumn,
            boolean logX, boolean logY, String svgPath, String title,
            String xAxisLabel, String yAxisLabel) {
    }

    public static void render(Config config) throws IOException {
        Map<String, List<double[]>> series = readSeries(config);
        String svg = buildSvg(series, config);
        try (FileWriter writer = new FileWriter(config.svgPath())) {
            writer.write(svg);
        }
    }

    private static Map<String, List<double[]>> readSeries(Config config) throws IOException {
        Map<String, List<double[]>> series = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(config.csvPath()))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                String seriesName = parts[config.seriesColumn()];
                double x = Double.parseDouble(parts[config.xColumn()]);
                double y = Double.parseDouble(parts[config.yColumn()]);
                series.computeIfAbsent(seriesName, k -> new ArrayList<>()).add(new double[] {x, y});
            }
        }
        return series;
    }

    private static String buildSvg(Map<String, List<double[]>> series, Config config) {
        int width = 900;
        int height = 520;
        int marginLeft = 90;
        int marginRight = 220;
        int marginTop = 60;
        int marginBottom = 70;
        int plotWidth = width - marginLeft - marginRight;
        int plotHeight = height - marginTop - marginBottom;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (List<double[]> points : series.values()) {
            for (double[] p : points) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        // Linear-scale charts need a non-zero span; log-scale charts need
        // strictly positive values (guaranteed by the experiment data, but
        // guarded here rather than dividing by zero on a degenerate input).
        if (maxX == minX) {
            maxX = minX + 1;
        }
        if (maxY == minY) {
            maxY = minY + 1;
        }

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
           .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width)
           .append(" ").append(height).append("\" font-family=\"Helvetica, Arial, sans-serif\">\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
        svg.append("<text x=\"").append(width / 2).append("\" y=\"28\" font-size=\"16\" font-weight=\"bold\" ")
           .append("text-anchor=\"middle\" fill=\"#1a1a2e\">").append(escape(config.title())).append("</text>\n");

        double finalMinX = minX, finalMaxX = maxX, finalMinY = minY, finalMaxY = maxY;

        // Y gridlines
        if (config.logY()) {
            int firstDecade = (int) Math.floor(Math.log10(finalMinY));
            int lastDecade = (int) Math.floor(Math.log10(finalMaxY));
            for (int decade = firstDecade; decade <= lastDecade; decade++) {
                double value = Math.pow(10, decade);
                double yFrac = (Math.log10(value) - Math.log10(finalMinY)) / (Math.log10(finalMaxY) - Math.log10(finalMinY));
                drawYGridline(svg, marginLeft, marginTop, plotWidth, plotHeight, yFrac, formatNumber(value));
            }
        } else {
            int steps = 5;
            for (int i = 0; i <= steps; i++) {
                double value = finalMinY + (finalMaxY - finalMinY) * i / steps;
                double yFrac = (double) i / steps;
                drawYGridline(svg, marginLeft, marginTop, plotWidth, plotHeight, yFrac, formatNumber(value));
            }
        }

        // X gridlines: one per distinct x value present
        TreeSet<Double> xValues = new TreeSet<>();
        for (List<double[]> points : series.values()) {
            for (double[] p : points) {
                xValues.add(p[0]);
            }
        }
        for (double xValue : xValues) {
            double xFrac = config.logX()
                    ? (Math.log10(xValue) - Math.log10(finalMinX)) / (Math.log10(finalMaxX) - Math.log10(finalMinX))
                    : (xValue - finalMinX) / (finalMaxX - finalMinX);
            double x = marginLeft + xFrac * plotWidth;
            svg.append("<line x1=\"").append(fmt(x)).append("\" y1=\"").append(marginTop)
               .append("\" x2=\"").append(fmt(x)).append("\" y2=\"").append(marginTop + plotHeight)
               .append("\" stroke=\"#f0f0f0\" stroke-width=\"1\"/>\n");
            svg.append("<text x=\"").append(fmt(x)).append("\" y=\"").append(marginTop + plotHeight + 20)
               .append("\" font-size=\"11\" text-anchor=\"middle\" fill=\"#555\">")
               .append(formatNumber(xValue)).append("</text>\n");
        }

        // Axes
        svg.append("<line x1=\"").append(marginLeft).append("\" y1=\"").append(marginTop + plotHeight)
           .append("\" x2=\"").append(marginLeft + plotWidth).append("\" y2=\"").append(marginTop + plotHeight)
           .append("\" stroke=\"#1a1a2e\" stroke-width=\"1.5\"/>\n");
        svg.append("<line x1=\"").append(marginLeft).append("\" y1=\"").append(marginTop)
           .append("\" x2=\"").append(marginLeft).append("\" y2=\"").append(marginTop + plotHeight)
           .append("\" stroke=\"#1a1a2e\" stroke-width=\"1.5\"/>\n");

        svg.append("<text x=\"").append(marginLeft + plotWidth / 2).append("\" y=\"").append(height - 20)
           .append("\" font-size=\"12\" text-anchor=\"middle\" fill=\"#1a1a2e\">")
           .append(escape(config.xAxisLabel())).append(config.logX() ? " (log scale)" : "").append("</text>\n");
        svg.append("<text x=\"20\" y=\"").append(marginTop + plotHeight / 2)
           .append("\" font-size=\"12\" text-anchor=\"middle\" fill=\"#1a1a2e\" ")
           .append("transform=\"rotate(-90 20 ").append(marginTop + plotHeight / 2).append(")\">")
           .append(escape(config.yAxisLabel())).append(config.logY() ? " (log scale)" : "").append("</text>\n");

        // Series lines + points + legend
        int colorIndex = 0;
        int legendY = marginTop;
        for (Map.Entry<String, List<double[]>> entry : series.entrySet()) {
            String color = COLORS[colorIndex % COLORS.length];
            List<double[]> points = entry.getValue();
            points.sort((a, b) -> Double.compare(a[0], b[0]));

            StringBuilder path = new StringBuilder("M ");
            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);
                double xFrac = config.logX()
                        ? (Math.log10(p[0]) - Math.log10(finalMinX)) / (Math.log10(finalMaxX) - Math.log10(finalMinX))
                        : (p[0] - finalMinX) / (finalMaxX - finalMinX);
                double yFrac = config.logY()
                        ? (Math.log10(p[1]) - Math.log10(finalMinY)) / (Math.log10(finalMaxY) - Math.log10(finalMinY))
                        : (p[1] - finalMinY) / (finalMaxY - finalMinY);
                double px = marginLeft + xFrac * plotWidth;
                double py = marginTop + plotHeight - yFrac * plotHeight;
                if (i > 0) {
                    path.append(" L ");
                }
                path.append(fmt(px)).append(" ").append(fmt(py));
                svg.append("<circle cx=\"").append(fmt(px)).append("\" cy=\"").append(fmt(py))
                   .append("\" r=\"3.5\" fill=\"").append(color).append("\"/>\n");
            }
            svg.append("<path d=\"").append(path).append("\" fill=\"none\" stroke=\"").append(color)
               .append("\" stroke-width=\"2\"/>\n");

            int legendX = marginLeft + plotWidth + 20;
            svg.append("<line x1=\"").append(legendX).append("\" y1=\"").append(legendY + 12)
               .append("\" x2=\"").append(legendX + 24).append("\" y2=\"").append(legendY + 12)
               .append("\" stroke=\"").append(color).append("\" stroke-width=\"3\"/>\n");
            svg.append("<text x=\"").append(legendX + 30).append("\" y=\"").append(legendY + 16)
               .append("\" font-size=\"12\" fill=\"#1a1a2e\">").append(escape(entry.getKey())).append("</text>\n");
            legendY += 24;
            colorIndex++;
        }

        svg.append("</svg>\n");
        return svg.toString();
    }

    private static void drawYGridline(
            StringBuilder svg, int marginLeft, int marginTop, int plotWidth, int plotHeight,
            double yFrac, String label) {
        double y = marginTop + plotHeight - yFrac * plotHeight;
        svg.append("<line x1=\"").append(marginLeft).append("\" y1=\"").append(fmt(y))
           .append("\" x2=\"").append(marginLeft + plotWidth).append("\" y2=\"").append(fmt(y))
           .append("\" stroke=\"#e0e0e0\" stroke-width=\"1\"/>\n");
        svg.append("<text x=\"").append(marginLeft - 10).append("\" y=\"").append(fmt(y + 4))
           .append("\" font-size=\"11\" text-anchor=\"end\" fill=\"#555\">").append(label).append("</text>\n");
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 1e-9) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.2f", value);
    }

    private static String fmt(double value) {
        return String.format("%.2f", value);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

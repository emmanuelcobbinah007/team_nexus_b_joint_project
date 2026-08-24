package edu.ug.nexusb.bench;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Renders the two T073 CSVs ({@code graph_experiments_size.csv},
 * {@code graph_experiments_density.csv}) as log-log SVG line charts --
 * self-contained (no charting library, no external assets), matching the
 * project's "inline everything" constraint. Run after {@link
 * GraphExperiments} has produced the CSVs.
 */
public final class GraphExperimentsChart {

    private static final String[] COLORS = {
        "#4c6ef5", "#e8590c", "#2f9e44", "#e03131", "#7048e6"
    };

    private GraphExperimentsChart() {
    }

    public static void main(String[] args) throws IOException {
        // CSV columns: 0=Algorithm, 1=Vertices, 2=Edges, 3=AverageTimeNs
        renderChart(
            "results/csv/graph_experiments_size.csv", 1,
            "results/graphs/graph_experiments_size.svg",
            "T073: Graph Algorithms vs. Graph Size (|V|), fixed density |E| ~ 3|V|",
            "Vertices (|V|)");

        renderChart(
            "results/csv/graph_experiments_density.csv", 2,
            "results/graphs/graph_experiments_density.svg",
            "T073: Graph Algorithms vs. Graph Density (|E|), fixed |V| = 300",
            "Edges (|E|)");

        System.out.println("Charts written to results/graphs/.");
    }

    private static void renderChart(String csvPath, int xColumn, String svgPath, String title, String xAxisLabel)
            throws IOException {
        Map<String, List<double[]>> series = readSeries(csvPath, xColumn);
        String svg = buildSvg(series, title, xAxisLabel);
        try (FileWriter writer = new FileWriter(svgPath)) {
            writer.write(svg);
        }
    }

    private static Map<String, List<double[]>> readSeries(String csvPath, int xColumn) throws IOException {
        Map<String, List<double[]>> series = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            reader.readLine(); // header: "Algorithm,Vertices,Edges,AverageTimeNs"
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                String algorithm = parts[0];
                double x = Double.parseDouble(parts[xColumn]);
                double timeNs = Double.parseDouble(parts[3]);
                series.computeIfAbsent(algorithm, k -> new ArrayList<>()).add(new double[] {x, timeNs});
            }
        }
        return series;
    }

    private static String buildSvg(Map<String, List<double[]>> series, String title, String xAxisLabel) {
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
        double logMinX = Math.log10(minX);
        double logMaxX = Math.log10(maxX);
        double logMinY = Math.log10(minY);
        double logMaxY = Math.log10(maxY);

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
           .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width)
           .append(" ").append(height).append("\" font-family=\"Helvetica, Arial, sans-serif\">\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
        svg.append("<text x=\"").append(width / 2).append("\" y=\"28\" font-size=\"16\" font-weight=\"bold\" ")
           .append("text-anchor=\"middle\" fill=\"#1a1a2e\">").append(escape(title)).append("</text>\n");

        // Y gridlines + labels (log scale, one line per power of ten in range)
        int firstDecade = (int) Math.floor(logMinY);
        int lastDecade = (int) Math.ceil(logMaxY);
        for (int decade = firstDecade; decade <= lastDecade; decade++) {
            double yFrac = (decade - logMinY) / (logMaxY - logMinY);
            double y = marginTop + plotHeight - yFrac * plotHeight;
            svg.append("<line x1=\"").append(marginLeft).append("\" y1=\"").append(fmt(y))
               .append("\" x2=\"").append(marginLeft + plotWidth).append("\" y2=\"").append(fmt(y))
               .append("\" stroke=\"#e0e0e0\" stroke-width=\"1\"/>\n");
            svg.append("<text x=\"").append(marginLeft - 10).append("\" y=\"").append(fmt(y + 4))
               .append("\" font-size=\"11\" text-anchor=\"end\" fill=\"#555\">")
               .append(formatNs(Math.pow(10, decade))).append("</text>\n");
        }

        // X gridlines + labels (log scale, one per distinct x value present)
        java.util.TreeSet<Double> xValues = new java.util.TreeSet<>();
        for (List<double[]> points : series.values()) {
            for (double[] p : points) {
                xValues.add(p[0]);
            }
        }
        for (double xValue : xValues) {
            double xFrac = (Math.log10(xValue) - logMinX) / (logMaxX - logMinX);
            double x = marginLeft + xFrac * plotWidth;
            svg.append("<line x1=\"").append(fmt(x)).append("\" y1=\"").append(marginTop)
               .append("\" x2=\"").append(fmt(x)).append("\" y2=\"").append(marginTop + plotHeight)
               .append("\" stroke=\"#f0f0f0\" stroke-width=\"1\"/>\n");
            svg.append("<text x=\"").append(fmt(x)).append("\" y=\"").append(marginTop + plotHeight + 20)
               .append("\" font-size=\"11\" text-anchor=\"middle\" fill=\"#555\">")
               .append((long) xValue).append("</text>\n");
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
           .append(escape(xAxisLabel)).append(" (log scale)</text>\n");
        svg.append("<text x=\"20\" y=\"").append(marginTop + plotHeight / 2)
           .append("\" font-size=\"12\" text-anchor=\"middle\" fill=\"#1a1a2e\" ")
           .append("transform=\"rotate(-90 20 ").append(marginTop + plotHeight / 2).append(")\">")
           .append("Average time (log scale)</text>\n");

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
                double xFrac = (Math.log10(p[0]) - logMinX) / (logMaxX - logMinX);
                double yFrac = (Math.log10(p[1]) - logMinY) / (logMaxY - logMinY);
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

    private static String formatNs(double ns) {
        if (ns >= 1_000_000) {
            return String.format("%.0f ms", ns / 1_000_000);
        }
        if (ns >= 1_000) {
            return String.format("%.0f µs", ns / 1_000);
        }
        return String.format("%.0f ns", ns);
    }

    private static String fmt(double value) {
        return String.format("%.2f", value);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

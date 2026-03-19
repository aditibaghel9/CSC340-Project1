import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * @author aditibaghel9, KFrancis05, help from claude.ai
 */
public class CSVStatsService {

    private static final boolean DEBUG = false;

    // Used for streaming large files from socket
    public String processCSVFromSocket(BufferedReader in) {
        try {
            List<List<Double>> columns = new ArrayList<>();
            boolean firstLine = true;
            int lineCount = 0;

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_CSV")) break;

                // Each "line" from server is a chunk with \\n separators
                String[] csvLines = line.split("\\\\n");

                for (String csvLine : csvLines) {
                    csvLine = csvLine.trim();
                    if (csvLine.isEmpty()) continue;

                    // Check if first line is header
                    if (firstLine) {
                        firstLine = false;
                        String[] firstCols = csvLine.split(",");
                        if (firstCols.length > 0 && !isNumeric(firstCols[0].trim())) {
                            continue; // skip header
                        }
                    }

                    String[] values = csvLine.split(",");
                    while (columns.size() < values.length) {
                        columns.add(new ArrayList<>());
                    }
                    for (int j = 0; j < values.length; j++) {
                        try {
                            double value = Double.parseDouble(values[j].trim());
                            columns.get(j).add(value);
                        } catch (NumberFormatException e) {
                            // skip non-numeric
                        }
                    }
                    lineCount++;
                }
            }

            System.out.println("Processed " + lineCount + " data rows");

            if (columns.isEmpty()) {
                return "ERROR|No numeric data found in CSV";
            }

            return buildResult(columns);

        } catch (Exception e) {
            return "ERROR|CSV processing failed: " + e.getMessage();
        }
    }

    // Used for small manual CSV input
    public String processCSV(String csvData) {
        try {
            csvData = csvData.replace("\\n", "\n");
            List<List<Double>> columns = parseCSV(csvData);
            if (columns.isEmpty()) {
                return "ERROR|No numeric data found in CSV";
            }
            return buildResult(columns);
        } catch (Exception e) {
            return "ERROR|CSV processing failed: " + e.getMessage();
        }
    }

    // Shared result builder
    private String buildResult(List<List<Double>> columns) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            List<Double> column = columns.get(i);
            if (column.isEmpty()) continue;

            double mean = calculateMean(column);
            double median = calculateMedian(column);
            double std = calculateStd(column);
            double min = Collections.min(column);
            double max = Collections.max(column);

            result.append("Column ").append(i + 1).append(": ");
            result.append(String.format("mean=%.2f", mean));
            result.append(String.format(", median=%.2f", median));
            result.append(String.format(", std=%.2f", std));
            result.append(String.format(", min=%.2f", min));
            result.append(String.format(", max=%.2f", max));
            result.append(" | ");
        }
        return result.toString();
    }

    private List<List<Double>> parseCSV(String csvData) {
        List<List<Double>> columns = new ArrayList<>();
        String[] lines = csvData.split("\n");

        if (lines.length == 0) return columns;

        int startLine = 0;
        String[] firstLine = lines[0].split(",");
        if (firstLine.length > 0 && !isNumeric(firstLine[0].trim())) {
            startLine = 1;
        }

        int dataRowCount = 0;
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] values = line.split(",");
            while (columns.size() < values.length) {
                columns.add(new ArrayList<>());
            }
            for (int j = 0; j < values.length; j++) {
                try {
                    double value = Double.parseDouble(values[j].trim());
                    columns.get(j).add(value);
                } catch (NumberFormatException e) {
                    // skip non-numeric
                }
            }
            dataRowCount++;

            if (DEBUG && dataRowCount % 10000 == 0) {
                System.out.println("Processed " + dataRowCount + " rows..");
            }
        }
        return columns;
    }

    private double calculateMean(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private double calculateStd(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double mean = calculateMean(values);
        double sumSquaredDiff = 0.0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / values.size());
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        CSVStatsService service = new CSVStatsService();

        System.out.println("=".repeat(60));
        System.out.println("CSV STATISTICS SERVICE - TEST");
        System.out.println("=".repeat(60));

        String test1 = "Age,Height,Weight\n25,175,70\n30,180,75\n35,170,68\n40,185,80";
        System.out.println("\nTest 1: CSV with header");
        System.out.println("Input:\n" + test1);
        System.out.println("\nOutput:\n" + service.processCSV(test1));

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 2: Large CSV (1000 rows)");
        StringBuilder largeCSV = new StringBuilder("ID,Value1,Value2,Value3\n");
        for (int i = 1; i <= 1000; i++) {
            largeCSV.append(i).append(",")
                .append(Math.random() * 100).append(",")
                .append(Math.random() * 50).append(",")
                .append(Math.random() * 200).append("\n");
        }
        long startTime = System.currentTimeMillis();
        String result = service.processCSV(largeCSV.toString());
        long endTime = System.currentTimeMillis();
        System.out.println("Processed 1000 rows in " + (endTime - startTime) + "ms");
        System.out.println("Output:\n" + result);
    }
}
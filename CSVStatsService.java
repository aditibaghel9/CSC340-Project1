import java.io.BufferedReader;
import java.util.*;

/**
 * @author aditibaghel9, KFrancis05, help from claude.ai
 */
public class CSVStatsService {

    private static final boolean DEBUG = false;

    private static class MedianEstimator {
        private double[] q = new double[5];
        private int[] n = {1, 2, 3, 4, 5};
        private double[] dn = {0, 0.25, 0.5, 0.75, 1.0};
        private int count = 0;
        private double[] initial = new double[5];

        public void add(double x) {
            count++;
            if (count <= 5) {
                initial[count - 1] = x;
                if (count == 5) {
                    Arrays.sort(initial);
                    q = initial.clone();
                }
                return;
            }

            int k;
            if (x < q[0]) { q[0] = x; k = 0; }
            else if (x < q[1]) k = 0;
            else if (x < q[2]) k = 1;
            else if (x < q[3]) k = 2;
            else if (x <= q[4]) k = 3;
            else { q[4] = x; k = 3; }

            for (int i = k + 1; i < 5; i++) n[i]++;

            double[] nDesired = {
                1,
                1 + (count - 1) * 0.25,
                1 + (count - 1) * 0.5,
                1 + (count - 1) * 0.75,
                count
            };

            for (int i = 1; i <= 3; i++) {
                double d = nDesired[i] - n[i];
                if ((d >= 1 && n[i+1] - n[i] > 1) || (d <= -1 && n[i-1] - n[i] < -1)) {
                    int sign = d > 0 ? 1 : -1;
                    double qNew = parabolic(i, sign);
                    if (q[i-1] < qNew && qNew < q[i+1]) {
                        q[i] = qNew;
                    } else {
                        q[i] = q[i] + sign * (q[i + sign] - q[i]) / (n[i + sign] - n[i]);
                    }
                    n[i] += sign;
                }
            }
        }

        private double parabolic(int i, int d) {
            return q[i] + (double) d / (n[i+1] - n[i-1]) *
                ((n[i] - n[i-1] + d) * (q[i+1] - q[i]) / (n[i+1] - n[i]) +
                 (n[i+1] - n[i] - d) * (q[i] - q[i-1]) / (n[i] - n[i-1]));
        }

        public double getMedian() {
            if (count == 0) return 0;
            if (count < 5) {
                Arrays.sort(initial, 0, count);
                return initial[count / 2];
            }
            return q[2];
        }
    }

    public String processCSVFromSocket(BufferedReader in) {
        try {
            List<Long> counts = new ArrayList<>();
            List<Double> sums = new ArrayList<>();
            List<Double> sumSquares = new ArrayList<>();
            List<Double> mins = new ArrayList<>();
            List<Double> maxs = new ArrayList<>();
            List<MedianEstimator> medians = new ArrayList<>();

            boolean firstLine = true;
            int lineCount = 0;

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END_CSV")) break;

                String[] csvLines = line.split("\\\\n");

                for (String csvLine : csvLines) {
                    csvLine = csvLine.trim();
                    if (csvLine.isEmpty()) continue;

                    if (firstLine) {
                        firstLine = false;
                        String[] firstCols = csvLine.split(",");
                        if (firstCols.length > 0 && !isNumeric(firstCols[0].trim())) {
                            continue;
                        }
                    }

                    String[] values = csvLine.split(",");

                    while (counts.size() < values.length) {
                        counts.add(0L);
                        sums.add(0.0);
                        sumSquares.add(0.0);
                        mins.add(Double.MAX_VALUE);
                        maxs.add(-Double.MAX_VALUE);
                        medians.add(new MedianEstimator());
                    }

                    for (int j = 0; j < values.length; j++) {
                        try {
                            double val = Double.parseDouble(values[j].trim());
                            counts.set(j, counts.get(j) + 1);
                            sums.set(j, sums.get(j) + val);
                            sumSquares.set(j, sumSquares.get(j) + val * val);
                            if (val < mins.get(j)) mins.set(j, val);
                            if (val > maxs.get(j)) maxs.set(j, val);
                            medians.get(j).add(val);
                        } catch (NumberFormatException e) {
                            // skip non-numeric
                        }
                    }
                    lineCount++;
                }
            }

            System.out.println("Processed " + lineCount + " data rows");

            if (counts.isEmpty() || counts.stream().allMatch(c -> c == 0)) {
                return "ERROR|No numeric data found in CSV";
            }

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < counts.size(); i++) {
                long count = counts.get(i);
                if (count == 0) continue;

                double mean = sums.get(i) / count;
                double variance = (sumSquares.get(i) / count) - (mean * mean);
                double std = Math.sqrt(Math.max(0, variance));
                double min = mins.get(i);
                double max = maxs.get(i);
                double median = medians.get(i).getMedian();

                result.append("Column ").append(i + 1).append(": ");
                result.append(String.format("mean=%.2f", mean));
                result.append(String.format(", median=%.2f", median));
                result.append(String.format(", std=%.2f", std));
                result.append(String.format(", min=%.2f", min));
                result.append(String.format(", max=%.2f", max));
                result.append(String.format(", count=%d", count));
                result.append(" | ");
            }

            return result.toString();

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
            result.append(String.format(", count=%d", column.size()));
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
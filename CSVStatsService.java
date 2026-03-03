import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

public class CSVStatsService {
    public String processCSV(String csvData) {
        // Parse CSV
        List<List<Double>> columns = parseCSV(csvData);
        
        // Calculate stats for each column
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            List<Double> column = columns.get(i);
            result.append("Column ").append(i + 1).append(": ");
            result.append("mean=").append(calculateMean(column));
            result.append(", median=").append(calculateMedian(column));
            result.append(", std=").append(calculateStd(column));
            result.append(", min=").append(Collections.min(column));
            result.append(", max=").append(Collections.max(column));
            result.append("\n");
        }
        return result.toString();
    }
    
    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
}
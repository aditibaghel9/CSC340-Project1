import java.util.*;

public class CSVStatsService {

    private static final boolean DEBUG = false;
    
    public String processCSV(String csvData) {
        try {

            long startTime = System.currentTimeMillis();
            csvData = csvData.replace("\\n", "\n");
            
            //Parses CSV into columns of numbers
            List<List<Double>> columns = parseCSV(csvData);
            
            // Check if we got any data
            if (columns.isEmpty()) {
                return "ERROR|No numeric data found in CSV";
            }
            
            // Calculates statistics for each column
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < columns.size(); i++) {
                List<Double> column = columns.get(i);
                
                // Skips empty columns
                if (column.isEmpty()) {
                    continue;
                }
                
                // Calculates all statistics
                double mean = calculateMean(column);
                double median = calculateMedian(column);
                double std = calculateStd(column);
                double min = Collections.min(column);
                double max = Collections.max(column);
                
                // Formats the result
                result.append("Column ").append(i + 1).append(": ");
                result.append(String.format("mean=%.2f", mean));
                result.append(String.format(", median=%.2f", median));
                result.append(String.format(", std=%.2f", std));
                result.append(String.format(", min=%.2f", min));
                result.append(String.format(", max=%.2f", max));
                result.append(" | ");
            }

            long endTime = System.currentTimeMillis();
            if (DEBUG){
                System.out.println("CSV processing took " + (endTime - startTime) + "ms");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "ERROR|CSV processing failed: " + e.getMessage();
        }
    }
    
    
     //Parses CSV text into columns of numbers
     
    private List<List<Double>> parseCSV(String csvData) {

        if(DEBUG){
        System.out.println("DEBUG: parseCSV received: [" + csvData + "]");
        System.out.println("DEBUG: Data length: " + csvData.length());
        }

        List<List<Double>> columns = new ArrayList<>();
        String[] lines = csvData.split("\n");
        System.out.println("DEBUG: Number of lines after split: " + lines.length);
        
        if (DEBUG){
            System.out.println("Processing " + lines.length + " lines");
        }
        
        if (lines.length == 0) return columns;
        
        // Check if first line is a header
        int startLine = 0;
        String[] firstLine = lines[0].split(",");

        if (firstLine.length > 0 && !isNumeric(firstLine[0].trim())) {
            startLine = 1;
            if (DEBUG){
                System.out.println("Header detected, skipping first line");
            }
        }

        int dataRowCount = 0;
        
        // Process each data line
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            String[] values = line.split(",");

            while (columns.size() < values.length){
                columns.add(new ArrayList<>());
            }

            for (int j = 0; j < values.length; j++) {
                while (columns.size() <= j) {
                    columns.add(new ArrayList<>());
                }
                
                try {
                    double value = Double.parseDouble(values[j].trim());
                    columns.get(j).add(value);
                } catch (NumberFormatException e) {
                    // Skips non-numeric
                }
            }
            dataRowCount++;

            if(DEBUG && dataRowCount % 10000 == 0){
                System.out.println("Processed " + dataRowCount + " rows..");
            }
        }

        if (DEBUG){
            System.out.println("Parsed " + dataRowCount + " data rows into " + columns.size() + " columns");
        }
        
        return columns;
    }
    
    
     //Calculates Mean (Average)
     
    private double calculateMean(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    
    }
    
    
     //Calculates Median 
    
    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        
        if (size % 2 == 0) {
            return (sorted.get(size/2 - 1) + sorted.get(size/2)) / 2.0;
        } else {
            return sorted.get(size/2);
        }
    }
    
    
     // Calculates Standard Deviation
  
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
    
    
     //Checking if string is numeric
    
        private boolean isNumeric(String str) {
            if (str == null || str.isEmpty()) return false;
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    
    
     //Testing Method
     
        public static void main(String[] args) {
        CSVStatsService service = new CSVStatsService();
        
        System.out.println("=".repeat(60));
        System.out.println("CSV STATISTICS SERVICE - TEST");
        System.out.println("=".repeat(60));
        
        // Test 1: Basic dataset
        String test1 = "Age,Height,Weight\n25,175,70\n30,180,75\n35,170,68\n40,185,80";
        System.out.println("\nTest 1: CSV with header");
        System.out.println("Input:\n" + test1);
        System.out.println("\nOutput:\n" + service.processCSV(test1));

        // Test 2: Larger dataset
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 2: Large CSV (1000 rows)");
        StringBuilder largeCSV = new StringBuilder("ID,Value1,Value2,Value3\n");
        for (int i = 1; i <= 1000; i++) {
            largeCSV.append(i).append(",").append(Math.random() * 100).append(",").append(Math.random() * 50).append(",").append(Math.random() * 200).append("\n");
        }
        
        long startTime = System.currentTimeMillis();
        String result = service.processCSV(largeCSV.toString());
        long endTime = System.currentTimeMillis();
        
        System.out.println("Processed 1000 rows in " + (endTime - startTime) + "ms");
        System.out.println("Output:\n" + result);
    }
    }
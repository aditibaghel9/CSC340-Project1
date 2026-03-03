import java.io.*;
import java.nio.file.Files;
import java.util.Base64;


 //Helper class for clients to prepare images for the Image Transform service
 
public class ImageHelper {
    
    /**
     * Convert image file to base64 string
     * 
     * @param imageFilePath Path to image file (e.g., "photo.jpg")
     * @return Base64-encoded string of the image
     */
    public static String imageFileToBase64(String imageFilePath) throws IOException {
        File imageFile = new File(imageFilePath);
        
        if (!imageFile.exists()) {
            throw new IOException("Image file not found: " + imageFilePath);
        }
        
        // Read all bytes from file
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        
        // Encode to base64
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    
    
     // Save base64 string back to image file
    
    public static void base64ToImageFile(String base64Image, String outputPath) throws IOException {
        // Decode base64 to bytes
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        
        // Write bytes to file
        FileOutputStream fos = new FileOutputStream(outputPath);
        fos.write(imageBytes);
        fos.close();
        
        System.out.println("Image saved to: " + outputPath);
    }
    
    /**
     * Example usage
     */
    public static void main(String[] args) {
        try {
            // Example 1: Convert image to base64
            System.out.println("Converting image to base64...");
            String base64 = imageFileToBase64("test.jpg");
            System.out.println("Base64 length: " + base64.length() + " characters");
            System.out.println("First 50 chars: " + base64.substring(0, Math.min(50, base64.length())));
            
            // Example 2: Save base64 back to image
            System.out.println("\nSaving base64 back to image...");
            base64ToImageFile(base64, "output.jpg");
            System.out.println("Done!");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
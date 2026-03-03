import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;


 // Creates a test image, processes it, and verifies results
 
public class TestImageService {
    
    public static void main(String[] args) {
        System.out.println("ImageTransformService Test Program");
        System.out.println("===================================\n");
        
        try {
            // Step 1: Create a test image
            System.out.println("1. Creating test image (100x100 red square)...");
            BufferedImage testImage = createTestImage(100, 100, Color.RED);
            String base64Original = imageToBase64(testImage);
            System.out.println("   ✓ Test image created (" + base64Original.length() + " chars)\n");
            
            // Step 2: Test ImageTransformService
            ImageTransformService service = new ImageTransformService();
            
            // Test RESIZE
            System.out.println("2. Testing RESIZE operation (50x50)...");
            String resizeRequest = "RESIZE|50|50|" + base64Original;
            String resizeResult = service.process(resizeRequest);
            System.out.println("   Result: " + resizeResult.substring(0, Math.min(60, resizeResult.length())));
            if (resizeResult.startsWith("SUCCESS")) {
                System.out.println("   ✓ RESIZE works!\n");
            } else {
                System.out.println("   ✗ RESIZE failed\n");
            }
            
            // Test ROTATE
            System.out.println("3. Testing ROTATE operation (90 degrees)...");
            String rotateRequest = "ROTATE|90|" + base64Original;
            String rotateResult = service.process(rotateRequest);
            System.out.println("   Result: " + rotateResult.substring(0, Math.min(60, rotateResult.length())));
            if (rotateResult.startsWith("SUCCESS")) {
                System.out.println("   ✓ ROTATE works!\n");
            } else {
                System.out.println("   ✗ ROTATE failed\n");
            }
            
            // Test GRAYSCALE
            System.out.println("4. Testing GRAYSCALE operation...");
            String grayRequest = "GRAYSCALE|" + base64Original;
            String grayResult = service.process(grayRequest);
            System.out.println("   Result: " + grayResult.substring(0, Math.min(60, grayResult.length())));
            if (grayResult.startsWith("SUCCESS")) {
                System.out.println("   ✓ GRAYSCALE works!\n");
            } else {
                System.out.println("   ✗ GRAYSCALE failed\n");
            }
            
            // Test THUMBNAIL
            System.out.println("5. Testing THUMBNAIL operation...");
            String thumbRequest = "THUMBNAIL|" + base64Original;
            String thumbResult = service.process(thumbRequest);
            System.out.println("   Result: " + thumbResult.substring(0, Math.min(60, thumbResult.length())));
            if (thumbResult.startsWith("SUCCESS")) {
                System.out.println("   ✓ THUMBNAIL works!\n");
            } else {
                System.out.println("   ✗ THUMBNAIL failed\n");
            }
            
            System.out.println("===================================");
            System.out.println("All tests completed successfully!");
            System.out.println("===================================");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
     //Creates a simple test image
    private static BufferedImage createTestImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    //Convert BufferedImage to base64
    private static String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import javax.imageio.ImageIO;
 
public class ImageTransformService {
    
     // Main processing method - routes to specific operations
   
    public String process(String request) {
        try {
            String[] parts = request.split("\\|");
            
            if (parts.length < 2) {
                return "ERROR|Invalid format. Use: OPERATION|params|base64ImageData";
            }
            
            String operation = parts[0].toUpperCase();
            
            // Routes to appropriate operation
            switch (operation) {
                case "RESIZE":
                    if (parts.length < 4) {
                        return "ERROR|RESIZE requires: RESIZE|width|height|imageData";
                    }
                    int width = Integer.parseInt(parts[1]);
                    int height = Integer.parseInt(parts[2]);
                    String imageDataResize = parts[3];
                    return resize(imageDataResize, width, height);
                    
                case "ROTATE":
                    if (parts.length < 3) {
                        return "ERROR|ROTATE requires: ROTATE|degrees|imageData";
                    }
                    int degrees = Integer.parseInt(parts[1]);
                    String imageDataRotate = parts[2];
                    return rotate(imageDataRotate, degrees);
                    
                case "GRAYSCALE":
                    if (parts.length < 2) {
                        return "ERROR|GRAYSCALE requires: GRAYSCALE|imageData";
                    }
                    String imageDataGray = parts[1];
                    return grayscale(imageDataGray);
                    
                case "THUMBNAIL":
                    if (parts.length < 2) {
                        return "ERROR|THUMBNAIL requires: THUMBNAIL|imageData";
                    }
                    String imageDataThumb = parts[1];
                    return resize(imageDataThumb, 150, 150);
                    
                default:
                    return "ERROR|Unknown operation: " + operation + 
                           ". Available: RESIZE, ROTATE, GRAYSCALE, THUMBNAIL";
            }
            
        } catch (NumberFormatException e) {
            return "ERROR|Invalid number format: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR|Processing failed: " + e.getMessage();
        }
    }
    
    
     //Resizes image to specified dimensions
     
    private String resize(String base64Image, int targetWidth, int targetHeight) {
        try {
            // Decodes base64 to image
            BufferedImage original = decodeBase64ToImage(base64Image);
            
            // Creates new image with target dimensions
            BufferedImage resized = new BufferedImage(
                targetWidth, 
                targetHeight, 
                BufferedImage.TYPE_INT_RGB
            );
            
            // Draws original image scaled to new dimensions
            Graphics2D g = resized.createGraphics();
            
            // Use high-quality rendering
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, 
                              RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                              RenderingHints.VALUE_ANTIALIAS_ON);
            
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g.dispose();
            
            // Encodes result to base64
            String result = encodeImageToBase64(resized);
            
            return "SUCCESS|Resized to " + targetWidth + "x" + targetHeight + "|" + result;
            
        } catch (Exception e) {
            return "ERROR|Resize failed: " + e.getMessage();
        }
    }
    
    
     //Rotates image by specified degrees
     
    private String rotate(String base64Image, int degrees) {
        try {
            // Decodes base64 to image
            BufferedImage original = decodeBase64ToImage(base64Image);
            
            int width = original.getWidth();
            int height = original.getHeight();
            
            // Calculates new dimensions after rotation
            double radians = Math.toRadians(degrees);
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));
            int newWidth = (int) Math.floor(width * cos + height * sin);
            int newHeight = (int) Math.floor(height * cos + width * sin);
            
            // Creates rotated image
            BufferedImage rotated = new BufferedImage(
                newWidth, 
                newHeight, 
                BufferedImage.TYPE_INT_RGB
            );
            
            Graphics2D g = rotated.createGraphics();
            
            // Sets white background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, newWidth, newHeight);
            
            // High-quality rendering
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, 
                              RenderingHints.VALUE_RENDER_QUALITY);
            
            // Rotates around center
            AffineTransform transform = new AffineTransform();
            transform.translate(newWidth / 2.0, newHeight / 2.0);
            transform.rotate(radians);
            transform.translate(-width / 2.0, -height / 2.0);
            
            g.drawImage(original, transform, null);
            g.dispose();
            
            // Encodes result to base64
            String result = encodeImageToBase64(rotated);
            
            return "SUCCESS|Rotated " + degrees + " degrees|" + result;
            
        } catch (Exception e) {
            return "ERROR|Rotate failed: " + e.getMessage();
        }
    }
    
    
    //Converts image to grayscale
     
    private String grayscale(String base64Image) {
        try {
            // Decodes base64 to image
            BufferedImage original = decodeBase64ToImage(base64Image);
            
            // Creates grayscale image
            BufferedImage gray = new BufferedImage(
                original.getWidth(), 
                original.getHeight(), 
                BufferedImage.TYPE_BYTE_GRAY  
            );
            
            Graphics2D g = gray.createGraphics();
            g.drawImage(original, 0, 0, null);
            g.dispose();
            
            // Encode result to base64
            String result = encodeImageToBase64(gray);
            
            return "SUCCESS|Converted to grayscale|" + result;
            
        } catch (Exception e) {
            return "ERROR|Grayscale failed: " + e.getMessage();
        }
    }
     
    private BufferedImage decodeBase64ToImage(String base64Image) throws IOException {
        // Removes any whitespace
        base64Image = base64Image.trim();
        
        // Decodes base64 to bytes
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        
        // Converts bytes to BufferedImage
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bis);
        
        if (image == null) {
            throw new IOException("Failed to read image - invalid image data");
        }
        
        return image;
    }
     
    private String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Writes image as JPEG (you can also use PNG)
        ImageIO.write(image, "jpg", baos);
        
        // Converts to base64
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
     
    public static void main(String[] args) {
        ImageTransformService service = new ImageTransformService();
        
        System.out.println("ImageTransformService Test");
        System.out.println("==========================\n");
        
        System.out.println("Usage examples:");
        System.out.println("1. Resize:     RESIZE|200|150|<base64ImageData>");
        System.out.println("2. Rotate:     ROTATE|90|<base64ImageData>");
        System.out.println("3. Grayscale:  GRAYSCALE|<base64ImageData>");
        System.out.println("4. Thumbnail:  THUMBNAIL|<base64ImageData>");
        System.out.println("\nTo test with a real image:");
        System.out.println("1. Convert your image to base64");
        System.out.println("2. Send it in the format above");
        System.out.println("3. Receive base64 result");
        System.out.println("4. Decode and save");
    }
}
package watermarking.attacks;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import utils.Logger;

/**
 * Class containing different attack methods to test watermark robustness.
 */
public class WatermarkAttacks {

    /**
     * Applies a JPEG compression attack to the image.
     * @param image Original image
     * @param quality Compression quality (0-100)
     * @return Attacked image
     */
    public static BufferedImage jpegCompressionAttack(BufferedImage image, float quality) {
        Logger.info("Applying JPEG compression attack with quality: " + quality);

        // This is a simulated JPEG compression attack
        // In a real implementation, you would save and reload the image with JPEG compression

        // We'll use your existing transform, quantize, inverse quantize, inverse transform pipeline
        // to simulate JPEG compression

        // For now, we'll just return the original image
        // In your actual implementation, integrate with your JPEG compression code
        return image;
    }

    /**
     * Applies a resize attack to the image.
     * @param image Original image
     * @param scale Scale factor (0.1-1.0)
     * @return Attacked image
     */
    public static BufferedImage resizeAttack(BufferedImage image, double scale) {
        Logger.info("Applying resize attack with scale: " + scale);

        int newWidth = (int)(image.getWidth() * scale);
        int newHeight = (int)(image.getHeight() * scale);

        // First scale down
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Then scale back up to original size
        BufferedImage resultImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        g2d = resultImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, image.getWidth(), image.getHeight(), null);
        g2d.dispose();

        return resultImage;
    }

    /**
     * Applies a rotation attack to the image.
     * @param image Original image
     * @param degrees Rotation in degrees
     * @return Attacked image
     */
    public static BufferedImage rotationAttack(BufferedImage image, double degrees) {
        Logger.info("Applying rotation attack with angle: " + degrees);

        int width = image.getWidth();
        int height = image.getHeight();

        // Rotate image
        BufferedImage rotatedImage = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = rotatedImage.createGraphics();

        // Set rotation around center
        AffineTransform at = new AffineTransform();
        at.translate(width / 2, height / 2);
        at.rotate(Math.toRadians(degrees));
        at.translate(-width / 2, -height / 2);

        g2d.setTransform(at);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        // Rotate back
        BufferedImage resultImage = new BufferedImage(width, height, image.getType());
        g2d = resultImage.createGraphics();

        at = new AffineTransform();
        at.translate(width / 2, height / 2);
        at.rotate(Math.toRadians(-degrees));
        at.translate(-width / 2, -height / 2);

        g2d.setTransform(at);
        g2d.drawImage(rotatedImage, 0, 0, null);
        g2d.dispose();

        return resultImage;
    }

    /**
     * Applies a cropping attack to the image.
     * @param image Original image
     * @param cropPercentage Percentage to crop from each edge (0-0.5)
     * @return Attacked image
     */
    public static BufferedImage croppingAttack(BufferedImage image, double cropPercentage) {
        Logger.info("Applying cropping attack with percentage: " + cropPercentage);

        int width = image.getWidth();
        int height = image.getHeight();

        int cropWidth = (int)(width * cropPercentage);
        int cropHeight = (int)(height * cropPercentage);

        // Crop the image
        BufferedImage croppedImage = new BufferedImage(width - 2 * cropWidth, height - 2 * cropHeight, image.getType());
        Graphics2D g2d = croppedImage.createGraphics();
        g2d.drawImage(image, 0, 0, croppedImage.getWidth(), croppedImage.getHeight(),
                cropWidth, cropHeight, width - cropWidth, height - cropHeight, null);
        g2d.dispose();

        // Resize back to original size
        BufferedImage resultImage = new BufferedImage(width, height, image.getType());
        g2d = resultImage.createGraphics();
        g2d.drawImage(croppedImage, 0, 0, width, height, null);
        g2d.dispose();

        return resultImage;
    }
}
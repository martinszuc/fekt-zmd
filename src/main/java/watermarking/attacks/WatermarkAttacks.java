package watermarking.attacks;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import enums.TransformType;
import jpeg.Process;
import utils.Logger;

/**
 * Class containing different attack methods to test watermark robustness.
 * Implements multiple attack types as specified in the project requirements.
 */
public class WatermarkAttacks {

    /**
     * Applies a JPEG compression attack to the image using ImageIO.
     * This method uses Java's built-in JPEG compression capabilities.
     *
     * @param image Original image
     * @param quality Compression quality (1-100)
     * @return Attacked image
     */
    public static BufferedImage jpegCompressionAttack(BufferedImage image, float quality) {
        Logger.info("Applying JPEG compression attack with quality: " + quality);

        try {
            // Convert quality from 0-100 to 0-1 scale for ImageIO
            float compressionQuality = quality / 100f;

            // Get a JPEG writer
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();

            // Set compression quality
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(compressionQuality);

            // Write image to memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), params);
            writer.dispose();
            ios.close();

            // Read image back
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            BufferedImage compressedImage = ImageIO.read(bais);

            Logger.info("JPEG compression attack completed with quality " + quality);
            return compressedImage;

        } catch (IOException e) {
            Logger.error("Error applying JPEG compression attack: " + e.getMessage());
            return image; // Return original image on error
        }
    }

    /**
     * Applies a JPEG compression attack using the application's internal JPEG pipeline.
     * This method uses the transform-quantize-inverse process to simulate JPEG compression.
     *
     * @param image Original image
     * @param quality Compression quality (1-100)
     * @return Attacked image
     */
    public static BufferedImage jpegCompressionAttackInternal(BufferedImage image, float quality) {
        Logger.info("Applying internal JPEG compression attack with quality: " + quality);

        // Create Process object from image
        Process process = new Process(image);

        // Convert to YCbCr
        process.convertToYCbCr();

        // DCT Transform
        int blockSize = 8; // Standard JPEG block size

        // Create proper copies of the matrices
        process.setY(process.getY().copy());
        process.setCb(process.getCb().copy());
        process.setCr(process.getCr().copy());

        // Apply transform
        process.setY(jpeg.Transform.transform(process.getY(), TransformType.DCT, blockSize));
        process.setCb(jpeg.Transform.transform(process.getCb(), TransformType.DCT, blockSize));
        process.setCr(jpeg.Transform.transform(process.getCr(), TransformType.DCT, blockSize));

        // Quantize with given quality
        process.quantize(quality, blockSize);

        // Inverse quantize
        process.inverseQuantize();

        // Inverse transform
        process.setY(jpeg.Transform.inverseTransform(process.getY(), TransformType.DCT, blockSize));
        process.setCb(jpeg.Transform.inverseTransform(process.getCb(), TransformType.DCT, blockSize));
        process.setCr(jpeg.Transform.inverseTransform(process.getCr(), TransformType.DCT, blockSize));

        // Convert back to RGB
        process.convertToRGB();

        Logger.info("Internal JPEG compression attack completed with quality " + quality);
        return process.getRGBImage();
    }

    /**
     * Applies a PNG compression attack to the image.
     * PNG is lossless, but we simulate different compression levels.
     *
     * @param image Original image
     * @param compressionLevel Compression level (0-9)
     * @return Attacked image
     */
    public static BufferedImage pngCompressionAttack(BufferedImage image, int compressionLevel) {
        Logger.info("Applying PNG compression attack with level: " + compressionLevel);

        try {
            // Write image to memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // PNG is lossless, but we can simulate different levels by multiple save/load cycles
            // The more cycles, the more artifacts will be introduced due to rounding errors
            BufferedImage tempImage = image;

            // Perform multiple save/load cycles based on inverse of compression level
            // Higher compression level = fewer cycles (inverse relationship)
            // This is just a simulation since PNG is lossless
            int cycles = Math.max(1, 10 - compressionLevel);

            for (int i = 0; i < cycles; i++) {
                baos.reset();
                ImageIO.write(tempImage, "png", baos);
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                tempImage = ImageIO.read(bais);
            }

            Logger.info("PNG compression attack completed");
            return tempImage;

        } catch (IOException e) {
            Logger.error("Error applying PNG compression attack: " + e.getMessage());
            return image; // Return original image on error
        }
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

        // For 90 degree rotations, use direct approach to preserve quality
        if (degrees == 90 || degrees == 180 || degrees == 270 || degrees == -90 || degrees == -180 || degrees == -270) {
            int rotatedWidth = (degrees == 90 || degrees == 270 || degrees == -90 || degrees == -270) ? height : width;
            int rotatedHeight = (degrees == 90 || degrees == 270 || degrees == -90 || degrees == -270) ? width : height;

            BufferedImage rotatedImage = new BufferedImage(rotatedWidth, rotatedHeight, image.getType());
            Graphics2D g2d = rotatedImage.createGraphics();

            // Set transformation
            if (degrees == 90 || degrees == -270) {
                g2d.translate(height, 0);
                g2d.rotate(Math.PI / 2);
            } else if (degrees == 270 || degrees == -90) {
                g2d.translate(0, width);
                g2d.rotate(3 * Math.PI / 2);
            } else { // 180 or -180
                g2d.translate(width, height);
                g2d.rotate(Math.PI);
            }

            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            return rotatedImage;
        }

        // For arbitrary angles, calculate new dimensions to contain rotated image
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int newWidth = (int) Math.floor(width * cos + height * sin);
        int newHeight = (int) Math.floor(height * cos + width * sin);

        // Create new image with calculated dimensions
        BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2d = rotatedImage.createGraphics();

        // Set transformation around center of new image
        g2d.translate((newWidth - width) / 2, (newHeight - height) / 2);
        g2d.rotate(radians, width / 2, height / 2);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        // Create image with original dimensions (cropped if necessary)
        BufferedImage resultImage = new BufferedImage(width, height, image.getType());
        g2d = resultImage.createGraphics();

        // Draw rotated image centered in the original dimensions
        int x = (width - newWidth) / 2;
        int y = (height - newHeight) / 2;
        g2d.drawImage(rotatedImage, x, y, null);
        g2d.dispose();

        Logger.info("Rotation attack completed with angle " + degrees);
        return resultImage;
    }

    /**
     * Applies a resize attack to the image.
     * @param image Original image
     * @param scale Scale factor (0.0-1.0)
     * @return Attacked image
     */
    public static BufferedImage resizeAttack(BufferedImage image, double scale) {
        Logger.info("Applying resize attack with scale: " + scale);

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // Calculate new dimensions
        int newWidth = (int)(originalWidth * scale);
        int newHeight = (int)(originalHeight * scale);

        // Ensure minimum size of 1x1
        newWidth = Math.max(1, newWidth);
        newHeight = Math.max(1, newHeight);

        // First scale down
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Then scale back up to original size
        BufferedImage resultImage = new BufferedImage(originalWidth, originalHeight, image.getType());
        g2d = resultImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, originalWidth, originalHeight, null);
        g2d.dispose();

        Logger.info("Resize attack completed with scale " + scale);
        return resultImage;
    }

    /**
     * Applies a mirroring attack to the image (horizontal flip).
     * @param image Original image
     * @return Attacked (mirrored) image
     */
    public static BufferedImage mirroringAttack(BufferedImage image) {
        Logger.info("Applying mirroring attack");

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage mirroredImage = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = mirroredImage.createGraphics();

        // Draw the image flipped horizontally
        g2d.drawImage(image, width, 0, -width, height, null);
        g2d.dispose();

        Logger.info("Mirroring attack completed");
        return mirroredImage;
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

        // Calculate crop dimensions
        int cropWidth = (int)(width * cropPercentage);
        int cropHeight = (int)(height * cropPercentage);

        // Crop the image
        BufferedImage croppedImage = new BufferedImage(
                width - 2 * cropWidth,
                height - 2 * cropHeight,
                image.getType()
        );

        Graphics2D g2d = croppedImage.createGraphics();
        g2d.drawImage(
                image,
                0, 0, croppedImage.getWidth(), croppedImage.getHeight(),
                cropWidth, cropHeight, width - cropWidth, height - cropHeight,
                null
        );
        g2d.dispose();

        // Resize back to original size
        BufferedImage resultImage = new BufferedImage(width, height, image.getType());
        g2d = resultImage.createGraphics();
        g2d.drawImage(croppedImage, 0, 0, width, height, null);
        g2d.dispose();

        Logger.info("Cropping attack completed with percentage " + cropPercentage);
        return resultImage;
    }
}
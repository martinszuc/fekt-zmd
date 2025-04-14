package watermarking.spatial;

import Jama.Matrix;
import utils.Logger;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Implementation of LSB (Least Significant Bit) watermarking in the spatial domain.
 */
public class LSBWatermarking {

    /**
     * Embeds a watermark into an image using LSB technique.
     *
     * @param imageMatrix The matrix containing image data (Y, Cb, or Cr)
     * @param watermark The watermark image (should be binary)
     * @param bitPlane The bit plane to use (0-7, where 0 is LSB)
     * @param permute Whether to permute watermark bits
     * @param key The key for permutation
     * @return The watermarked matrix
     */
    public static Matrix embed(Matrix imageMatrix, BufferedImage watermark, int bitPlane, boolean permute, String key) {
        Logger.info("Embedding watermark using LSB technique on bit plane " + bitPlane);

        if (imageMatrix == null || watermark == null) {
            Logger.error("Cannot embed watermark: null input");
            return null;
        }

        // Convert watermark to binary if not already
        boolean[][] binaryWatermark = convertToBinary(watermark);

        // Permute watermark if requested
        if (permute && key != null) {
            binaryWatermark = permuteBits(binaryWatermark, key, false);
        }

        int watermarkWidth = binaryWatermark[0].length;
        int watermarkHeight = binaryWatermark.length;

        Logger.info("Watermark dimensions: " + watermarkWidth + "x" + watermarkHeight);

        // Check if watermark can fit in the image
        int imageWidth = imageMatrix.getColumnDimension();
        int imageHeight = imageMatrix.getRowDimension();

        if (watermarkWidth > imageWidth || watermarkHeight > imageHeight) {
            Logger.error("Watermark is larger than the image");
            return null;
        }

        // Clone the original matrix to avoid modifying it
        Matrix watermarkedMatrix = imageMatrix.copy();
        double[][] watermarkedData = watermarkedMatrix.getArray();

        // Embed watermark
        for (int y = 0; y < watermarkHeight; y++) {
            for (int x = 0; x < watermarkWidth; x++) {
                // Get pixel value
                double pixelValue = watermarkedData[y][x];

                // Convert to int for bitwise operations
                int pixelValueInt = (int)Math.floor(pixelValue);

                // Clear the bit at the specified bit plane
                int bitMask = ~(1 << bitPlane);
                pixelValueInt = pixelValueInt & bitMask;

                // Set the bit according to watermark
                if (binaryWatermark[y][x]) {
                    pixelValueInt |= (1 << bitPlane);
                }

                // Update the pixel value
                watermarkedData[y][x] = pixelValueInt;
            }
        }

        Logger.info("Watermark embedded successfully using LSB on bit plane " + bitPlane);
        return watermarkedMatrix;
    }

    /**
     * Extracts a watermark from an image using LSB technique.
     *
     * @param watermarkedMatrix The watermarked matrix
     * @param bitPlane The bit plane used for embedding (0-7)
     * @param permute Whether the watermark was permuted
     * @param key The key used for permutation
     * @param width The width of the watermark
     * @param height The height of the watermark
     * @return The extracted watermark as a BufferedImage
     */
    public static BufferedImage extract(Matrix watermarkedMatrix, int bitPlane, boolean permute, String key, int width, int height) {
        Logger.info("Extracting watermark using LSB technique from bit plane " + bitPlane);

        if (watermarkedMatrix == null) {
            Logger.error("Cannot extract watermark: null input");
            return null;
        }

        // Check if dimensions are valid
        int imageWidth = watermarkedMatrix.getColumnDimension();
        int imageHeight = watermarkedMatrix.getRowDimension();

        if (width > imageWidth || height > imageHeight) {
            Logger.error("Specified watermark dimensions exceed the image size");
            return null;
        }

        // Extract the binary watermark
        boolean[][] extractedBits = new boolean[height][width];
        double[][] watermarkedData = watermarkedMatrix.getArray();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get the bit at the specified bit plane
                double pixelValue = watermarkedData[y][x];
                int pixelValueInt = (int)pixelValue;
                extractedBits[y][x] = ((pixelValueInt & (1 << bitPlane)) != 0);
            }
        }

        // Reverse permutation if necessary
        if (permute && key != null) {
            extractedBits = permuteBits(extractedBits, key, true);
        }

        // Convert binary to image
        BufferedImage extractedWatermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbValue = extractedBits[y][x] ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                extractedWatermark.setRGB(x, y, rgbValue);
            }
        }

        Logger.info("Watermark extracted successfully using LSB from bit plane " + bitPlane);
        return extractedWatermark;
    }

    /**
     * Converts an image to binary format.
     */
    private static boolean[][] convertToBinary(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] binary = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                // Convert to binary using luminance
                int luminance = (int)(0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
                binary[y][x] = luminance > 128;
            }
        }

        return binary;
    }

    /**
     * Permutes the bits of a watermark using a key.
     */
    private static boolean[][] permuteBits(boolean[][] watermark, String key, boolean inverse) {
        int height = watermark.length;
        int width = watermark[0].length;
        boolean[][] permuted = new boolean[height][width];

        // Create a seeded random number generator
        Random random = new Random(key.hashCode());

        // Generate permutation mapping
        int size = width * height;
        int[] indices = new int[size];

        for (int i = 0; i < size; i++) {
            indices[i] = i;
        }

        // Fisher-Yates shuffle
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = indices[i];
            indices[i] = indices[j];
            indices[j] = temp;
        }

        // Apply permutation
        if (!inverse) {
            // Forward permutation
            for (int i = 0; i < size; i++) {
                int srcY = i / width;
                int srcX = i % width;
                int dstIdx = indices[i];
                int dstY = dstIdx / width;
                int dstX = dstIdx % width;

                permuted[dstY][dstX] = watermark[srcY][srcX];
            }
        } else {
            // Inverse permutation
            for (int i = 0; i < size; i++) {
                int dstY = i / width;
                int dstX = i % width;
                int srcIdx = indices[i];
                int srcY = srcIdx / width;
                int srcX = srcIdx % width;

                permuted[dstY][dstX] = watermark[srcY][srcX];
            }
        }

        return permuted;
    }
}
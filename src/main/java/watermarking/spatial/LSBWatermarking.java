package watermarking.spatial;

import Jama.Matrix;
import utils.Logger;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Implementation of LSB (Least Significant Bit) watermarking in spatial domain.
 *
 * This class implements the LSB watermarking technique as described in the project
 * documentation. The technique embeds a watermark into a specific bit plane of
 * an image matrix and can optionally permute the watermark for improved robustness.
 */
public class LSBWatermarking {

    /**
     * Embeds a watermark into an image using LSB technique.
     *
     * This method implements the following steps:
     * 1. Convert watermark to binary format
     * 2. Optionally permute watermark bits using the provided key
     * 3. Clear the specified bit plane in the host image
     * 4. Set the bits according to the watermark in the specified bit plane
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

        // Step 1: Convert watermark to binary if not already
        boolean[][] binaryWatermark = convertToBinary(watermark);

        // Step 2: Permute watermark if requested
        if (permute && key != null) {
            Logger.info("Permuting watermark with key: " + key);
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

        // Step 3 & 4: Clear the bit plane and set watermark bits
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
     * This method implements the following steps:
     * 1. Extract the bits from the specified bit plane
     * 2. Optionally reverse the permutation using the provided key
     * 3. Convert the binary data back to an image
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

        // Step 1: Extract the binary watermark
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

        // Step 2: Reverse permutation if necessary
        if (permute && key != null) {
            Logger.info("Reverse permuting watermark with key: " + key);
            extractedBits = permuteBits(extractedBits, key, true);
        }

        // Step 3: Convert binary to image
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
     * Pixels with luminance > 128 are considered white (true), others are black (false).
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
     * Uses Fisher-Yates shuffle algorithm with seeded random generator.
     *
     * @param watermark The binary watermark data
     * @param key The seed key for permutation
     * @param inverse Whether to perform inverse permutation (for extraction)
     * @return Permuted binary data
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
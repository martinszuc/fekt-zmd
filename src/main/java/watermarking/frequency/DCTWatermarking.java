package watermarking.frequency;

import Jama.Matrix;
import utils.Logger;
import jpeg.Transform;
import enums.TransformType;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Implementation of watermarking using DCT coefficient pair modification.
 *
 * This class implements the DCT watermarking technique described in the project
 * documentation. The method embeds watermark bits by modifying coefficient pairs
 * in the frequency domain after DCT transformation.
 */
public class DCTWatermarking {

    /**
     * Embeds a watermark into an image using DCT coefficient modification.
     *
     * The algorithm works as follows:
     * 1. Convert watermark to binary format
     * 2. Divide the host image into blocks of specified size
     * 3. Transform each block using DCT
     * 4. Modify coefficient pairs based on watermark bits:
     *    - For bit 0: Ensure coef1 > coef2
     *    - For bit 1: Ensure coef1 <= coef2
     * 5. Apply strength parameter to increase robustness
     * 6. Inverse transform each block
     *
     * @param imageMatrix The matrix containing image data (Y, Cb, or Cr)
     * @param watermark The watermark image (should be binary)
     * @param blockSize The size of DCT blocks (e.g., 8)
     * @param coefPair1 First coefficient position (e.g., {3,1})
     * @param coefPair2 Second coefficient position (e.g., {4,1})
     * @param strength Watermark embedding strength (h parameter)
     * @return The watermarked matrix
     */
    public static Matrix embed(Matrix imageMatrix, BufferedImage watermark, int blockSize,
                               int[] coefPair1, int[] coefPair2, double strength) {
        Logger.info("Embedding watermark using DCT coefficient pairs {" + coefPair1[0] + "," + coefPair1[1] + "} and {" +
                coefPair2[0] + "," + coefPair2[1] + "} with strength " + strength);

        if (imageMatrix == null || watermark == null) {
            Logger.error("Cannot embed watermark: null input");
            return null;
        }

        // Step 1: Convert watermark to binary
        boolean[][] binaryWatermark = convertToBinary(watermark);
        int watermarkWidth = binaryWatermark[0].length;
        int watermarkHeight = binaryWatermark.length;

        // Step 2: Get image dimensions and calculate blocks
        int imageWidth = imageMatrix.getColumnDimension();
        int imageHeight = imageMatrix.getRowDimension();

        // Calculate number of blocks in image
        int blocksInRow = imageWidth / blockSize;
        int blocksInCol = imageHeight / blockSize;

        // Check if watermark can fit
        if (watermarkWidth * watermarkHeight > blocksInRow * blocksInCol) {
            Logger.error("Watermark is too large for the image with the given block size");
            return null;
        }

        // Make a copy of the input matrix
        Matrix watermarkedMatrix = imageMatrix.copy();

        // Step 3-6: Process each block
        int watermarkIdx = 0;

        for (int blockRow = 0; blockRow < blocksInCol; blockRow++) {
            for (int blockCol = 0; blockCol < blocksInRow; blockCol++) {
                // Get watermark bit
                int watermarkY = watermarkIdx / watermarkWidth;
                int watermarkX = watermarkIdx % watermarkWidth;

                // Break if we've processed all watermark bits
                if (watermarkY >= watermarkHeight) break;

                boolean watermarkBit = binaryWatermark[watermarkY][watermarkX];

                // Extract block
                Matrix block = watermarkedMatrix.getMatrix(
                        blockRow * blockSize, (blockRow + 1) * blockSize - 1,
                        blockCol * blockSize, (blockCol + 1) * blockSize - 1
                );

                // Apply DCT
                Matrix dctBlock = Transform.transform(block, TransformType.DCT, blockSize);

                // Get coefficient values
                double coef1 = dctBlock.get(coefPair1[0], coefPair1[1]);
                double coef2 = dctBlock.get(coefPair2[0], coefPair2[1]);

                // Step 4&5: Modify coefficients based on watermark bit
                if (watermarkBit) {
                    // For bit 1: Make sure coef1 <= coef2
                    if (coef1 > coef2) {
                        // Swap coefficients
                        dctBlock.set(coefPair1[0], coefPair1[1], coef2);
                        dctBlock.set(coefPair2[0], coefPair2[1], coef1);
                    }

                    // Ensure the difference is larger than strength for robustness
                    if (Math.abs(coef1 - coef2) <= strength) {
                        dctBlock.set(coefPair1[0], coefPair1[1], coef1 - strength/2);
                        dctBlock.set(coefPair2[0], coefPair2[1], coef2 + strength/2);
                    }
                } else {
                    // For bit 0: Make sure coef1 > coef2
                    if (coef1 <= coef2) {
                        // Swap coefficients
                        dctBlock.set(coefPair1[0], coefPair1[1], coef2);
                        dctBlock.set(coefPair2[0], coefPair2[1], coef1);
                    }

                    // Ensure the difference is larger than strength for robustness
                    if (Math.abs(coef1 - coef2) <= strength) {
                        dctBlock.set(coefPair1[0], coefPair1[1], coef1 + strength/2);
                        dctBlock.set(coefPair2[0], coefPair2[1], coef2 - strength/2);
                    }
                }

                // Apply inverse DCT
                Matrix idctBlock = Transform.inverseTransform(dctBlock, TransformType.DCT, blockSize);

                // Place the block back
                watermarkedMatrix.setMatrix(
                        blockRow * blockSize, (blockRow + 1) * blockSize - 1,
                        blockCol * blockSize, (blockCol + 1) * blockSize - 1,
                        idctBlock
                );

                watermarkIdx++;
            }

            if (watermarkIdx / watermarkWidth >= watermarkHeight) break;
        }

        Logger.info("Watermark embedded successfully using DCT coefficient pairs");
        return watermarkedMatrix;
    }

    /**
     * Extracts a watermark from an image using DCT coefficient pairs.
     *
     * The algorithm works as follows:
     * 1. Divide the watermarked image into blocks of specified size
     * 2. Transform each block using DCT
     * 3. Extract watermark bits by comparing coefficient pairs:
     *    - If coef1 > coef2: Bit value is 0
     *    - If coef1 <= coef2: Bit value is 1
     * 4. Reconstruct the watermark image from binary data
     *
     * @param watermarkedMatrix The watermarked matrix
     * @param blockSize The block size used for DCT
     * @param coefPair1 First coefficient position
     * @param coefPair2 Second coefficient position
     * @param width The width of the watermark
     * @param height The height of the watermark
     * @return The extracted watermark as a BufferedImage
     */
    public static BufferedImage extract(Matrix watermarkedMatrix, int blockSize,
                                        int[] coefPair1, int[] coefPair2, int width, int height) {
        Logger.info("Extracting watermark using DCT coefficient pairs {" + coefPair1[0] + "," + coefPair1[1] + "} and {" +
                coefPair2[0] + "," + coefPair2[1] + "}");

        if (watermarkedMatrix == null) {
            Logger.error("Cannot extract watermark: null input");
            return null;
        }

        // Get image dimensions
        int imageWidth = watermarkedMatrix.getColumnDimension();
        int imageHeight = watermarkedMatrix.getRowDimension();

        // Calculate number of blocks in image
        int blocksInRow = imageWidth / blockSize;
        int blocksInCol = imageHeight / blockSize;

        // Check if dimensions are valid
        if (width * height > blocksInRow * blocksInCol) {
            Logger.error("Specified watermark dimensions exceed available blocks");
            return null;
        }

        // Extract the binary watermark
        boolean[][] extractedBits = new boolean[height][width];

        // Process each block
        int watermarkIdx = 0;

        for (int blockRow = 0; blockRow < blocksInCol; blockRow++) {
            for (int blockCol = 0; blockCol < blocksInRow; blockCol++) {
                // Calculate position in watermark
                int watermarkY = watermarkIdx / width;
                int watermarkX = watermarkIdx % width;

                // Break if we've processed all watermark bits
                if (watermarkY >= height) break;

                // Extract block
                Matrix block = watermarkedMatrix.getMatrix(
                        blockRow * blockSize, (blockRow + 1) * blockSize - 1,
                        blockCol * blockSize, (blockCol + 1) * blockSize - 1
                );

                // Apply DCT
                Matrix dctBlock = Transform.transform(block, TransformType.DCT, blockSize);

                // Get coefficient values
                double coef1 = dctBlock.get(coefPair1[0], coefPair1[1]);
                double coef2 = dctBlock.get(coefPair2[0], coefPair2[1]);

                // Extract watermark bit
                extractedBits[watermarkY][watermarkX] = coef1 <= coef2;

                watermarkIdx++;
            }

            if (watermarkIdx / width >= height) break;
        }

        // Convert binary to image
        BufferedImage extractedWatermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbValue = extractedBits[y][x] ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                extractedWatermark.setRGB(x, y, rgbValue);
            }
        }

        Logger.info("Watermark extracted successfully using DCT coefficient pairs");
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
}
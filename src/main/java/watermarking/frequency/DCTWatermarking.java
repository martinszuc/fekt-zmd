package watermarking.frequency;

import Jama.Matrix;
import utils.Logger;
import jpeg.Transform;
import enums.TransformType;
import watermarking.core.AbstractWatermarking;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Implementation of watermarking using DCT coefficient pair modification.
 *
 * This class implements a frequency domain watermarking technique that embeds
 * watermark bits by modifying coefficient pairs in the DCT domain. The method
 * provides good robustness against various attacks while maintaining visual
 * imperceptibility of the watermark.
 */
public class DCTWatermarking extends AbstractWatermarking {

    @Override
    public Matrix embed(Matrix imageMatrix, BufferedImage watermark, Object... params) {
        // Extract parameters
        int blockSize = (int) params[0];
        int[] coefPair1 = (int[]) params[1];
        int[] coefPair2 = (int[]) params[2];
        double strength = (double) params[3];

        Logger.info("Embedding watermark using DCT coefficient pairs {" + coefPair1[0] + "," + coefPair1[1] + "} and {" +
                coefPair2[0] + "," + coefPair2[1] + "} with strength " + strength);

        if (imageMatrix == null || watermark == null) {
            Logger.error("Cannot embed watermark: null input");
            return null;
        }

        // Convert watermark to binary format
        boolean[][] binaryWatermark = convertToBinary(watermark);
        int watermarkWidth = binaryWatermark[0].length;
        int watermarkHeight = binaryWatermark.length;

        // Get image dimensions and calculate blocks
        int imageWidth = imageMatrix.getColumnDimension();
        int imageHeight = imageMatrix.getRowDimension();

        // Calculate number of blocks in image
        int blocksInRow = imageWidth / blockSize;
        int blocksInCol = imageHeight / blockSize;

        // Verify watermark can fit in the available blocks
        if (watermarkWidth * watermarkHeight > blocksInRow * blocksInCol) {
            Logger.error("Watermark is too large for the image with the given block size");
            return null;
        }

        // Create a copy of the input matrix
        Matrix watermarkedMatrix = imageMatrix.copy();

        // Process each block to embed watermark bits
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

                // Apply DCT transform to convert block to frequency domain
                Matrix dctBlock = Transform.transform(block, TransformType.DCT, blockSize);

                // Get coefficient values
                double coef1 = dctBlock.get(coefPair1[0], coefPair1[1]);
                double coef2 = dctBlock.get(coefPair2[0], coefPair2[1]);

                // Modify coefficients based on watermark bit
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

                // Apply inverse DCT to convert back to spatial domain
                Matrix idctBlock = Transform.inverseTransform(dctBlock, TransformType.DCT, blockSize);

                // Place the modified block back in the watermarked matrix
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

    @Override
    public BufferedImage extract(Matrix watermarkedMatrix, int width, int height, Object... params) {
        // Extract parameters
        int blockSize = (int) params[0];
        int[] coefPair1 = (int[]) params[1];
        int[] coefPair2 = (int[]) params[2];

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

        // Verify dimensions are valid
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

                // Extract watermark bit based on coefficient relationship
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

    @Override
    public String getTechniqueName() {
        return "DCT (Frequency Domain)";
    }
}
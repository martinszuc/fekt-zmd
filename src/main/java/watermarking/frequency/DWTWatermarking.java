package watermarking.frequency;

import Jama.Matrix;
import utils.Logger;
import watermarking.core.AbstractWatermarking;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Implementation of DWT (Discrete Wavelet Transform) based watermarking.
 * This technique embeds the watermark in the wavelet coefficients,
 * providing good robustness and imperceptibility.
 */
public class DWTWatermarking extends AbstractWatermarking {

    @Override
    public Matrix embed(Matrix imageMatrix, BufferedImage watermark, Object... params) {
        // Extract parameters
        double strength = (double) params[0];
        String subband = (String) params[1]; // Options: "LL", "LH", "HL", "HH"

        Logger.info("Embedding watermark using DWT in " + subband + " subband with strength " + strength);

        if (imageMatrix == null || watermark == null) {
            Logger.error("Cannot embed watermark: null input");
            return null;
        }

        // Convert watermark to binary format
        boolean[][] binaryWatermark = convertToBinary(watermark);
        int watermarkWidth = binaryWatermark[0].length;
        int watermarkHeight = binaryWatermark.length;

        // Get image dimensions
        int imageHeight = imageMatrix.getRowDimension();
        int imageWidth = imageMatrix.getColumnDimension();

        // Check if watermark can fit in the wavelet subband
        if (watermarkWidth > imageWidth/2 || watermarkHeight > imageHeight/2) {
            Logger.error("Watermark too large for DWT embedding");
            return null;
        }

        // Create a copy of the input matrix
        Matrix watermarkedMatrix = imageMatrix.copy();

        // Perform single-level DWT decomposition
        Matrix[] subbands = performDWT(watermarkedMatrix);
        Matrix ll = subbands[0]; // Approximation (LL)
        Matrix lh = subbands[1]; // Horizontal detail (LH)
        Matrix hl = subbands[2]; // Vertical detail (HL)
        Matrix hh = subbands[3]; // Diagonal detail (HH)

        // Select target subband based on parameter
        Matrix targetSubband;
        switch (subband) {
            case "LL": targetSubband = ll; break;
            case "LH": targetSubband = lh; break;
            case "HL": targetSubband = hl; break;
            case "HH": targetSubband = hh; break;
            default:
                Logger.error("Invalid subband specified: " + subband);
                return null;
        }

        // Embed watermark in the selected subband
        for (int y = 0; y < watermarkHeight; y++) {
            for (int x = 0; x < watermarkWidth; x++) {
                // Get coefficient value
                double value = targetSubband.get(y, x);

                // Modify coefficient based on watermark bit
                if (binaryWatermark[y][x]) {
                    // For bit 1, increase value
                    value += strength;
                } else {
                    // For bit 0, decrease value
                    value -= strength;
                }

                // Update coefficient
                targetSubband.set(y, x, value);
            }
        }

        // Perform inverse DWT
        Matrix result = performInverseDWT(ll, lh, hl, hh, imageHeight, imageWidth);

        Logger.info("Watermark embedded successfully using DWT");
        return result;
    }

    @Override
    public BufferedImage extract(Matrix watermarkedMatrix, int width, int height, Object... params) {
        // Extract parameters
        double strength = (double) params[0];
        String subband = (String) params[1];

        Logger.info("Extracting watermark using DWT from " + subband + " subband");

        if (watermarkedMatrix == null) {
            Logger.error("Cannot extract watermark: null input");
            return null;
        }

        // Perform single-level DWT decomposition
        Matrix[] subbands = performDWT(watermarkedMatrix);
        Matrix ll = subbands[0];
        Matrix lh = subbands[1];
        Matrix hl = subbands[2];
        Matrix hh = subbands[3];

        // Select target subband based on parameter
        Matrix targetSubband;
        switch (subband) {
            case "LL": targetSubband = ll; break;
            case "LH": targetSubband = lh; break;
            case "HL": targetSubband = hl; break;
            case "HH": targetSubband = hh; break;
            default:
                Logger.error("Invalid subband specified: " + subband);
                return null;
        }

        // Extract watermark
        boolean[][] extractedBits = new boolean[height][width];

        // Threshold for determining watermark bit
        double threshold = 0;

        // Extract watermark bits by comparing coefficient values to threshold
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = targetSubband.get(y, x);
                extractedBits[y][x] = value > threshold;
            }
        }

        // Convert binary to image
        BufferedImage extractedWatermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbValue = extractedBits[y][x] ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                extractedWatermark.setRGB(x, y, rgbValue);
            }
        }

        Logger.info("Watermark extracted successfully using DWT");
        return extractedWatermark;
    }

    @Override
    public String getTechniqueName() {
        return "DWT (Wavelet Domain)";
    }

    /**
     * Performs single-level Haar Discrete Wavelet Transform.
     * Returns LL, LH, HL, HH subbands.
     */
    private Matrix[] performDWT(Matrix input) {
        int height = input.getRowDimension();
        int width = input.getColumnDimension();

        int newHeight = height / 2;
        int newWidth = width / 2;

        Matrix ll = new Matrix(newHeight, newWidth);
        Matrix lh = new Matrix(newHeight, newWidth);
        Matrix hl = new Matrix(newHeight, newWidth);
        Matrix hh = new Matrix(newHeight, newWidth);

        // Perform Haar wavelet transform
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                double a = input.get(2*y, 2*x);
                double b = input.get(2*y, 2*x+1);
                double c = input.get(2*y+1, 2*x);
                double d = input.get(2*y+1, 2*x+1);

                // Calculate wavelet coefficients
                ll.set(y, x, (a + b + c + d) / 4.0);
                lh.set(y, x, (a + b - c - d) / 4.0);
                hl.set(y, x, (a - b + c - d) / 4.0);
                hh.set(y, x, (a - b - c + d) / 4.0);
            }
        }

        return new Matrix[] {ll, lh, hl, hh};
    }

    /**
     * Performs inverse single-level Haar Discrete Wavelet Transform.
     */
    private Matrix performInverseDWT(Matrix ll, Matrix lh, Matrix hl, Matrix hh, int originalHeight, int originalWidth) {
        Matrix result = new Matrix(originalHeight, originalWidth);

        int subHeight = ll.getRowDimension();
        int subWidth = ll.getColumnDimension();

        for (int y = 0; y < subHeight; y++) {
            for (int x = 0; x < subWidth; x++) {
                double a = ll.get(y, x);
                double b = lh.get(y, x);
                double c = hl.get(y, x);
                double d = hh.get(y, x);

                // Calculate original pixel values
                result.set(2*y, 2*x, a + b + c + d);
                result.set(2*y, 2*x+1, a + b - c - d);
                result.set(2*y+1, 2*x, a - b + c - d);
                result.set(2*y+1, 2*x+1, a - b - c + d);
            }
        }

        return result;
    }
}
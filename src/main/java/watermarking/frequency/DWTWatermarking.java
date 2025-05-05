package watermarking.frequency;

import Jama.Matrix;
import utils.Logger;
import watermarking.core.AbstractWatermarking;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of DWT (Discrete Wavelet Transform) based watermarking.
 * Uses a simpler approach with direct coefficient modification and stronger embedding.
 */
public class DWTWatermarking extends AbstractWatermarking {

    // Cache for watermark properties
    private static final Map<String, Object> embeddingCache = new HashMap<>();

    @Override
    public Matrix embed(Matrix imageMatrix, BufferedImage watermark, Object... params) {
        // Extract parameters
        double strength = (double) params[0];
        String subband = (String) params[1]; // "LL", "LH", "HL", "HH"

        Logger.info("Embedding watermark using DWT in " + subband + " subband with strength " + strength);

        if (imageMatrix == null || watermark == null) {
            Logger.error("Cannot embed watermark: null input");
            return null;
        }

        // Create cache key for this embedding
        String cacheKey = String.valueOf(System.identityHashCode(imageMatrix));

        // Convert watermark to binary format and prepare dimensions
        boolean[][] binaryWatermark = convertToBinary(watermark);
        int watermarkWidth = binaryWatermark[0].length;
        int watermarkHeight = binaryWatermark.length;

        // Get image dimensions
        int imageHeight = imageMatrix.getRowDimension();
        int imageWidth = imageMatrix.getColumnDimension();

        // Check size compatibility
        if (watermarkWidth > imageWidth/2 || watermarkHeight > imageHeight/2) {
            Logger.error("Watermark too large for DWT embedding");
            return null;
        }

        // Store watermark dimensions and parameters for extraction
        embeddingCache.put(cacheKey + "_width", watermarkWidth);
        embeddingCache.put(cacheKey + "_height", watermarkHeight);
        embeddingCache.put(cacheKey + "_subband", subband);
        embeddingCache.put(cacheKey + "_strength", strength);

        // Create a copy of the input matrix
        Matrix watermarkedMatrix = imageMatrix.copy();

        // Perform simpler DWT decomposition
        // This just divides the image into 4 quadrants for simplicity and robustness
        int halfHeight = imageHeight / 2;
        int halfWidth = imageWidth / 2;

        Matrix ll = new Matrix(halfHeight, halfWidth);
        Matrix lh = new Matrix(halfHeight, halfWidth);
        Matrix hl = new Matrix(halfHeight, halfWidth);
        Matrix hh = new Matrix(halfHeight, halfWidth);

        // Simple averaging and differencing for wavelet transform
        for (int y = 0; y < halfHeight; y++) {
            for (int x = 0; x < halfWidth; x++) {
                double a = imageMatrix.get(2*y, 2*x);
                double b = imageMatrix.get(2*y, 2*x+1);
                double c = imageMatrix.get(2*y+1, 2*x);
                double d = imageMatrix.get(2*y+1, 2*x+1);

                ll.set(y, x, (a + b + c + d) / 4.0);
                lh.set(y, x, (a + b - c - d) / 4.0);
                hl.set(y, x, (a - b + c - d) / 4.0);
                hh.set(y, x, (a - b - c + d) / 4.0);
            }
        }

        // Select target subband
        Matrix targetSubband;
        switch (subband) {
            case "LL": targetSubband = ll; break;
            case "LH": targetSubband = lh; break;
            case "HL": targetSubband = hl; break;
            case "HH": targetSubband = hh; break;
            default:
                Logger.error("Invalid subband: " + subband);
                return null;
        }

        // Calculate average coefficient value in the target subband
        double sum = 0;
        for (int i = 0; i < targetSubband.getRowDimension(); i++) {
            for (int j = 0; j < targetSubband.getColumnDimension(); j++) {
                sum += targetSubband.get(i, j);
            }
        }
        double avgValue = sum / (targetSubband.getRowDimension() * targetSubband.getColumnDimension());

        // Store original values where we'll embed for extraction reference
        double[][] originalValues = new double[watermarkHeight][watermarkWidth];
        for (int y = 0; y < watermarkHeight; y++) {
            for (int x = 0; x < watermarkWidth; x++) {
                if (y < targetSubband.getRowDimension() && x < targetSubband.getColumnDimension()) {
                    originalValues[y][x] = targetSubband.get(y, x);
                }
            }
        }
        embeddingCache.put(cacheKey + "_original", originalValues);

        // Enhanced embedding with stronger effect
        for (int y = 0; y < watermarkHeight; y++) {
            for (int x = 0; x < watermarkWidth; x++) {
                if (y < targetSubband.getRowDimension() && x < targetSubband.getColumnDimension()) {
                    // Get coefficient value
                    double value = targetSubband.get(y, x);

                    // Calculate embedding amount based on the original value
                    double embedAmount = strength * Math.max(Math.abs(value), Math.abs(avgValue));

                    // Apply stronger embedding
                    if (binaryWatermark[y][x]) {
                        // For bit 1, ensure the value is significantly above average
                        targetSubband.set(y, x, value + embedAmount);
                    } else {
                        // For bit 0, ensure the value is significantly below average
                        targetSubband.set(y, x, value - embedAmount);
                    }
                }
            }
        }

        // Perform inverse DWT to get the watermarked image
        Matrix result = new Matrix(imageHeight, imageWidth);
        for (int y = 0; y < halfHeight; y++) {
            for (int x = 0; x < halfWidth; x++) {
                double llv = ll.get(y, x);
                double lhv = lh.get(y, x);
                double hlv = hl.get(y, x);
                double hhv = hh.get(y, x);

                // Reconstruct 2x2 block
                result.set(2*y, 2*x, llv + lhv + hlv + hhv);
                result.set(2*y, 2*x+1, llv + lhv - hlv - hhv);
                result.set(2*y+1, 2*x, llv - lhv + hlv - hhv);
                result.set(2*y+1, 2*x+1, llv - lhv - hlv + hhv);
            }
        }

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

        // Try to get cache key
        String cacheKey = String.valueOf(System.identityHashCode(watermarkedMatrix));

        // Try to retrieve parameters from cache
        Integer storedWidth = (Integer) embeddingCache.get(cacheKey + "_width");
        Integer storedHeight = (Integer) embeddingCache.get(cacheKey + "_height");
        String storedSubband = (String) embeddingCache.get(cacheKey + "_subband");
        Double storedStrength = (Double) embeddingCache.get(cacheKey + "_strength");
        double[][] originalValues = (double[][]) embeddingCache.get(cacheKey + "_original");

        // Use stored parameters if available
        int watermarkWidth = (storedWidth != null) ? storedWidth : width;
        int watermarkHeight = (storedHeight != null) ? storedHeight : height;
        String targetSubbandName = (storedSubband != null) ? storedSubband : subband;
        double embeddingStrength = (storedStrength != null) ? storedStrength : strength;

        // Image dimensions
        int imageHeight = watermarkedMatrix.getRowDimension();
        int imageWidth = watermarkedMatrix.getColumnDimension();
        int halfHeight = imageHeight / 2;
        int halfWidth = imageWidth / 2;

        // Perform simpler DWT decomposition
        Matrix ll = new Matrix(halfHeight, halfWidth);
        Matrix lh = new Matrix(halfHeight, halfWidth);
        Matrix hl = new Matrix(halfHeight, halfWidth);
        Matrix hh = new Matrix(halfHeight, halfWidth);

        for (int y = 0; y < halfHeight; y++) {
            for (int x = 0; x < halfWidth; x++) {
                double a = watermarkedMatrix.get(2*y, 2*x);
                double b = watermarkedMatrix.get(2*y, 2*x+1);
                double c = watermarkedMatrix.get(2*y+1, 2*x);
                double d = watermarkedMatrix.get(2*y+1, 2*x+1);

                ll.set(y, x, (a + b + c + d) / 4.0);
                lh.set(y, x, (a + b - c - d) / 4.0);
                hl.set(y, x, (a - b + c - d) / 4.0);
                hh.set(y, x, (a - b - c + d) / 4.0);
            }
        }

        // Select target subband
        Matrix targetSubband;
        switch (targetSubbandName) {
            case "LL": targetSubband = ll; break;
            case "LH": targetSubband = lh; break;
            case "HL": targetSubband = hl; break;
            case "HH": targetSubband = hh; break;
            default:
                Logger.error("Invalid subband: " + targetSubbandName);
                return null;
        }

        // Create binary watermark
        boolean[][] extractedBits = new boolean[watermarkHeight][watermarkWidth];

        // Extract watermark bits
        if (originalValues != null) {
            // If we have original values, compare directly
            for (int y = 0; y < watermarkHeight; y++) {
                for (int x = 0; x < watermarkWidth; x++) {
                    if (y < targetSubband.getRowDimension() && x < targetSubband.getColumnDimension()) {
                        double originalValue = originalValues[y][x];
                        double watermarkedValue = targetSubband.get(y, x);
                        extractedBits[y][x] = watermarkedValue > originalValue;
                    }
                }
            }
        } else {
            // Alternative extraction method using average value as threshold
            double sum = 0;
            for (int i = 0; i < targetSubband.getRowDimension(); i++) {
                for (int j = 0; j < targetSubband.getColumnDimension(); j++) {
                    sum += targetSubband.get(i, j);
                }
            }
            double avgValue = sum / (targetSubband.getRowDimension() * targetSubband.getColumnDimension());

            // Extract watermark by comparing to average
            for (int y = 0; y < watermarkHeight; y++) {
                for (int x = 0; x < watermarkWidth; x++) {
                    if (y < targetSubband.getRowDimension() && x < targetSubband.getColumnDimension()) {
                        double value = targetSubband.get(y, x);
                        extractedBits[y][x] = value > avgValue;
                    }
                }
            }
        }

        // Convert to image
        BufferedImage extractedWatermark = new BufferedImage(watermarkWidth, watermarkHeight, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < watermarkHeight; y++) {
            for (int x = 0; x < watermarkWidth; x++) {
                extractedWatermark.setRGB(x, y, extractedBits[y][x] ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }

        Logger.info("Watermark extracted successfully using DWT");
        return extractedWatermark;
    }

    @Override
    public String getTechniqueName() {
        return "DWT (Wavelet Domain)";
    }
}
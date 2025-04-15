package watermarking.core;

import java.awt.Color;
import java.awt.image.BufferedImage;
import utils.Logger;

/**
 * Utilities for evaluating watermark quality and robustness.
 */
public class WatermarkEvaluation {

    /**
     * Calculates the bit error rate between original and extracted watermarks.
     * @param original Original watermark
     * @param extracted Extracted watermark
     * @return Bit error rate (0.0-1.0)
     */
    public static double calculateBER(BufferedImage original, BufferedImage extracted) {
        if (original.getWidth() != extracted.getWidth() || original.getHeight() != extracted.getHeight()) {
            Logger.error("Cannot calculate BER: Watermarks have different dimensions");
            return 1.0; // Maximum error
        }

        int width = original.getWidth();
        int height = original.getHeight();
        int totalBits = width * height;
        int errorBits = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color origColor = new Color(original.getRGB(x, y));
                Color extrColor = new Color(extracted.getRGB(x, y));

                boolean origBit = getBinaryValue(origColor);
                boolean extrBit = getBinaryValue(extrColor);

                if (origBit != extrBit) {
                    errorBits++;
                }
            }
        }

        return (double) errorBits / totalBits;
    }

    /**
     * Converts a color to a binary value based on luminance.
     */
    private static boolean getBinaryValue(Color color) {
        int luminance = (int)(0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
        return luminance > 128;
    }

    /**
     * Calculates the normalized correlation between original and extracted watermarks.
     * @param original Original watermark
     * @param extracted Extracted watermark
     * @return Normalized correlation (-1.0 to 1.0)
     */
    public static double calculateNC(BufferedImage original, BufferedImage extracted) {
        if (original.getWidth() != extracted.getWidth() || original.getHeight() != extracted.getHeight()) {
            Logger.error("Cannot calculate NC: Watermarks have different dimensions");
            return 0.0; // No correlation
        }

        int width = original.getWidth();
        int height = original.getHeight();

        double sum1 = 0;
        double sum2 = 0;
        double sum12 = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color origColor = new Color(original.getRGB(x, y));
                Color extrColor = new Color(extracted.getRGB(x, y));

                double origValue = getBinaryValue(origColor) ? 1.0 : -1.0;
                double extrValue = getBinaryValue(extrColor) ? 1.0 : -1.0;

                sum1 += origValue * origValue;
                sum2 += extrValue * extrValue;
                sum12 += origValue * extrValue;
            }
        }

        if (sum1 == 0 || sum2 == 0) {
            return 0.0;
        }

        return sum12 / (Math.sqrt(sum1) * Math.sqrt(sum2));
    }

    /**
     * Calculates the Peak Signal-to-Noise Ratio (PSNR) between original and watermarked images.
     * Higher PSNR values indicate better image quality after watermarking.
     *
     * @param original Original image
     * @param watermarked Watermarked image
     * @return PSNR value in dB
     */
    public static double calculatePSNR(BufferedImage original, BufferedImage watermarked) {
        if (original.getWidth() != watermarked.getWidth() ||
                original.getHeight() != watermarked.getHeight()) {
            Logger.error("Cannot calculate PSNR: Images have different dimensions");
            return 0.0;
        }

        int width = original.getWidth();
        int height = original.getHeight();
        long sumSquaredDiff = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color origColor = new Color(original.getRGB(x, y));
                Color wmColor = new Color(watermarked.getRGB(x, y));

                int diffR = origColor.getRed() - wmColor.getRed();
                int diffG = origColor.getGreen() - wmColor.getGreen();
                int diffB = origColor.getBlue() - wmColor.getBlue();

                sumSquaredDiff += diffR * diffR + diffG * diffG + diffB * diffB;
            }
        }

        double mse = (double) sumSquaredDiff / (width * height * 3); // 3 for RGB channels

        if (mse == 0) {
            return 100.0; // Perfect match
        }

        // For 8-bit images, maximum pixel value is 255
        double psnr = 10 * Math.log10((255.0 * 255.0) / mse);

        return psnr;
    }

    /**
     * Calculates the watermark-to-noise ratio (WNR) - a measure of watermark visibility.
     * Lower WNR means the watermark is less visible.
     *
     * @param original Original image
     * @param watermarked Watermarked image
     * @return WNR value in dB
     */
    public static double calculateWNR(BufferedImage original, BufferedImage watermarked) {
        if (original.getWidth() != watermarked.getWidth() ||
                original.getHeight() != watermarked.getHeight()) {
            Logger.error("Cannot calculate WNR: Images have different dimensions");
            return 0.0;
        }

        int width = original.getWidth();
        int height = original.getHeight();
        long sumOriginalSquared = 0;
        long sumDiffSquared = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color origColor = new Color(original.getRGB(x, y));
                Color wmColor = new Color(watermarked.getRGB(x, y));

                int origR = origColor.getRed();
                int origG = origColor.getGreen();
                int origB = origColor.getBlue();

                int diffR = origR - wmColor.getRed();
                int diffG = origG - wmColor.getGreen();
                int diffB = origB - wmColor.getBlue();

                sumOriginalSquared += origR * origR + origG * origG + origB * origB;
                sumDiffSquared += diffR * diffR + diffG * diffG + diffB * diffB;
            }
        }

        if (sumDiffSquared == 0) {
            return 0.0; // No difference between images
        }

        double wnr = 10 * Math.log10((double) sumOriginalSquared / sumDiffSquared);

        return wnr;
    }
}
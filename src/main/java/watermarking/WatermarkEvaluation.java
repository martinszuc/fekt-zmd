package watermarking;

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
}
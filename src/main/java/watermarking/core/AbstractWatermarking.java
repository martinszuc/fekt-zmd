package watermarking.core;

import Jama.Matrix;
import java.awt.image.BufferedImage;
import java.awt.Color;

/**
 * Base abstract class for all watermarking techniques.
 * Provides common functionality and enforces a consistent interface
 * for different watermarking implementations.
 */
public abstract class AbstractWatermarking {

    /**
     * Embeds a watermark into an image matrix.
     *
     * @param imageMatrix The matrix containing image data
     * @param watermark The watermark image
     * @param params Array of parameters specific to the watermarking technique
     * @return The watermarked matrix
     */
    public abstract Matrix embed(Matrix imageMatrix, BufferedImage watermark, Object... params);

    /**
     * Extracts a watermark from a watermarked matrix.
     *
     * @param watermarkedMatrix The matrix containing the watermarked image data
     * @param width Expected width of the watermark
     * @param height Expected height of the watermark
     * @param params Array of parameters specific to the watermarking technique
     * @return The extracted watermark
     */
    public abstract BufferedImage extract(Matrix watermarkedMatrix, int width, int height, Object... params);

    /**
     * Gets the name of the watermarking technique.
     *
     * @return Name of the watermarking technique
     */
    public abstract String getTechniqueName();

    /**
     * Converts an image to binary format based on luminance values.
     * Pixels with luminance > 128 are considered white (true), others black (false).
     *
     * @param image Input image to convert to binary
     * @return 2D array of boolean values representing binary image
     */
    protected boolean[][] convertToBinary(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] binary = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                // Calculate luminance using standard coefficients
                int luminance = (int)(0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
                binary[y][x] = luminance > 128;
            }
        }

        return binary;
    }
}
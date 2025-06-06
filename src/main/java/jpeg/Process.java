package jpeg;

import Jama.Matrix;
import enums.SamplingType;
import enums.TransformType;
import utils.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Process {
    private BufferedImage image;
    private int[][] red, green, blue;
    private Matrix Y, Cb, Cr;
    private boolean isYCbCrConverted = false;
    private boolean isQuantized = false;
    private double quantizationQuality = 50.0;
    private int quantizationBlockSize = 8;

    public Process(BufferedImage image) {
        this.image = image;
        loadRGBArrays();
    }

    /**
     * Sets the Y component matrix.
     */
    public void setY(Matrix y) {
        this.Y = y;
    }

    /**
     * Sets the Cb component matrix.
     */
    public void setCb(Matrix cb) {
        this.Cb = cb;
    }

    /**
     * Sets the Cr component matrix.
     */
    public void setCr(Matrix cr) {
        this.Cr = cr;
    }

    // Extracts RGB channels into arrays
    private void loadRGBArrays() {
        Logger.info("Loading RGB arrays from image");
        int width = image.getWidth();
        int height = image.getHeight();
        red = new int[height][width];
        green = new int[height][width];
        blue = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                Color color = new Color(pixel, true);
                red[y][x] = color.getRed();
                green[y][x] = color.getGreen();
                blue[y][x] = color.getBlue();
            }
        }
        Logger.info("RGB arrays loaded - " + width + "x" + height);
    }

    /**
     * Converts RGB to YCbCr.
     */
    public void convertToYCbCr() {
        Logger.info("Converting RGB to YCbCr");

        long startTime = System.currentTimeMillis();

        Matrix[] result = ColorTransform.convertOriginalRGBtoYcBcR(red, green, blue);
        Y = result[0];
        Cb = result[1];
        Cr = result[2];
        isYCbCrConverted = true;

        long endTime = System.currentTimeMillis();

        Logger.info("RGB to YCbCr conversion completed in " + (endTime - startTime) + "ms");
        Logger.info("Y dimensions: " + Y.getRowDimension() + "x" + Y.getColumnDimension());
        Logger.info("Cb dimensions: " + Cb.getRowDimension() + "x" + Cb.getColumnDimension());
        Logger.info("Cr dimensions: " + Cr.getRowDimension() + "x" + Cr.getColumnDimension());
    }

    /**
     * Checks if image is converted to YCbCr.
     */
    public boolean isYCbCrConverted() {
        return isYCbCrConverted;
    }

    /**
     * Converts YCbCr back to RGB.
     */
    public void convertToRGB() {
        if (!isYCbCrConverted) {
            Logger.warning("Attempted to convert to RGB without YCbCr data");
            return;
        }

        Logger.info("Converting YCbCr to RGB");
        long startTime = System.currentTimeMillis();

        Object[] result = ColorTransform.convertModifiedYcBcRtoRGB(Y, Cb, Cr);
        red = (int[][]) result[0];
        green = (int[][]) result[1];
        blue = (int[][]) result[2];

        long endTime = System.currentTimeMillis();
        Logger.info("YCbCr to RGB conversion completed in " + (endTime - startTime) + "ms");
    }

    /**
     * Returns a BufferedImage from the current RGB arrays.
     */
    public BufferedImage getRGBImage() {
        Logger.info("Creating RGB image from arrays");
        int width = red[0].length;
        int height = red.length;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = red[y][x];
                int g = green[y][x];
                int b = blue[y][x];
                Color color = new Color(r, g, b);
                result.setRGB(x, y, color.getRGB());
            }
        }

        Logger.info("RGB image created - " + width + "x" + height);
        return result;
    }

    /**
     * Returns a BufferedImage for a single RGB channel.
     */
    public BufferedImage getChannelImage(int[][] channel, String channelType) {
        Logger.info("Creating channel image for " + channelType);
        int width = channel[0].length;
        int height = channel.length;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = channel[y][x];
                int r = 0, g = 0, b = 0;
                switch (channelType) {
                    case "RED":
                        r = value;
                        break;
                    case "GREEN":
                        g = value;
                        break;
                    case "BLUE":
                        b = value;
                        break;
                }
                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        Logger.info(channelType + " channel image created - " + width + "x" + height);
        return result;
    }

    /**
     * Returns a grayscale image for a YCbCr channel.
     */
    public BufferedImage getChannelImage(Matrix channel) {
        if (channel == null) {
            Logger.warning("Attempted to create image from null channel");
            return null;
        }

        int width = channel.getColumnDimension();
        int height = channel.getRowDimension();
        Logger.info("Creating grayscale image from matrix - " + width + "x" + height);

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double[][] array = channel.getArray();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (int) Math.round(array[y][x]);
                value = Math.max(0, Math.min(255, value));
                result.setRGB(x, y, new Color(value, value, value).getRGB());
            }
        }

        Logger.info("Grayscale image created - " + width + "x" + height);
        return result;
    }

    /**
     * Downsamples the chrominance channels according to the selected sampling pattern.
     */
    public void downSample(SamplingType samplingType) {
        if (!isYCbCrConverted) {
            Logger.warning("Attempted to downsample without YCbCr conversion");
            return;
        }

        Logger.info("Downsampling with pattern: " + samplingType);
        long startTime = System.currentTimeMillis();

        Logger.info("Before downsampling - Cb: " + Cb.getRowDimension() + "x" + Cb.getColumnDimension() +
                ", Cr: " + Cr.getRowDimension() + "x" + Cr.getColumnDimension());

        Cb = Sampling.sampleDown(Cb, samplingType);
        Cr = Sampling.sampleDown(Cr, samplingType);

        Logger.info("After downsampling - Cb: " + Cb.getRowDimension() + "x" + Cb.getColumnDimension() +
                ", Cr: " + Cr.getRowDimension() + "x" + Cr.getColumnDimension());

        long endTime = System.currentTimeMillis();
        Logger.info("Downsampling completed in " + (endTime - startTime) + "ms");
    }

    /**
     * Upsamples the chrominance channels to restore their original resolution.
     */
    public void upSample(SamplingType samplingType) {
        if (!isYCbCrConverted) {
            Logger.warning("Attempted to upsample without YCbCr conversion");
            return;
        }

        Logger.info("Upsampling with pattern: " + samplingType);
        long startTime = System.currentTimeMillis();

        Logger.info("Before upsampling - Cb: " + Cb.getRowDimension() + "x" + Cb.getColumnDimension() +
                ", Cr: " + Cr.getRowDimension() + "x" + Cr.getColumnDimension());

        Cb = Sampling.sampleUp(Cb, samplingType);
        Cr = Sampling.sampleUp(Cr, samplingType);

        Logger.info("After upsampling - Cb: " + Cb.getRowDimension() + "x" + Cb.getColumnDimension() +
                ", Cr: " + Cr.getRowDimension() + "x" + Cr.getColumnDimension());

        long endTime = System.currentTimeMillis();
        Logger.info("Upsampling completed in " + (endTime - startTime) + "ms");
    }

    /**
     * Performs quantization on Y, Cb, Cr channels.
     * @param quality Quality value (1-100)
     * @param blockSize Block size for quantization
     */
    public void quantize(double quality, int blockSize) {
        if (!isYCbCrConverted) {
            Logger.warning("Cannot quantize - YCbCr conversion not performed");
            return;
        }

        if (Y == null || Cb == null || Cr == null) {
            Logger.warning("Cannot quantize - YCbCr channels not available");
            return;
        }

        Logger.info("Quantizing with quality " + quality + " and block size " + blockSize);
        long startTime = System.currentTimeMillis();

        Y = Quantization.quantize(Y, blockSize, quality, true);
        Cb = Quantization.quantize(Cb, blockSize, quality, false);
        Cr = Quantization.quantize(Cr, blockSize, quality, false);

        isQuantized = true;
        quantizationQuality = quality;
        quantizationBlockSize = blockSize;

        long endTime = System.currentTimeMillis();
        Logger.info("Quantization completed in " + (endTime - startTime) + "ms");
    }

    /**
     * Performs inverse quantization on Y, Cb, Cr channels.
     */
    public void inverseQuantize() {
        if (!isYCbCrConverted || !isQuantized) {
            Logger.warning("Cannot perform inverse quantization - data not quantized");
            return;
        }

        if (Y == null || Cb == null || Cr == null) {
            Logger.warning("Cannot perform inverse quantization - YCbCr channels not available");
            return;
        }

        Logger.info("Inverse quantizing with quality " + quantizationQuality +
                " and block size " + quantizationBlockSize);
        long startTime = System.currentTimeMillis();

        Y = Quantization.inverseQuantize(Y, quantizationBlockSize, quantizationQuality, true);
        Cb = Quantization.inverseQuantize(Cb, quantizationBlockSize, quantizationQuality, false);
        Cr = Quantization.inverseQuantize(Cr, quantizationBlockSize, quantizationQuality, false);

        isQuantized = false;

        long endTime = System.currentTimeMillis();
        Logger.info("Inverse quantization completed in " + (endTime - startTime) + "ms");
    }

    /**
     * Checks if the data is currently quantized.
     */
    public boolean isQuantized() {
        return isQuantized;
    }

    /**
     * Gets the current quantization quality.
     */
    public double getQuantizationQuality() {
        return quantizationQuality;
    }

    /**
     * Gets the current quantization block size.
     */
    public int getQuantizationBlockSize() {
        return quantizationBlockSize;
    }

    /**
     * Applies DCT transformation to Y, Cb, and Cr components.
     *
     * @param transformType The type of transform to apply (DCT or WHT)
     * @param blockSize The size of blocks to use for the transform
     */
    public void transform(TransformType transformType, int blockSize) {
        if (!isYCbCrConverted || Y == null || Cb == null || Cr == null) {
            Logger.warning("Cannot transform: YCbCr components not available");
            return;
        }

        Logger.info("Transforming YCbCr components using " + transformType +
                " with block size " + blockSize);

        // Apply the transform to each component
        Y = Transform.transform(Y, transformType, blockSize);
        Cb = Transform.transform(Cb, transformType, blockSize);
        Cr = Transform.transform(Cr, transformType, blockSize);

        Logger.info("Transform completed");
    }

    /**
     * Applies inverse DCT transformation to Y, Cb, and Cr components.
     *
     * @param transformType The type of transform to apply (DCT or WHT)
     * @param blockSize The size of blocks to use for the transform
     */
    public void inverseTransform(TransformType transformType, int blockSize) {
        if (!isYCbCrConverted || Y == null || Cb == null || Cr == null) {
            Logger.warning("Cannot inverse transform: YCbCr components not available");
            return;
        }

        Logger.info("Inverse transforming YCbCr components using " + transformType +
                " with block size " + blockSize);

        // Apply the inverse transform to each component
        Y = Transform.inverseTransform(Y, transformType, blockSize);
        Cb = Transform.inverseTransform(Cb, transformType, blockSize);
        Cr = Transform.inverseTransform(Cr, transformType, blockSize);

        Logger.info("Inverse transform completed");
    }

    // Getters
    public BufferedImage getImage() {
        return image;
    }

    public int[][] getRed() {
        return red;
    }

    public int[][] getGreen() {
        return green;
    }

    public int[][] getBlue() {
        return blue;
    }

    public Matrix getY() {
        return Y;
    }

    public Matrix getCb() {
        return Cb;
    }

    public Matrix getCr() {
        return Cr;
    }
}
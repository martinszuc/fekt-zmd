package jpeg;

import Jama.Matrix;
import Core.Helper;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Process {
    private BufferedImage image;
    private int[][] red, green, blue;
    private Matrix Y, Cb, Cr;
    private boolean isYCbCrConverted = false;

    public Process(BufferedImage image) {
        this.image = image;
        loadRGBArrays();
    }

    // Extracts RGB channels into arrays.
    private void loadRGBArrays() {
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
    }

    // Converts RGB to YCbCr.
    public void convertToYCbCr() {
        Matrix[] result = ColorTransform.convertOriginalRGBtoYcBcR(red, green, blue);
        Y = result[0];
        Cb = result[1];
        Cr = result[2];
        isYCbCrConverted = true;
    }

    public boolean isYCbCrConverted() {
        return isYCbCrConverted;
    }

    // Converts YCbCr back to RGB.
    public void convertToRGB() {
        Object[] result = ColorTransform.convertModifiedYcBcRtoRGB(Y, Cb, Cr);
        red = (int[][]) result[0];
        green = (int[][]) result[1];
        blue = (int[][]) result[2];
    }

    // Returns a BufferedImage from the current RGB arrays.
    public BufferedImage getRGBImage() {
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
        return result;
    }

    // Returns a BufferedImage for a single RGB channel.
    public BufferedImage getChannelImage(int[][] channel, String channelType) {
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
        return result;
    }

    // Returns a grayscale image for a YCbCr channel.
    public BufferedImage getChannelImage(Matrix channel) {
        if (channel == null) return null;
        int width = channel.getColumnDimension();
        int height = channel.getRowDimension();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double[][] array = channel.getArray();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (int) Math.round(array[y][x]);
                value = Math.max(0, Math.min(255, value));
                result.setRGB(x, y, new Color(value, value, value).getRGB());
            }
        }
        return result;
    }

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

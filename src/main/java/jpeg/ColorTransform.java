package jpeg;

import Jama.Matrix;
import Core.Helper;

public class ColorTransform {

    /**
     * Converts RGB arrays to YCbCr matrices using SDTV standard.
     * Y  = 0.257R + 0.504G + 0.098B + 16
     * Cb = -0.148R - 0.291G + 0.439B + 128
     * Cr = 0.439R - 0.368G - 0.071B + 128
     *
     * @param red Red channel array
     * @param green Green channel array
     * @param blue Blue channel array
     * @return Array of Matrices: [Y, Cb, Cr]
     */
    public static Matrix[] convertOriginalRGBtoYcBcR(int[][] red, int[][] green, int[][] blue) {
        int height = red.length;
        int width = red[0].length;

        Matrix Y = new Matrix(height, width);
        Matrix Cb = new Matrix(height, width);
        Matrix Cr = new Matrix(height, width);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int r = red[i][j];
                int g = green[i][j];
                int b = blue[i][j];

                Y.set(i, j, 0.257 * r + 0.504 * g + 0.098 * b + 16);
                Cb.set(i, j, -0.148 * r - 0.291 * g + 0.439 * b + 128);
                Cr.set(i, j, 0.439 * r - 0.368 * g - 0.071 * b + 128);
            }
        }

        return new Matrix[]{Y, Cb, Cr};
    }

    /**
     * Converts YCbCr matrices back to RGB arrays.
     * R = 1.164(Y - 16) + 1.596(Cr - 128)
     * G = 1.164(Y - 16) - 0.813(Cr - 128) - 0.391(Cb - 128)
     * B = 1.164(Y - 16) + 2.018(Cb - 128)
     *
     * @param Y Y component matrix
     * @param Cb Cb component matrix
     * @param Cr Cr component matrix
     * @return Array of RGB arrays: [Red, Green, Blue]
     */
    public static Object[] convertModifiedYcBcRtoRGB(Matrix Y, Matrix Cb, Matrix Cr) {
        int height = Y.getRowDimension();
        int width = Y.getColumnDimension();

        int[][] red = new int[height][width];
        int[][] green = new int[height][width];
        int[][] blue = new int[height][width];

        double[][] yArray = Y.getArray();
        double[][] cbArray = Cb.getArray();
        double[][] crArray = Cr.getArray();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double y = yArray[i][j];
                double cb = cbArray[i][j];
                double cr = crArray[i][j];

                int r = Helper.checkValue(Math.round(1.164 * (y - 16) + 1.596 * (cr - 128)));
                int g = Helper.checkValue(Math.round(1.164 * (y - 16) - 0.813 * (cr - 128) - 0.391 * (cb - 128)));
                int b = Helper.checkValue(Math.round(1.164 * (y - 16) + 2.018 * (cb - 128)));

                red[i][j] = r;
                green[i][j] = g;
                blue[i][j] = b;
            }
        }

        return new Object[]{red, green, blue};
    }
}
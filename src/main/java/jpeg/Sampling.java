package jpeg;

import Jama.Matrix;
import enums.SamplingType;
import utils.Logger;

public class Sampling {

    /**
     * Downsamples an input matrix according to the selected sampling pattern.
     * @param inputMatrix Input matrix to downsample
     * @param samplingType Selected sampling pattern
     * @return Downsampled matrix
     */
    public static Matrix sampleDown(Matrix inputMatrix, SamplingType samplingType) {
        if (inputMatrix == null) {
            Logger.warning("NULL matrix passed to sampleDown");
            return null;
        }

        int originalRows = inputMatrix.getRowDimension();
        int originalCols = inputMatrix.getColumnDimension();
        Logger.info("Starting downsampling operation: " + samplingType +
                " on matrix " + originalRows + "x" + originalCols);

        Matrix result;
        long startTime = System.currentTimeMillis();

        switch (samplingType) {
            case S_4_4_4:
                // 4:4:4 - No subsampling
                result = inputMatrix.copy();
                Logger.info("4:4:4 sampling - No downsampling performed (copied original)");
                break;

            case S_4_2_2:
                // 4:2:2 - Horizontal subsampling by factor of 2
                result = horizontalDownsample(inputMatrix);
                Logger.info("4:2:2 sampling - Horizontal downsampling by factor of 2");
                break;

            case S_4_2_0:
                // 4:2:0 - Horizontal and vertical subsampling by factor of 2
                Matrix horizontalDownsampled = horizontalDownsample(inputMatrix);
                result = verticalDownsample(horizontalDownsampled);
                Logger.info("4:2:0 sampling - Horizontal and vertical downsampling by factor of 2");
                break;

            case S_4_1_1:
                // 4:1:1 - Horizontal subsampling by factor of 4
                result = horizontalDownsample4to1(inputMatrix);
                Logger.info("4:1:1 sampling - Horizontal downsampling by factor of 4");
                break;

            default:
                result = inputMatrix.copy();
                Logger.warning("Unknown sampling type " + samplingType + ", no downsampling performed");
                break;
        }

        long endTime = System.currentTimeMillis();
        int newRows = result.getRowDimension();
        int newCols = result.getColumnDimension();

        // Calculate size reduction percentage
        int originalSize = originalRows * originalCols;
        int newSize = newRows * newCols;
        double reductionPercent = 100.0 * (originalSize - newSize) / originalSize;

        Logger.info("Downsampling completed in " + (endTime - startTime) + "ms");
        Logger.info("Original size: " + originalRows + "x" + originalCols + " = " + originalSize + " pixels");
        Logger.info("New size: " + newRows + "x" + newCols + " = " + newSize + " pixels");
        Logger.info("Size reduction: " + String.format("%.2f%%", reductionPercent));

        return result;
    }

    /**
     * Upsamples an input matrix according to the selected sampling pattern.
     * @param inputMatrix Input matrix to upsample
     * @param samplingType Selected sampling pattern
     * @return Upsampled matrix
     */
    public static Matrix sampleUp(Matrix inputMatrix, SamplingType samplingType) {
        if (inputMatrix == null) {
            Logger.warning("NULL matrix passed to sampleUp");
            return null;
        }

        int originalRows = inputMatrix.getRowDimension();
        int originalCols = inputMatrix.getColumnDimension();
        Logger.info("Starting upsampling operation: " + samplingType +
                " on matrix " + originalRows + "x" + originalCols);

        Matrix result;
        long startTime = System.currentTimeMillis();

        switch (samplingType) {
            case S_4_4_4:
                // 4:4:4 - No subsampling
                result = inputMatrix.copy();
                Logger.info("4:4:4 sampling - No upsampling performed (copied original)");
                break;

            case S_4_2_2:
                // 4:2:2 - Horizontal upsampling by factor of 2
                result = horizontalUpsample(inputMatrix);
                Logger.info("4:2:2 sampling - Horizontal upsampling by factor of 2");
                break;

            case S_4_2_0:
                // 4:2:0 - Vertical then horizontal upsampling by factor of 2
                Matrix verticalUpsampled = verticalUpsample(inputMatrix);
                result = horizontalUpsample(verticalUpsampled);
                Logger.info("4:2:0 sampling - Vertical and horizontal upsampling by factor of 2");
                break;

            case S_4_1_1:
                // 4:1:1 - Horizontal upsampling by factor of 4
                result = horizontalUpsample4to1(inputMatrix);
                Logger.info("4:1:1 sampling - Horizontal upsampling by factor of 4");
                break;

            default:
                result = inputMatrix.copy();
                Logger.warning("Unknown sampling type " + samplingType + ", no upsampling performed");
                break;
        }

        long endTime = System.currentTimeMillis();
        int newRows = result.getRowDimension();
        int newCols = result.getColumnDimension();

        // Calculate size increase percentage
        int originalSize = originalRows * originalCols;
        int newSize = newRows * newCols;
        double increasePercent = 100.0 * (newSize - originalSize) / originalSize;

        Logger.info("Upsampling completed in " + (endTime - startTime) + "ms");
        Logger.info("Original size: " + originalRows + "x" + originalCols + " = " + originalSize + " pixels");
        Logger.info("New size: " + newRows + "x" + newCols + " = " + newSize + " pixels");
        Logger.info("Size increase: " + String.format("%.2f%%", increasePercent));

        return result;
    }

    // Private helper methods for different sampling operations

    private static Matrix horizontalDownsample(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newCols = cols / 2;

        Logger.debug("Horizontal downsampling: " + rows + "x" + cols + " -> " + rows + "x" + newCols);

        Matrix result = new Matrix(rows, newCols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < newCols; j++) {
                result.set(i, j, matrix.get(i, j * 2));
            }
        }

        return result;
    }

    private static Matrix verticalDownsample(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newRows = rows / 2;

        Logger.debug("Vertical downsampling: " + rows + "x" + cols + " -> " + newRows + "x" + cols);

        Matrix result = new Matrix(newRows, cols);

        for (int i = 0; i < newRows; i++) {
            for (int j = 0; j < cols; j++) {
                result.set(i, j, matrix.get(i * 2, j));
            }
        }

        return result;
    }

    private static Matrix horizontalDownsample4to1(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newCols = cols / 4;

        Logger.debug("Horizontal 4:1:1 downsampling: " + rows + "x" + cols + " -> " + rows + "x" + newCols);

        Matrix result = new Matrix(rows, newCols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < newCols; j++) {
                result.set(i, j, matrix.get(i, j * 4));
            }
        }

        return result;
    }

    private static Matrix horizontalUpsample(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newCols = cols * 2;

        Logger.debug("Horizontal upsampling: " + rows + "x" + cols + " -> " + rows + "x" + newCols);

        Matrix result = new Matrix(rows, newCols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double value = matrix.get(i, j);
                result.set(i, j * 2, value);     // Even columns
                result.set(i, j * 2 + 1, value); // Odd columns
            }
        }

        return result;
    }

    private static Matrix verticalUpsample(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newRows = rows * 2;

        Logger.debug("Vertical upsampling: " + rows + "x" + cols + " -> " + newRows + "x" + cols);

        Matrix result = new Matrix(newRows, cols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double value = matrix.get(i, j);
                result.set(i * 2, j, value);     // Even rows
                result.set(i * 2 + 1, j, value); // Odd rows
            }
        }

        return result;
    }

    private static Matrix horizontalUpsample4to1(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newCols = cols * 4;

        Logger.debug("Horizontal 4:1:1 upsampling: " + rows + "x" + cols + " -> " + rows + "x" + newCols);

        Matrix result = new Matrix(rows, newCols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double value = matrix.get(i, j);
                // Expand each sample to 4 pixels
                result.set(i, j * 4, value);
                result.set(i, j * 4 + 1, value);
                result.set(i, j * 4 + 2, value);
                result.set(i, j * 4 + 3, value);
            }
        }

        return result;
    }
}
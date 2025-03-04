package jpeg;

import Jama.Matrix;
import enums.SamplingType;

public class Sampling {

    public static Matrix sampleDown(Matrix inputMatrix, SamplingType samplingType) {
        if (inputMatrix == null) return null;

        switch (samplingType) {
            case S_4_4_4:
                return inputMatrix.copy();
            case S_4_2_2:
                return horizontalDownsample(inputMatrix);
            case S_4_2_0:
                Matrix horizontalDownsampled = horizontalDownsample(inputMatrix);
                return verticalDownsample(horizontalDownsampled);
            case S_4_1_1:
                return horizontalDownsample4to1(inputMatrix);
            default:
                return inputMatrix.copy();
        }
    }

    public static Matrix sampleUp(Matrix inputMatrix, SamplingType samplingType) {
        if (inputMatrix == null) return null;

        switch (samplingType) {
            case S_4_4_4:
                return inputMatrix.copy();
            case S_4_2_2:
                return horizontalUpsample(inputMatrix);
            case S_4_2_0:
                Matrix verticalUpsampled = verticalUpsample(inputMatrix);
                return horizontalUpsample(verticalUpsampled);
            case S_4_1_1:
                return horizontalUpsample4to1(inputMatrix);
            default:
                return inputMatrix.copy();
        }
    }

    private static Matrix horizontalDownsample(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newCols = cols / 2;

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

        Matrix result = new Matrix(rows, newCols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double value = matrix.get(i, j);
                result.set(i, j * 2, value);
                result.set(i, j * 2 + 1, value);
            }
        }

        return result;
    }

    private static Matrix verticalUpsample(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newRows = rows * 2;

        Matrix result = new Matrix(newRows, cols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double value = matrix.get(i, j);
                result.set(i * 2, j, value);
                result.set(i * 2 + 1, j, value);
            }
        }

        return result;
    }

    private static Matrix horizontalUpsample4to1(Matrix matrix) {
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        int newCols = cols * 4;

        Matrix result = new Matrix(rows, newCols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double value = matrix.get(i, j);
                result.set(i, j * 4, value);
                result.set(i, j * 4 + 1, value);
                result.set(i, j * 4 + 2, value);
                result.set(i, j * 4 + 3, value);
            }
        }

        return result;
    }
}
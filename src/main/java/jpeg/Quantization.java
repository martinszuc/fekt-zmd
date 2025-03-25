package jpeg;

import Jama.Matrix;

public class Quantization {
    // Standard JPEG quantization matrices
    private static final double[][] LUMINANCE_MATRIX = {
            {16, 11, 10, 16, 24, 40, 51, 61},
            {12, 12, 14, 19, 26, 58, 60, 55},
            {14, 13, 16, 24, 40, 57, 69, 56},
            {14, 17, 22, 29, 51, 87, 80, 62},
            {18, 22, 37, 56, 68, 109, 103, 77},
            {24, 35, 55, 64, 81, 104, 113, 92},
            {49, 64, 78, 87, 103, 121, 120, 101},
            {72, 92, 95, 98, 112, 100, 103, 99}
    };

    private static final double[][] CHROMINANCE_MATRIX = {
            {17, 18, 24, 47, 99, 99, 99, 99},
            {18, 21, 26, 66, 99, 99, 99, 99},
            {24, 26, 56, 99, 99, 99, 99, 99},
            {47, 66, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99}
    };

    /**
     * Gets a quantization matrix for the specified parameters.
     * @param blockSize Size of the block (e.g., 4, 8, 16)
     * @param quality Quality value between 1 and 100
     * @param matrixY True for luminance (Y), false for chrominance (Cb, Cr)
     * @return Quantization matrix scaled to the right size and quality
     */
    public static Matrix getQuantizationMatrix(int blockSize, double quality, boolean matrixY) {
        // For quality 100, return a matrix with all 1's
        if (quality == 100) {
            return new Matrix(blockSize, blockSize, 1.0);
        }

        // Calculate alpha based on quality
        double alpha;
        if (quality >= 1 && quality <= 50) {
            alpha = 50.0 / quality;
        } else { // quality between 51 and 99
            alpha = 2.0 - (2.0 * quality / 100.0);
        }

        // Select the base matrix
        double[][] baseMatrix = matrixY ? LUMINANCE_MATRIX : CHROMINANCE_MATRIX;

        // Create the result matrix
        Matrix result = new Matrix(blockSize, blockSize);

        // Resize and scale the matrix
        if (blockSize == 8) {
            // No resize needed, just apply quality scaling
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    result.set(i, j, alpha * baseMatrix[i][j]);
                }
            }
        } else if (blockSize < 8) {
            // Downsize the matrix by skipping elements
            int step = 8 / blockSize;
            for (int i = 0; i < blockSize; i++) {
                for (int j = 0; j < blockSize; j++) {
                    result.set(i, j, alpha * baseMatrix[i * step][j * step]);
                }
            }
        } else {
            // Upsize the matrix by duplicating elements
            int step = blockSize / 8;
            for (int i = 0; i < blockSize; i++) {
                for (int j = 0; j < blockSize; j++) {
                    result.set(i, j, alpha * baseMatrix[i / step][j / step]);
                }
            }
        }

        return result;
    }

    /**
     * Quantizes a matrix using the specified parameters.
     * Formula: Squv = [Suv / Quv]
     */
    public static Matrix quantize(Matrix input, int blockSize, double quality, boolean matrixY) {
        if (input == null) {
            return null;
        }

        Matrix quantMatrix = getQuantizationMatrix(blockSize, quality, matrixY);
        Matrix result = new Matrix(input.getRowDimension(), input.getColumnDimension());

        int rows = input.getRowDimension();
        int cols = input.getColumnDimension();

        // Process the input in blocks
        for (int blockRow = 0; blockRow < rows; blockRow += blockSize) {
            for (int blockCol = 0; blockCol < cols; blockCol += blockSize) {
                // Determine actual block dimensions (may be smaller at edges)
                int endRow = Math.min(blockRow + blockSize - 1, rows - 1);
                int endCol = Math.min(blockCol + blockSize - 1, cols - 1);
                int blockHeight = endRow - blockRow + 1;
                int blockWidth = endCol - blockCol + 1;

                // Only process full-sized blocks
                if (blockHeight == blockSize && blockWidth == blockSize) {
                    Matrix block = input.getMatrix(blockRow, endRow, blockCol, endCol);
                    Matrix quantizedBlock = new Matrix(blockSize, blockSize);

                    // Apply quantization to each element
                    for (int i = 0; i < blockSize; i++) {
                        for (int j = 0; j < blockSize; j++) {
                            double value = block.get(i, j) / quantMatrix.get(i, j);

                            // Apply special rounding
                            if (value > -0.2 && value < 0.2) {
                                // Round to 2 decimal places for values between -0.2 and 0.2
                                value = Math.round(value * 100) / 100.0;
                            } else {
                                // Round to 1 decimal place for other values
                                value = Math.round(value * 10) / 10.0;
                            }

                            quantizedBlock.set(i, j, value);
                        }
                    }

                    // Place the quantized block back
                    result.setMatrix(blockRow, endRow, blockCol, endCol, quantizedBlock);
                } else {
                    // For partial blocks, just copy the original values
                    for (int r = blockRow; r <= endRow; r++) {
                        for (int c = blockCol; c <= endCol; c++) {
                            result.set(r, c, input.get(r, c));
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Applies inverse quantization to a quantized matrix.
     * Formula: Siquv = Squv * Quv
     */
    public static Matrix inverseQuantize(Matrix input, int blockSize, double quality, boolean matrixY) {
        if (input == null) {
            return null;
        }

        Matrix quantMatrix = getQuantizationMatrix(blockSize, quality, matrixY);
        Matrix result = new Matrix(input.getRowDimension(), input.getColumnDimension());

        int rows = input.getRowDimension();
        int cols = input.getColumnDimension();

        // Process the input in blocks
        for (int blockRow = 0; blockRow < rows; blockRow += blockSize) {
            for (int blockCol = 0; blockCol < cols; blockCol += blockSize) {
                // Determine actual block dimensions (may be smaller at edges)
                int endRow = Math.min(blockRow + blockSize - 1, rows - 1);
                int endCol = Math.min(blockCol + blockSize - 1, cols - 1);
                int blockHeight = endRow - blockRow + 1;
                int blockWidth = endCol - blockCol + 1;

                // Only process full-sized blocks
                if (blockHeight == blockSize && blockWidth == blockSize) {
                    Matrix block = input.getMatrix(blockRow, endRow, blockCol, endCol);
                    Matrix invQuantizedBlock = new Matrix(blockSize, blockSize);

                    // Apply inverse quantization to each element
                    for (int i = 0; i < blockSize; i++) {
                        for (int j = 0; j < blockSize; j++) {
                            invQuantizedBlock.set(i, j, block.get(i, j) * quantMatrix.get(i, j));
                        }
                    }

                    // Place the inverse quantized block back
                    result.setMatrix(blockRow, endRow, blockCol, endCol, invQuantizedBlock);
                } else {
                    // For partial blocks, just copy the original values
                    for (int r = blockRow; r <= endRow; r++) {
                        for (int c = blockCol; c <= endCol; c++) {
                            result.set(r, c, input.get(r, c));
                        }
                    }
                }
            }
        }

        return result;
    }
}
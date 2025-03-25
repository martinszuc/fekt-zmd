package jpeg;

import Jama.Matrix;

public class Quantization {

    // Standard quantization matrices for JPEG compression
    private static final double[][] quantizationMatrix8Y = {
            {16, 11, 10, 16, 24, 40, 51, 61},
            {12, 12, 14, 19, 26, 58, 60, 55},
            {14, 13, 16, 24, 40, 57, 69, 56},
            {14, 17, 22, 29, 51, 87, 80, 62},
            {18, 22, 37, 56, 68, 109, 103, 77},
            {24, 35, 55, 64, 81, 104, 113, 92},
            {49, 64, 78, 87, 103, 121, 120, 101},
            {72, 92, 95, 98, 112, 100, 103, 99}
    };

    private static final double[][] quantizationMatrix8C = {
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
     * Get quantization matrix adjusted for block size and quality
     *
     * @param blockSize Size of the block (must be a power of 2)
     * @param quality Quality factor (1-100)
     * @param matrixY If true, returns Y matrix, otherwise returns C matrix
     * @return Quantization matrix
     */
    public static Matrix getQuantizationMatrix(int blockSize, double quality, boolean matrixY) {
        // Select appropriate base matrix based on type (Y or C)
        double[][] baseMatrix = matrixY ? quantizationMatrix8Y : quantizationMatrix8C;

        // Handle special case for quality 100
        if (quality == 100) {
            return new Matrix(blockSize, blockSize, 1.0);
        }

        // Calculate alpha coefficient based on quality
        double alpha;
        if (quality >= 50) {
            alpha = 2.0 - (2.0 * quality / 100.0);
        } else {
            alpha = 50.0 / quality;
        }

        // Create adjusted matrix with appropriate size
        Matrix result = new Matrix(blockSize, blockSize);

        // Calculate scale factor for resizing
        int scaleFactor = blockSize / 8;

        // Fill matrix with scaled values
        for (int i = 0; i < blockSize; i++) {
            for (int j = 0; j < blockSize; j++) {
                // Map position to original 8x8 matrix
                int sourceRow = i / scaleFactor;
                int sourceCol = j / scaleFactor;

                // Apply quality factor
                double value = baseMatrix[sourceRow][sourceCol] * alpha;
                result.set(i, j, value);
            }
        }

        return result;
    }

    /**
     * Quantize a matrix using JPEG quantization
     *
     * @param input Input matrix
     * @param blockSize Block size for processing
     * @param quality Quality factor (1-100)
     * @param matrixY If true, uses Y quantization matrix, otherwise uses C matrix
     * @return Quantized matrix
     */
    public static Matrix quantize(Matrix input, int blockSize, double quality, boolean matrixY) {
        if (input == null) {
            return null;
        }

        int rows = input.getRowDimension();
        int cols = input.getColumnDimension();
        Matrix output = new Matrix(rows, cols);

        // Get quantization matrix
        Matrix quantMatrix = getQuantizationMatrix(blockSize, quality, matrixY);

        // Process image in blocks
        for (int i = 0; i < rows; i += blockSize) {
            for (int j = 0; j < cols; j += blockSize) {
                // Determine block boundaries (handle edge cases)
                int endRow = Math.min(i + blockSize - 1, rows - 1);
                int endCol = Math.min(j + blockSize - 1, cols - 1);
                int blockHeight = endRow - i + 1;
                int blockWidth = endCol - j + 1;

                // Only process full blocks
                if (blockHeight == blockSize && blockWidth == blockSize) {
                    // Extract block
                    Matrix block = input.getMatrix(i, endRow, j, endCol);

                    // Create quantized block
                    Matrix quantizedBlock = new Matrix(blockSize, blockSize);

                    // Quantize each value in the block
                    for (int m = 0; m < blockSize; m++) {
                        for (int n = 0; n < blockSize; n++) {
                            double value = block.get(m, n) / quantMatrix.get(m, n);

                            // Special rounding rules
                            if (value > -0.2 && value < 0.2) {
                                // Round to 2 decimal places for small values
                                value = Math.round(value * 100) / 100.0;
                            } else {
                                // Round to 1 decimal place for other values
                                value = Math.round(value * 10) / 10.0;
                            }

                            quantizedBlock.set(m, n, value);
                        }
                    }

                    // Put block back into output
                    output.setMatrix(i, endRow, j, endCol, quantizedBlock);
                } else {
                    // For partial blocks, just copy original values
                    for (int r = i; r <= endRow; r++) {
                        for (int c = j; c <= endCol; c++) {
                            output.set(r, c, input.get(r, c));
                        }
                    }
                }
            }
        }

        return output;
    }

    /**
     * Perform inverse quantization
     *
     * @param input Quantized matrix to be inverse-quantized
     * @param blockSize Block size for processing
     * @param quality Quality factor (1-100)
     * @param matrixY If true, uses Y quantization matrix, otherwise uses C matrix
     * @return Inverse-quantized matrix
     */
    public static Matrix inverseQuantize(Matrix input, int blockSize, double quality, boolean matrixY) {
        if (input == null) {
            return null;
        }

        int rows = input.getRowDimension();
        int cols = input.getColumnDimension();
        Matrix output = new Matrix(rows, cols);

        // Get quantization matrix
        Matrix quantMatrix = getQuantizationMatrix(blockSize, quality, matrixY);

        // Process image in blocks
        for (int i = 0; i < rows; i += blockSize) {
            for (int j = 0; j < cols; j += blockSize) {
                // Determine block boundaries (handle edge cases)
                int endRow = Math.min(i + blockSize - 1, rows - 1);
                int endCol = Math.min(j + blockSize - 1, cols - 1);
                int blockHeight = endRow - i + 1;
                int blockWidth = endCol - j + 1;

                // Only process full blocks
                if (blockHeight == blockSize && blockWidth == blockSize) {
                    // Extract block
                    Matrix block = input.getMatrix(i, endRow, j, endCol);

                    // Inverse quantize block by element-wise multiplication
                    Matrix invQuantizedBlock = block.arrayTimes(quantMatrix);

                    // Put block back into output
                    output.setMatrix(i, endRow, j, endCol, invQuantizedBlock);
                } else {
                    // For partial blocks, just copy original values
                    for (int r = i; r <= endRow; r++) {
                        for (int c = j; c <= endCol; c++) {
                            output.set(r, c, input.get(r, c));
                        }
                    }
                }
            }
        }

        return output;
    }
}
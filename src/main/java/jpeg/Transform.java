package jpeg;

import Jama.Matrix;
import enums.TransformType;

/**
 * Class for performing DCT and WHT transformations on matrices.
 * Used for transforming image data into frequency domain and back.
 */
public class Transform {

    /**
     * Gets a transformation matrix for the specified transform type and block size.
     *
     * @param type The type of transform (DCT or WHT)
     * @param blockSize The size of the square transformation matrix
     * @return The transformation matrix
     */
    public static Matrix getTransformMatrix(TransformType type, int blockSize) {
        switch (type) {
            case DCT:
                return createDCTMatrix(blockSize);
            case WHT:
                return createWHTMatrix(blockSize);
            default:
                throw new IllegalArgumentException("Unsupported transform type: " + type);
        }
    }

    /**
     * Transforms a matrix using the specified transform type and block size.
     * For forward transform: Θ = AXA^T
     *
     * @param input The input matrix to transform
     * @param type The type of transform (DCT or WHT)
     * @param blockSize The size of the transform blocks
     * @return The transformed matrix
     */
    public static Matrix transform(Matrix input, TransformType type, int blockSize) {
        Matrix transformMatrix = getTransformMatrix(type, blockSize);

        if (input.getRowDimension() == blockSize && input.getColumnDimension() == blockSize) {
            // Transform a single block: A * X * A^T
            return transformMatrix.times(input).times(transformMatrix.transpose());
        } else {
            // Transform multiple blocks
            return transformByBlocks(input, transformMatrix, blockSize, false);
        }
    }

    /**
     * Performs inverse transformation on a matrix using the specified transform type and block size.
     * For inverse transform: X = A^T * Θ * A
     *
     * @param input The input matrix to inverse transform
     * @param type The type of transform (DCT or WHT)
     * @param blockSize The size of the transform blocks
     * @return The inverse transformed matrix
     */
    public static Matrix inverseTransform(Matrix input, TransformType type, int blockSize) {
        Matrix transformMatrix = getTransformMatrix(type, blockSize);

        if (input.getRowDimension() == blockSize && input.getColumnDimension() == blockSize) {
            // Inverse transform a single block: A^T * X * A
            return transformMatrix.transpose().times(input).times(transformMatrix);
        } else {
            // Inverse transform multiple blocks
            return transformByBlocks(input, transformMatrix, blockSize, true);
        }
    }

    /**
     * Creates a DCT transform matrix of the specified size.
     * Based on formulas 5.11 and 5.12 from the specification.
     *
     * @param size The size of the square matrix
     * @return The DCT transform matrix
     */
    private static Matrix createDCTMatrix(int size) {
        Matrix matrix = new Matrix(size, size);

        // For i=0 (first row): simplified because cos(0) = 1
        double firstRowValue = Math.sqrt(1.0 / size);
        for (int j = 0; j < size; j++) {
            matrix.set(0, j, firstRowValue);
        }

        // For i=1 to size-1 (remaining rows)
        for (int i = 1; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix.set(i, j, Math.sqrt(2.0 / size) * Math.cos((2 * j + 1) * i * Math.PI / (2 * size)));
            }
        }

        return matrix;
    }

    /**
     * Creates a WHT transform matrix of the specified size.
     *
     * @param size The size of the square matrix (must be a power of 2)
     * @return The WHT transform matrix
     */
    private static Matrix createWHTMatrix(int size) {
        // Check if size is a power of 2
        if ((size & (size - 1)) != 0) {
            throw new IllegalArgumentException("Block size must be a power of 2 for WHT");
        }

        // Generate Hadamard matrix without normalization
        double[][] hadamard = generateHadamardMatrix(size);

        // Create the Walsh-Hadamard transform matrix with normalization
        Matrix wht = new Matrix(hadamard);
        return wht.times(1.0 / Math.sqrt(size));
    }

    /**
     * Generates a Hadamard matrix of the specified size (without normalization).
     * Uses the recursive definition: H_n = [[H_(n/2), H_(n/2)], [H_(n/2), -H_(n/2)]]
     *
     * @param size The size of the matrix (must be a power of 2)
     * @return The Hadamard matrix
     */
    private static double[][] generateHadamardMatrix(int size) {
        double[][] hadamard = new double[size][size];

        // Base case: H_1 = [[1]]
        if (size == 1) {
            hadamard[0][0] = 1.0;
            return hadamard;
        }

        // Generate the smaller Hadamard matrix
        double[][] smallerHadamard = generateHadamardMatrix(size / 2);

        // Fill the four quadrants according to the recursive definition
        for (int i = 0; i < size / 2; i++) {
            for (int j = 0; j < size / 2; j++) {
                double value = smallerHadamard[i][j];
                hadamard[i][j] = value;                   // Top-left
                hadamard[i][j + size / 2] = value;        // Top-right
                hadamard[i + size / 2][j] = value;        // Bottom-left
                hadamard[i + size / 2][j + size / 2] = -value; // Bottom-right
            }
        }

        return hadamard;
    }

    /**
     * Transforms a matrix by processing it in blocks of the specified size.
     *
     * @param input The input matrix
     * @param transformMatrix The transform matrix to use
     * @param blockSize The size of each block
     * @param inverse Whether to perform inverse transformation
     * @return The transformed matrix
     */
    private static Matrix transformByBlocks(Matrix input, Matrix transformMatrix, int blockSize, boolean inverse) {
        int rows = input.getRowDimension();
        int cols = input.getColumnDimension();

        // Create a new matrix for the result
        Matrix result = new Matrix(rows, cols);

        // Process each block
        for (int i = 0; i < rows; i += blockSize) {
            for (int j = 0; j < cols; j += blockSize) {
                // Determine the actual block dimensions (may be smaller at edges)
                int endRow = Math.min(i + blockSize - 1, rows - 1);
                int endCol = Math.min(j + blockSize - 1, cols - 1);
                int blockHeight = endRow - i + 1;
                int blockWidth = endCol - j + 1;

                // Only process full-sized blocks
                if (blockHeight == blockSize && blockWidth == blockSize) {
                    // Extract the block
                    Matrix block = input.getMatrix(i, endRow, j, endCol);

                    // Transform the block
                    Matrix transformedBlock;
                    if (inverse) {
                        transformedBlock = transformMatrix.transpose().times(block).times(transformMatrix);
                    } else {
                        transformedBlock = transformMatrix.times(block).times(transformMatrix.transpose());
                    }

                    // Put the transformed block back
                    result.setMatrix(i, endRow, j, endCol, transformedBlock);
                } else {
                    // For partial blocks, just copy the original values
                    for (int r = i; r <= endRow; r++) {
                        for (int c = j; c <= endCol; c++) {
                            result.set(r, c, input.get(r, c));
                        }
                    }
                }
            }
        }

        return result;
    }
}
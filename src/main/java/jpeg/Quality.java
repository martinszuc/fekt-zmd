package jpeg;

import Jama.Matrix;
import utils.Logger;

/**
 * Implements objective quality assessment metrics for image comparison.
 */
public class Quality {

    /**
     * Calculates the Mean Squared Error between two matrices.
     * MSE = (1/MN) * Σ[x(m,n) - x'(m,n)]²
     */
    public static double countMSE(double[][] original, double[][] modified) {
        Logger.info("Calculating MSE");
        int height = original.length;
        int width = original[0].length;
        double sum = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double diff = original[i][j] - modified[i][j];
                sum += diff * diff;
            }
        }

        double mse = sum / (height * width);
        Logger.info("MSE calculation result: " + mse);
        return mse;
    }

    /**
     * Calculates the Mean Absolute Error between two matrices.
     * MAE = (1/MN) * Σ|x(m,n) - x'(m,n)|
     */
    public static double countMAE(double[][] original, double[][] modified) {
        Logger.info("Calculating MAE");
        int height = original.length;
        int width = original[0].length;
        double sum = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                sum += Math.abs(original[i][j] - modified[i][j]);
            }
        }

        double mae = sum / (height * width);
        Logger.info("MAE calculation result: " + mae);
        return mae;
    }

    /**
     * Calculates the Sum of Absolute Errors between two matrices.
     * SAE = Σ|x(m,n) - x'(m,n)|
     */
    public static double countSAE(double[][] original, double[][] modified) {
        Logger.info("Calculating SAE");
        int height = original.length;
        int width = original[0].length;
        double sum = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                sum += Math.abs(original[i][j] - modified[i][j]);
            }
        }

        Logger.info("SAE calculation result: " + sum);
        return sum;
    }

    /**
     * Calculates the Peak Signal-to-Noise Ratio using the MSE.
     * PSNR = 10log₁₀[(2ⁿ-1)² / MSE]
     * For 8-bit images, (2ⁿ-1)² = 255²
     */
    public static double countPSNR(double MSE) {
        Logger.info("Calculating PSNR");
        double maxValue = 255.0;
        double psnr = 10 * Math.log10(Math.pow(maxValue, 2) / MSE);
        Logger.info("PSNR calculation result: " + psnr + " dB");
        return psnr;
    }

    /**
     * Calculates PSNR for RGB images by averaging MSE values from three color channels.
     */
    public static double countPSNRforRGB(double mseRed, double mseGreen, double mseBlue) {
        Logger.info("Calculating PSNR for RGB image");
        double avgMSE = (mseRed + mseGreen + mseBlue) / 3.0;
        double psnr = countPSNR(avgMSE);
        Logger.info("RGB PSNR calculation result: " + psnr + " dB");
        return psnr;
    }

    /**
     * Calculates the Structural Similarity Index Measure (SSIM) between two matrices.
     * SSIM = [(2μₓμᵧ + C₁)(2σₓᵧ + C₂)]/[(μₓ² + μᵧ² + C₁)(σₓ² + σᵧ² + C₂)]
     */
    public static double countSSIM(Matrix original, Matrix modified) {
        Logger.info("Calculating SSIM");

        // Constants for stabilization
        double L = 255.0; // Dynamic range
        double k1 = 0.01;
        double k2 = 0.03;
        double C1 = Math.pow(k1 * L, 2);
        double C2 = Math.pow(k2 * L, 2);

        // Get arrays from matrices
        double[][] origArray = original.getArray();
        double[][] modArray = modified.getArray();

        // Calculate means
        double muX = calculateMean(origArray);
        double muY = calculateMean(modArray);

        // Calculate variances
        double sigmaX2 = calculateVariance(origArray, muX);
        double sigmaY2 = calculateVariance(modArray, muY);

        // Calculate covariance
        double sigmaXY = calculateCovariance(origArray, modArray, muX, muY);

        // Calculate SSIM
        double numerator = (2 * muX * muY + C1) * (2 * sigmaXY + C2);
        double denominator = (muX * muX + muY * muY + C1) * (sigmaX2 + sigmaY2 + C2);
        double ssim = numerator / denominator;

        Logger.info("SSIM calculation result: " + ssim);
        return ssim;
    }

    /**
     * Calculates the Mean SSIM (MSSIM) by dividing the image into 8x8 blocks
     * and averaging the SSIM values.
     */
    public static double countMSSIM(Matrix original, Matrix modified) {
        Logger.info("Calculating MSSIM");

        int rows = original.getRowDimension();
        int cols = original.getColumnDimension();
        int blockSize = 8; // 8x8 blocks

        double totalSSIM = 0;
        int blockCount = 0;

        for (int i = 0; i <= rows - blockSize; i += blockSize) {
            for (int j = 0; j <= cols - blockSize; j += blockSize) {
                Matrix origBlock = original.getMatrix(i, i + blockSize - 1, j, j + blockSize - 1);
                Matrix modBlock = modified.getMatrix(i, i + blockSize - 1, j, j + blockSize - 1);

                totalSSIM += countSSIM(origBlock, modBlock);
                blockCount++;
            }
        }

        double mssim = (blockCount > 0) ? totalSSIM / blockCount : 0;
        Logger.info("MSSIM calculation result: " + mssim);
        return mssim;
    }

    // Helper methods for SSIM calculation

    private static double calculateMean(double[][] array) {
        int rows = array.length;
        int cols = array[0].length;
        double sum = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sum += array[i][j];
            }
        }

        return sum / (rows * cols);
    }

    private static double calculateVariance(double[][] array, double mean) {
        int rows = array.length;
        int cols = array[0].length;
        double sum = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double diff = array[i][j] - mean;
                sum += diff * diff;
            }
        }

        return sum / (rows * cols - 1);
    }

    private static double calculateCovariance(double[][] array1, double[][] array2, double mean1, double mean2) {
        int rows = array1.length;
        int cols = array1[0].length;
        double sum = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sum += (array1[i][j] - mean1) * (array2[i][j] - mean2);
            }
        }

        return sum / (rows * cols - 1);
    }
}
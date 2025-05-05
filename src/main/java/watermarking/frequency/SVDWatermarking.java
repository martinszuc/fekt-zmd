package watermarking.frequency;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import utils.Logger;
import watermarking.core.AbstractWatermarking;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Implementation of SVD (Singular Value Decomposition) based watermarking.
 * This technique embeds the watermark by modifying the singular values
 * of the image, providing good robustness against various attacks.
 */
public class SVDWatermarking extends AbstractWatermarking {

    // Class to store SVD components for watermark extraction
    private static class SVDComponents {
        public Matrix U;
        public Matrix S;
        public Matrix V;
        public double alpha;

        public SVDComponents(Matrix U, Matrix S, Matrix V, double alpha) {
            this.U = U;
            this.S = S;
            this.V = V;
            this.alpha = alpha;
        }
    }

    // Static storage for SVD components (in a real application, this would be saved to a file)
    private static SVDComponents savedComponents = null;

    @Override
    public Matrix embed(Matrix imageMatrix, BufferedImage watermark, Object... params) {
        // Extract parameters
        double alpha = (double) params[0]; // Embedding strength

        Logger.info("Embedding watermark using SVD with strength " + alpha);

        if (imageMatrix == null || watermark == null) {
            Logger.error("Cannot embed watermark: null input");
            return null;
        }

        // Convert watermark to matrix format (grayscale)
        int watermarkWidth = watermark.getWidth();
        int watermarkHeight = watermark.getHeight();
        Matrix watermarkMatrix = new Matrix(watermarkHeight, watermarkWidth);

        for (int y = 0; y < watermarkHeight; y++) {
            for (int x = 0; x < watermarkWidth; x++) {
                Color color = new Color(watermark.getRGB(x, y));
                int gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                watermarkMatrix.set(y, x, gray);
            }
        }

        // Perform SVD on the original image
        SingularValueDecomposition svd = imageMatrix.svd();
        Matrix U = svd.getU();
        Matrix S = svd.getS();
        Matrix V = svd.getV();

        // Scale watermark to match singular values matrix dimensions
        int n = Math.min(S.getRowDimension(), S.getColumnDimension());
        Matrix scaledWatermark = new Matrix(n, n);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i < watermarkHeight && j < watermarkWidth) {
                    scaledWatermark.set(i, j, watermarkMatrix.get(i, j));
                }
            }
        }

        // Modify singular values with watermark
        Matrix Sw = S.copy();
        for (int i = 0; i < n; i++) {
            Sw.set(i, i, S.get(i, i) + alpha * scaledWatermark.get(i, i));
        }

        // Reconstruct watermarked image
        Matrix watermarkedMatrix = U.times(Sw).times(V.transpose());

        // Store the original SVD components for extraction
        // In a real application, these would be saved to a file
        savedComponents = new SVDComponents(U, S, V, alpha);

        Logger.info("Watermark embedded successfully using SVD");
        return watermarkedMatrix;
    }

    @Override
    public BufferedImage extract(Matrix watermarkedMatrix, int width, int height, Object... params) {
        Logger.info("Extracting watermark using SVD");

        if (watermarkedMatrix == null) {
            Logger.error("Cannot extract watermark: null input");
            return null;
        }

        // Retrieve SVD components
        if (savedComponents == null) {
            Logger.error("Cannot extract watermark: missing SVD components");
            return null;
        }

        Matrix U = savedComponents.U;
        Matrix S = savedComponents.S;
        Matrix V = savedComponents.V;
        double alpha = savedComponents.alpha;

        // Perform SVD on the watermarked image
        SingularValueDecomposition svd = watermarkedMatrix.svd();
        Matrix Sw = svd.getS();

        // Extract watermark by comparing singular values
        int n = Math.min(S.getRowDimension(), S.getColumnDimension());
        Matrix extractedWatermark = new Matrix(n, n);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    extractedWatermark.set(i, j, (Sw.get(i, i) - S.get(i, i)) / alpha);
                }
            }
        }

        // Convert to BufferedImage
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y < n && x < n) {
                    int value = (int) Math.round(extractedWatermark.get(y, x));
                    value = Math.max(0, Math.min(255, value));
                    result.setRGB(x, y, new Color(value, value, value).getRGB());
                } else {
                    result.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        Logger.info("Watermark extracted successfully using SVD");
        return result;
    }

    @Override
    public String getTechniqueName() {
        return "SVD (Singular Value Decomposition)";
    }
}
package watermarking.frequency;

import Jama.Matrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import watermarking.core.AbstractWatermarking;
import watermarking.core.WatermarkEvaluation;
import watermarking.core.WatermarkingFactory;
import watermarking.attacks.WatermarkAttacks;
import enums.WatermarkType;
import jpeg.Process;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DCT Watermarking implementation.
 */
public class DCTWatermarkingTest {

    private AbstractWatermarking dctWatermarking;
    private BufferedImage testImage;
    private BufferedImage watermark;
    private Process imageProcess;
    private Matrix yComponent;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize the DCT watermarking instance
        dctWatermarking = WatermarkingFactory.createWatermarking(WatermarkType.DCT);

        // Create a test image (or load from resources)
        testImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 256, 256);
        g.dispose();

        // Fill with some pattern
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                int value = (x + y) % 256;
                testImage.setRGB(x, y, new Color(value, value, value).getRGB());
            }
        }

        // Create a watermark image (or load from resources)
        watermark = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = watermark.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 32, 32);
        g2.setColor(Color.BLACK);

        // Create a simple pattern as watermark
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                if ((x / 4 + y / 4) % 2 == 0) {
                    watermark.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        g2.dispose();

        // Set up image process
        imageProcess = new Process(testImage);
        imageProcess.convertToYCbCr();
        yComponent = imageProcess.getY();
    }

    @Test
    @DisplayName("Test DCT Watermarking Embed and Extract")
    void testEmbedAndExtract() {
        // Define parameters for DCT watermarking
        int blockSize = 8;
        int[] coefPair1 = {3, 1}; // Middle-frequency coefficient
        int[] coefPair2 = {4, 1}; // Another middle-frequency coefficient
        double strength = 20.0;   // Embedding strength

        // Embed the watermark
        Matrix watermarkedMatrix = dctWatermarking.embed(
                yComponent,
                watermark,
                blockSize,
                coefPair1,
                coefPair2,
                strength
        );

        // Verify the watermarked matrix is not null
        assertNotNull(watermarkedMatrix);

        // Verify the dimensions of watermarked matrix match the original
        assertEquals(yComponent.getRowDimension(), watermarkedMatrix.getRowDimension());
        assertEquals(yComponent.getColumnDimension(), watermarkedMatrix.getColumnDimension());

        // Extract the watermark
        BufferedImage extractedWatermark = dctWatermarking.extract(
                watermarkedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                blockSize,
                coefPair1,
                coefPair2
        );

        // Verify the extracted watermark is not null
        assertNotNull(extractedWatermark);

        // Verify the dimensions of extracted watermark match the original
        assertEquals(watermark.getWidth(), extractedWatermark.getWidth());
        assertEquals(watermark.getHeight(), extractedWatermark.getHeight());

        // Calculate BER (Bit Error Rate) - should be 0 for perfect extraction
        double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);
        assertEquals(0.0, ber, 0.01, "BER should be close to zero for ideal extraction");

        // Calculate NC (Normalized Correlation) - should be close to 1
        double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);
        assertTrue(nc > 0.9, "NC should be close to 1 for ideal extraction");

        System.out.println("DCT Watermark Embed and Extract Test Passed!");
        System.out.println("BER: " + ber);
        System.out.println("NC: " + nc);
    }

    @Test
    @DisplayName("Test DCT Watermarking with Different Coefficient Pairs")
    void testDifferentCoefficientPairs() {
        // Test with different coefficient pairs
        int blockSize = 8;
        double strength = 20.0;

        // Define various coefficient pairs to test
        int[][] coefPairs = {
                {3, 1}, {4, 1},   // Original pairs from the project description
                {4, 3}, {5, 2},   // Another pair from the project description
                {1, 4}, {3, 3},   // Another pair from the project description
                {2, 2}, {3, 2},   // Another mid-frequency pair
                {2, 3}, {4, 2}    // Another mid-frequency pair
        };

        for (int i = 0; i < coefPairs.length; i += 2) {
            int[] coefPair1 = coefPairs[i];
            int[] coefPair2 = coefPairs[i+1];

            System.out.println("Testing coefficient pairs: [" + coefPair1[0] + "," + coefPair1[1] +
                    "] and [" + coefPair2[0] + "," + coefPair2[1] + "]");

            // Embed the watermark
            Matrix watermarkedMatrix = dctWatermarking.embed(
                    yComponent,
                    watermark,
                    blockSize,
                    coefPair1,
                    coefPair2,
                    strength
            );

            // Extract the watermark
            BufferedImage extractedWatermark = dctWatermarking.extract(
                    watermarkedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Calculate BER
            double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);

            // Calculate NC
            double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);

            System.out.println("Coefficient Pair [" + coefPair1[0] + "," + coefPair1[1] +
                    "] and [" + coefPair2[0] + "," + coefPair2[1] +
                    "]: BER = " + ber + ", NC = " + nc);

            // Verify successful extraction
            assertTrue(ber < 0.1, "BER should be low for coefficient pair [" +
                    coefPair1[0] + "," + coefPair1[1] + "] and [" + coefPair2[0] + "," + coefPair2[1] + "]");
            assertTrue(nc > 0.8, "NC should be high for coefficient pair [" +
                    coefPair1[0] + "," + coefPair1[1] + "] and [" + coefPair2[0] + "," + coefPair2[1] + "]");
        }
    }

    @Test
    @DisplayName("Test DCT Watermarking with Different Embedding Strengths")
    void testDifferentEmbeddingStrengths() {
        // Test with different embedding strengths
        int blockSize = 8;
        int[] coefPair1 = {3, 1};
        int[] coefPair2 = {4, 1};

        double[] strengths = {5.0, 10.0, 20.0, 30.0, 40.0, 50.0};

        for (double strength : strengths) {
            // Embed the watermark
            Matrix watermarkedMatrix = dctWatermarking.embed(
                    yComponent,
                    watermark,
                    blockSize,
                    coefPair1,
                    coefPair2,
                    strength
            );

            // Update the image process with watermarked component
            Process watermarkedProcess = new Process(testImage);
            watermarkedProcess.convertToYCbCr();
            watermarkedProcess.setY(watermarkedMatrix);
            watermarkedProcess.convertToRGB();

            BufferedImage watermarkedImage = watermarkedProcess.getRGBImage();

            // Calculate PSNR to measure visual quality
            double psnr = WatermarkEvaluation.calculatePSNR(testImage, watermarkedImage);

            // Extract the watermark
            BufferedImage extractedWatermark = dctWatermarking.extract(
                    watermarkedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Calculate BER
            double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);

            // Calculate NC
            double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);

            System.out.println("Embedding Strength " + strength + ": BER = " + ber +
                    ", NC = " + nc + ", PSNR = " + psnr + " dB");

            // Verify successful extraction
            assertTrue(ber < 0.1, "BER should be low for embedding strength " + strength);
            assertTrue(nc > 0.8, "NC should be high for embedding strength " + strength);

            // Higher strength should still maintain acceptable visual quality
            if (strength <= 30.0) {
                assertTrue(psnr > 30.0, "PSNR should be above 30dB for strength " + strength);
            }
        }
    }

    @Test
    @DisplayName("Test DCT Watermarking with Different Block Sizes")
    void testDifferentBlockSizes() {
        // Test with different block sizes
        int[] blockSizes = {4, 8, 16};
        int[] coefPair1 = {3, 1};
        int[] coefPair2 = {4, 1};
        double strength = 20.0;

        for (int blockSize : blockSizes) {
            // For smaller blocks, use proportionally smaller coefficients
            int[] scaledCoefPair1 = {Math.min(coefPair1[0], blockSize-1), Math.min(coefPair1[1], blockSize-1)};
            int[] scaledCoefPair2 = {Math.min(coefPair2[0], blockSize-1), Math.min(coefPair2[1], blockSize-1)};

            // Embed the watermark
            Matrix watermarkedMatrix = dctWatermarking.embed(
                    yComponent,
                    watermark,
                    blockSize,
                    scaledCoefPair1,
                    scaledCoefPair2,
                    strength
            );

            // Extract the watermark
            BufferedImage extractedWatermark = dctWatermarking.extract(
                    watermarkedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    scaledCoefPair1,
                    scaledCoefPair2
            );

            // Calculate BER
            double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);

            // Calculate NC
            double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);

            System.out.println("Block Size " + blockSize + ": BER = " + ber + ", NC = " + nc);

            // Verify successful extraction
            assertTrue(ber < 0.1, "BER should be low for block size " + blockSize);
            assertTrue(nc > 0.8, "NC should be high for block size " + blockSize);
        }
    }

    @Test
    @DisplayName("Test DCT Watermarking Attack Resilience")
    void testAttackResilience() throws IOException {
        // Embed watermark
        int blockSize = 8;
        int[] coefPair1 = {3, 1};
        int[] coefPair2 = {4, 1};
        double strength = 30.0;  // Higher strength for better resilience

        Matrix watermarkedMatrix = dctWatermarking.embed(
                yComponent,
                watermark,
                blockSize,
                coefPair1,
                coefPair2,
                strength
        );

        // Update the image process with watermarked component
        Process watermarkedProcess = new Process(testImage);
        watermarkedProcess.convertToYCbCr();
        watermarkedProcess.setY(watermarkedMatrix);
        watermarkedProcess.convertToRGB();

        BufferedImage watermarkedImage = watermarkedProcess.getRGBImage();

        // Apply various attacks and test robustness

        // 1. JPEG compression attack with various quality levels
        int[] jpegQualities = {90, 75, 50, 25};

        for (int quality : jpegQualities) {
            // Apply attack
            BufferedImage attackedImage = WatermarkAttacks.jpegCompressionAttack(watermarkedImage, quality);

            // Convert attacked image back to YCbCr
            Process attackedProcess = new Process(attackedImage);
            attackedProcess.convertToYCbCr();
            Matrix attackedMatrix = attackedProcess.getY();

            // Extract watermark from attacked image
            BufferedImage extractedWatermark = dctWatermarking.extract(
                    attackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Calculate BER and NC
            double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);
            double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);

            System.out.println("JPEG Compression Attack (Quality " + quality + "): BER = " + ber + ", NC = " + nc);

            // DCT should be somewhat resilient to JPEG compression
            if (quality >= 50) {
                assertTrue(ber < 0.3, "BER should be reasonable for JPEG quality " + quality);
                assertTrue(nc > 0.6, "NC should be reasonable for JPEG quality " + quality);
            }
        }

        // 2. Cropping attack
        double cropPercentage = 0.1; // Crop 10% from each edge
        BufferedImage croppedImage = WatermarkAttacks.croppingAttack(watermarkedImage, cropPercentage);

        Process croppedProcess = new Process(croppedImage);
        croppedProcess.convertToYCbCr();
        Matrix croppedMatrix = croppedProcess.getY();

        BufferedImage extractedFromCropped = dctWatermarking.extract(
                croppedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                blockSize,
                coefPair1,
                coefPair2
        );

        double berCropped = WatermarkEvaluation.calculateBER(watermark, extractedFromCropped);
        double ncCropped = WatermarkEvaluation.calculateNC(watermark, extractedFromCropped);

        System.out.println("Cropping Attack (10%): BER = " + berCropped + ", NC = " + ncCropped);

        // 3. Rotation attack
        BufferedImage rotatedImage = WatermarkAttacks.rotationAttack(watermarkedImage, 90);

        Process rotatedProcess = new Process(rotatedImage);
        rotatedProcess.convertToYCbCr();
        Matrix rotatedMatrix = rotatedProcess.getY();

        BufferedImage extractedFromRotated = dctWatermarking.extract(
                rotatedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                blockSize,
                coefPair1,
                coefPair2
        );

        double berRotated = WatermarkEvaluation.calculateBER(watermark, extractedFromRotated);
        double ncRotated = WatermarkEvaluation.calculateNC(watermark, extractedFromRotated);

        System.out.println("Rotation Attack (90°): BER = " + berRotated + ", NC = " + ncRotated);
    }

    @Test
    @DisplayName("Test DCT Watermarking Visual Imperceptibility")
    void testVisualImperceptibility() {
        // Embed with different strengths and measure PSNR
        int blockSize = 8;
        int[] coefPair1 = {3, 1};
        int[] coefPair2 = {4, 1};

        double[] strengths = {5.0, 10.0, 20.0, 30.0, 40.0, 50.0};

        for (double strength : strengths) {
            // Embed the watermark
            Matrix watermarkedMatrix = dctWatermarking.embed(
                    yComponent,
                    watermark,
                    blockSize,
                    coefPair1,
                    coefPair2,
                    strength
            );

            // Update the image process with watermarked component
            Process watermarkedProcess = new Process(testImage);
            watermarkedProcess.convertToYCbCr();
            watermarkedProcess.setY(watermarkedMatrix);
            watermarkedProcess.convertToRGB();

            BufferedImage watermarkedImage = watermarkedProcess.getRGBImage();

            // Calculate PSNR
            double psnr = WatermarkEvaluation.calculatePSNR(testImage, watermarkedImage);

            System.out.println("Strength " + strength + " PSNR: " + psnr + " dB");

            // PSNR should decrease as strength increases, but remain acceptable
            if (strength <= 20.0) {
                assertTrue(psnr > 35.0, "PSNR should be high for lower strength");
            }
        }
    }
}
package watermarking.testing;

import enums.AttackType;
import enums.QualityType;
import enums.WatermarkType;
import utils.Logger;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Protocol-specific test runner for watermarking evaluation.
 * Runs a targeted subset of tests focused on the parameters specified
 * in the protocol documentation requirements.
 */
public class ProtocolTestRunner {

    public static void main(String[] args) {
        Logger.info("Starting protocol-specific watermarking tests...");

        try {
            // Create test automation with default test image
            WatermarkTestingAutomation tester = new WatermarkTestingAutomation();

            // Run LSB tests
            testLSB(tester);

            // Run DCT tests
            testDCT(tester);

            // Run DWT tests
            testDWT(tester);

            // Run SVD tests
            testSVD(tester);

            Logger.info("Protocol-specific tests completed successfully.");

        } catch (Exception e) {
            Logger.error("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs LSB watermarking tests with protocol-specified parameters
     */
    private static void testLSB(WatermarkTestingAutomation tester) throws Exception {
        Logger.info("Running LSB tests...");

        // Use checkerboard watermark for all tests
        BufferedImage watermark = ImageIO.read(new File("test-watermarks/checkerboard.png"));

        // Test LSB on Y component with bit planes 1, 3, 5, 7
        int[] bitPlanes = {1, 3, 5, 7};

        for (int bitPlane : bitPlanes) {
            // Without permutation
            testWatermarkingWithAllAttacks(tester, WatermarkType.LSB, QualityType.Y,
                    watermark, "LSB_BP" + bitPlane + "_NoPerm",
                    new Object[]{bitPlane, false, "watermark-key"});

            // With permutation
            testWatermarkingWithAllAttacks(tester, WatermarkType.LSB, QualityType.Y,
                    watermark, "LSB_BP" + bitPlane + "_WithPerm",
                    new Object[]{bitPlane, true, "watermark-key"});
        }
    }

    /**
     * Runs DCT watermarking tests with protocol-specified parameters
     */
    private static void testDCT(WatermarkTestingAutomation tester) throws Exception {
        Logger.info("Running DCT tests...");

        // Use checkerboard watermark for all tests
        BufferedImage watermark = ImageIO.read(new File("test-watermarks/checkerboard.png"));

        // Test coefficient pairs
        int[][][] coeffPairs = {
                {{3, 1}, {4, 1}},  // Pair 1
                {{4, 3}, {5, 2}}   // Pair 2
        };

        // Test strength values
        double[] strengthValues = {5.0, 10.0, 15.0};

        // Block size (standard for DCT)
        int blockSize = 8;

        // Test each combination on Y component
        for (int[][] coeffPair : coeffPairs) {
            for (double strength : strengthValues) {
                String testName = String.format("DCT_C%d%d_%d%d_S%.0f",
                        coeffPair[0][0], coeffPair[0][1],
                        coeffPair[1][0], coeffPair[1][1], strength);

                testWatermarkingWithAllAttacks(tester, WatermarkType.DCT, QualityType.Y,
                        watermark, testName,
                        new Object[]{blockSize, coeffPair[0], coeffPair[1], strength});
            }
        }
    }

    /**
     * Runs DWT watermarking tests with protocol-specified parameters
     */
    private static void testDWT(WatermarkTestingAutomation tester) throws Exception {
        Logger.info("Running DWT tests...");

        // Use checkerboard watermark for all tests
        BufferedImage watermark = ImageIO.read(new File("test-watermarks/checkerboard.png"));

        // Test subbands
        String[] subbands = {"LL", "LH", "HL", "HH"};

        // Test strength values
        double[] strengthValues = {2.5, 5.0};

        // Test each combination on Y component
        for (String subband : subbands) {
            for (double strength : strengthValues) {
                String testName = String.format("DWT_%s_S%.1f", subband, strength);

                testWatermarkingWithAllAttacks(tester, WatermarkType.DWT, QualityType.Y,
                        watermark, testName,
                        new Object[]{strength, subband});
            }
        }
    }

    /**
     * Runs SVD watermarking tests with protocol-specified parameters
     */
    private static void testSVD(WatermarkTestingAutomation tester) throws Exception {
        Logger.info("Running SVD tests...");

        // Use checkerboard watermark for all tests
        BufferedImage watermark = ImageIO.read(new File("test-watermarks/checkerboard.png"));

        // Test alpha values
        double[] alphaValues = {0.5, 1.0, 2.0, 5.0};

        // Test each combination on Y component
        for (double alpha : alphaValues) {
            String testName = String.format("SVD_A%.1f", alpha);

            testWatermarkingWithAllAttacks(tester, WatermarkType.SVD, QualityType.Y,
                    watermark, testName,
                    new Object[]{alpha});
        }
    }

    /**
     * Tests a specific watermarking technique with all protocol-required attacks
     */
    private static void testWatermarkingWithAllAttacks(
            WatermarkTestingAutomation tester,
            WatermarkType method,
            QualityType component,
            BufferedImage watermark,
            String testName,
            Object[] embedParams) throws Exception {

        // Invoke the private test method through reflection
        java.lang.reflect.Method testMethod = WatermarkTestingAutomation.class.getDeclaredMethod(
                "testWatermarkWithAttacks",
                WatermarkType.class, QualityType.class, BufferedImage.class,
                String.class, Object[].class);
        testMethod.setAccessible(true);
        testMethod.invoke(tester, method, component, watermark, testName, embedParams);
    }
}
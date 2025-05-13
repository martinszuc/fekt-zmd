package watermarking.testing;

import enums.AttackType;
import enums.QualityType;
import enums.WatermarkType;
import utils.Logger;
import watermarking.core.AbstractWatermarking;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Protocol-specific test runner that executes exactly the required tests
 * from the protocol documentation with minimal configuration.
 */
public class ProtocolBatchRunner {

    public static void main(String[] args) {
        Logger.info("Starting protocol batch runner with predefined test parameters...");

        try {
            // Create directories for results
            File resultsDir = new File("protocol-results");
            resultsDir.mkdirs();

            // Create watermarks directory if it doesn't exist
            File watermarksDir = new File("test-watermarks");
            watermarksDir.mkdirs();

            // Create the watermarks if they don't exist
            createTestWatermarks();

            // Load default test image and watermark
            BufferedImage testImage = ImageIO.read(new File("Images/Lenna.png"));
            BufferedImage watermark = ImageIO.read(new File("test-watermarks/checkerboard.png"));

            // Run all protocol-required tests

            // 1. LSB Tests
            Logger.info("Running LSB Protocol Tests...");
            runLSBTests(testImage, watermark);

            // 2. DCT Tests
            Logger.info("Running DCT Protocol Tests...");
            runDCTTests(testImage, watermark);

            // 3. DWT Tests
            Logger.info("Running DWT Protocol Tests...");
            runDWTTests(testImage, watermark);

            // 4. SVD Tests
            Logger.info("Running SVD Protocol Tests...");
            runSVDTests(testImage, watermark);

            Logger.info("All protocol tests completed. Results saved in protocol-results directory.");

        } catch (Exception e) {
            Logger.error("Error running protocol tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates test watermarks for the protocol tests
     */
    private static void createTestWatermarks() throws Exception {
        // Create checkerboard pattern watermark (64x64)
        createCheckerboardWatermark(64, 64, "checkerboard.png");
    }

    /**
     * Creates a checkerboard pattern watermark
     */
    private static void createCheckerboardWatermark(int width, int height, String filename) throws Exception {
        File outputFile = new File("test-watermarks/" + filename);

        // Only create if it doesn't exist
        if (outputFile.exists()) {
            return;
        }

        BufferedImage watermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = watermark.createGraphics();

        // Fill with white
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw black checkboard pattern
        g2d.setColor(java.awt.Color.BLACK);

        int squareSize = Math.max(8, width / 8);
        for (int y = 0; y < height; y += squareSize) {
            for (int x = 0; x < width; x += squareSize) {
                if ((x / squareSize + y / squareSize) % 2 == 0) {
                    g2d.fillRect(x, y, squareSize, squareSize);
                }
            }
        }

        g2d.dispose();

        // Save watermark
        ImageIO.write(watermark, "png", outputFile);
        Logger.info("Created checkerboard watermark: " + outputFile.getAbsolutePath());
    }

    /**
     * Runs all required LSB tests from the protocol
     */
    private static void runLSBTests(BufferedImage testImage, BufferedImage watermark) throws Exception {
        // Test bit planes: 1, 3, 5, 7
        int[] bitPlanes = {1, 3, 5, 7};

        // Test with and without permutation
        boolean[] permutationOptions = {false, true};

        for (int bitPlane : bitPlanes) {
            for (boolean usePermutation : permutationOptions) {
                String testName = String.format("LSB_BP%d_%s",
                        bitPlane, usePermutation ? "WithPerm" : "NoPerm");

                Object[] embedParams = {bitPlane, usePermutation, "watermark-key"};

                // Run a watermarking test with all protocol attacks
                runProtocolTest(testImage, watermark, WatermarkType.LSB, QualityType.Y,
                        testName, embedParams);
            }
        }
    }

    /**
     * Runs all required DCT tests from the protocol
     */
    private static void runDCTTests(BufferedImage testImage, BufferedImage watermark) throws Exception {
        // Test coefficient pairs
        int[][][] coeffPairs = {
                {{3, 1}, {4, 1}},  // Pair 1
                {{4, 3}, {5, 2}}   // Pair 2
        };

        // Test strength values
        double[] strengthValues = {5.0, 10.0, 15.0};

        // Block size (standard for DCT)
        int blockSize = 8;

        for (int[][] coeffPair : coeffPairs) {
            for (double strength : strengthValues) {
                String testName = String.format("DCT_C%d%d_%d%d_S%.0f",
                        coeffPair[0][0], coeffPair[0][1],
                        coeffPair[1][0], coeffPair[1][1], strength);

                Object[] embedParams = {blockSize, coeffPair[0], coeffPair[1], strength};

                // Run a watermarking test with all protocol attacks
                runProtocolTest(testImage, watermark, WatermarkType.DCT, QualityType.Y,
                        testName, embedParams);
            }
        }
    }

    /**
     * Runs all required DWT tests from the protocol
     */
    private static void runDWTTests(BufferedImage testImage, BufferedImage watermark) throws Exception {
        // Test all subbands
        String[] subbands = {"LL", "LH", "HL", "HH"};

        // Test strength values
        double[] strengthValues = {2.5, 5.0};

        for (String subband : subbands) {
            for (double strength : strengthValues) {
                String testName = String.format("DWT_%s_S%.1f", subband, strength);

                Object[] embedParams = {strength, subband};

                // Run a watermarking test with all protocol attacks
                runProtocolTest(testImage, watermark, WatermarkType.DWT, QualityType.Y,
                        testName, embedParams);
            }
        }
    }

    /**
     * Runs all required SVD tests from the protocol
     */
    private static void runSVDTests(BufferedImage testImage, BufferedImage watermark) throws Exception {
        // Test alpha values
        double[] alphaValues = {0.5, 1.0, 2.0, 5.0};

        for (double alpha : alphaValues) {
            String testName = String.format("SVD_A%.1f", alpha);

            Object[] embedParams = {alpha};

            // Run a watermarking test with all protocol attacks
            runProtocolTest(testImage, watermark, WatermarkType.SVD, QualityType.Y,
                    testName, embedParams);
        }
    }

    /**
     * Runs a complete watermarking test with all protocol-required attacks
     */
    private static void runProtocolTest(
            BufferedImage testImage,
            BufferedImage watermark,
            WatermarkType method,
            QualityType component,
            String testName,
            Object[] embedParams) throws Exception {

        Logger.info("Running test: " + testName);

        // Create a watermarking instance
        AbstractWatermarking watermarking =
                watermarking.core.WatermarkingFactory.createWatermarking(method);

        // Create a process object with a copy of the test image
        jpeg.Process process = new jpeg.Process(copyImage(testImage));

        // Convert to YCbCr
        process.convertToYCbCr();

        // Get component matrix for embedding
        Jama.Matrix componentMatrix = null;
        switch (component) {
            case Y:
                componentMatrix = process.getY();
                break;
            case CB:
                componentMatrix = process.getCb();
                break;
            case CR:
                componentMatrix = process.getCr();
                break;
        }

        // Embed watermark
        Jama.Matrix watermarkedMatrix = watermarking.embed(componentMatrix, watermark, embedParams);

        // Update component in process
        switch (component) {
            case Y:
                process.setY(watermarkedMatrix);
                break;
            case CB:
                process.setCb(watermarkedMatrix);
                break;
            case CR:
                process.setCr(watermarkedMatrix);
                break;
        }

        // Convert back to RGB
        process.convertToRGB();

        // Save watermarked image
        String baseOutputPath = "protocol-results/" + testName;
        ImageIO.write(process.getRGBImage(), "png", new File(baseOutputPath + "_watermarked.png"));

        // Run baseline test (no attack)
        runAttack(AttackType.NONE, process, watermarking, component, watermark, embedParams,
                baseOutputPath, testName);

        // JPEG attacks
        float[] jpegQualities = {25.0f, 50.0f, 75.0f, 90.0f};
        for (float quality : jpegQualities) {
            runAttack(AttackType.JPEG_COMPRESSION, process, watermarking, component, watermark, embedParams,
                    baseOutputPath, testName, quality);
        }

        // PNG compression attacks
        int[] pngLevels = {1, 5, 9};
        for (int level : pngLevels) {
            runAttack(AttackType.PNG_COMPRESSION, process, watermarking, component, watermark, embedParams,
                    baseOutputPath, testName, level);
        }

        // Rotation attacks
        runAttack(AttackType.ROTATION_45, process, watermarking, component, watermark, embedParams,
                baseOutputPath, testName);
        runAttack(AttackType.ROTATION_90, process, watermarking, component, watermark, embedParams,
                baseOutputPath, testName);

        // Resize attacks
        runAttack(AttackType.RESIZE_50, process, watermarking, component, watermark, embedParams,
                baseOutputPath, testName);
        runAttack(AttackType.RESIZE_75, process, watermarking, component, watermark, embedParams,
                baseOutputPath, testName);

        // Mirroring attack
        runAttack(AttackType.MIRRORING, process, watermarking, component, watermark, embedParams,
                baseOutputPath, testName);

        // Cropping attacks
        runAttack(AttackType.CROPPING, process, watermarking, component, watermark, embedParams,
                baseOutputPath, testName, 0.1);
        runAttack(AttackType.CROPPING, process, watermarking, component, watermark, embedParams,
                baseOutputPath, testName, 0.2);

        Logger.info("Completed test: " + testName);
    }

    /**
     * Runs a specific attack and extracts the watermark
     */
    private static void runAttack(
            AttackType attackType,
            jpeg.Process watermarkedProcess,
            watermarking.core.AbstractWatermarking watermarking,
            QualityType component,
            BufferedImage originalWatermark,
            Object[] embedParams,
            String baseOutputPath,
            String testName,
            Object... attackParams) throws Exception {

        Logger.info("Applying attack: " + attackType.getDisplayName());

        // Get the watermarked RGB image
        BufferedImage watermarkedImage = copyImage(watermarkedProcess.getRGBImage());

        // Apply attack
        watermarking.attacks.AbstractWatermarkAttack attack =
                watermarking.attacks.WatermarkAttackFactory.getAttack(attackType);

        Map<String, Object> attackParamsMap = new HashMap<>();

        // Configure attack parameters
        if (attackType == AttackType.JPEG_COMPRESSION && attackParams.length > 0) {
            attackParamsMap.put("quality", attackParams[0]);
        } else if (attackType == AttackType.PNG_COMPRESSION && attackParams.length > 0) {
            attackParamsMap.put("level", attackParams[0]);
        } else if (attackType == AttackType.CROPPING && attackParams.length > 0) {
            attackParamsMap.put("percentage", attackParams[0]);
        }

        // Apply attack
        BufferedImage attackedImage = attack.apply(watermarkedImage, attackParamsMap);

        // Get attack parameters description
        String attackDescription = attack.getParametersDescription(attackParamsMap);

        // Save attacked image
        String attackedImagePath = baseOutputPath + "_" +
                attackType.name() + "_" + attackDescription.replace(": ", "_").replace("%", "pct") + ".png";
        ImageIO.write(attackedImage, "png", new File(attackedImagePath));

        // Create new process with attacked image
        jpeg.Process attackedProcess = new jpeg.Process(attackedImage);

        // Convert to YCbCr for extraction
        attackedProcess.convertToYCbCr();

        // Get component matrix for extraction
        Jama.Matrix componentMatrix = null;
        switch (component) {
            case Y:
                componentMatrix = attackedProcess.getY();
                break;
            case CB:
                componentMatrix = attackedProcess.getCb();
                break;
            case CR:
                componentMatrix = attackedProcess.getCr();
                break;
        }

        // Extract watermark
        BufferedImage extractedWatermark = watermarking.extract(componentMatrix,
                originalWatermark.getWidth(), originalWatermark.getHeight(), embedParams);

        // Save extracted watermark
        String extractedWatermarkPath = baseOutputPath + "_" +
                attackType.name() + "_" + attackDescription.replace(": ", "_").replace("%", "pct") + "_extracted.png";

        if (extractedWatermark != null) {
            ImageIO.write(extractedWatermark, "png", new File(extractedWatermarkPath));

            // Calculate quality metrics
            double ber = watermarking.core.WatermarkEvaluation.calculateBER(originalWatermark, extractedWatermark);
            double nc = watermarking.core.WatermarkEvaluation.calculateNC(originalWatermark, extractedWatermark);

            // Save metrics to a log file
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("protocol-results/metrics.txt"),
                    String.format("%s,%s,%s,%.6f,%.6f\n",
                            testName, attackType.name(), attackDescription, ber, nc).getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);

            Logger.info(String.format("Attack: %s, BER: %.6f, NC: %.6f",
                    attackType.getDisplayName() + " " + attackDescription, ber, nc));
        }
    }

    /**
     * Creates a copy of a BufferedImage
     */
    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(), source.getType());
        java.awt.Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        return copy;
    }
}
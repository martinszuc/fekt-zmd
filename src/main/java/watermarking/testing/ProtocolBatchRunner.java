package watermarking.testing;

import enums.AttackType;
import enums.QualityType;
import enums.WatermarkType;
import utils.Logger;
import watermarking.core.AbstractWatermarking;
import watermarking.core.WatermarkingFactory;    // ← import the factory

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
            BufferedImage watermark   = ImageIO.read(new File("test-watermarks/checkerboard.png"));

            // Run all protocol-required tests
            Logger.info("Running LSB Protocol Tests...");
            runLSBTests(testImage, watermark);

            Logger.info("Running DCT Protocol Tests...");
            runDCTTests(testImage, watermark);

            Logger.info("Running DWT Protocol Tests...");
            runDWTTests(testImage, watermark);

            Logger.info("Running SVD Protocol Tests...");
            runSVDTests(testImage, watermark);

            Logger.info("All protocol tests completed. Results saved in protocol-results directory.");
        } catch (Exception e) {
            Logger.error("Error running protocol tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Creates test watermarks for the protocol tests */
    private static void createTestWatermarks() throws Exception {
        createCheckerboardWatermark(64, 64, "checkerboard.png");
    }

    /** Creates a checkerboard pattern watermark */
    private static void createCheckerboardWatermark(int width, int height, String filename) throws Exception {
        File outputFile = new File("test-watermarks/" + filename);
        if (outputFile.exists()) return;

        BufferedImage wm = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = wm.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(java.awt.Color.BLACK);

        int squareSize = Math.max(8, width / 8);
        for (int y = 0; y < height; y += squareSize) {
            for (int x = 0; x < width; x += squareSize) {
                if ((x / squareSize + y / squareSize) % 2 == 0) {
                    g.fillRect(x, y, squareSize, squareSize);
                }
            }
        }
        g.dispose();

        ImageIO.write(wm, "png", outputFile);
        Logger.info("Created checkerboard watermark: " + outputFile.getAbsolutePath());
    }

    private static void runLSBTests(BufferedImage testImage, BufferedImage watermark) throws Exception {
        int[] bitPlanes = {1, 3, 5, 7};
        boolean[] permutationOptions = {false, true};
        for (int bp : bitPlanes) {
            for (boolean perm : permutationOptions) {
                String name = String.format("LSB_BP%d_%s", bp, perm ? "WithPerm" : "NoPerm");
                Object[] params = {bp, perm, "watermark-key"};
                runProtocolTest(testImage, watermark, WatermarkType.LSB, QualityType.Y, name, params);
            }
        }
    }

    private static void runDCTTests(BufferedImage testImage, BufferedImage watermark) throws Exception {
        int[][][] pairs = {
                {{3,1},{4,1}},
                {{4,3},{5,2}}
        };
        double[] strengths = {5.0, 10.0, 15.0};
        int blockSize = 8;

        for (int[][] p : pairs) {
            for (double s : strengths) {
                String name = String.format("DCT_C%d%d_%d%d_S%.0f",
                        p[0][0], p[0][1], p[1][0], p[1][1], s);
                Object[] params = {blockSize, p[0], p[1], s};
                runProtocolTest(testImage, watermark, WatermarkType.DCT, QualityType.Y, name, params);
            }
        }
    }

    private static void runDWTTests(BufferedImage testImage, BufferedImage watermark) throws Exception {
        String[] bands = {"LL","LH","HL","HH"};
        double[] strengths = {2.5, 5.0};
        for (String band : bands) {
            for (double s : strengths) {
                String name = String.format("DWT_%s_S%.1f", band, s);
                Object[] params = {s, band};
                runProtocolTest(testImage, watermark, WatermarkType.DWT, QualityType.Y, name, params);
            }
        }
    }

    private static void runSVDTests(BufferedImage testImage, BufferedImage watermark) throws Exception {
        double[] alphas = {0.5, 1.0, 2.0, 5.0};
        for (double a : alphas) {
            String name = String.format("SVD_A%.1f", a);
            Object[] params = {a};
            runProtocolTest(testImage, watermark, WatermarkType.SVD, QualityType.Y, name, params);
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

        // ← renamed local variable so it won't shadow the package
        AbstractWatermarking wmInstance = WatermarkingFactory.createWatermarking(method);

        // Prepare image for embedding
        jpeg.Process process = new jpeg.Process(copyImage(testImage));
        process.convertToYCbCr();

        // pick the right channel
        Jama.Matrix compMatrix;
        switch (component) {
            case Y  : compMatrix = process.getY();  break;
            case CB : compMatrix = process.getCb(); break;
            case CR : compMatrix = process.getCr(); break;
            default : throw new IllegalStateException("Unknown component: " + component);
        }

        // embed, save, then run attacks
        Jama.Matrix watermarked = wmInstance.embed(compMatrix, watermark, embedParams);
        switch (component) {
            case Y  : process.setY(watermarked);  break;
            case CB : process.setCb(watermarked); break;
            case CR : process.setCr(watermarked); break;
        }
        process.convertToRGB();

        String basePath = "protocol-results/" + testName;
        ImageIO.write(process.getRGBImage(), "png", new File(basePath + "_watermarked.png"));

        // baseline (no attack)
        runAttack(AttackType.NONE, process, wmInstance, component, watermark, embedParams, basePath, testName);

        // JPEG
        for (float q : new float[]{25f, 50f, 75f, 90f}) {
            runAttack(AttackType.JPEG_COMPRESSION, process, wmInstance, component, watermark, embedParams, basePath, testName, q);
        }

        // PNG
        for (int lvl : new int[]{1,5,9}) {
            runAttack(AttackType.PNG_COMPRESSION, process, wmInstance, component, watermark, embedParams, basePath, testName, lvl);
        }

        // rotations
        runAttack(AttackType.ROTATION_45, process, wmInstance, component, watermark, embedParams, basePath, testName);
        runAttack(AttackType.ROTATION_90, process, wmInstance, component, watermark, embedParams, basePath, testName);

        // resizes
        runAttack(AttackType.RESIZE_50, process, wmInstance, component, watermark, embedParams, basePath, testName);
        runAttack(AttackType.RESIZE_75, process, wmInstance, component, watermark, embedParams, basePath, testName);

        // mirror
        runAttack(AttackType.MIRRORING, process, wmInstance, component, watermark, embedParams, basePath, testName);

        // cropping
        runAttack(AttackType.CROPPING, process, wmInstance, component, watermark, embedParams, basePath, testName, 0.1);
        runAttack(AttackType.CROPPING, process, wmInstance, component, watermark, embedParams, basePath, testName, 0.2);

        Logger.info("Completed test: " + testName);
    }

    /**
     * Runs a specific attack and extracts the watermark
     */
    private static void runAttack(
            AttackType attackType,
            jpeg.Process watermarkedProcess,
            AbstractWatermarking wmInstance,     // ← use the renamed instance
            QualityType component,
            BufferedImage originalWatermark,
            Object[] embedParams,
            String baseOutputPath,
            String testName,
            Object... attackParams) throws Exception {

        Logger.info("Applying attack: " + attackType.getDisplayName());

        BufferedImage watermarkedImg = copyImage(watermarkedProcess.getRGBImage());
        watermarking.attacks.AbstractWatermarkAttack attack =
                watermarking.attacks.WatermarkAttackFactory.getAttack(attackType);

        Map<String, Object> params = new HashMap<>();
        if (attackType == AttackType.JPEG_COMPRESSION && attackParams.length > 0) {
            params.put("quality", attackParams[0]);
        } else if (attackType == AttackType.PNG_COMPRESSION && attackParams.length > 0) {
            params.put("level", attackParams[0]);
        } else if (attackType == AttackType.CROPPING && attackParams.length > 0) {
            params.put("percentage", attackParams[0]);
        }

        BufferedImage attacked = attack.apply(watermarkedImg, params);
        String desc = attack.getParametersDescription(params)
                .replace(": ", "_").replace("%", "pct");
        ImageIO.write(attacked, "png", new File(baseOutputPath + "_" + attackType.name() + "_" + desc + ".png"));

        // extract
        jpeg.Process attackedProc = new jpeg.Process(attacked);
        attackedProc.convertToYCbCr();
        Jama.Matrix compMatrix;
        switch (component) {
            case Y  : compMatrix = attackedProc.getY();  break;
            case CB : compMatrix = attackedProc.getCb(); break;
            case CR : compMatrix = attackedProc.getCr(); break;
            default : throw new IllegalStateException("Unknown component: " + component);
        }

        BufferedImage extracted = wmInstance.extract(compMatrix,
                originalWatermark.getWidth(), originalWatermark.getHeight(), embedParams);
        if (extracted != null) {
            ImageIO.write(extracted, "png", new File(baseOutputPath + "_" + attackType.name() + "_" + desc + "_extracted.png"));
            double ber = watermarking.core.WatermarkEvaluation.calculateBER(originalWatermark, extracted);
            double nc  = watermarking.core.WatermarkEvaluation.calculateNC(originalWatermark, extracted);
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("protocol-results/metrics.txt"),
                    String.format("%s,%s,%s,%.6f,%.6f\n", testName, attackType.name(), desc, ber, nc)
                            .getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
            Logger.info(String.format("Attack: %s, BER: %.6f, NC: %.6f",
                    attackType.getDisplayName() + " " + desc, ber, nc));
        }
    }

    /** Creates a copy of a BufferedImage */
    private static BufferedImage copyImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        java.awt.Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }
}

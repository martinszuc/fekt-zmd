package watermarking.testing;

import Jama.Matrix;
import enums.AttackType;
import enums.QualityType;
import enums.WatermarkType;
import jpeg.Process;
import utils.Logger;
import watermarking.attacks.AbstractWatermarkAttack;
import watermarking.attacks.WatermarkAttackFactory;
import watermarking.core.WatermarkEvaluation;
import watermarking.core.WatermarkResult;
import watermarking.core.WatermarkTestReport;
import watermarking.core.WatermarkingFactory;
import watermarking.core.AbstractWatermarking;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automated testing framework for watermarking techniques and attacks.
 * This class runs a comprehensive series of tests on all watermarking methods
 * against all attacks and generates reports with metrics for documentation.
 */
public class WatermarkTestingAutomation {

    // Test results storage
    private List<WatermarkResult> results = new ArrayList<>();

    // Default test image path
    private String testImagePath = "Images/Lenna.png";

    // Test watermarks directory
    private String watermarksDir = "test-watermarks";

    // Output directory
    private String outputDir = "test-results";

    // Current test image
    private BufferedImage testImage;

    // Current watermark configuration
    private String currentWatermarkConfig = "Default";

    // Current test ID counter
    private int testId = 1;

    /**
     * Constructor with default test image
     */
    public WatermarkTestingAutomation() {
        try {
            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputDir));
            Files.createDirectories(Paths.get(watermarksDir));

            // Load test image
            testImage = ImageIO.read(new File(testImagePath));
            Logger.info("Test image loaded: " + testImagePath + " (" +
                    testImage.getWidth() + "x" + testImage.getHeight() + ")");

            // Create test watermarks if they don't exist
            createTestWatermarks();

        } catch (IOException e) {
            Logger.error("Error initializing testing framework: " + e.getMessage());
            throw new RuntimeException("Failed to initialize testing framework", e);
        }
    }

    /**
     * Constructor with custom test image
     *
     * @param imagePath Path to the test image
     */
    public WatermarkTestingAutomation(String imagePath) {
        this.testImagePath = imagePath;

        try {
            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputDir));
            Files.createDirectories(Paths.get(watermarksDir));

            // Load test image
            testImage = ImageIO.read(new File(testImagePath));
            Logger.info("Test image loaded: " + testImagePath + " (" +
                    testImage.getWidth() + "x" + testImage.getHeight() + ")");

            // Create test watermarks if they don't exist
            createTestWatermarks();

        } catch (IOException e) {
            Logger.error("Error initializing testing framework: " + e.getMessage());
            throw new RuntimeException("Failed to initialize testing framework", e);
        }
    }

    /**
     * Creates test watermarks for testing
     */
    private void createTestWatermarks() throws IOException {
        // Checkerboard pattern watermark (64x64)
        createCheckerboardWatermark(64, 64, "checkerboard.png");

        // Logo watermark (64x64)
        createLogoWatermark(64, 64, "logo.png");

        // Text watermark (64x64)
        createTextWatermark(64, 64, "text.png");
    }

    /**
     * Creates a checkerboard pattern watermark
     */
    private void createCheckerboardWatermark(int width, int height, String filename) throws IOException {
        BufferedImage watermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = watermark.createGraphics();

        // Fill with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw black checkboard pattern
        g2d.setColor(Color.BLACK);

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
        File outputFile = new File(watermarksDir + "/" + filename);
        ImageIO.write(watermark, "png", outputFile);
        Logger.info("Created checkerboard watermark: " + outputFile.getAbsolutePath());
    }

    /**
     * Creates a logo watermark
     */
    private void createLogoWatermark(int width, int height, String filename) throws IOException {
        BufferedImage watermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = watermark.createGraphics();

        // Fill with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw a simple shape in the center
        g2d.setColor(Color.BLACK);
        int centerSize = Math.min(width, height) / 2;
        g2d.fillOval(width/2 - centerSize/2, height/2 - centerSize/2, centerSize, centerSize);

        // Draw crosshairs
        int lineWidth = Math.max(2, width / 16);
        g2d.fillRect(0, height/2 - lineWidth/2, width, lineWidth);
        g2d.fillRect(width/2 - lineWidth/2, 0, lineWidth, height);

        g2d.dispose();

        // Save watermark
        File outputFile = new File(watermarksDir + "/" + filename);
        ImageIO.write(watermark, "png", outputFile);
        Logger.info("Created logo watermark: " + outputFile.getAbsolutePath());
    }

    /**
     * Creates a text watermark
     */
    private void createTextWatermark(int width, int height, String filename) throws IOException {
        BufferedImage watermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = watermark.createGraphics();

        // Fill with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, height / 4));
        g2d.drawString("ZMD", width/4, height/2);

        g2d.dispose();

        // Save watermark
        File outputFile = new File(watermarksDir + "/" + filename);
        ImageIO.write(watermark, "png", outputFile);
        Logger.info("Created text watermark: " + outputFile.getAbsolutePath());
    }

    /**
     * Runs a complete test suite with all watermarking methods and attacks
     */
    public void runComprehensiveTests() {
        Logger.info("Starting comprehensive watermarking tests...");

        // Clear previous results
        results.clear();

        // Test LSB watermarking
        testLSBWatermarking();

        // Test DCT watermarking
        testDCTWatermarking();

        // Test DWT watermarking
        testDWTWatermarking();

        // Test SVD watermarking
        testSVDWatermarking();

        // Generate report
        generateTestReport();

        Logger.info("Comprehensive watermarking tests completed!");
    }

    /**
     * Tests LSB watermarking with various parameters
     */
    private void testLSBWatermarking() {
        Logger.info("Testing LSB watermarking...");
        WatermarkType method = WatermarkType.LSB;

        // Test bit planes
        int[] bitPlanes = {1, 3, 5, 7};

        // Test components
        QualityType[] components = {QualityType.Y, QualityType.CB, QualityType.CR};

        // Test permutation options
        boolean[] permutationOptions = {false, true};

        // Test different watermarks
        String[] watermarkFiles = {"checkerboard.png", "logo.png", "text.png"};

        for (String watermarkFile : watermarkFiles) {
            try {
                // Load watermark
                BufferedImage watermark = ImageIO.read(new File(watermarksDir + "/" + watermarkFile));
                currentWatermarkConfig = watermarkFile.replace(".png", "");

                // Test each component
                for (QualityType component : components) {
                    for (int bitPlane : bitPlanes) {
                        for (boolean usePermutation : permutationOptions) {
                            // Set up permutation key
                            String key = usePermutation ? "watermark-key" : null;

                            // Test name
                            String testName = String.format("LSB_%s_BP%d_%s_%s",
                                    component, bitPlane,
                                    usePermutation ? "Permuted" : "NotPermuted",
                                    currentWatermarkConfig);

                            // Run this watermarking test with all attacks
                            testWatermarkWithAttacks(method, component, watermark,
                                    testName, new Object[]{bitPlane, usePermutation, key});
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error("Error loading watermark: " + e.getMessage());
            }
        }
    }

    /**
     * Tests DCT watermarking with various parameters
     */
    private void testDCTWatermarking() {
        Logger.info("Testing DCT watermarking...");
        WatermarkType method = WatermarkType.DCT;

        // Test block sizes
        int blockSize = 8;  // Standard block size for DCT

        // Test coefficient pairs
        int[][][] coeffPairs = {
                {{3, 1}, {4, 1}},  // Pair 1
                {{4, 3}, {5, 2}}   // Pair 2
        };

        // Test strength values
        double[] strengthValues = {5.0, 10.0, 15.0};

        // Test components
        QualityType[] components = {QualityType.Y, QualityType.CB, QualityType.CR};

        // Test different watermarks
        String[] watermarkFiles = {"checkerboard.png", "logo.png", "text.png"};

        for (String watermarkFile : watermarkFiles) {
            try {
                // Load watermark
                BufferedImage watermark = ImageIO.read(new File(watermarksDir + "/" + watermarkFile));
                currentWatermarkConfig = watermarkFile.replace(".png", "");

                // Test each component
                for (QualityType component : components) {
                    for (int[][] coeffPair : coeffPairs) {
                        for (double strength : strengthValues) {
                            // Test name
                            String testName = String.format("DCT_%s_C%d%d_%d%d_S%.1f_%s",
                                    component,
                                    coeffPair[0][0], coeffPair[0][1],
                                    coeffPair[1][0], coeffPair[1][1],
                                    strength, currentWatermarkConfig);

                            // Run this watermarking test with all attacks
                            testWatermarkWithAttacks(method, component, watermark,
                                    testName, new Object[]{blockSize, coeffPair[0], coeffPair[1], strength});
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error("Error loading watermark: " + e.getMessage());
            }
        }
    }

    /**
     * Tests DWT watermarking with various parameters
     */
    private void testDWTWatermarking() {
        Logger.info("Testing DWT watermarking...");
        WatermarkType method = WatermarkType.DWT;

        // Test strength values
        double[] strengthValues = {2.5, 5.0};

        // Test subbands
        String[] subbands = {"LL", "LH", "HL", "HH"};

        // Test components
        QualityType[] components = {QualityType.Y, QualityType.CB, QualityType.CR};

        // Test different watermarks
        String[] watermarkFiles = {"checkerboard.png", "logo.png", "text.png"};

        for (String watermarkFile : watermarkFiles) {
            try {
                // Load watermark
                BufferedImage watermark = ImageIO.read(new File(watermarksDir + "/" + watermarkFile));
                currentWatermarkConfig = watermarkFile.replace(".png", "");

                // Test each component
                for (QualityType component : components) {
                    for (String subband : subbands) {
                        for (double strength : strengthValues) {
                            // Test name
                            String testName = String.format("DWT_%s_%s_S%.1f_%s",
                                    component, subband, strength, currentWatermarkConfig);

                            // Run this watermarking test with all attacks
                            testWatermarkWithAttacks(method, component, watermark,
                                    testName, new Object[]{strength, subband});
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error("Error loading watermark: " + e.getMessage());
            }
        }
    }

    /**
     * Tests SVD watermarking with various parameters
     */
    private void testSVDWatermarking() {
        Logger.info("Testing SVD watermarking...");
        WatermarkType method = WatermarkType.SVD;

        // Test alpha values
        double[] alphaValues = {0.5, 1.0, 2.0, 5.0};

        // Test components
        QualityType[] components = {QualityType.Y, QualityType.CB, QualityType.CR};

        // Test different watermarks
        String[] watermarkFiles = {"checkerboard.png", "logo.png", "text.png"};

        for (String watermarkFile : watermarkFiles) {
            try {
                // Load watermark
                BufferedImage watermark = ImageIO.read(new File(watermarksDir + "/" + watermarkFile));
                currentWatermarkConfig = watermarkFile.replace(".png", "");

                // Test each component
                for (QualityType component : components) {
                    for (double alpha : alphaValues) {
                        // Test name
                        String testName = String.format("SVD_%s_A%.1f_%s",
                                component, alpha, currentWatermarkConfig);

                        // Run this watermarking test with all attacks
                        testWatermarkWithAttacks(method, component, watermark,
                                testName, new Object[]{alpha});
                    }
                }
            } catch (IOException e) {
                Logger.error("Error loading watermark: " + e.getMessage());
            }
        }
    }

    /**
     * Tests a watermarking method with all attacks
     */
    private void testWatermarkWithAttacks(WatermarkType method, QualityType component,
                                          BufferedImage watermark, String testName,
                                          Object[] embedParams) {
        Logger.info("Testing " + testName);

        try {
            // Clone test image to avoid modifying the original
            BufferedImage testImageCopy = new BufferedImage(
                    testImage.getWidth(), testImage.getHeight(), testImage.getType());
            Graphics2D g2d = testImageCopy.createGraphics();
            g2d.drawImage(testImage, 0, 0, null);
            g2d.dispose();

            // Create process object
            Process process = new Process(testImageCopy);

            // Convert to YCbCr (required for watermarking)
            process.convertToYCbCr();

            // Get appropriate component matrix
            Matrix componentMatrix = null;
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
                default:
                    Logger.error("Unsupported component: " + component);
                    return;
            }

            // Get watermarking implementation
            AbstractWatermarking watermarking = WatermarkingFactory.createWatermarking(method);

            // Embed watermark
            Matrix watermarkedMatrix = watermarking.embed(componentMatrix, watermark, embedParams);

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

            // Convert back to RGB for display and attacks
            process.convertToRGB();

            // Save watermarked image
            String watermarkedImagePath = outputDir + "/" + testName + "_watermarked.png";
            ImageIO.write(process.getRGBImage(), "png", new File(watermarkedImagePath));

            // First, let's extract without attack for baseline
            testAttack(AttackType.NONE, process, method, component, watermark, embedParams, testName);

            // Test JPEG compression attacks
            testAttack(AttackType.JPEG_COMPRESSION, process, method, component, watermark, embedParams, testName,
                    25.0f);
            testAttack(AttackType.JPEG_COMPRESSION, process, method, component, watermark, embedParams, testName,
                    50.0f);
            testAttack(AttackType.JPEG_COMPRESSION, process, method, component, watermark, embedParams, testName,
                    75.0f);
            testAttack(AttackType.JPEG_COMPRESSION, process, method, component, watermark, embedParams, testName,
                    90.0f);

            // Test PNG compression attacks
            testAttack(AttackType.PNG_COMPRESSION, process, method, component, watermark, embedParams, testName,
                    1);
            testAttack(AttackType.PNG_COMPRESSION, process, method, component, watermark, embedParams, testName,
                    5);
            testAttack(AttackType.PNG_COMPRESSION, process, method, component, watermark, embedParams, testName,
                    9);

            // Test rotation attacks
            testAttack(AttackType.ROTATION_45, process, method, component, watermark, embedParams, testName);
            testAttack(AttackType.ROTATION_90, process, method, component, watermark, embedParams, testName);

            // Test resize attacks
            testAttack(AttackType.RESIZE_50, process, method, component, watermark, embedParams, testName);
            testAttack(AttackType.RESIZE_75, process, method, component, watermark, embedParams, testName);

            // Test mirroring attack
            testAttack(AttackType.MIRRORING, process, method, component, watermark, embedParams, testName);

            // Test cropping attacks
            testAttack(AttackType.CROPPING, process, method, component, watermark, embedParams, testName,
                    0.1);
            testAttack(AttackType.CROPPING, process, method, component, watermark, embedParams, testName,
                    0.2);

        } catch (Exception e) {
            Logger.error("Error testing " + testName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tests a specific attack on a watermarked image
     */
    private void testAttack(AttackType attackType, Process watermarkedProcess,
                            WatermarkType method, QualityType component,
                            BufferedImage originalWatermark, Object[] embedParams,
                            String testPrefix, Object... attackParams) {
        try {
            Logger.info("Applying attack: " + attackType.getDisplayName());

            // Get the watermarked RGB image (copy to avoid modifying the original)
            BufferedImage watermarkedImage = new BufferedImage(
                    watermarkedProcess.getRGBImage().getWidth(),
                    watermarkedProcess.getRGBImage().getHeight(),
                    watermarkedProcess.getRGBImage().getType());
            Graphics2D g2d = watermarkedImage.createGraphics();
            g2d.drawImage(watermarkedProcess.getRGBImage(), 0, 0, null);
            g2d.dispose();

            // Apply attack
            AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(attackType);
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
            String attackedImagePath = outputDir + "/" + testPrefix + "_" +
                    attackType.name() + "_" + attackDescription.replace(": ", "_").replace("%", "pct") + ".png";
            ImageIO.write(attackedImage, "png", new File(attackedImagePath));

            // Create new process with attacked image
            Process attackedProcess = new Process(attackedImage);

            // Convert to YCbCr for extraction
            attackedProcess.convertToYCbCr();

            // Get component matrix for extraction
            Matrix componentMatrix = null;
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

            // Get watermarking implementation
            AbstractWatermarking watermarking = WatermarkingFactory.createWatermarking(method);

            // Extract watermark
            BufferedImage extractedWatermark = watermarking.extract(componentMatrix,
                    originalWatermark.getWidth(), originalWatermark.getHeight(), embedParams);

            // Save extracted watermark
            String extractedWatermarkPath = outputDir + "/" + testPrefix + "_" +
                    attackType.name() + "_" + attackDescription.replace(": ", "_").replace("%", "pct") + "_extracted.png";

            if (extractedWatermark != null) {
                ImageIO.write(extractedWatermark, "png", new File(extractedWatermarkPath));

                // Calculate quality metrics
                double ber = WatermarkEvaluation.calculateBER(originalWatermark, extractedWatermark);
                double nc = WatermarkEvaluation.calculateNC(originalWatermark, extractedWatermark);
                double psnr = WatermarkEvaluation.calculatePSNR(watermarkedProcess.getRGBImage(), attackedImage);

                // Record result
                WatermarkResult result = new WatermarkResult(
                        attackType, method, component.toString(),
                        getParameterDescription(method, embedParams),
                        ber, nc, psnr, 0.0, attackDescription, currentWatermarkConfig);

                // Add to results list
                results.add(result);

                // Log result
                Logger.info("Test #" + testId++ + ": " + testPrefix + " - " + attackType.getDisplayName() +
                        " (" + attackDescription + ") - BER: " + ber + ", NC: " + nc);
            } else {
                Logger.error("Extraction failed - null watermark returned");
            }

        } catch (Exception e) {
            Logger.error("Error testing attack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets a description of parameters used for watermarking
     */
    private String getParameterDescription(WatermarkType method, Object[] params) {
        switch (method) {
            case LSB:
                return "BitPlane: " + params[0] + ", Permute: " + params[1];
            case DCT:
                int[] coef1 = (int[]) params[1];
                int[] coef2 = (int[]) params[2];
                return "Block: " + params[0] + ", Coef1: (" + coef1[0] + "," + coef1[1] +
                        "), Coef2: (" + coef2[0] + "," + coef2[1] + "), Strength: " + params[3];
            case DWT:
                return "Strength: " + params[0] + ", Subband: " + params[1];
            case SVD:
                return "Alpha: " + params[0];
            default:
                return "Unknown";
        }
    }

    /**
     * Generates test reports from collected results
     */
    private void generateTestReport() {
        try {
            // Generate timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            // Generate Excel report
            String excelPath = outputDir + "/watermark_test_report_" + timestamp + ".xlsx";
            WatermarkTestReport.generateReport(results, excelPath);
            Logger.info("Excel report generated: " + excelPath);

            // Generate CSV report
            String csvPath = outputDir + "/watermark_test_report_" + timestamp + ".csv";
            WatermarkTestReport.exportToCsv(results, csvPath);
            Logger.info("CSV report generated: " + csvPath);

        } catch (IOException e) {
            Logger.error("Error generating reports: " + e.getMessage());
        }
    }

    // Main method for standalone testing
    public static void main(String[] args) {
        WatermarkTestingAutomation tester = new WatermarkTestingAutomation();
        tester.runComprehensiveTests();
    }
}
package watermarking;

import Jama.Matrix;
import enums.WatermarkType;
import jpeg.Process;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import watermarking.attacks.WatermarkAttacks;
import watermarking.core.AbstractWatermarking;
import watermarking.core.WatermarkEvaluation;
import watermarking.core.WatermarkResult;
import watermarking.core.WatermarkingFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

/**
 * Complete testing suite for watermarking techniques.
 * This class runs comprehensive tests on both LSB and DCT watermarking methods,
 * evaluates their robustness against various attacks, and generates detailed
 * reports with comparison charts.
 */
public class WatermarkTestingSuite {

    private static AbstractWatermarking lsbWatermarking;
    private static AbstractWatermarking dctWatermarking;
    private static BufferedImage testImage;
    private static BufferedImage watermark;
    private static Process imageProcess;
    private static Matrix yComponent;

    // Parameters for both watermarking methods
    private static final int LSB_BIT_PLANE = 3;
    private static final boolean PERMUTE = true;
    private static final String KEY = "zmd2025-project-key";

    private static final int DCT_BLOCK_SIZE = 8;
    private static final int[] DCT_COEF_PAIR1 = {3, 1};
    private static final int[] DCT_COEF_PAIR2 = {4, 1};
    private static final double DCT_STRENGTH = 20.0;

    // Results storage
    private static List<WatermarkResult> results = new ArrayList<>();

    @BeforeAll
    static void setUp() throws IOException {
        System.out.println("=== Setting up Watermark Testing Suite ===");

        // Initialize watermarking instances
        lsbWatermarking = WatermarkingFactory.createWatermarking(WatermarkType.LSB);
        dctWatermarking = WatermarkingFactory.createWatermarking(WatermarkType.DCT);

        // Create output directory
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        // Create a test image with gradient pattern
        testImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 512, 512);

        // Create gradient pattern for more realistic image
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                int r = (x * 255) / 512;
                int g2 = (y * 255) / 512;
                int b = ((x + y) * 255) / (2 * 512);
                testImage.setRGB(x, y, new Color(r, g2, b).getRGB());
            }
        }
        g.dispose();

        // Save original test image
        ImageIO.write(testImage, "png", new File("output/original_test_image.png"));

        // Create a watermark image (checkerboard pattern with center logo)
        watermark = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = watermark.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 64, 64);

        // Create checkboard pattern
        g2.setColor(Color.BLACK);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                if ((x / 8 + y / 8) % 2 == 0) {
                    watermark.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        // Add a central circle and text
        g2.setColor(Color.BLACK);
        g2.fillOval(16, 16, 32, 32);
        g2.setColor(Color.WHITE);
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 10));
        g2.drawString("ZMD", 24, 36);
        g2.dispose();

        // Save watermark
        ImageIO.write(watermark, "png", new File("output/watermark.png"));

        // Set up image process
        imageProcess = new Process(testImage);
        imageProcess.convertToYCbCr();
        yComponent = imageProcess.getY();

        System.out.println("Test image dimensions: " + testImage.getWidth() + "x" + testImage.getHeight());
        System.out.println("Watermark dimensions: " + watermark.getWidth() + "x" + watermark.getHeight());
    }

    @Test
    @DisplayName("Run Complete Watermark Testing Suite")
    void runCompleteSuite() throws IOException {
        System.out.println("\n=== Starting Complete Watermark Testing Suite ===");

        // Create watermarked images with both methods
        System.out.println("Embedding watermarks...");

        // Embed using LSB method
        Matrix lsbWatermarkedMatrix = lsbWatermarking.embed(
                yComponent.copy(),
                watermark,
                LSB_BIT_PLANE,
                PERMUTE,
                KEY
        );

        // Embed using DCT method
        Matrix dctWatermarkedMatrix = dctWatermarking.embed(
                yComponent.copy(),
                watermark,
                DCT_BLOCK_SIZE,
                DCT_COEF_PAIR1,
                DCT_COEF_PAIR2,
                DCT_STRENGTH
        );

        // Create watermarked images
        Process lsbProcess = new Process(testImage);
        lsbProcess.convertToYCbCr();
        lsbProcess.setY(lsbWatermarkedMatrix);
        lsbProcess.convertToRGB();
        BufferedImage lsbImage = lsbProcess.getRGBImage();

        Process dctProcess = new Process(testImage);
        dctProcess.convertToYCbCr();
        dctProcess.setY(dctWatermarkedMatrix);
        dctProcess.convertToRGB();
        BufferedImage dctImage = dctProcess.getRGBImage();

        // Save watermarked images
        ImageIO.write(lsbImage, "png", new File("output/lsb_watermarked.png"));
        ImageIO.write(dctImage, "png", new File("output/dct_watermarked.png"));

        // Test watermark imperceptibility
        double lsbPsnr = WatermarkEvaluation.calculatePSNR(testImage, lsbImage);
        double dctPsnr = WatermarkEvaluation.calculatePSNR(testImage, dctImage);

        System.out.println("\n=== Watermark Imperceptibility ===");
        System.out.println("LSB PSNR: " + lsbPsnr + " dB");
        System.out.println("DCT PSNR: " + dctPsnr + " dB");

        // Extract watermarks without attacks to verify initial embedding
        BufferedImage lsbExtracted = lsbWatermarking.extract(
                lsbWatermarkedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                LSB_BIT_PLANE,
                PERMUTE,
                KEY
        );

        BufferedImage dctExtracted = dctWatermarking.extract(
                dctWatermarkedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                DCT_BLOCK_SIZE,
                DCT_COEF_PAIR1,
                DCT_COEF_PAIR2
        );

        // Save extracted watermarks
        ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_original.png"));
        ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_original.png"));

        // Calculate initial BER and NC
        double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
        double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);

        double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
        double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);

        System.out.println("\n=== Initial Watermark Extraction Quality ===");
        System.out.println("LSB BER: " + lsbBer + ", NC: " + lsbNc);
        System.out.println("DCT BER: " + dctBer + ", NC: " + dctNc);

        // Store initial results
        results.add(new WatermarkResult(
                "No Attack",
                "LSB",
                "Y",
                "Bit Plane " + LSB_BIT_PLANE,
                lsbBer,
                lsbNc,
                lsbPsnr,
                0.0 // No WNR for initial extraction
        ));

        results.add(new WatermarkResult(
                "No Attack",
                "DCT",
                "Y",
                "Block " + DCT_BLOCK_SIZE + ", Strength " + DCT_STRENGTH,
                dctBer,
                dctNc,
                dctPsnr,
                0.0 // No WNR for initial extraction
        ));

        // Now run attack tests
        System.out.println("\n=== Running Attack Tests ===");

        // 1. JPEG Compression Attack
        testJpegCompressionAttack(lsbImage, dctImage);

        // 2. PNG Compression Attack
        testPngCompressionAttack(lsbImage, dctImage);

        // 3. Cropping Attack
        testCroppingAttack(lsbImage, dctImage);

        // 4. Rotation Attack
        testRotationAttack(lsbImage, dctImage);

        // 5. Resize Attack
        testResizeAttack(lsbImage, dctImage);

        // 6. Mirroring Attack
        testMirroringAttack(lsbImage, dctImage);

        // Generate final reports
        generateReports();
    }

    private void testJpegCompressionAttack(BufferedImage lsbImage, BufferedImage dctImage) throws IOException {
        System.out.println("Testing JPEG Compression Attack...");

        int[] qualities = {90, 75, 50, 25};

        for (int quality : qualities) {
            // Apply attack
            BufferedImage lsbAttacked = WatermarkAttacks.jpegCompressionAttack(lsbImage, quality);
            BufferedImage dctAttacked = WatermarkAttacks.jpegCompressionAttack(dctImage, quality);

            // Save attacked images
            ImageIO.write(lsbAttacked, "png", new File("output/lsb_jpeg_" + quality + ".png"));
            ImageIO.write(dctAttacked, "png", new File("output/dct_jpeg_" + quality + ".png"));

            // Extract watermarks
            Process lsbAttackedProcess = new Process(lsbAttacked);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctAttacked);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    LSB_BIT_PLANE,
                    PERMUTE,
                    KEY
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    DCT_BLOCK_SIZE,
                    DCT_COEF_PAIR1,
                    DCT_COEF_PAIR2
            );

            // Save extracted watermarks
            ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_jpeg_" + quality + ".png"));
            ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_jpeg_" + quality + ".png"));

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);
            double lsbPsnr = WatermarkEvaluation.calculatePSNR(testImage, lsbAttacked);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);
            double dctPsnr = WatermarkEvaluation.calculatePSNR(testImage, dctAttacked);

            System.out.println("JPEG Quality " + quality + ":");
            System.out.println("  LSB - BER: " + lsbBer + ", NC: " + lsbNc + ", PSNR: " + lsbPsnr);
            System.out.println("  DCT - BER: " + dctBer + ", NC: " + dctNc + ", PSNR: " + dctPsnr);

            // Store results
            results.add(new WatermarkResult(
                    "JPEG Compression " + quality,
                    "LSB",
                    "Y",
                    "Bit Plane " + LSB_BIT_PLANE,
                    lsbBer,
                    lsbNc,
                    lsbPsnr,
                    0.0
            ));

            results.add(new WatermarkResult(
                    "JPEG Compression " + quality,
                    "DCT",
                    "Y",
                    "Block " + DCT_BLOCK_SIZE + ", Strength " + DCT_STRENGTH,
                    dctBer,
                    dctNc,
                    dctPsnr,
                    0.0
            ));
        }
    }

    private void testPngCompressionAttack(BufferedImage lsbImage, BufferedImage dctImage) throws IOException {
        System.out.println("Testing PNG Compression Attack...");

        int[] levels = {3, 6, 9};

        for (int level : levels) {
            // Apply attack
            BufferedImage lsbAttacked = WatermarkAttacks.pngCompressionAttack(lsbImage, level);
            BufferedImage dctAttacked = WatermarkAttacks.pngCompressionAttack(dctImage, level);

            // Save attacked images
            ImageIO.write(lsbAttacked, "png", new File("output/lsb_png_" + level + ".png"));
            ImageIO.write(dctAttacked, "png", new File("output/dct_png_" + level + ".png"));

            // Extract watermarks
            Process lsbAttackedProcess = new Process(lsbAttacked);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctAttacked);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    LSB_BIT_PLANE,
                    PERMUTE,
                    KEY
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    DCT_BLOCK_SIZE,
                    DCT_COEF_PAIR1,
                    DCT_COEF_PAIR2
            );

            // Save extracted watermarks
            ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_png_" + level + ".png"));
            ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_png_" + level + ".png"));

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);
            double lsbPsnr = WatermarkEvaluation.calculatePSNR(testImage, lsbAttacked);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);
            double dctPsnr = WatermarkEvaluation.calculatePSNR(testImage, dctAttacked);

            System.out.println("PNG Level " + level + ":");
            System.out.println("  LSB - BER: " + lsbBer + ", NC: " + lsbNc + ", PSNR: " + lsbPsnr);
            System.out.println("  DCT - BER: " + dctBer + ", NC: " + dctNc + ", PSNR: " + dctPsnr);

            // Store results
            results.add(new WatermarkResult(
                    "PNG Compression " + level,
                    "LSB",
                    "Y",
                    "Bit Plane " + LSB_BIT_PLANE,
                    lsbBer,
                    lsbNc,
                    lsbPsnr,
                    0.0
            ));

            results.add(new WatermarkResult(
                    "PNG Compression " + level,
                    "DCT",
                    "Y",
                    "Block " + DCT_BLOCK_SIZE + ", Strength " + DCT_STRENGTH,
                    dctBer,
                    dctNc,
                    dctPsnr,
                    0.0
            ));
        }
    }

    private void testCroppingAttack(BufferedImage lsbImage, BufferedImage dctImage) throws IOException {
        System.out.println("Testing Cropping Attack...");

        double[] cropPercentages = {0.05, 0.1, 0.2};

        for (double cropPercentage : cropPercentages) {
            // Apply attack
            BufferedImage lsbAttacked = WatermarkAttacks.croppingAttack(lsbImage, cropPercentage);
            BufferedImage dctAttacked = WatermarkAttacks.croppingAttack(dctImage, cropPercentage);

            // Save attacked images
            ImageIO.write(lsbAttacked, "png", new File("output/lsb_crop_" + (int)(cropPercentage*100) + ".png"));
            ImageIO.write(dctAttacked, "png", new File("output/dct_crop_" + (int)(cropPercentage*100) + ".png"));

            // Extract watermarks
            Process lsbAttackedProcess = new Process(lsbAttacked);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctAttacked);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    LSB_BIT_PLANE,
                    PERMUTE,
                    KEY
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    DCT_BLOCK_SIZE,
                    DCT_COEF_PAIR1,
                    DCT_COEF_PAIR2
            );

            // Save extracted watermarks
            ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_crop_" + (int)(cropPercentage*100) + ".png"));
            ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_crop_" + (int)(cropPercentage*100) + ".png"));

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);
            double lsbPsnr = WatermarkEvaluation.calculatePSNR(testImage, lsbAttacked);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);
            double dctPsnr = WatermarkEvaluation.calculatePSNR(testImage, dctAttacked);

            System.out.println("Crop " + (int)(cropPercentage*100) + "%:");
            System.out.println("  LSB - BER: " + lsbBer + ", NC: " + lsbNc + ", PSNR: " + lsbPsnr);
            System.out.println("  DCT - BER: " + dctBer + ", NC: " + dctNc + ", PSNR: " + dctPsnr);

            // Store results
            results.add(new WatermarkResult(
                    "Cropping " + (int)(cropPercentage*100) + "%",
                    "LSB",
                    "Y",
                    "Bit Plane " + LSB_BIT_PLANE,
                    lsbBer,
                    lsbNc,
                    lsbPsnr,
                    0.0
            ));

            results.add(new WatermarkResult(
                    "Cropping " + (int)(cropPercentage*100) + "%",
                    "DCT",
                    "Y",
                    "Block " + DCT_BLOCK_SIZE + ", Strength " + DCT_STRENGTH,
                    dctBer,
                    dctNc,
                    dctPsnr,
                    0.0
            ));
        }
    }

    private void testRotationAttack(BufferedImage lsbImage, BufferedImage dctImage) throws IOException {
        System.out.println("Testing Rotation Attack...");

        int[] angles = {45, 90};

        for (int angle : angles) {
            // Apply attack
            BufferedImage lsbAttacked = WatermarkAttacks.rotationAttack(lsbImage, angle);
            BufferedImage dctAttacked = WatermarkAttacks.rotationAttack(dctImage, angle);

            // Save attacked images
            ImageIO.write(lsbAttacked, "png", new File("output/lsb_rotate_" + angle + ".png"));
            ImageIO.write(dctAttacked, "png", new File("output/dct_rotate_" + angle + ".png"));

            // Extract watermarks
            Process lsbAttackedProcess = new Process(lsbAttacked);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctAttacked);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    LSB_BIT_PLANE,
                    PERMUTE,
                    KEY
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    DCT_BLOCK_SIZE,
                    DCT_COEF_PAIR1,
                    DCT_COEF_PAIR2
            );

            // Save extracted watermarks
            ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_rotate_" + angle + ".png"));
            ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_rotate_" + angle + ".png"));

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);
            double lsbPsnr = WatermarkEvaluation.calculatePSNR(testImage, lsbAttacked);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);
            double dctPsnr = WatermarkEvaluation.calculatePSNR(testImage, dctAttacked);

            System.out.println("Rotation " + angle + "°:");
            System.out.println("  LSB - BER: " + lsbBer + ", NC: " + lsbNc + ", PSNR: " + lsbPsnr);
            System.out.println("  DCT - BER: " + dctBer + ", NC: " + dctNc + ", PSNR: " + dctPsnr);

            // Store results
            results.add(new WatermarkResult(
                    "Rotation " + angle + "°",
                    "LSB",
                    "Y",
                    "Bit Plane " + LSB_BIT_PLANE,
                    lsbBer,
                    lsbNc,
                    lsbPsnr,
                    0.0
            ));

            results.add(new WatermarkResult(
                    "Rotation " + angle + "°",
                    "DCT",
                    "Y",
                    "Block " + DCT_BLOCK_SIZE + ", Strength " + DCT_STRENGTH,
                    dctBer,
                    dctNc,
                    dctPsnr,
                    0.0
            ));
        }
    }

    private void testResizeAttack(BufferedImage lsbImage, BufferedImage dctImage) throws IOException {
        System.out.println("Testing Resize Attack...");

        double[] scales = {0.75, 0.5};

        for (double scale : scales) {
            // Apply attack
            BufferedImage lsbAttacked = WatermarkAttacks.resizeAttack(lsbImage, scale);
            BufferedImage dctAttacked = WatermarkAttacks.resizeAttack(dctImage, scale);

            // Save attacked images
            ImageIO.write(lsbAttacked, "png", new File("output/lsb_resize_" + (int)(scale*100) + ".png"));
            ImageIO.write(dctAttacked, "png", new File("output/dct_resize_" + (int)(scale*100) + ".png"));

            // Extract watermarks
            Process lsbAttackedProcess = new Process(lsbAttacked);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctAttacked);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    LSB_BIT_PLANE,
                    PERMUTE,
                    KEY
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    DCT_BLOCK_SIZE,
                    DCT_COEF_PAIR1,
                    DCT_COEF_PAIR2
            );

            // Save extracted watermarks
            ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_resize_" + (int)(scale*100) + ".png"));
            ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_resize_" + (int)(scale*100) + ".png"));

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);
            double lsbPsnr = WatermarkEvaluation.calculatePSNR(testImage, lsbAttacked);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);
            double dctPsnr = WatermarkEvaluation.calculatePSNR(testImage, dctAttacked);

            System.out.println("Resize " + (int)(scale*100) + "%:");
            System.out.println("  LSB - BER: " + lsbBer + ", NC: " + lsbNc + ", PSNR: " + lsbPsnr);
            System.out.println("  DCT - BER: " + dctBer + ", NC: " + dctNc + ", PSNR: " + dctPsnr);

            // Store results
            results.add(new WatermarkResult(
                    "Resize " + (int)(scale*100) + "%",
                    "LSB",
                    "Y",
                    "Bit Plane " + LSB_BIT_PLANE,
                    lsbBer,
                    lsbNc,
                    lsbPsnr,
                    0.0
            ));

            results.add(new WatermarkResult(
                    "Resize " + (int)(scale*100) + "%",
                    "DCT",
                    "Y",
                    "Block " + DCT_BLOCK_SIZE + ", Strength " + DCT_STRENGTH,
                    dctBer,
                    dctNc,
                    dctPsnr,
                    0.0
            ));
        }
    }

    private void testMirroringAttack(BufferedImage lsbImage, BufferedImage dctImage) throws IOException {
        System.out.println("Testing Mirroring Attack...");

        // Apply attack
        BufferedImage lsbAttacked = WatermarkAttacks.mirroringAttack(lsbImage);
        BufferedImage dctAttacked = WatermarkAttacks.mirroringAttack(dctImage);

        // Save attacked images
        ImageIO.write(lsbAttacked, "png", new File("output/lsb_mirror.png"));
        ImageIO.write(dctAttacked, "png", new File("output/dct_mirror.png"));

        // Extract watermarks
        Process lsbAttackedProcess = new Process(lsbAttacked);
        lsbAttackedProcess.convertToYCbCr();
        Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

        Process dctAttackedProcess = new Process(dctAttacked);
        dctAttackedProcess.convertToYCbCr();
        Matrix dctAttackedMatrix = dctAttackedProcess.getY();

        BufferedImage lsbExtracted = lsbWatermarking.extract(
                lsbAttackedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                LSB_BIT_PLANE,
                PERMUTE,
                KEY
        );

        BufferedImage dctExtracted = dctWatermarking.extract(
                dctAttackedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                DCT_BLOCK_SIZE,
                DCT_COEF_PAIR1,
                DCT_COEF_PAIR2
        );

        // Save extracted watermarks
        ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_mirror.png"));
        ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_mirror.png"));

        // Calculate metrics
        double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
        double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);
        double lsbPsnr = WatermarkEvaluation.calculatePSNR(testImage, lsbAttacked);

        double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
        double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);
        double dctPsnr = WatermarkEvaluation.calculatePSNR(testImage, dctAttacked);

        System.out.println("Mirroring:");
        System.out.println("  LSB - BER: " + lsbBer + ", NC: " + lsbNc + ", PSNR: " + lsbPsnr);
        System.out.println("  DCT - BER: " + dctBer + ", NC: " + dctNc + ", PSNR: " + dctPsnr);

        // Store results
        results.add(new WatermarkResult(
                "Mirroring",
                "LSB",
                "Y",
                "Bit Plane " + LSB_BIT_PLANE,
                lsbBer,
                lsbNc,
                lsbPsnr,
                0.0
        ));

        results.add(new WatermarkResult(
                "Mirroring",
                "DCT",
                "Y",
                "Block " + DCT_BLOCK_SIZE + ", Strength " + DCT_STRENGTH,
                dctBer,
                dctNc,
                dctPsnr,
                0.0
        ));
    }

    private void generateReports() throws IOException {
        System.out.println("\n=== Generating Reports ===");

        // Generate CSV report
        generateCsvReport();

        // Generate Excel report
        generateExcelReport();

        // Print summary statistics
        printSummaryStatistics();
    }

    private void generateCsvReport() throws IOException {
        // Create CSV file
        StringBuilder csv = new StringBuilder();
        csv.append("Attack Type,Method,Component,Parameter,BER,NC,PSNR,Quality Rating\n");

        for (WatermarkResult result : results) {
            csv.append(String.format("%s,%s,%s,%s,%.4f,%.4f,%.2f,%s\n",
                    result.getAttackName(),
                    result.getMethod(),
                    result.getComponent(),
                    result.getParameter(),
                    result.getBer(),
                    result.getNc(),
                    result.getPsnr(),
                    result.getQualityRating()));
        }

        // Write to file
        java.io.FileWriter writer = new java.io.FileWriter("output/watermark_results.csv");
        writer.write(csv.toString());
        writer.close();

        System.out.println("CSV report generated: output/watermark_results.csv");
    }

    private void generateExcelReport() throws IOException {
        // Create workbook
        Workbook workbook = new XSSFWorkbook();

        // Create main results sheet
        Sheet resultsSheet = workbook.createSheet("Results");

        // Create header row
        Row headerRow = resultsSheet.createRow(0);
        String[] headers = {"Attack Type", "Method", "Component", "Parameter", "BER", "NC", "PSNR", "Quality Rating"};

        // Create header cell style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Add headers
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add data rows
        int rowNum = 1;
        for (WatermarkResult result : results) {
            Row row = resultsSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(result.getAttackName());
            row.createCell(1).setCellValue(result.getMethod());
            row.createCell(2).setCellValue(result.getComponent());
            row.createCell(3).setCellValue(result.getParameter());
            row.createCell(4).setCellValue(result.getBer());
            row.createCell(5).setCellValue(result.getNc());
            row.createCell(6).setCellValue(result.getPsnr());
            row.createCell(7).setCellValue(result.getQualityRating());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            resultsSheet.autoSizeColumn(i);
        }

        // Create summary sheet
        Sheet summarySheet = workbook.createSheet("Summary");

        // Create summary data
        Map<String, double[]> lsbSummary = new HashMap<>();
        Map<String, double[]> dctSummary = new HashMap<>();

        for (WatermarkResult result : results) {
            String attackName = result.getAttackName();
            if (result.getMethod().equals("LSB")) {
                lsbSummary.computeIfAbsent(attackName, k -> new double[3])[0] = result.getBer();
                lsbSummary.computeIfAbsent(attackName, k -> new double[3])[1] = result.getNc();
                lsbSummary.computeIfAbsent(attackName, k -> new double[3])[2] = result.getPsnr();
            } else if (result.getMethod().equals("DCT")) {
                dctSummary.computeIfAbsent(attackName, k -> new double[3])[0] = result.getBer();
                dctSummary.computeIfAbsent(attackName, k -> new double[3])[1] = result.getNc();
                dctSummary.computeIfAbsent(attackName, k -> new double[3])[2] = result.getPsnr();
            }
        }

        // Create header row for summary
        Row summaryHeaderRow = summarySheet.createRow(0);
        String[] summaryHeaders = {"Attack Type", "LSB BER", "LSB NC", "LSB PSNR", "DCT BER", "DCT NC", "DCT PSNR", "Winner"};

        for (int i = 0; i < summaryHeaders.length; i++) {
            Cell cell = summaryHeaderRow.createCell(i);
            cell.setCellValue(summaryHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add summary data rows
        int summaryRowNum = 1;
        for (String attackName : lsbSummary.keySet()) {
            if (dctSummary.containsKey(attackName)) {
                Row row = summarySheet.createRow(summaryRowNum++);
                row.createCell(0).setCellValue(attackName);

                double lsbBer = lsbSummary.get(attackName)[0];
                double lsbNc = lsbSummary.get(attackName)[1];
                double lsbPsnr = lsbSummary.get(attackName)[2];

                double dctBer = dctSummary.get(attackName)[0];
                double dctNc = dctSummary.get(attackName)[1];
                double dctPsnr = dctSummary.get(attackName)[2];

                row.createCell(1).setCellValue(lsbBer);
                row.createCell(2).setCellValue(lsbNc);
                row.createCell(3).setCellValue(lsbPsnr);
                row.createCell(4).setCellValue(dctBer);
                row.createCell(5).setCellValue(dctNc);
                row.createCell(6).setCellValue(dctPsnr);

                // Determine winner based on BER and NC
                String winner;
                if (lsbBer < dctBer && lsbNc > dctNc) {
                    winner = "LSB";
                } else if (dctBer < lsbBer && dctNc > lsbNc) {
                    winner = "DCT";
                } else if (dctBer < lsbBer) {
                    winner = "DCT (BER)";
                } else if (lsbBer < dctBer) {
                    winner = "LSB (BER)";
                } else if (lsbNc > dctNc) {
                    winner = "LSB (NC)";
                } else if (dctNc > lsbNc) {
                    winner = "DCT (NC)";
                } else {
                    winner = "Tie";
                }

                row.createCell(7).setCellValue(winner);
            }
        }

        // Auto-size columns for summary
        for (int i = 0; i < summaryHeaders.length; i++) {
            summarySheet.autoSizeColumn(i);
        }

        // Generate report filename with date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateTime = sdf.format(new Date());
        String fileName = "output/watermark_report_" + dateTime + ".xlsx";

        // Write to file
        try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(fileName)) {
            workbook.write(fileOut);
        }

        workbook.close();
        System.out.println("Excel report generated: " + fileName);
    }

    private void printSummaryStatistics() {
        System.out.println("\n=== Summary Statistics ===");

        // Calculate average BER and NC per method
        double lsbTotalBer = 0, lsbTotalNc = 0, lsbTotalPsnr = 0;
        double dctTotalBer = 0, dctTotalNc = 0, dctTotalPsnr = 0;
        int lsbCount = 0, dctCount = 0;

        for (WatermarkResult result : results) {
            if (result.getMethod().equals("LSB")) {
                lsbTotalBer += result.getBer();
                lsbTotalNc += result.getNc();
                lsbTotalPsnr += result.getPsnr();
                lsbCount++;
            } else if (result.getMethod().equals("DCT")) {
                dctTotalBer += result.getBer();
                dctTotalNc += result.getNc();
                dctTotalPsnr += result.getPsnr();
                dctCount++;
            }
        }

        double lsbAvgBer = lsbTotalBer / lsbCount;
        double lsbAvgNc = lsbTotalNc / lsbCount;
        double lsbAvgPsnr = lsbTotalPsnr / lsbCount;

        double dctAvgBer = dctTotalBer / dctCount;
        double dctAvgNc = dctTotalNc / dctCount;
        double dctAvgPsnr = dctTotalPsnr / dctCount;

        System.out.println("LSB Average BER: " + String.format("%.6f", lsbAvgBer));
        System.out.println("LSB Average NC: " + String.format("%.6f", lsbAvgNc));
        System.out.println("LSB Average PSNR: " + String.format("%.2f dB", lsbAvgPsnr));
        System.out.println();
        System.out.println("DCT Average BER: " + String.format("%.6f", dctAvgBer));
        System.out.println("DCT Average NC: " + String.format("%.6f", dctAvgNc));
        System.out.println("DCT Average PSNR: " + String.format("%.2f dB", dctAvgPsnr));
        System.out.println();
        System.out.println("Overall Winner: " + ((dctAvgBer < lsbAvgBer) ? "DCT" : "LSB"));

        System.out.println("\n=== Watermark Testing Suite Complete ===");
        System.out.println("See 'output' directory for all generated images and reports");
    }
}
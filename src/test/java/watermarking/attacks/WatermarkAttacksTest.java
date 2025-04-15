package watermarking.attacks;

import Jama.Matrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import watermarking.core.AbstractWatermarking;
import watermarking.core.WatermarkEvaluation;
import watermarking.core.WatermarkResult;
import watermarking.core.WatermarkingFactory;
import enums.WatermarkType;
import jpeg.Process;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for evaluating watermarking techniques against various attacks.
 */
public class WatermarkAttacksTest {

    private AbstractWatermarking lsbWatermarking;
    private AbstractWatermarking dctWatermarking;
    private BufferedImage testImage;
    private BufferedImage watermark;
    private Process imageProcess;
    private Matrix yComponent;

    // Results storage for generating tables
    private List<WatermarkResult> results = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        // Initialize watermarking instances
        lsbWatermarking = WatermarkingFactory.createWatermarking(WatermarkType.LSB);
        dctWatermarking = WatermarkingFactory.createWatermarking(WatermarkType.DCT);

        // Create a test image with a gradient pattern
        testImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 512, 512);

        // Create a gradient pattern to simulate a natural image
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                int r = (x * 255) / 512;
                int g2 = (y * 255) / 512;
                int b = ((x + y) * 255) / (2 * 512);
                testImage.setRGB(x, y, new Color(r, g2, b).getRGB());
            }
        }
        g.dispose();

        // Create a watermark image with a checkboard pattern
        watermark = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = watermark.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 64, 64);

        // Add your own logo or pattern in the center
        g2.setColor(Color.BLACK);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                // Create a checkerboard pattern
                if ((x / 8 + y / 8) % 2 == 0) {
                    watermark.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        // Add a central circle
        g2.setColor(Color.BLACK);
        g2.fillOval(16, 16, 32, 32);

        g2.dispose();

        // Save the watermark for visual inspection if needed
        try {
            File outputDir = new File("output");
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }
            ImageIO.write(watermark, "png", new File("output/watermark.png"));
        } catch (IOException e) {
            System.err.println("Could not save watermark: " + e.getMessage());
        }

        // Set up image process
        imageProcess = new Process(testImage);
        imageProcess.convertToYCbCr();
        yComponent = imageProcess.getY();
    }

    @Test
    @DisplayName("Test LSB vs DCT Robustness Against JPEG Compression")
    void testJpegCompressionAttack() throws IOException {
        // Parameters for LSB watermarking
        int lsbBitPlane = 6; // Higher bit plane for better visibility in comparisons
        boolean permute = true;
        String key = "test-key";

        // Parameters for DCT watermarking
        int blockSize = 8;
        int[] coefPair1 = {3, 1};
        int[] coefPair2 = {4, 1};
        double strength = 30.0; // Higher strength for better resilience

        // Embed watermarks using both methods
        Matrix lsbWatermarkedMatrix = lsbWatermarking.embed(
                yComponent.copy(),
                watermark,
                lsbBitPlane,
                permute,
                key
        );

        Matrix dctWatermarkedMatrix = dctWatermarking.embed(
                yComponent.copy(),
                watermark,
                blockSize,
                coefPair1,
                coefPair2,
                strength
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

        // Save watermarked images for visual inspection
        try {
            ImageIO.write(lsbImage, "png", new File("output/lsb_watermarked.png"));
            ImageIO.write(dctImage, "png", new File("output/dct_watermarked.png"));
        } catch (IOException e) {
            System.err.println("Could not save watermarked images: " + e.getMessage());
        }

        // Test JPEG compression with various quality levels
        int[] jpegQualities = {90, 75, 50, 25};

        System.out.println("=== JPEG Compression Attack Results ===");
        System.out.println("Quality | LSB BER | LSB NC | DCT BER | DCT NC");
        System.out.println("--------|---------|--------|---------|--------");

        for (int quality : jpegQualities) {
            // Apply JPEG compression attack
            BufferedImage lsbAttacked = WatermarkAttacks.jpegCompressionAttack(lsbImage, quality);
            BufferedImage dctAttacked = WatermarkAttacks.jpegCompressionAttack(dctImage, quality);

            // Save attacked images
            try {
                ImageIO.write(lsbAttacked, "png", new File("output/lsb_jpeg_" + quality + ".png"));
                ImageIO.write(dctAttacked, "png", new File("output/dct_jpeg_" + quality + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save attacked images: " + e.getMessage());
            }

            // Extract watermarks from attacked images
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
                    lsbBitPlane,
                    permute,
                    key
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Save extracted watermarks
            try {
                ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_jpeg_" + quality + ".png"));
                ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_jpeg_" + quality + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save extracted watermarks: " + e.getMessage());
            }

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);

            // Print results
            System.out.println(String.format("%7d | %7.4f | %6.4f | %7.4f | %6.4f",
                    quality, lsbBer, lsbNc, dctBer, dctNc));

            // Store results for later use
            results.add(new WatermarkResult(
                    "JPEG Compression " + quality,
                    "LSB",
                    "Y",
                    "Bit Plane " + lsbBitPlane,
                    lsbBer,
                    lsbNc
            ));

            results.add(new WatermarkResult(
                    "JPEG Compression " + quality,
                    "DCT",
                    "Y",
                    "Block " + blockSize + ", Strength " + strength,
                    dctBer,
                    dctNc
            ));
        }
    }

    @Test
    @DisplayName("Test LSB vs DCT Robustness Against Geometric Attacks")
    void testGeometricAttacks() throws IOException {
        // Parameters for LSB watermarking
        int lsbBitPlane = 6; // Higher bit plane for better visibility
        boolean permute = true;
        String key = "test-key";

        // Parameters for DCT watermarking
        int blockSize = 8;
        int[] coefPair1 = {3, 1};
        int[] coefPair2 = {4, 1};
        double strength = 30.0;

        // Embed watermarks using both methods
        Matrix lsbWatermarkedMatrix = lsbWatermarking.embed(
                yComponent.copy(),
                watermark,
                lsbBitPlane,
                permute,
                key
        );

        Matrix dctWatermarkedMatrix = dctWatermarking.embed(
                yComponent.copy(),
                watermark,
                blockSize,
                coefPair1,
                coefPair2,
                strength
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

        System.out.println("\n=== Geometric Attack Results ===");
        System.out.println("Attack Type   | LSB BER | LSB NC | DCT BER | DCT NC");
        System.out.println("--------------|---------|--------|---------|--------");

        // Test rotation attacks
        int[] rotationAngles = {45, 90};
        for (int angle : rotationAngles) {
            // Apply rotation attack
            BufferedImage lsbRotated = WatermarkAttacks.rotationAttack(lsbImage, angle);
            BufferedImage dctRotated = WatermarkAttacks.rotationAttack(dctImage, angle);

            // Save attacked images
            try {
                ImageIO.write(lsbRotated, "png", new File("output/lsb_rotated_" + angle + ".png"));
                ImageIO.write(dctRotated, "png", new File("output/dct_rotated_" + angle + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save rotated images: " + e.getMessage());
            }

            // Extract watermarks from attacked images
            Process lsbAttackedProcess = new Process(lsbRotated);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctRotated);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    lsbBitPlane,
                    permute,
                    key
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Save extracted watermarks
            try {
                ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_rotated_" + angle + ".png"));
                ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_rotated_" + angle + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save extracted watermarks: " + e.getMessage());
            }

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);

            // Print results
            System.out.println(String.format("Rotation %3d° | %7.4f | %6.4f | %7.4f | %6.4f",
                    angle, lsbBer, lsbNc, dctBer, dctNc));

            // Store results
            results.add(new WatermarkResult(
                    "Rotation " + angle + "°",
                    "LSB",
                    "Y",
                    "Bit Plane " + lsbBitPlane,
                    lsbBer,
                    lsbNc
            ));

            results.add(new WatermarkResult(
                    "Rotation " + angle + "°",
                    "DCT",
                    "Y",
                    "Block " + blockSize + ", Strength " + strength,
                    dctBer,
                    dctNc
            ));
        }

        // Test resize attacks
        double[] scaleFactors = {0.75, 0.5};
        for (double scale : scaleFactors) {
            // Apply resize attack
            BufferedImage lsbResized = WatermarkAttacks.resizeAttack(lsbImage, scale);
            BufferedImage dctResized = WatermarkAttacks.resizeAttack(dctImage, scale);

            // Save attacked images
            try {
                ImageIO.write(lsbResized, "png", new File("output/lsb_resized_" + (int)(scale*100) + ".png"));
                ImageIO.write(dctResized, "png", new File("output/dct_resized_" + (int)(scale*100) + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save resized images: " + e.getMessage());
            }

            // Extract watermarks from attacked images
            Process lsbAttackedProcess = new Process(lsbResized);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctResized);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    lsbBitPlane,
                    permute,
                    key
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Save extracted watermarks
            try {
                ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_resized_" + (int)(scale*100) + ".png"));
                ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_resized_" + (int)(scale*100) + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save extracted watermarks: " + e.getMessage());
            }

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);

            // Print results
            System.out.println(String.format("Resize %3d%%   | %7.4f | %6.4f | %7.4f | %6.4f",
                    (int)(scale*100), lsbBer, lsbNc, dctBer, dctNc));

            // Store results
            results.add(new WatermarkResult(
                    "Resize " + (int)(scale*100) + "%",
                    "LSB",
                    "Y",
                    "Bit Plane " + lsbBitPlane,
                    lsbBer,
                    lsbNc
            ));

            results.add(new WatermarkResult(
                    "Resize " + (int)(scale*100) + "%",
                    "DCT",
                    "Y",
                    "Block " + blockSize + ", Strength " + strength,
                    dctBer,
                    dctNc
            ));
        }

        // Test mirroring attack
        {
            // Apply mirroring attack
            BufferedImage lsbMirrored = WatermarkAttacks.mirroringAttack(lsbImage);
            BufferedImage dctMirrored = WatermarkAttacks.mirroringAttack(dctImage);

            // Save attacked images
            try {
                ImageIO.write(lsbMirrored, "png", new File("output/lsb_mirrored.png"));
                ImageIO.write(dctMirrored, "png", new File("output/dct_mirrored.png"));
            } catch (IOException e) {
                System.err.println("Could not save mirrored images: " + e.getMessage());
            }

            // Extract watermarks from attacked images
            Process lsbAttackedProcess = new Process(lsbMirrored);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctMirrored);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    lsbBitPlane,
                    permute,
                    key
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Save extracted watermarks
            try {
                ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_mirrored.png"));
                ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_mirrored.png"));
            } catch (IOException e) {
                System.err.println("Could not save extracted watermarks: " + e.getMessage());
            }

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);

            // Print results
            System.out.println(String.format("Mirroring     | %7.4f | %6.4f | %7.4f | %6.4f",
                    lsbBer, lsbNc, dctBer, dctNc));

            // Store results
            results.add(new WatermarkResult(
                    "Mirroring",
                    "LSB",
                    "Y",
                    "Bit Plane " + lsbBitPlane,
                    lsbBer,
                    lsbNc
            ));

            results.add(new WatermarkResult(
                    "Mirroring",
                    "DCT",
                    "Y",
                    "Block " + blockSize + ", Strength " + strength,
                    dctBer,
                    dctNc
            ));
        }
    }

    @Test
    @DisplayName("Test LSB vs DCT Robustness Against Cropping Attack")
    void testCroppingAttack() throws IOException {
        // Parameters for LSB watermarking
        int lsbBitPlane = 6;
        boolean permute = true;
        String key = "test-key";

        // Parameters for DCT watermarking
        int blockSize = 8;
        int[] coefPair1 = {3, 1};
        int[] coefPair2 = {4, 1};
        double strength = 30.0;

        // Embed watermarks using both methods
        Matrix lsbWatermarkedMatrix = lsbWatermarking.embed(
                yComponent.copy(),
                watermark,
                lsbBitPlane,
                permute,
                key
        );

        Matrix dctWatermarkedMatrix = dctWatermarking.embed(
                yComponent.copy(),
                watermark,
                blockSize,
                coefPair1,
                coefPair2,
                strength
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

        System.out.println("\n=== Cropping Attack Results ===");
        System.out.println("Crop Amount | LSB BER | LSB NC | DCT BER | DCT NC");
        System.out.println("-----------|---------|--------|---------|--------");

        // Test cropping with various percentages
        double[] cropPercentages = {0.05, 0.1, 0.2};

        for (double cropPercentage : cropPercentages) {
            // Apply cropping attack
            BufferedImage lsbCropped = WatermarkAttacks.croppingAttack(lsbImage, cropPercentage);
            BufferedImage dctCropped = WatermarkAttacks.croppingAttack(dctImage, cropPercentage);

            // Save attacked images
            try {
                ImageIO.write(lsbCropped, "png", new File("output/lsb_cropped_" + (int)(cropPercentage*100) + ".png"));
                ImageIO.write(dctCropped, "png", new File("output/dct_cropped_" + (int)(cropPercentage*100) + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save cropped images: " + e.getMessage());
            }

            // Extract watermarks from attacked images
            Process lsbAttackedProcess = new Process(lsbCropped);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctCropped);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    lsbBitPlane,
                    permute,
                    key
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Save extracted watermarks
            try {
                ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_cropped_" + (int)(cropPercentage*100) + ".png"));
                ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_cropped_" + (int)(cropPercentage*100) + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save extracted watermarks: " + e.getMessage());
            }

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);

            // Print results
            System.out.println(String.format("%3d%%       | %7.4f | %6.4f | %7.4f | %6.4f",
                    (int)(cropPercentage*100), lsbBer, lsbNc, dctBer, dctNc));

            // Store results
            results.add(new WatermarkResult(
                    "Cropping " + (int)(cropPercentage*100) + "%",
                    "LSB",
                    "Y",
                    "Bit Plane " + lsbBitPlane,
                    lsbBer,
                    lsbNc
            ));

            results.add(new WatermarkResult(
                    "Cropping " + (int)(cropPercentage*100) + "%",
                    "DCT",
                    "Y",
                    "Block " + blockSize + ", Strength " + strength,
                    dctBer,
                    dctNc
            ));
        }
    }

    @Test
    @DisplayName("Test PNG Compression Attack")
    void testPngCompressionAttack() throws IOException {
        // Parameters for LSB watermarking
        int lsbBitPlane = 6;
        boolean permute = true;
        String key = "test-key";

        // Parameters for DCT watermarking
        int blockSize = 8;
        int[] coefPair1 = {3, 1};
        int[] coefPair2 = {4, 1};
        double strength = 30.0;

        // Embed watermarks using both methods
        Matrix lsbWatermarkedMatrix = lsbWatermarking.embed(
                yComponent.copy(),
                watermark,
                lsbBitPlane,
                permute,
                key
        );

        Matrix dctWatermarkedMatrix = dctWatermarking.embed(
                yComponent.copy(),
                watermark,
                blockSize,
                coefPair1,
                coefPair2,
                strength
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

        System.out.println("\n=== PNG Compression Attack Results ===");
        System.out.println("Comp Level | LSB BER | LSB NC | DCT BER | DCT NC");
        System.out.println("-----------|---------|--------|---------|--------");

        // Test PNG compression with various levels
        int[] compressionLevels = {3, 6, 9};

        for (int level : compressionLevels) {
            // Apply PNG compression attack
            BufferedImage lsbCompressed = WatermarkAttacks.pngCompressionAttack(lsbImage, level);
            BufferedImage dctCompressed = WatermarkAttacks.pngCompressionAttack(dctImage, level);

            // Save attacked images
            try {
                ImageIO.write(lsbCompressed, "png", new File("output/lsb_png_comp_" + level + ".png"));
                ImageIO.write(dctCompressed, "png", new File("output/dct_png_comp_" + level + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save PNG compressed images: " + e.getMessage());
            }

            // Extract watermarks from attacked images
            Process lsbAttackedProcess = new Process(lsbCompressed);
            lsbAttackedProcess.convertToYCbCr();
            Matrix lsbAttackedMatrix = lsbAttackedProcess.getY();

            Process dctAttackedProcess = new Process(dctCompressed);
            dctAttackedProcess.convertToYCbCr();
            Matrix dctAttackedMatrix = dctAttackedProcess.getY();

            BufferedImage lsbExtracted = lsbWatermarking.extract(
                    lsbAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    lsbBitPlane,
                    permute,
                    key
            );

            BufferedImage dctExtracted = dctWatermarking.extract(
                    dctAttackedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    blockSize,
                    coefPair1,
                    coefPair2
            );

            // Save extracted watermarks
            try {
                ImageIO.write(lsbExtracted, "png", new File("output/lsb_extracted_png_comp_" + level + ".png"));
                ImageIO.write(dctExtracted, "png", new File("output/dct_extracted_png_comp_" + level + ".png"));
            } catch (IOException e) {
                System.err.println("Could not save extracted watermarks: " + e.getMessage());
            }

            // Calculate metrics
            double lsbBer = WatermarkEvaluation.calculateBER(watermark, lsbExtracted);
            double lsbNc = WatermarkEvaluation.calculateNC(watermark, lsbExtracted);

            double dctBer = WatermarkEvaluation.calculateBER(watermark, dctExtracted);
            double dctNc = WatermarkEvaluation.calculateNC(watermark, dctExtracted);

            // Print results
            System.out.println(String.format("%d          | %7.4f | %6.4f | %7.4f | %6.4f",
                    level, lsbBer, lsbNc, dctBer, dctNc));

            // Store results
            results.add(new WatermarkResult(
                    "PNG Compression " + level,
                    "LSB",
                    "Y",
                    "Bit Plane " + lsbBitPlane,
                    lsbBer,
                    lsbNc
            ));

            results.add(new WatermarkResult(
                    "PNG Compression " + level,
                    "DCT",
                    "Y",
                    "Block " + blockSize + ", Strength " + strength,
                    dctBer,
                    dctNc
            ));
        }
    }

    @Test
    @DisplayName("Generate Comprehensive Results Summary")
    void generateResultsSummary() {
        // Run all attack tests first
        try {
            testJpegCompressionAttack();
            testPngCompressionAttack();
            testGeometricAttacks();
            testCroppingAttack();
        } catch (IOException e) {
            System.err.println("Error generating results summary: " + e.getMessage());
            return;
        }

        // Print comprehensive table of results
        System.out.println("\n=== Comprehensive Watermarking Attack Evaluation ===");
        System.out.println("Attack Type        | Method | Component | Parameter         | BER    | NC     | Quality");
        System.out.println("-------------------|--------|-----------|-------------------|--------|--------|--------");

        for (WatermarkResult result : results) {
            System.out.println(String.format("%-19s | %-6s | %-9s | %-17s | %6.4f | %6.4f | %s",
                    result.getAttackName(),
                    result.getMethod(),
                    result.getComponent(),
                    result.getParameter(),
                    result.getBer(),
                    result.getNc(),
                    result.getQualityRating()));
        }

        // Generate summary statistics
        System.out.println("\n=== Summary Statistics ===");

        // Calculate average BER and NC per method
        double lsbTotalBer = 0, lsbTotalNc = 0;
        double dctTotalBer = 0, dctTotalNc = 0;
        int lsbCount = 0, dctCount = 0;

        for (WatermarkResult result : results) {
            if (result.getMethod().equals("LSB")) {
                lsbTotalBer += result.getBer();
                lsbTotalNc += result.getNc();
                lsbCount++;
            } else if (result.getMethod().equals("DCT")) {
                dctTotalBer += result.getBer();
                dctTotalNc += result.getNc();
                dctCount++;
            }
        }

        double lsbAvgBer = lsbTotalBer / lsbCount;
        double lsbAvgNc = lsbTotalNc / lsbCount;
        double dctAvgBer = dctTotalBer / dctCount;
        double dctAvgNc = dctTotalNc / dctCount;

        System.out.println("LSB Average BER: " + String.format("%.4f", lsbAvgBer));
        System.out.println("LSB Average NC: " + String.format("%.4f", lsbAvgNc));
        System.out.println("DCT Average BER: " + String.format("%.4f", dctAvgBer));
        System.out.println("DCT Average NC: " + String.format("%.4f", dctAvgNc));

        System.out.println("\nOverall Winner: " + ((dctAvgBer < lsbAvgBer) ? "DCT" : "LSB"));

        try {
            outputResultsCSV();
        } catch (IOException e) {
            System.err.println("Error writing results to CSV: " + e.getMessage());
        }
    }

    private void outputResultsCSV() throws IOException {
        // Create output directory if it doesn't exist
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        // Write results to CSV file for importing into Excel
        StringBuilder csv = new StringBuilder();
        csv.append("Attack Type,Method,Component,Parameter,BER,NC,Quality Rating\n");

        for (WatermarkResult result : results) {
            csv.append(String.format("%s,%s,%s,%s,%.4f,%.4f,%s\n",
                    result.getAttackName(),
                    result.getMethod(),
                    result.getComponent(),
                    result.getParameter(),
                    result.getBer(),
                    result.getNc(),
                    result.getQualityRating()));
        }

        java.io.FileWriter writer = new java.io.FileWriter("output/watermark_results.csv");
        writer.write(csv.toString());
        writer.close();

        System.out.println("Results written to output/watermark_results.csv");
    }
}
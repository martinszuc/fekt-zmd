package watermarking.spatial;

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
 * Test class for LSB Watermarking implementation.
 */
public class LSBWatermarkingTest {

    private AbstractWatermarking lsbWatermarking;
    private BufferedImage testImage;
    private BufferedImage watermark;
    private Process imageProcess;
    private Matrix yComponent;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize the LSB watermarking instance
        lsbWatermarking = WatermarkingFactory.createWatermarking(WatermarkType.LSB);

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
        watermark = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = watermark.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 64, 64);
        g2.setColor(Color.BLACK);

        // Create a simple pattern as watermark
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                if ((x / 8 + y / 8) % 2 == 0) {
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
    @DisplayName("Test LSB Watermarking Embed and Extract")
    void testEmbedAndExtract() {
        // Embed watermark in bit plane 3
        int bitPlane = 3;
        boolean permute = true;
        String key = "test-key";

        // Embed the watermark
        Matrix watermarkedMatrix = lsbWatermarking.embed(
                yComponent,
                watermark,
                bitPlane,
                permute,
                key
        );

        // Verify the watermarked matrix is not null
        assertNotNull(watermarkedMatrix);

        // Verify the dimensions of watermarked matrix match the original
        assertEquals(yComponent.getRowDimension(), watermarkedMatrix.getRowDimension());
        assertEquals(yComponent.getColumnDimension(), watermarkedMatrix.getColumnDimension());

        // Extract the watermark
        BufferedImage extractedWatermark = lsbWatermarking.extract(
                watermarkedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                bitPlane,
                permute,
                key
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

        System.out.println("LSB Watermark Embed and Extract Test Passed!");
        System.out.println("BER: " + ber);
        System.out.println("NC: " + nc);
    }

    @Test
    @DisplayName("Test LSB Watermarking with Different Bit Planes")
    void testDifferentBitPlanes() {
        // Test with different bit planes (0-7)
        String key = "test-key";
        boolean permute = true;

        for (int bitPlane = 0; bitPlane <= 7; bitPlane++) {
            // Embed the watermark
            Matrix watermarkedMatrix = lsbWatermarking.embed(
                    yComponent,
                    watermark,
                    bitPlane,
                    permute,
                    key
            );

            // Extract the watermark
            BufferedImage extractedWatermark = lsbWatermarking.extract(
                    watermarkedMatrix,
                    watermark.getWidth(),
                    watermark.getHeight(),
                    bitPlane,
                    permute,
                    key
            );

            // Calculate BER
            double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);

            // Calculate NC
            double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);

            System.out.println("Bit Plane " + bitPlane + ": BER = " + ber + ", NC = " + nc);

            // Verify successful extraction
            assertTrue(ber < 0.1, "BER should be low for bit plane " + bitPlane);
            assertTrue(nc > 0.8, "NC should be high for bit plane " + bitPlane);
        }
    }

    @Test
    @DisplayName("Test LSB Watermarking with Wrong Key")
    void testWrongKey() {
        // Embed with one key, extract with another
        int bitPlane = 3;
        boolean permute = true;
        String embedKey = "embed-key";
        String extractKey = "wrong-key";

        // Embed the watermark
        Matrix watermarkedMatrix = lsbWatermarking.embed(
                yComponent,
                watermark,
                bitPlane,
                permute,
                embedKey
        );

        // Extract with wrong key
        BufferedImage extractedWatermark = lsbWatermarking.extract(
                watermarkedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                bitPlane,
                permute,
                extractKey
        );

        // Calculate BER - should be high for wrong key
        double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);

        // Calculate NC - should be low for wrong key
        double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);

        System.out.println("Wrong Key Test: BER = " + ber + ", NC = " + nc);

        // Watermark with wrong key should be significantly different
        assertTrue(ber > 0.4, "BER should be high when using wrong key");
        assertTrue(nc < 0.6, "NC should be low when using wrong key");
    }

    @Test
    @DisplayName("Test LSB Watermarking with No Permutation")
    void testNoPermutation() {
        // Test without permutation
        int bitPlane = 3;
        boolean permute = false;
        String key = "test-key"; // Not used when permute is false

        // Embed the watermark
        Matrix watermarkedMatrix = lsbWatermarking.embed(
                yComponent,
                watermark,
                bitPlane,
                permute,
                key
        );

        // Extract the watermark
        BufferedImage extractedWatermark = lsbWatermarking.extract(
                watermarkedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                bitPlane,
                permute,
                key
        );

        // Calculate BER
        double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);

        // Calculate NC
        double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);

        System.out.println("No Permutation Test: BER = " + ber + ", NC = " + nc);

        // Verify successful extraction
        assertEquals(0.0, ber, 0.01, "BER should be close to zero without permutation");
        assertTrue(nc > 0.9, "NC should be close to 1 without permutation");
    }

    @Test
    @DisplayName("Test LSB Watermarking Attack Resilience")
    void testAttackResilience() throws IOException {
        // Embed watermark
        int bitPlane = 3;
        boolean permute = true;
        String key = "test-key";

        Matrix watermarkedMatrix = lsbWatermarking.embed(
                yComponent,
                watermark,
                bitPlane,
                permute,
                key
        );

        // Update the image process with watermarked component
        Process watermarkedProcess = new Process(testImage);
        watermarkedProcess.convertToYCbCr();
        watermarkedProcess.setY(watermarkedMatrix);
        watermarkedProcess.convertToRGB();

        BufferedImage watermarkedImage = watermarkedProcess.getRGBImage();

        // Apply JPEG compression attack
        BufferedImage attackedImage = WatermarkAttacks.jpegCompressionAttack(watermarkedImage, 75);

        // Convert attacked image back to YCbCr
        Process attackedProcess = new Process(attackedImage);
        attackedProcess.convertToYCbCr();
        Matrix attackedMatrix = attackedProcess.getY();

        // Extract watermark from attacked image
        BufferedImage extractedWatermark = lsbWatermarking.extract(
                attackedMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                bitPlane,
                permute,
                key
        );

        // Calculate BER and NC
        double ber = WatermarkEvaluation.calculateBER(watermark, extractedWatermark);
        double nc = WatermarkEvaluation.calculateNC(watermark, extractedWatermark);

        System.out.println("JPEG Attack (Quality 75) - LSB Bit Plane " + bitPlane + ": BER = " + ber + ", NC = " + nc);

        // LSB on lower bit planes is very vulnerable to JPEG compression
        // We just verify that extraction still produces a result
        assertNotNull(extractedWatermark);

        // Try with other attacks and document results
        // Mirroring attack
        BufferedImage mirroredImage = WatermarkAttacks.mirroringAttack(watermarkedImage);
        Process mirroredProcess = new Process(mirroredImage);
        mirroredProcess.convertToYCbCr();
        Matrix mirroredMatrix = mirroredProcess.getY();

        BufferedImage extractedFromMirrored = lsbWatermarking.extract(
                mirroredMatrix,
                watermark.getWidth(),
                watermark.getHeight(),
                bitPlane,
                permute,
                key
        );

        double berMirrored = WatermarkEvaluation.calculateBER(watermark, extractedFromMirrored);
        double ncMirrored = WatermarkEvaluation.calculateNC(watermark, extractedFromMirrored);

        System.out.println("Mirroring Attack - LSB Bit Plane " + bitPlane + ": BER = " + berMirrored + ", NC = " + ncMirrored);
    }

    @Test
    @DisplayName("Test LSB Watermarking Visual Imperceptibility")
    void testVisualImperceptibility() {
        // Embed with different bit planes and measure PSNR
        String key = "test-key";
        boolean permute = true;

        for (int bitPlane = 0; bitPlane <= 7; bitPlane++) {
            // Embed the watermark
            Matrix watermarkedMatrix = lsbWatermarking.embed(
                    yComponent,
                    watermark,
                    bitPlane,
                    permute,
                    key
            );

            // Update the image process with watermarked component
            Process watermarkedProcess = new Process(testImage);
            watermarkedProcess.convertToYCbCr();
            watermarkedProcess.setY(watermarkedMatrix);
            watermarkedProcess.convertToRGB();

            BufferedImage watermarkedImage = watermarkedProcess.getRGBImage();

            // Calculate PSNR
            double psnr = WatermarkEvaluation.calculatePSNR(testImage, watermarkedImage);

            System.out.println("Bit Plane " + bitPlane + " PSNR: " + psnr + " dB");

            // Lower bit planes should have higher PSNR (less visual difference)
            if (bitPlane <= 4) {
                assertTrue(psnr > 30, "PSNR should be high for lower bit planes");
            }
        }
    }
}
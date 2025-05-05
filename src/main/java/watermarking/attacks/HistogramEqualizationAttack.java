package watermarking.attacks;

import enums.AttackType;
import enums.TransformType;
import jpeg.Process;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies JPEG compression using the application's internal JPEG pipeline.
 * Uses the DCT transform, quantization, and inverse processes from the application's
 * JPEG implementation to simulate compression artifacts.
 */
public class HistogramEqualizationAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for compression quality.
     */
    public static final String PARAM_QUALITY = "quality";

    /**
     * Default quality value (0-100).
     */
    public static final float DEFAULT_QUALITY = 75.0f;

    /**
     * Default block size for DCT transform.
     */
    public static final int DEFAULT_BLOCK_SIZE = 8;

    /**
     * Creates a new internal JPEG compression attack.
     */
    public HistogramEqualizationAttack() {
        super(AttackType.JPEG_COMPRESSION_INTERNAL);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        float quality = DEFAULT_QUALITY;
        if (params.containsKey(PARAM_QUALITY)) {
            quality = ((Number) params.get(PARAM_QUALITY)).floatValue();
        }

        // Create Process object from image
        Process process = new Process(image);

        // Convert to YCbCr color space
        process.convertToYCbCr();

        // Standard JPEG block size
        int blockSize = DEFAULT_BLOCK_SIZE;

        // Create proper copies of the matrices
        process.setY(process.getY().copy());
        process.setCb(process.getCb().copy());
        process.setCr(process.getCr().copy());

        // Apply DCT transform
        process.setY(jpeg.Transform.transform(process.getY(), TransformType.DCT, blockSize));
        process.setCb(jpeg.Transform.transform(process.getCb(), TransformType.DCT, blockSize));
        process.setCr(jpeg.Transform.transform(process.getCr(), TransformType.DCT, blockSize));

        // Quantize with given quality
        process.quantize(quality, blockSize);

        // Inverse quantize - simulates data loss from quantization
        process.inverseQuantize();

        // Inverse transform
        process.setY(jpeg.Transform.inverseTransform(process.getY(), TransformType.DCT, blockSize));
        process.setCb(jpeg.Transform.inverseTransform(process.getCb(), TransformType.DCT, blockSize));
        process.setCr(jpeg.Transform.inverseTransform(process.getCr(), TransformType.DCT, blockSize));

        // Convert back to RGB color space
        process.convertToRGB();

        logAttackComplete();
        return process.getRGBImage();
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        float quality = DEFAULT_QUALITY;
        if (params.containsKey(PARAM_QUALITY)) {
            quality = ((Number) params.get(PARAM_QUALITY)).floatValue();
        }
        return "Quality: " + quality + "%";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_QUALITY, DEFAULT_QUALITY);
        return params;
    }
}
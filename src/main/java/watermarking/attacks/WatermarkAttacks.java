package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Façade class for accessing watermark attacks.
 *
 * This class provides static methods that redirect to the appropriate attack implementation,
 * maintaining backward compatibility with code that uses the old direct method approach.
 */
public class WatermarkAttacks {

    /**
     * Applies JPEG compression attack to the image.
     *
     * @param image Original image
     * @param quality Compression quality (1-100)
     * @return Attacked image
     */
    public static BufferedImage jpegCompressionAttack(BufferedImage image, float quality) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.JPEG_COMPRESSION);
        Map<String, Object> params = new HashMap<>();
        params.put(JpegCompressionAttack.PARAM_QUALITY, quality);
        return attack.apply(image, params);
    }

    /**
     * Applies JPEG compression attack using the application's internal JPEG pipeline.
     *
     * @param image Original image
     * @param quality Compression quality (1-100)
     * @return Attacked image
     */
    public static BufferedImage jpegCompressionAttackInternal(BufferedImage image, float quality) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.JPEG_COMPRESSION_INTERNAL);
        Map<String, Object> params = new HashMap<>();
        params.put(JpegCompressionInternalAttack.PARAM_QUALITY, quality);
        return attack.apply(image, params);
    }

    /**
     * Applies PNG compression attack to the image.
     *
     * @param image Original image
     * @param compressionLevel Compression level (1-9)
     * @return Attacked image
     */
    public static BufferedImage pngCompressionAttack(BufferedImage image, int compressionLevel) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.PNG_COMPRESSION);
        Map<String, Object> params = new HashMap<>();
        params.put(PngCompressionAttack.PARAM_LEVEL, compressionLevel);
        return attack.apply(image, params);
    }

    /**
     * Applies rotation attack to the image.
     *
     * @param image Original image
     * @param degrees Rotation angle in degrees
     * @return Attacked image
     */
    public static BufferedImage rotationAttack(BufferedImage image, double degrees) {
        // Choose appropriate attack type based on angle
        AttackType attackType;
        if (Math.abs(degrees - 45) < 0.1) {
            attackType = AttackType.ROTATION_45;
        } else if (Math.abs(degrees - 90) < 0.1) {
            attackType = AttackType.ROTATION_90;
        } else {
            // Create a custom rotation attack with the specified angle
            RotationAttack attack = new RotationAttack(degrees);
            Map<String, Object> params = new HashMap<>();
            params.put(RotationAttack.PARAM_ANGLE, degrees);
            return attack.apply(image, params);
        }

        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(attackType);
        Map<String, Object> params = new HashMap<>();
        return attack.apply(image, params);
    }

    /**
     * Applies resize attack to the image.
     *
     * @param image Original image
     * @param scale Scale factor (0.0-1.0)
     * @return Attacked image
     */
    public static BufferedImage resizeAttack(BufferedImage image, double scale) {
        // Choose appropriate attack type based on scale
        AttackType attackType;
        if (Math.abs(scale - 0.75) < 0.01) {
            attackType = AttackType.RESIZE_75;
        } else if (Math.abs(scale - 0.50) < 0.01) {
            attackType = AttackType.RESIZE_50;
        } else {
            // Create a custom resize attack with the specified scale
            ResizeAttack attack = new ResizeAttack(scale);
            Map<String, Object> params = new HashMap<>();
            params.put(ResizeAttack.PARAM_SCALE, scale);
            return attack.apply(image, params);
        }

        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(attackType);
        Map<String, Object> params = new HashMap<>();
        return attack.apply(image, params);
    }

    /**
     * Applies mirroring attack to the image.
     *
     * @param image Original image
     * @return Attacked image
     */
    public static BufferedImage mirroringAttack(BufferedImage image) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.MIRRORING);
        Map<String, Object> params = new HashMap<>();
        return attack.apply(image, params);
    }

    /**
     * Applies cropping attack to the image.
     *
     * @param image Original image
     * @param cropPercentage Percentage to crop (0.0-0.5)
     * @return Attacked image
     */
    public static BufferedImage croppingAttack(BufferedImage image, double cropPercentage) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.CROPPING);
        Map<String, Object> params = new HashMap<>();
        params.put(CroppingAttack.PARAM_PERCENTAGE, cropPercentage);
        return attack.apply(image, params);
    }

    /**
     * Applies Gaussian noise attack to the image.
     *
     * @param image Original image
     * @param standardDeviation Amount of noise (e.g., 10.0 for moderate noise)
     * @return Attacked image
     */
    public static BufferedImage gaussianNoiseAttack(BufferedImage image, double standardDeviation) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.GAUSSIAN_NOISE);
        Map<String, Object> params = new HashMap<>();
        params.put(GaussianNoiseAttack.PARAM_STD_DEV, standardDeviation);
        return attack.apply(image, params);
    }

    /**
     * Applies median filtering attack to the image.
     *
     * @param image Original image
     * @param radius Filter radius (1-5)
     * @return Attacked image
     */
    public static BufferedImage medianFilterAttack(BufferedImage image, int radius) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.MEDIAN_FILTER);
        Map<String, Object> params = new HashMap<>();
        params.put(MedianFilterAttack.PARAM_RADIUS, radius);
        return attack.apply(image, params);
    }

    /**
     * Applies histogram equalization attack to the image.
     *
     * @param image Original image
     * @return Attacked image
     */
    public static BufferedImage histogramEqualizationAttack(BufferedImage image) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.HISTOGRAM_EQUALIZATION);
        Map<String, Object> params = new HashMap<>();
        return attack.apply(image, params);
    }

    /**
     * Applies sharpening attack to the image.
     *
     * @param image Original image
     * @param amount Sharpening amount (0.0-2.0)
     * @return Attacked image
     */
    public static BufferedImage sharpeningAttack(BufferedImage image, float amount) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(AttackType.SHARPENING);
        Map<String, Object> params = new HashMap<>();
        params.put(SharpeningAttack.PARAM_AMOUNT, amount);
        return attack.apply(image, params);
    }

    /**
     * Applies a specified attack to an image.
     *
     * @param image The image to attack
     * @param attackType The type of attack to apply
     * @param params Parameters for the attack (quality, scale, etc.)
     * @return The attacked image
     */
    public static BufferedImage applyAttack(BufferedImage image, AttackType attackType, Object... params) {
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(attackType);
        Map<String, Object> paramMap = new HashMap<>();

        // Convert params to map based on attack type
        switch (attackType) {
            case JPEG_COMPRESSION:
            case JPEG_COMPRESSION_INTERNAL:
                if (params.length > 0) {
                    paramMap.put(JpegCompressionAttack.PARAM_QUALITY, ((Number) params[0]).floatValue());
                }
                break;

            case PNG_COMPRESSION:
                if (params.length > 0) {
                    paramMap.put(PngCompressionAttack.PARAM_LEVEL, ((Number) params[0]).intValue());
                }
                break;

            case ROTATION_45:
            case ROTATION_90:
                // These have fixed angles in the attack implementation
                break;

            case RESIZE_75:
            case RESIZE_50:
                // These have fixed scales in the attack implementation
                break;

            case CROPPING:
                if (params.length > 0) {
                    paramMap.put(CroppingAttack.PARAM_PERCENTAGE, ((Number) params[0]).doubleValue());
                }
                break;

            case GAUSSIAN_NOISE:
                if (params.length > 0) {
                    paramMap.put(GaussianNoiseAttack.PARAM_STD_DEV, ((Number) params[0]).doubleValue());
                }
                break;

            case MEDIAN_FILTER:
                if (params.length > 0) {
                    paramMap.put(MedianFilterAttack.PARAM_RADIUS, ((Number) params[0]).intValue());
                }
                break;

            case SHARPENING:
                if (params.length > 0) {
                    paramMap.put(SharpeningAttack.PARAM_AMOUNT, ((Number) params[0]).floatValue());
                }
                break;
        }

        return attack.apply(image, paramMap);
    }
}
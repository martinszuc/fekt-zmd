package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating watermark attack instances.
 * Provides a centralized way to get attack implementations.
 */
public class WatermarkAttackFactory {

    // Cache attack instances for reuse
    private static final Map<AttackType, AbstractWatermarkAttack> attackCache = new HashMap<>();

    /**
     * Gets an attack implementation for the specified attack type.
     *
     * @param attackType The type of attack to create
     * @return The attack implementation
     */
    public static AbstractWatermarkAttack getAttack(AttackType attackType) {
        // Check cache first
        if (attackCache.containsKey(attackType)) {
            return attackCache.get(attackType);
        }

        // Create a new attack instance
        AbstractWatermarkAttack attack;

        switch (attackType) {
            case JPEG_COMPRESSION:
                attack = new JpegCompressionAttack();
                break;
            case JPEG_COMPRESSION_INTERNAL:
                attack = new JpegCompressionInternalAttack();
                break;
            case PNG_COMPRESSION:
                attack = new PngCompressionAttack();
                break;
            case ROTATION_45:
                attack = new RotationAttack(AttackType.ROTATION_45, 45);
                break;
            case ROTATION_90:
                attack = new RotationAttack(AttackType.ROTATION_90, 90);
                break;
            case RESIZE_75:
                attack = new ResizeAttack(AttackType.RESIZE_75, 0.75);
                break;
            case RESIZE_50:
                attack = new ResizeAttack(AttackType.RESIZE_50, 0.50);
                break;
            case MIRRORING:
                attack = new MirroringAttack();
                break;
            case CROPPING:
                attack = new CroppingAttack();
                break;
            case GAUSSIAN_NOISE:
                attack = new GaussianNoiseAttack();
                break;
            case MEDIAN_FILTER:
                attack = new MedianFilterAttack();
                break;
            case HISTOGRAM_EQUALIZATION:
                attack = new HistogramEqualizationAttack();
                break;
            case SHARPENING:
                attack = new SharpeningAttack();
                break;
            case NONE:
            default:
                attack = new NoAttack();
                break;
        }

        // Cache the attack instance
        attackCache.put(attackType, attack);
        Logger.info("Created attack instance: " + attackType.getDisplayName());

        return attack;
    }

    /**
     * Gets all available attack implementations.
     *
     * @return Map of attack types to attack implementations
     */
    public static Map<AttackType, AbstractWatermarkAttack> getAllAttacks() {
        // Make sure all attack types have been instantiated
        for (AttackType attackType : AttackType.values()) {
            getAttack(attackType);
        }

        return new HashMap<>(attackCache);
    }
}
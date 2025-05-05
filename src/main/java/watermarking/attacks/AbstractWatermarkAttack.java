package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Abstract base class for watermark attacks.
 * Provides a common interface and utility methods for all attack implementations.
 */
public abstract class AbstractWatermarkAttack {

    /** The type of this attack */
    protected final AttackType attackType;

    /**
     * Constructs an attack with the specified type.
     *
     * @param attackType The type of attack
     */
    protected AbstractWatermarkAttack(AttackType attackType) {
        this.attackType = attackType;
    }

    /**
     * Applies the attack to an image.
     *
     * @param image The image to attack
     * @param params Map of parameter names to values (type depends on the specific attack)
     * @return The attacked image
     */
    public abstract BufferedImage apply(BufferedImage image, Map<String, Object> params);

    /**
     * Gets a formatted string describing the attack parameters.
     *
     * @param params Map of parameter names to values
     * @return String describing the parameters (e.g., "Quality: 75%")
     */
    public abstract String getParametersDescription(Map<String, Object> params);

    /**
     * Gets the attack type.
     *
     * @return The attack type
     */
    public AttackType getAttackType() {
        return attackType;
    }

    /**
     * Gets the display name of the attack.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return attackType.getDisplayName();
    }

    /**
     * Gets the description of the attack.
     *
     * @return The description
     */
    public String getDescription() {
        return attackType.getDescription();
    }

    /**
     * Creates default parameters for the attack.
     *
     * @return Map of parameter names to default values
     */
    public abstract Map<String, Object> getDefaultParameters();

    /**
     * Logs the start of an attack.
     *
     * @param params Attack parameters
     */
    protected void logAttackStart(Map<String, Object> params) {
        Logger.info("Applying " + attackType.getDisplayName() + " attack with parameters: " +
                getParametersDescription(params));
    }

    /**
     * Logs the completion of an attack.
     */
    protected void logAttackComplete() {
        Logger.info(attackType.getDisplayName() + " attack completed");
    }

    /**
     * Logs an error during the attack.
     *
     * @param e The exception that occurred
     */
    protected void logAttackError(Exception e) {
        Logger.error("Error applying " + attackType.getDisplayName() + " attack: " + e.getMessage());
    }
}
package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies resize attack to an image.
 * Resizing first shrinks the image, then expands it back to original dimensions,
 * causing interpolation errors that can damage watermarks.
 */
public class ResizeAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for resize scale.
     */
    public static final String PARAM_SCALE = "scale";

    /**
     * Default scale factor.
     */
    private final double defaultScale;

    /**
     * Creates a resize attack with the specified scale.
     *
     * @param attackType The attack type
     * @param defaultScale The default scale factor (0.0-1.0)
     */
    public ResizeAttack(AttackType attackType, double defaultScale) {
        super(attackType);
        this.defaultScale = defaultScale;
    }

    /**
     * Creates a resize attack with a custom scale.
     *
     * @param scale The scale factor (0.0-1.0)
     */
    public ResizeAttack(double scale) {
        super(AttackType.RESIZE_75); // Default type, will be overridden in parameters
        this.defaultScale = scale;
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        double scale = defaultScale;
        if (params.containsKey(PARAM_SCALE)) {
            scale = ((Number) params.get(PARAM_SCALE)).doubleValue();
        }

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // Calculate new dimensions
        int newWidth = (int)(originalWidth * scale);
        int newHeight = (int)(originalHeight * scale);

        // Ensure minimum size of 1x1
        newWidth = Math.max(1, newWidth);
        newHeight = Math.max(1, newHeight);

        // First scale down
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Then scale back up to original size
        BufferedImage resultImage = new BufferedImage(originalWidth, originalHeight, image.getType());
        g2d = resultImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, originalWidth, originalHeight, null);
        g2d.dispose();

        logAttackComplete();
        return resultImage;
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        double scale = defaultScale;
        if (params.containsKey(PARAM_SCALE)) {
            scale = ((Number) params.get(PARAM_SCALE)).doubleValue();
        }
        return "Scale: " + (scale * 100) + "%";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_SCALE, defaultScale);
        return params;
    }
}
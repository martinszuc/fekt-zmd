package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies sharpening to an image.
 * Sharpening enhances edges and high-frequency components, which can
 * affect watermarks embedded in these areas.
 */
public class SharpeningAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for sharpening amount.
     */
    public static final String PARAM_AMOUNT = "amount";

    /**
     * Default sharpening amount.
     */
    public static final float DEFAULT_AMOUNT = 1.0f;

    /**
     * Creates a new sharpening attack.
     */
    public SharpeningAttack() {
        super(AttackType.SHARPENING);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        float amount = DEFAULT_AMOUNT;
        if (params.containsKey(PARAM_AMOUNT)) {
            amount = ((Number) params.get(PARAM_AMOUNT)).floatValue();
        }

        // Ensure amount is within valid range (0.0-2.0)
        amount = Math.max(0.0f, Math.min(2.0f, amount));

        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage sharpenedImage = new BufferedImage(width, height, image.getType());

        // Define sharpening kernel
        float center = 1.0f + 4.0f * amount;
        float corner = -amount / 4.0f;
        float side = -amount;

        float[] sharpenKernel = {
                corner, side, corner,
                side, center, side,
                corner, side, corner
        };

        Kernel kernel = new Kernel(3, 3, sharpenKernel);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        op.filter(image, sharpenedImage);

        logAttackComplete();
        return sharpenedImage;
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        float amount = DEFAULT_AMOUNT;
        if (params.containsKey(PARAM_AMOUNT)) {
            amount = ((Number) params.get(PARAM_AMOUNT)).floatValue();
        }
        return "Amount: " + amount;
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_AMOUNT, DEFAULT_AMOUNT);
        return params;
    }
}
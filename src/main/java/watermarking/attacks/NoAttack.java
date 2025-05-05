package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the absence of any attack.
 * Used for baseline comparisons in watermark testing.
 */
public class NoAttack extends AbstractWatermarkAttack {

    /**
     * Creates a "no attack" instance.
     */
    public NoAttack() {
        super(AttackType.NONE);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Simply return a copy of the original image
        BufferedImage copy = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                image.getType()
        );
        copy.getGraphics().drawImage(image, 0, 0, null);

        logAttackComplete();
        return copy;
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        return "None";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        return new HashMap<>();
    }
}
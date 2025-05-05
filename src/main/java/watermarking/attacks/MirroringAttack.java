package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies mirroring attack to an image (horizontal flip).
 * Mirroring can completely destroy many watermarking schemes, especially
 * those based on spatial patterns.
 */
public class MirroringAttack extends AbstractWatermarkAttack {

    /**
     * Parameter for flip direction (horizontal or vertical).
     */
    public static final String PARAM_DIRECTION = "direction";

    /**
     * Horizontal flip direction.
     */
    public static final String DIRECTION_HORIZONTAL = "horizontal";

    /**
     * Vertical flip direction.
     */
    public static final String DIRECTION_VERTICAL = "vertical";

    /**
     * Default flip direction.
     */
    public static final String DEFAULT_DIRECTION = DIRECTION_HORIZONTAL;

    /**
     * Creates a new mirroring attack.
     */
    public MirroringAttack() {
        super(AttackType.MIRRORING);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        String direction = DEFAULT_DIRECTION;
        if (params.containsKey(PARAM_DIRECTION)) {
            direction = (String) params.get(PARAM_DIRECTION);
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage mirroredImage = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = mirroredImage.createGraphics();

        // Draw the image flipped based on direction
        if (DIRECTION_HORIZONTAL.equals(direction)) {
            // Horizontal flip (mirror)
            g2d.drawImage(image, width, 0, -width, height, null);
        } else {
            // Vertical flip
            g2d.drawImage(image, 0, height, width, -height, null);
        }
        g2d.dispose();

        logAttackComplete();
        return mirroredImage;
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        String direction = DEFAULT_DIRECTION;
        if (params.containsKey(PARAM_DIRECTION)) {
            direction = (String) params.get(PARAM_DIRECTION);
        }
        return "Direction: " + direction;
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_DIRECTION, DEFAULT_DIRECTION);
        return params;
    }
}
package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies cropping attack to an image.
 * Cropping removes the edges of the image and then resizes back to
 * the original dimensions, which can destroy parts of the watermark.
 */
public class CroppingAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for crop percentage.
     */
    public static final String PARAM_PERCENTAGE = "percentage";

    /**
     * Default crop percentage.
     */
    public static final double DEFAULT_PERCENTAGE = 0.2;

    /**
     * Creates a new cropping attack.
     */
    public CroppingAttack() {
        super(AttackType.CROPPING);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        double cropPercentage = DEFAULT_PERCENTAGE;
        if (params.containsKey(PARAM_PERCENTAGE)) {
            cropPercentage = ((Number) params.get(PARAM_PERCENTAGE)).doubleValue();
        }

        // Ensure crop percentage is within valid range (0.0-0.5)
        cropPercentage = Math.max(0.0, Math.min(0.5, cropPercentage));

        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate crop dimensions
        int cropWidth = (int)(width * cropPercentage);
        int cropHeight = (int)(height * cropPercentage);

        // Crop the image
        BufferedImage croppedImage = new BufferedImage(
                width - 2 * cropWidth,
                height - 2 * cropHeight,
                image.getType()
        );

        Graphics2D g2d = croppedImage.createGraphics();
        g2d.drawImage(
                image,
                0, 0, croppedImage.getWidth(), croppedImage.getHeight(),
                cropWidth, cropHeight, width - cropWidth, height - cropHeight,
                null
        );
        g2d.dispose();

        // Resize back to original size
        BufferedImage resultImage = new BufferedImage(width, height, image.getType());
        g2d = resultImage.createGraphics();
        g2d.drawImage(croppedImage, 0, 0, width, height, null);
        g2d.dispose();

        logAttackComplete();
        return resultImage;
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        double percentage = DEFAULT_PERCENTAGE;
        if (params.containsKey(PARAM_PERCENTAGE)) {
            percentage = ((Number) params.get(PARAM_PERCENTAGE)).doubleValue();
        }
        return "Crop: " + (percentage * 100) + "%";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_PERCENTAGE, DEFAULT_PERCENTAGE);
        return params;
    }
}
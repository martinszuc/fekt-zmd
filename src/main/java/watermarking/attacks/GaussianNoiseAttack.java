package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Applies Gaussian noise to an image.
 * This attack adds random noise to the image that can degrade the watermark.
 */
public class GaussianNoiseAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for noise standard deviation.
     */
    public static final String PARAM_STD_DEV = "stddev";

    /**
     * Default standard deviation.
     */
    public static final double DEFAULT_STD_DEV = 10.0;

    /**
     * Creates a new Gaussian noise attack.
     */
    public GaussianNoiseAttack() {
        super(AttackType.GAUSSIAN_NOISE);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        double standardDeviation = DEFAULT_STD_DEV;
        if (params.containsKey(PARAM_STD_DEV)) {
            standardDeviation = ((Number) params.get(PARAM_STD_DEV)).doubleValue();
        }

        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage noisyImage = new BufferedImage(width, height, image.getType());

        // Create random number generator
        Random random = new Random();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));

                // Add Gaussian noise to each channel
                int red = color.getRed() + (int)(random.nextGaussian() * standardDeviation);
                int green = color.getGreen() + (int)(random.nextGaussian() * standardDeviation);
                int blue = color.getBlue() + (int)(random.nextGaussian() * standardDeviation);

                // Clamp values to valid range [0, 255]
                red = Math.max(0, Math.min(255, red));
                green = Math.max(0, Math.min(255, green));
                blue = Math.max(0, Math.min(255, blue));

                noisyImage.setRGB(x, y, new Color(red, green, blue).getRGB());
            }
        }

        logAttackComplete();
        return noisyImage;
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        double stdDev = DEFAULT_STD_DEV;
        if (params.containsKey(PARAM_STD_DEV)) {
            stdDev = ((Number) params.get(PARAM_STD_DEV)).doubleValue();
        }
        return "StdDev: " + stdDev;
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_STD_DEV, DEFAULT_STD_DEV);
        return params;
    }
}
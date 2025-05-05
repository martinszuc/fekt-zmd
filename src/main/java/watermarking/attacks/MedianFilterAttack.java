package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies median filtering to an image.
 * This is a common image processing operation that can damage watermarks,
 * especially those that use high-frequency components.
 */
public class MedianFilterAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for filter radius.
     */
    public static final String PARAM_RADIUS = "radius";

    /**
     * Default filter radius.
     */
    public static final int DEFAULT_RADIUS = 1;

    /**
     * Creates a new median filter attack.
     */
    public MedianFilterAttack() {
        super(AttackType.MEDIAN_FILTER);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        int radius = DEFAULT_RADIUS;
        if (params.containsKey(PARAM_RADIUS)) {
            radius = ((Number) params.get(PARAM_RADIUS)).intValue();
        }

        // Ensure radius is within valid range (1-5)
        radius = Math.max(1, Math.min(5, radius));

        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage filteredImage = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Collect pixel values in the neighborhood
                ArrayList<Integer> redValues = new ArrayList<>();
                ArrayList<Integer> greenValues = new ArrayList<>();
                ArrayList<Integer> blueValues = new ArrayList<>();

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = Math.max(0, Math.min(width - 1, x + dx));
                        int ny = Math.max(0, Math.min(height - 1, y + dy));

                        Color color = new Color(image.getRGB(nx, ny));
                        redValues.add(color.getRed());
                        greenValues.add(color.getGreen());
                        blueValues.add(color.getBlue());
                    }
                }

                // Sort and find median values
                Collections.sort(redValues);
                Collections.sort(greenValues);
                Collections.sort(blueValues);

                int medianIndex = redValues.size() / 2;
                int medianRed = redValues.get(medianIndex);
                int medianGreen = greenValues.get(medianIndex);
                int medianBlue = blueValues.get(medianIndex);

                filteredImage.setRGB(x, y, new Color(medianRed, medianGreen, medianBlue).getRGB());
            }
        }

        logAttackComplete();
        return filteredImage;
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        int radius = DEFAULT_RADIUS;
        if (params.containsKey(PARAM_RADIUS)) {
            radius = ((Number) params.get(PARAM_RADIUS)).intValue();
        }
        return "Radius: " + radius;
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_RADIUS, DEFAULT_RADIUS);
        return params;
    }
}
package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies PNG compression to an image.
 * Although PNG is lossless, each save/load cycle can introduce small
 * artifacts due to rounding errors, which can affect watermarks.
 */
public class PngCompressionAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for compression level.
     */
    public static final String PARAM_LEVEL = "level";

    /**
     * Default compression level (1-9).
     */
    public static final int DEFAULT_LEVEL = 5;

    /**
     * Creates a new PNG compression attack.
     */
    public PngCompressionAttack() {
        super(AttackType.PNG_COMPRESSION);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        int compressionLevel = DEFAULT_LEVEL;
        if (params.containsKey(PARAM_LEVEL)) {
            compressionLevel = ((Number) params.get(PARAM_LEVEL)).intValue();
        }

        try {
            // Write image to memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // PNG is lossless, but we can simulate different levels by multiple save/load cycles
            // The more cycles, the more artifacts will be introduced due to rounding errors
            BufferedImage tempImage = image;

            // Perform multiple save/load cycles based on inverse of compression level
            // Higher compression level = fewer cycles (inverse relationship)
            int cycles = Math.max(1, 10 - compressionLevel);

            for (int i = 0; i < cycles; i++) {
                baos.reset();
                ImageIO.write(tempImage, "png", baos);
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                tempImage = ImageIO.read(bais);
            }

            logAttackComplete();
            return tempImage;

        } catch (IOException e) {
            logAttackError(e);
            return image; // Return original image on error
        }
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        int level = DEFAULT_LEVEL;
        if (params.containsKey(PARAM_LEVEL)) {
            level = ((Number) params.get(PARAM_LEVEL)).intValue();
        }
        return "Level: " + level + " (1-9)";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_LEVEL, DEFAULT_LEVEL);
        return params;
    }
}
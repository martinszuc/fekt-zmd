package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies JPEG compression to an image.
 * Uses Java's built-in compression capabilities to simulate JPEG compression artifacts.
 */
public class JpegCompressionAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for JPEG quality.
     */
    public static final String PARAM_QUALITY = "quality";

    /**
     * Default quality value (0-100).
     */
    public static final float DEFAULT_QUALITY = 75.0f;

    /**
     * Creates a new JPEG compression attack.
     */
    public JpegCompressionAttack() {
        super(AttackType.JPEG_COMPRESSION);
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        float quality = DEFAULT_QUALITY;
        if (params.containsKey(PARAM_QUALITY)) {
            quality = ((Number) params.get(PARAM_QUALITY)).floatValue();
        }

        try {
            // Convert quality from 0-100 to 0-1 scale for ImageIO
            float compressionQuality = quality / 100f;

            // Get a JPEG writer
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();

            // Set compression quality
            ImageWriteParam writerParams = writer.getDefaultWriteParam();
            writerParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writerParams.setCompressionQuality(compressionQuality);

            // Write image to memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writerParams);
            writer.dispose();
            ios.close();

            // Read image back
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            BufferedImage compressedImage = ImageIO.read(bais);

            logAttackComplete();
            return compressedImage;

        } catch (IOException e) {
            logAttackError(e);
            return image; // Return original image on error
        }
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        float quality = DEFAULT_QUALITY;
        if (params.containsKey(PARAM_QUALITY)) {
            quality = ((Number) params.get(PARAM_QUALITY)).floatValue();
        }
        return "Quality: " + quality + "%";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_QUALITY, DEFAULT_QUALITY);
        return params;
    }
}
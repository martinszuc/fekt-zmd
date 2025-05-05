package watermarking.attacks;

import enums.AttackType;
import utils.Logger;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Applies rotation to an image.
 * Rotation can severely damage watermarks, especially when the angle
 * is not a multiple of 90 degrees, requiring interpolation.
 */
public class RotationAttack extends AbstractWatermarkAttack {

    /**
     * Parameter name for rotation angle.
     */
    public static final String PARAM_ANGLE = "angle";

    /**
     * The rotation angle in degrees.
     */
    private final double defaultAngle;

    /**
     * Creates a rotation attack with the specified angle.
     *
     * @param attackType The attack type
     * @param defaultAngle The default rotation angle
     */
    public RotationAttack(AttackType attackType, double defaultAngle) {
        super(attackType);
        this.defaultAngle = defaultAngle;
    }

    /**
     * Creates a rotation attack with a custom angle.
     *
     * @param angle The rotation angle
     */
    public RotationAttack(double angle) {
        super(AttackType.ROTATION_45); // Default type, will be overridden in parameters
        this.defaultAngle = angle;
    }

    @Override
    public BufferedImage apply(BufferedImage image, Map<String, Object> params) {
        logAttackStart(params);

        // Extract parameters
        double degrees = defaultAngle;
        if (params.containsKey(PARAM_ANGLE)) {
            degrees = ((Number) params.get(PARAM_ANGLE)).doubleValue();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // For 90 degree rotations, use direct approach to preserve quality
        if (degrees == 90 || degrees == 180 || degrees == 270 || degrees == -90 || degrees == -180 || degrees == -270) {
            int rotatedWidth = (degrees == 90 || degrees == 270 || degrees == -90 || degrees == -270) ? height : width;
            int rotatedHeight = (degrees == 90 || degrees == 270 || degrees == -90 || degrees == -270) ? width : height;

            BufferedImage rotatedImage = new BufferedImage(rotatedWidth, rotatedHeight, image.getType());
            Graphics2D g2d = rotatedImage.createGraphics();

            // Set transformation
            if (degrees == 90 || degrees == -270) {
                g2d.translate(height, 0);
                g2d.rotate(Math.PI / 2);
            } else if (degrees == 270 || degrees == -90) {
                g2d.translate(0, width);
                g2d.rotate(3 * Math.PI / 2);
            } else { // 180 or -180
                g2d.translate(width, height);
                g2d.rotate(Math.PI);
            }

            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            logAttackComplete();
            return rotatedImage;
        }

        // For arbitrary angles, calculate new dimensions to contain rotated image
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int newWidth = (int) Math.floor(width * cos + height * sin);
        int newHeight = (int) Math.floor(height * cos + width * sin);

        // Create new image with calculated dimensions
        BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2d = rotatedImage.createGraphics();

        // Set transformation around center of new image
        g2d.translate((newWidth - width) / 2, (newHeight - height) / 2);
        g2d.rotate(radians, width / 2, height / 2);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        // Create image with original dimensions (cropped if necessary)
        BufferedImage resultImage = new BufferedImage(width, height, image.getType());
        g2d = resultImage.createGraphics();

        // Draw rotated image centered in the original dimensions
        int x = (width - newWidth) / 2;
        int y = (height - newHeight) / 2;
        g2d.drawImage(rotatedImage, x, y, null);
        g2d.dispose();

        logAttackComplete();
        return resultImage;
    }

    @Override
    public String getParametersDescription(Map<String, Object> params) {
        double angle = defaultAngle;
        if (params.containsKey(PARAM_ANGLE)) {
            angle = ((Number) params.get(PARAM_ANGLE)).doubleValue();
        }
        return "Angle: " + angle + "°";
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(PARAM_ANGLE, defaultAngle);
        return params;
    }
}
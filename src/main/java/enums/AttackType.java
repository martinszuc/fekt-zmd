package enums;

/**
 * Enumeration of available watermark attack types.
 * Each attack type includes a display name and description for the UI.
 */
public enum AttackType {
    NONE("No Attack", "Original watermarked image without any attacks"),
    JPEG_COMPRESSION("JPEG Compression", "Applies JPEG compression with specified quality"),
    JPEG_COMPRESSION_INTERNAL("Internal JPEG Compression", "Uses application's compression pipeline"),
    PNG_COMPRESSION("PNG Compression", "Applies PNG compression with specified level"),
    ROTATION_45("Rotation 45°", "Rotates image by 45 degrees"),
    ROTATION_90("Rotation 90°", "Rotates image by 90 degrees"),
    RESIZE_75("Resize 75%", "Resizes image to 75% and back"),
    RESIZE_50("Resize 50%", "Resizes image to 50% and back"),
    MIRRORING("Mirroring", "Flips image horizontally"),
    CROPPING("Cropping", "Crops image edges and resizes back"),
    GAUSSIAN_NOISE("Gaussian Noise", "Adds random noise to the image"),
    MEDIAN_FILTER("Median Filter", "Applies median filtering to reduce noise"),
    HISTOGRAM_EQUALIZATION("Histogram Equalization", "Enhances contrast through histogram equalization"),
    SHARPENING("Sharpening", "Enhances edges in the image");

    private final String displayName;
    private final String description;

    /**
     * Constructs an attack type with display name and description.
     *
     * @param displayName The name to display in the UI
     * @param description A brief description of the attack
     */
    AttackType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the display name of the attack.
     *
     * @return Display name for UI
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of the attack.
     *
     * @return Description for tooltips and documentation
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
package enums;

/**
 * Enum for the different quality component types that can be selected
 * for quality assessment in the application.
 */
public enum QualityType {
    RGB("RGB"),
    RED("Red"),
    GREEN("Green"),
    BLUE("Blue"),
    Y("Y"),
    CB("Cb"),
    CR("Cr"),
    YCBCR("YCbCr");

    private final String name;

    QualityType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
package enums;

/**
 * Enum defining available watermarking methods.
 */
public enum WatermarkType {
    LSB("LSB (Spatial Domain)"),
    DCT("DCT (Frequency Domain)"),
    DWT("DWT (Wavelet Domain)"),
    SVD("SVD (Singular Value Decomposition)");

    private final String displayName;

    WatermarkType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}


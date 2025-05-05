package watermarking.core;

import enums.WatermarkType;
import watermarking.frequency.DCTWatermarking;
import watermarking.frequency.DWTWatermarking;
import watermarking.frequency.SVDWatermarking;
import watermarking.spatial.LSBWatermarking;

/**
 * Factory class for creating watermarking technique instances.
 */
public class WatermarkingFactory {

    /**
     * Creates a watermarking technique instance based on the selected method.
     *
     * @param method The watermarking method to create
     * @return The watermarking technique instance
     */
    public static AbstractWatermarking createWatermarking(WatermarkType method) {
        switch (method) {
            case LSB:
                return new LSBWatermarking();
            case DCT:
                return new DCTWatermarking();
            case DWT:
                return new DWTWatermarking();
            case SVD:
                return new SVDWatermarking();
            default:
                throw new IllegalArgumentException("Unsupported watermarking method: " + method);
        }
    }
}

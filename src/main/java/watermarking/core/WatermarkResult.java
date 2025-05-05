package watermarking.core;

import enums.AttackType;
import enums.WatermarkType;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class for storing watermark evaluation results.
 * Used for tracking and comparing different attack effectiveness.
 */
public class WatermarkResult {
    private int testId;                  // Unique test ID
    private AttackType attackType;       // Type of the attack applied
    private String attackName;           // Name of the attack applied
    private WatermarkType watermarkType; // Watermarking method used
    private String method;               // Watermarking method display name
    private String component;            // Component used (Y, Cb, Cr)
    private String parameter;            // Key parameter for watermarking
    private double ber;                  // Bit Error Rate
    private double nc;                   // Normalized Correlation
    private double psnr;                 // Peak Signal-to-Noise Ratio
    private double wnr;                  // Watermark-to-Noise Ratio
    private String timestamp;            // When the test was performed
    private String attackParameters;     // Parameters used for the attack (e.g., JPEG quality)
    private String watermarkConfig;      // Description of watermark configuration used

    // Static counter for generating unique test IDs
    private static int nextTestId = 1;

    /**
     * Creates a new watermark evaluation result with enhanced tracking.
     *
     * @param attackType The type of attack applied
     * @param watermarkType The type of watermarking used
     * @param component The image component (Y, Cb, Cr)
     * @param parameter Key parameter used in watermarking
     * @param ber Bit Error Rate result
     * @param nc Normalized Correlation result
     */
    public WatermarkResult(AttackType attackType, WatermarkType watermarkType,
                           String component, String parameter, double ber, double nc) {
        this.testId = nextTestId++;
        this.attackType = attackType;
        this.attackName = attackType.getDisplayName();
        this.watermarkType = watermarkType;
        this.method = watermarkType.toString();
        this.component = component;
        this.parameter = parameter;
        this.ber = ber;
        this.nc = nc;
        this.psnr = 0.0;
        this.wnr = 0.0;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.attackParameters = "Default";
        this.watermarkConfig = "Default";
    }

    /**
     * Creates a new watermark evaluation result with all metrics and attack parameters.
     *
     * @param attackType The type of attack applied
     * @param watermarkType The type of watermarking used
     * @param component The image component (Y, Cb, Cr)
     * @param parameter Key parameter used in watermarking
     * @param ber Bit Error Rate result
     * @param nc Normalized Correlation result
     * @param psnr Peak Signal-to-Noise Ratio result
     * @param wnr Watermark-to-Noise Ratio result
     * @param attackParameters Description of attack parameters used
     */
    public WatermarkResult(AttackType attackType, WatermarkType watermarkType,
                           String component, String parameter, double ber, double nc,
                           double psnr, double wnr, String attackParameters) {
        this(attackType, watermarkType, component, parameter, ber, nc);
        this.psnr = psnr;
        this.wnr = wnr;
        this.attackParameters = attackParameters;
    }

    /**
     * Creates a new watermark evaluation result with all metrics, attack parameters,
     * and watermark configuration information.
     *
     * @param attackType The type of attack applied
     * @param watermarkType The type of watermarking used
     * @param component The image component (Y, Cb, Cr)
     * @param parameter Key parameter used in watermarking
     * @param ber Bit Error Rate result
     * @param nc Normalized Correlation result
     * @param psnr Peak Signal-to-Noise Ratio result
     * @param wnr Watermark-to-Noise Ratio result
     * @param attackParameters Description of attack parameters used
     * @param watermarkConfig Description of watermark configuration used
     */
    public WatermarkResult(AttackType attackType, WatermarkType watermarkType,
                           String component, String parameter, double ber, double nc,
                           double psnr, double wnr, String attackParameters,
                           String watermarkConfig) {
        this(attackType, watermarkType, component, parameter, ber, nc, psnr, wnr, attackParameters);
        this.watermarkConfig = watermarkConfig;
    }

    // Getters

    /**
     * Gets the unique test ID.
     *
     * @return The test ID
     */
    public int getTestId() {
        return testId;
    }

    /**
     * Gets the timestamp of when the test was performed.
     *
     * @return Formatted timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the description of attack parameters.
     *
     * @return Attack parameters description
     */
    public String getAttackParameters() {
        return attackParameters;
    }

    /**
     * Gets the description of watermark configuration.
     *
     * @return Watermark configuration description
     */
    public String getWatermarkConfig() {
        return watermarkConfig;
    }

    /**
     * Sets the watermark configuration description.
     *
     * @param watermarkConfig The watermark configuration description
     */
    public void setWatermarkConfig(String watermarkConfig) {
        this.watermarkConfig = watermarkConfig;
    }

    /**
     * Gets the attack type.
     *
     * @return The attack type
     */
    public AttackType getAttackType() {
        return attackType;
    }

    /**
     * Gets the attack name.
     *
     * @return The attack name
     */
    public String getAttackName() {
        return attackName;
    }

    /**
     * Gets the watermark type.
     *
     * @return The watermark type
     */
    public WatermarkType getWatermarkType() {
        return watermarkType;
    }

    /**
     * Gets the watermarking method name.
     *
     * @return The method name
     */
    public String getMethod() {
        return method;
    }

    /**
     * Gets the component used for watermarking.
     *
     * @return The component (Y, Cb, Cr)
     */
    public String getComponent() {
        return component;
    }

    /**
     * Gets the key parameter used for watermarking.
     *
     * @return The parameter description
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * Gets the Bit Error Rate.
     *
     * @return The BER value
     */
    public double getBer() {
        return ber;
    }

    /**
     * Gets the Normalized Correlation.
     *
     * @return The NC value
     */
    public double getNc() {
        return nc;
    }

    /**
     * Gets the Peak Signal-to-Noise Ratio.
     *
     * @return The PSNR value
     */
    public double getPsnr() {
        return psnr;
    }

    /**
     * Gets the Watermark-to-Noise Ratio.
     *
     * @return The WNR value
     */
    public double getWnr() {
        return wnr;
    }

    @Override
    public String toString() {
        return String.format("Test #%d - %s - Attack: %s (%s), Method: %s, Component: %s, Parameter: %s, BER: %.4f, NC: %.4f, Config: %s",
                testId, timestamp, attackName, attackParameters, method, component, parameter, ber, nc, watermarkConfig);
    }

    /**
     * Gets a quality rating based on BER value.
     *
     * @return Quality rating string
     */
    public String getQualityRating() {
        if (ber < 0.01) {
            return "Excellent";
        } else if (ber < 0.05) {
            return "Very Good";
        } else if (ber < 0.10) {
            return "Good";
        } else if (ber < 0.20) {
            return "Fair";
        } else if (ber < 0.40) {
            return "Poor";
        } else {
            return "Failed";
        }
    }

    /**
     * Gets the robustness level based on the BER value and attack type.
     *
     * @return Robustness level string
     */
    public String getRobustnessLevel() {
        // More sophisticated robustness rating that considers attack type
        double threshold;

        // Different thresholds for different attacks
        if (attackType == AttackType.JPEG_COMPRESSION ||
                attackType == AttackType.JPEG_COMPRESSION_INTERNAL ||
                attackType == AttackType.PNG_COMPRESSION) {
            threshold = 0.15; // Compression is common, so be more lenient
        } else if (attackType == AttackType.ROTATION_45 ||
                attackType == AttackType.ROTATION_90) {
            threshold = 0.30; // Rotation is tough on watermarks
        } else if (attackType == AttackType.CROPPING) {
            threshold = 0.25; // Cropping can be severe
        } else if (attackType == AttackType.GAUSSIAN_NOISE) {
            threshold = 0.22; // Noise can be disruptive
        } else if (attackType == AttackType.MEDIAN_FILTER ||
                attackType == AttackType.HISTOGRAM_EQUALIZATION ||
                attackType == AttackType.SHARPENING) {
            threshold = 0.18; // Image processing operations
        } else {
            threshold = 0.20; // Default threshold
        }

        if (ber < threshold / 4) {
            return "High";
        } else if (ber < threshold / 2) {
            return "Good";
        } else if (ber < threshold) {
            return "Moderate";
        } else {
            return "Low";
        }
    }
}
package watermarking.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import watermarking.attacks.WatermarkAttacks.AttackType;

/**
 * Class for storing watermark evaluation results.
 * Used for comparing different attack effectiveness.
 */
public class WatermarkResult {
    private int testId;                  // Unique test ID
    private String attackName;           // Name of the attack applied
    private String method;               // Watermarking method used
    private String component;            // Component used (Y, Cb, Cr)
    private String parameter;            // Key parameter
    private double ber;                  // Bit Error Rate
    private double nc;                   // Normalized Correlation
    private double psnr;                 // Peak Signal-to-Noise Ratio
    private double wnr;                  // Watermark-to-Noise Ratio
    private String timestamp;            // When the test was performed
    private String attackParameters;     // Parameters used for the attack (e.g., JPEG quality)

    // Static counter for generating unique test IDs
    private static int nextTestId = 1;

    /**
     * Creates a new watermark evaluation result with enhanced tracking
     */
    public WatermarkResult(String attackName, String method, String component,
                           String parameter, double ber, double nc) {
        this.testId = nextTestId++;
        this.attackName = attackName;
        this.method = method;
        this.component = component;
        this.parameter = parameter;
        this.ber = ber;
        this.nc = nc;
        this.psnr = 0.0;
        this.wnr = 0.0;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.attackParameters = "Default";
    }

    /**
     * Creates a new watermark evaluation result with all metrics and attack parameters
     */
    public WatermarkResult(String attackName, String method, String component,
                           String parameter, double ber, double nc, double psnr, double wnr,
                           String attackParameters) {
        this(attackName, method, component, parameter, ber, nc);
        this.psnr = psnr;
        this.wnr = wnr;
        this.attackParameters = attackParameters;
    }

    /**
     * Creates a new watermark evaluation result with attack type enum and parameters
     */
    public WatermarkResult(AttackType attackType, String method, String component,
                           String parameter, double ber, double nc, String attackParameters) {
        this(attackType.toString(), method, component, parameter, ber, nc);
        this.attackParameters = attackParameters;
    }

    // Additional getters
    public int getTestId() {
        return testId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAttackParameters() {
        return attackParameters;
    }

    // Existing getters...
    public String getAttackName() {
        return attackName;
    }

    public String getMethod() {
        return method;
    }

    public String getComponent() {
        return component;
    }

    public String getParameter() {
        return parameter;
    }

    public double getBer() {
        return ber;
    }

    public double getNc() {
        return nc;
    }

    public double getPsnr() {
        return psnr;
    }

    public double getWnr() {
        return wnr;
    }

    @Override
    public String toString() {
        return String.format("Test #%d - %s - Attack: %s (%s), Method: %s, Component: %s, Parameter: %s, BER: %.4f, NC: %.4f",
                testId, timestamp, attackName, attackParameters, method, component, parameter, ber, nc);
    }

    /**
     * Gets a quality rating based on BER value
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
     * Gets the robustness level based on the BER value and attack type
     */
    public String getRobustnessLevel() {
        // More sophisticated robustness rating that considers attack type
        double threshold;

        // Different thresholds for different attacks
        if (attackName.contains("JPEG") || attackName.contains("PNG")) {
            threshold = 0.15; // Compression is common, so be more lenient
        } else if (attackName.contains("Rotation")) {
            threshold = 0.30; // Rotation is tough on watermarks
        } else if (attackName.contains("Cropping")) {
            threshold = 0.25; // Cropping can be severe
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
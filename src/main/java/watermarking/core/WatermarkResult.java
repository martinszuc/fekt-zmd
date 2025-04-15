package watermarking.core;

import watermarking.attacks.WatermarkAttacks.AttackType;

/**
 * Class for storing watermark evaluation results.
 * Used for comparing different attack effectiveness.
 */
public class WatermarkResult {
    private String attackName;
    private String method;
    private String component;
    private String parameter;
    private double ber;
    private double nc;
    private double psnr;
    private double wnr;

    /**
     * Creates a new watermark evaluation result with basic metrics
     *
     * @param attackName Name of the attack applied (or "None")
     * @param method Watermarking method used (LSB, DCT)
     * @param component Component used (Y, Cb, Cr)
     * @param parameter Key parameter (bit plane for LSB, block size for DCT)
     * @param ber Bit Error Rate (0.0-1.0)
     * @param nc Normalized Correlation (-1.0-1.0)
     */
    public WatermarkResult(String attackName, String method, String component,
                           String parameter, double ber, double nc) {
        this.attackName = attackName;
        this.method = method;
        this.component = component;
        this.parameter = parameter;
        this.ber = ber;
        this.nc = nc;
        this.psnr = 0.0;
        this.wnr = 0.0;
    }

    /**
     * Creates a new watermark evaluation result with all metrics
     *
     * @param attackName Name of the attack applied (or "None")
     * @param method Watermarking method used (LSB, DCT)
     * @param component Component used (Y, Cb, Cr)
     * @param parameter Key parameter (bit plane for LSB, block size for DCT)
     * @param ber Bit Error Rate (0.0-1.0)
     * @param nc Normalized Correlation (-1.0-1.0)
     * @param psnr Peak Signal-to-Noise Ratio
     * @param wnr Watermark-to-Noise Ratio
     */
    public WatermarkResult(String attackName, String method, String component,
                           String parameter, double ber, double nc, double psnr, double wnr) {
        this.attackName = attackName;
        this.method = method;
        this.component = component;
        this.parameter = parameter;
        this.ber = ber;
        this.nc = nc;
        this.psnr = psnr;
        this.wnr = wnr;
    }

    /**
     * Creates a new watermark evaluation result with attack type enum
     *
     * @param attackType Type of attack applied
     * @param method Watermarking method used (LSB, DCT)
     * @param component Component used (Y, Cb, Cr)
     * @param parameter Key parameter (bit plane for LSB, block size for DCT)
     * @param ber Bit Error Rate (0.0-1.0)
     * @param nc Normalized Correlation (-1.0-1.0)
     */
    public WatermarkResult(AttackType attackType, String method, String component,
                           String parameter, double ber, double nc) {
        this(attackType.toString(), method, component, parameter, ber, nc);
    }

    /**
     * Creates a new watermark evaluation result with attack type enum and all metrics
     *
     * @param attackType Type of attack applied
     * @param method Watermarking method used (LSB, DCT)
     * @param component Component used (Y, Cb, Cr)
     * @param parameter Key parameter (bit plane for LSB, block size for DCT)
     * @param ber Bit Error Rate (0.0-1.0)
     * @param nc Normalized Correlation (-1.0-1.0)
     * @param psnr Peak Signal-to-Noise Ratio
     * @param wnr Watermark-to-Noise Ratio
     */
    public WatermarkResult(AttackType attackType, String method, String component,
                           String parameter, double ber, double nc, double psnr, double wnr) {
        this(attackType.toString(), method, component, parameter, ber, nc, psnr, wnr);
    }

    // Getters
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
        return String.format("Attack: %s, Method: %s, Component: %s, Parameter: %s, BER: %.4f, NC: %.4f",
                attackName, method, component, parameter, ber, nc);
    }

    /**
     * Gets a quality rating based on BER value
     * @return Quality rating from "Excellent" to "Failed"
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
}
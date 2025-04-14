package watermarking;

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

    /**
     * Creates a new watermark evaluation result
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

    @Override
    public String toString() {
        return String.format("Attack: %s, Method: %s, Component: %s, Parameter: %s, BER: %.4f, NC: %.4f",
                attackName, method, component, parameter, ber, nc);
    }
}
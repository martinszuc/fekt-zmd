package watermarking.testing;

import enums.AttackType;
import enums.WatermarkType;
import utils.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates CSV files specifically formatted for creating comparative graphs
 * between different watermarking techniques.
 */
public class WatermarkComparisonGraphs {

    // Directory for graph data
    private static final String GRAPH_DATA_DIR = "graph-data";

    // Metrics to compare
    private static final String[] METRICS = {"BER", "NC", "PSNR"};

    /**
     * Main method to run the graph data generator
     */
    public static void main(String[] args) {
        Logger.info("Starting watermark comparison graph data generator...");

        try {
            // Create directory for graph data
            Files.createDirectories(Paths.get(GRAPH_DATA_DIR));

            // Process existing test results
            processTestResults();

            // Or run new tests if no results exist
            if (!new File("protocol-results/metrics.txt").exists()) {
                Logger.info("No existing test results found. Running new tests...");
                ProtocolBatchRunner.main(args);
                processTestResults();
            }

            Logger.info("Graph data generation complete. CSV files are in the " + GRAPH_DATA_DIR + " directory.");

        } catch (Exception e) {
            Logger.error("Error generating graph data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process existing test results and generate graph data
     */
    private static void processTestResults() throws IOException {
        Logger.info("Processing test results to generate graph data...");

        // Parse metrics.txt if it exists
        List<TestResult> results = new ArrayList<>();
        File metricsFile = new File("protocol-results/metrics.txt");

        if (metricsFile.exists()) {
            List<String> lines = Files.readAllLines(Paths.get(metricsFile.getPath()));
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    TestResult result = new TestResult();
                    result.testName = parts[0];
                    result.attackType = parts[1];
                    result.attackParams = parts[2];
                    result.ber = Double.parseDouble(parts[3]);
                    result.nc = Double.parseDouble(parts[4]);
                    results.add(result);
                }
            }
            Logger.info("Loaded " + results.size() + " test results from metrics.txt");
        } else {
            Logger.warning("metrics.txt not found. No test results to process.");
            return;
        }

        // Generate comparison by watermarking method
        generateMethodComparisonCSV(results);

        // Generate comparison by attack type
        generateAttackComparisonCSV(results);

        // Generate parameter comparison for each method
        generateParameterComparisonCSV(results);
    }

    /**
     * Generates CSV comparing different watermarking methods against all attacks
     */
    private static void generateMethodComparisonCSV(List<TestResult> results) throws IOException {
        Logger.info("Generating method comparison CSV...");

        // Create CSV file for BER comparison
        try (BufferedWriter berWriter = new BufferedWriter(new FileWriter(GRAPH_DATA_DIR + "/method_comparison_ber.csv"))) {

            // Write header
            berWriter.write("Attack,LSB,DCT,DWT,SVD\n");

            // Map to store average BER values by attack and method
            Map<String, Map<String, List<Double>>> berValues = new HashMap<>();

            // Group results by attack and method
            for (TestResult result : results) {
                String attackKey = result.attackType + " " + result.attackParams;
                String methodKey = extractMethod(result.testName);

                berValues.putIfAbsent(attackKey, new HashMap<>());
                berValues.get(attackKey).putIfAbsent(methodKey, new ArrayList<>());
                berValues.get(attackKey).get(methodKey).add(result.ber);
            }

            // Write data rows
            for (String attackKey : berValues.keySet()) {
                berWriter.write(attackKey);

                for (String method : new String[]{"LSB", "DCT", "DWT", "SVD"}) {
                    berWriter.write(",");

                    if (berValues.get(attackKey).containsKey(method)) {
                        List<Double> values = berValues.get(attackKey).get(method);
                        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        berWriter.write(String.format("%.4f", average));
                    } else {
                        berWriter.write("N/A");
                    }
                }

                berWriter.write("\n");
            }
        }

        // Create CSV file for NC comparison
        try (BufferedWriter ncWriter = new BufferedWriter(new FileWriter(GRAPH_DATA_DIR + "/method_comparison_nc.csv"))) {

            // Write header
            ncWriter.write("Attack,LSB,DCT,DWT,SVD\n");

            // Map to store average NC values by attack and method
            Map<String, Map<String, List<Double>>> ncValues = new HashMap<>();

            // Group results by attack and method
            for (TestResult result : results) {
                String attackKey = result.attackType + " " + result.attackParams;
                String methodKey = extractMethod(result.testName);

                ncValues.putIfAbsent(attackKey, new HashMap<>());
                ncValues.get(attackKey).putIfAbsent(methodKey, new ArrayList<>());
                ncValues.get(attackKey).get(methodKey).add(result.nc);
            }

            // Write data rows
            for (String attackKey : ncValues.keySet()) {
                ncWriter.write(attackKey);

                for (String method : new String[]{"LSB", "DCT", "DWT", "SVD"}) {
                    ncWriter.write(",");

                    if (ncValues.get(attackKey).containsKey(method)) {
                        List<Double> values = ncValues.get(attackKey).get(method);
                        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        ncWriter.write(String.format("%.4f", average));
                    } else {
                        ncWriter.write("N/A");
                    }
                }

                ncWriter.write("\n");
            }
        }
    }

    /**
     * Generates CSV comparing different attacks for each watermarking method
     */
    private static void generateAttackComparisonCSV(List<TestResult> results) throws IOException {
        Logger.info("Generating attack comparison CSV...");

        // Array of methods to generate CSVs for
        String[] methods = {"LSB", "DCT", "DWT", "SVD"};

        for (String method : methods) {
            // Create CSV file for BER comparison
            try (BufferedWriter berWriter = new BufferedWriter(
                    new FileWriter(GRAPH_DATA_DIR + "/" + method + "_attack_comparison_ber.csv"))) {

                // Write header
                berWriter.write("Parameter,None,JPEG_25%,JPEG_50%,JPEG_75%,JPEG_90%,Rotation_45,Rotation_90,Resize_50%,Resize_75%,Mirroring,Cropping_10%,Cropping_20%\n");

                // Map to store BER values by parameter and attack
                Map<String, Map<String, Double>> berValues = new HashMap<>();

                // Group results by parameter and attack
                for (TestResult result : results) {
                    String methodKey = extractMethod(result.testName);

                    if (methodKey.equals(method)) {
                        String paramKey = extractParameter(result.testName);
                        String attackKey = result.attackType +
                                (result.attackParams.isEmpty() ? "" : " " + result.attackParams);

                        berValues.putIfAbsent(paramKey, new HashMap<>());
                        berValues.get(paramKey).put(attackKey, result.ber);
                    }
                }

                // Attack types in the order we want them
                String[] attackTypes = {
                        "NONE None",
                        "JPEG_COMPRESSION Quality: 25.0%",
                        "JPEG_COMPRESSION Quality: 50.0%",
                        "JPEG_COMPRESSION Quality: 75.0%",
                        "JPEG_COMPRESSION Quality: 90.0%",
                        "ROTATION_45 None",
                        "ROTATION_90 None",
                        "RESIZE_50 None",
                        "RESIZE_75 None",
                        "MIRRORING None",
                        "CROPPING Crop: 10.0%",
                        "CROPPING Crop: 20.0%"
                };

                // Write data rows
                for (String paramKey : berValues.keySet()) {
                    berWriter.write(paramKey);

                    for (String attackKey : attackTypes) {
                        berWriter.write(",");

                        if (berValues.get(paramKey).containsKey(attackKey)) {
                            double value = berValues.get(paramKey).get(attackKey);
                            berWriter.write(String.format("%.4f", value));
                        } else {
                            berWriter.write("N/A");
                        }
                    }

                    berWriter.write("\n");
                }
            }

            // Create CSV file for NC comparison
            try (BufferedWriter ncWriter = new BufferedWriter(
                    new FileWriter(GRAPH_DATA_DIR + "/" + method + "_attack_comparison_nc.csv"))) {

                // Write header
                ncWriter.write("Parameter,None,JPEG_25%,JPEG_50%,JPEG_75%,JPEG_90%,Rotation_45,Rotation_90,Resize_50%,Resize_75%,Mirroring,Cropping_10%,Cropping_20%\n");

                // Map to store NC values by parameter and attack
                Map<String, Map<String, Double>> ncValues = new HashMap<>();

                // Group results by parameter and attack
                for (TestResult result : results) {
                    String methodKey = extractMethod(result.testName);

                    if (methodKey.equals(method)) {
                        String paramKey = extractParameter(result.testName);
                        String attackKey = result.attackType +
                                (result.attackParams.isEmpty() ? "" : " " + result.attackParams);

                        ncValues.putIfAbsent(paramKey, new HashMap<>());
                        ncValues.get(paramKey).put(attackKey, result.nc);
                    }
                }

                // Attack types in the order we want them
                String[] attackTypes = {
                        "NONE None",
                        "JPEG_COMPRESSION Quality: 25.0%",
                        "JPEG_COMPRESSION Quality: 50.0%",
                        "JPEG_COMPRESSION Quality: 75.0%",
                        "JPEG_COMPRESSION Quality: 90.0%",
                        "ROTATION_45 None",
                        "ROTATION_90 None",
                        "RESIZE_50 None",
                        "RESIZE_75 None",
                        "MIRRORING None",
                        "CROPPING Crop: 10.0%",
                        "CROPPING Crop: 20.0%"
                };

                // Write data rows
                for (String paramKey : ncValues.keySet()) {
                    ncWriter.write(paramKey);

                    for (String attackKey : attackTypes) {
                        ncWriter.write(",");

                        if (ncValues.get(paramKey).containsKey(attackKey)) {
                            double value = ncValues.get(paramKey).get(attackKey);
                            ncWriter.write(String.format("%.4f", value));
                        } else {
                            ncWriter.write("N/A");
                        }
                    }

                    ncWriter.write("\n");
                }
            }
        }
    }

    /**
     * Generates CSV comparing different parameters for each watermarking method
     */
    private static void generateParameterComparisonCSV(List<TestResult> results) throws IOException {
        Logger.info("Generating parameter comparison CSV...");

        // Create CSV for LSB bit plane comparison
        try (BufferedWriter lsbWriter = new BufferedWriter(
                new FileWriter(GRAPH_DATA_DIR + "/lsb_parameter_comparison.csv"))) {

            // Write header
            lsbWriter.write("Attack,BitPlane1,BitPlane3,BitPlane5,BitPlane7\n");

            // Map to store average BER values by attack and bit plane
            Map<String, Map<String, List<Double>>> berValues = new HashMap<>();

            // Group results by attack and bit plane
            for (TestResult result : results) {
                String methodKey = extractMethod(result.testName);

                if (methodKey.equals("LSB")) {
                    String paramKey = extractParameter(result.testName);
                    String attackKey = result.attackType +
                            (result.attackParams.isEmpty() ? "" : " " + result.attackParams);

                    berValues.putIfAbsent(attackKey, new HashMap<>());
                    berValues.get(attackKey).putIfAbsent(paramKey, new ArrayList<>());
                    berValues.get(attackKey).get(paramKey).add(result.ber);
                }
            }

            // Write data rows
            for (String attackKey : berValues.keySet()) {
                lsbWriter.write(attackKey);

                for (String param : new String[]{"BP1", "BP3", "BP5", "BP7"}) {
                    lsbWriter.write(",");

                    if (berValues.get(attackKey).containsKey(param)) {
                        List<Double> values = berValues.get(attackKey).get(param);
                        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        lsbWriter.write(String.format("%.4f", average));
                    } else {
                        lsbWriter.write("N/A");
                    }
                }

                lsbWriter.write("\n");
            }
        }

        // Create CSV for DCT strength comparison
        try (BufferedWriter dctWriter = new BufferedWriter(
                new FileWriter(GRAPH_DATA_DIR + "/dct_parameter_comparison.csv"))) {

            // Write header
            dctWriter.write("Attack,Strength5,Strength10,Strength15\n");

            // Map to store average BER values by attack and strength
            Map<String, Map<String, List<Double>>> berValues = new HashMap<>();

            // Group results by attack and strength
            for (TestResult result : results) {
                String methodKey = extractMethod(result.testName);

                if (methodKey.equals("DCT")) {
                    String paramKey = extractParameter(result.testName);
                    // Only compare strength parameters
                    if (paramKey.startsWith("S")) {
                        String attackKey = result.attackType +
                                (result.attackParams.isEmpty() ? "" : " " + result.attackParams);

                        berValues.putIfAbsent(attackKey, new HashMap<>());
                        berValues.get(attackKey).putIfAbsent(paramKey, new ArrayList<>());
                        berValues.get(attackKey).get(paramKey).add(result.ber);
                    }
                }
            }

            // Write data rows
            for (String attackKey : berValues.keySet()) {
                dctWriter.write(attackKey);

                for (String param : new String[]{"S5", "S10", "S15"}) {
                    dctWriter.write(",");

                    if (berValues.get(attackKey).containsKey(param)) {
                        List<Double> values = berValues.get(attackKey).get(param);
                        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        dctWriter.write(String.format("%.4f", average));
                    } else {
                        dctWriter.write("N/A");
                    }
                }

                dctWriter.write("\n");
            }
        }

        // Create CSV for DWT subband comparison
        try (BufferedWriter dwtWriter = new BufferedWriter(
                new FileWriter(GRAPH_DATA_DIR + "/dwt_parameter_comparison.csv"))) {

            // Write header
            dwtWriter.write("Attack,LL,LH,HL,HH\n");

            // Map to store average BER values by attack and subband
            Map<String, Map<String, List<Double>>> berValues = new HashMap<>();

            // Group results by attack and subband
            for (TestResult result : results) {
                String methodKey = extractMethod(result.testName);

                if (methodKey.equals("DWT")) {
                    String paramKey = extractParameter(result.testName);
                    // Only compare subband parameters
                    if (paramKey.equals("LL") || paramKey.equals("LH") ||
                            paramKey.equals("HL") || paramKey.equals("HH")) {
                        String attackKey = result.attackType +
                                (result.attackParams.isEmpty() ? "" : " " + result.attackParams);

                        berValues.putIfAbsent(attackKey, new HashMap<>());
                        berValues.get(attackKey).putIfAbsent(paramKey, new ArrayList<>());
                        berValues.get(attackKey).get(paramKey).add(result.ber);
                    }
                }
            }

            // Write data rows
            for (String attackKey : berValues.keySet()) {
                dwtWriter.write(attackKey);

                for (String param : new String[]{"LL", "LH", "HL", "HH"}) {
                    dwtWriter.write(",");

                    if (berValues.get(attackKey).containsKey(param)) {
                        List<Double> values = berValues.get(attackKey).get(param);
                        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        dwtWriter.write(String.format("%.4f", average));
                    } else {
                        dwtWriter.write("N/A");
                    }
                }

                dwtWriter.write("\n");
            }
        }

        // Create CSV for SVD alpha comparison
        try (BufferedWriter svdWriter = new BufferedWriter(
                new FileWriter(GRAPH_DATA_DIR + "/svd_parameter_comparison.csv"))) {

            // Write header
            svdWriter.write("Attack,Alpha0.5,Alpha1.0,Alpha2.0,Alpha5.0\n");

            // Map to store average BER values by attack and alpha
            Map<String, Map<String, List<Double>>> berValues = new HashMap<>();

            // Group results by attack and alpha
            for (TestResult result : results) {
                String methodKey = extractMethod(result.testName);

                if (methodKey.equals("SVD")) {
                    String paramKey = extractParameter(result.testName);
                    String attackKey = result.attackType +
                            (result.attackParams.isEmpty() ? "" : " " + result.attackParams);

                    berValues.putIfAbsent(attackKey, new HashMap<>());
                    berValues.get(attackKey).putIfAbsent(paramKey, new ArrayList<>());
                    berValues.get(attackKey).get(paramKey).add(result.ber);
                }
            }

            // Write data rows
            for (String attackKey : berValues.keySet()) {
                svdWriter.write(attackKey);

                for (String param : new String[]{"A0.5", "A1.0", "A2.0", "A5.0"}) {
                    svdWriter.write(",");

                    if (berValues.get(attackKey).containsKey(param)) {
                        List<Double> values = berValues.get(attackKey).get(param);
                        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        svdWriter.write(String.format("%.4f", average));
                    } else {
                        svdWriter.write("N/A");
                    }
                }

                svdWriter.write("\n");
            }
        }
    }

    /**
     * Extracts the watermarking method from a test name
     */
    private static String extractMethod(String testName) {
        if (testName.startsWith("LSB_")) {
            return "LSB";
        } else if (testName.startsWith("DCT_")) {
            return "DCT";
        } else if (testName.startsWith("DWT_")) {
            return "DWT";
        } else if (testName.startsWith("SVD_")) {
            return "SVD";
        }
        return "Unknown";
    }

    /**
     * Extracts the parameter from a test name
     */
    private static String extractParameter(String testName) {
        String method = extractMethod(testName);

        if (method.equals("LSB")) {
            // Format: LSB_BP1_WithPerm or LSB_BP3_NoPerm
            if (testName.contains("_BP")) {
                int bitPlane = Integer.parseInt(testName.split("_BP")[1].substring(0, 1));
                return "BP" + bitPlane;
            }
        } else if (method.equals("DCT")) {
            // Format: DCT_C31_41_S15 (coefficient pairs 3,1 + 4,1 with strength 15)
            if (testName.contains("_S")) {
                String strengthPart = testName.split("_S")[1];
                return "S" + strengthPart;
            }
        } else if (method.equals("DWT")) {
            // Format: DWT_LL_S2.5 or DWT_HH_S5.0
            if (testName.contains("DWT_")) {
                String[] parts = testName.split("_");
                if (parts.length >= 2) {
                    return parts[1]; // LL, LH, HL, or HH
                }
            }
        } else if (method.equals("SVD")) {
            // Format: SVD_A0.5 or SVD_A1.0
            if (testName.contains("SVD_A")) {
                return testName.substring(4); // A0.5, A1.0, etc.
            }
        }

        return "Unknown";
    }

    /**
     * Simple class to hold test results
     */
    private static class TestResult {
        String testName;
        String attackType;
        String attackParams;
        double ber;
        double nc;
    }
}
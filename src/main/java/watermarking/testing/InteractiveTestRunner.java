package watermarking.testing;

import enums.AttackType;
import enums.QualityType;
import enums.WatermarkType;
import utils.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Scanner;

/**
 * Interactive test runner for watermarking evaluation.
 * Provides a command-line interface to run specific watermarking tests.
 */
public class InteractiveTestRunner {

    private static WatermarkTestingAutomation tester;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Logger.info("Starting interactive watermarking test runner...");

        try {
            // Create test automation
            tester = new WatermarkTestingAutomation();

            // Main menu loop
            boolean running = true;
            while (running) {
                displayMainMenu();
                int choice = getIntInput("Enter your choice: ");

                switch (choice) {
                    case 1: // Run comprehensive tests
                        tester.runComprehensiveTests();
                        break;
                    case 2: // Run LSB tests
                        runLSBTests();
                        break;
                    case 3: // Run DCT tests
                        runDCTTests();
                        break;
                    case 4: // Run DWT tests
                        runDWTTests();
                        break;
                    case 5: // Run SVD tests
                        runSVDTests();
                        break;
                    case 6: // Run custom test
                        runCustomTest();
                        break;
                    case 0: // Exit
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }

            System.out.println("Test runner exited. Check the test-results directory for outputs.");

        } catch (Exception e) {
            Logger.error("Error in test runner: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void displayMainMenu() {
        System.out.println("\n=== Watermarking Test Runner ===");
        System.out.println("1. Run Comprehensive Tests (All Methods & Attacks)");
        System.out.println("2. Run LSB Tests");
        System.out.println("3. Run DCT Tests");
        System.out.println("4. Run DWT Tests");
        System.out.println("5. Run SVD Tests");
        System.out.println("6. Run Custom Test");
        System.out.println("0. Exit");
    }

    private static void runLSBTests() {
        System.out.println("\n=== LSB Watermarking Tests ===");

        try {
            // Get watermark
            BufferedImage watermark = getWatermarkImage();

            // Select component
            QualityType component = selectComponent();

            // Select bit plane
            int bitPlane = getIntInput("Enter bit plane (0-7): ");
            if (bitPlane < 0 || bitPlane > 7) {
                System.out.println("Invalid bit plane. Using default value 3.");
                bitPlane = 3;
            }

            // Use permutation?
            boolean usePermutation = getBooleanInput("Use permutation (y/n): ");

            // Set up parameters
            String testName = String.format("LSB_%s_BP%d_%s",
                    component, bitPlane, usePermutation ? "Permuted" : "NotPermuted");
            Object[] embedParams = {bitPlane, usePermutation, "watermark-key"};

            // Run tests with selected attacks
            runTestWithSelectedAttacks(WatermarkType.LSB, component, watermark, testName, embedParams);

        } catch (Exception e) {
            Logger.error("Error running LSB tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDCTTests() {
        System.out.println("\n=== DCT Watermarking Tests ===");

        try {
            // Get watermark
            BufferedImage watermark = getWatermarkImage();

            // Select component
            QualityType component = selectComponent();

            // Select coefficient pair
            System.out.println("Select coefficient pair:");
            System.out.println("1. (3,1) + (4,1)");
            System.out.println("2. (4,3) + (5,2)");
            int pairChoice = getIntInput("Enter choice: ");

            int[][] coeffPair;
            if (pairChoice == 2) {
                coeffPair = new int[][]{{4, 3}, {5, 2}};
            } else {
                coeffPair = new int[][]{{3, 1}, {4, 1}};
            }

            // Select strength
            double strength = getDoubleInput("Enter strength value (5-15): ");
            if (strength < 1 || strength > 50) {
                System.out.println("Invalid strength. Using default value 10.");
                strength = 10.0;
            }

            // Set block size
            int blockSize = 8;

            // Set up parameters
            String testName = String.format("DCT_%s_C%d%d_%d%d_S%.1f",
                    component,
                    coeffPair[0][0], coeffPair[0][1],
                    coeffPair[1][0], coeffPair[1][1],
                    strength);
            Object[] embedParams = {blockSize, coeffPair[0], coeffPair[1], strength};

            // Run tests with selected attacks
            runTestWithSelectedAttacks(WatermarkType.DCT, component, watermark, testName, embedParams);

        } catch (Exception e) {
            Logger.error("Error running DCT tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDWTTests() {
        System.out.println("\n=== DWT Watermarking Tests ===");

        try {
            // Get watermark
            BufferedImage watermark = getWatermarkImage();

            // Select component
            QualityType component = selectComponent();

            // Select subband
            System.out.println("Select subband:");
            System.out.println("1. LL (Low-Low)");
            System.out.println("2. LH (Low-High)");
            System.out.println("3. HL (High-Low)");
            System.out.println("4. HH (High-High)");
            int subbandChoice = getIntInput("Enter choice: ");

            String subband;
            switch (subbandChoice) {
                case 2: subband = "LH"; break;
                case 3: subband = "HL"; break;
                case 4: subband = "HH"; break;
                default: subband = "LL"; break;
            }

            // Select strength
            double strength = getDoubleInput("Enter strength value (1-10): ");
            if (strength < 0.5 || strength > 10) {
                System.out.println("Invalid strength. Using default value 2.5.");
                strength = 2.5;
            }

            // Set up parameters
            String testName = String.format("DWT_%s_%s_S%.1f", component, subband, strength);
            Object[] embedParams = {strength, subband};

            // Run tests with selected attacks
            runTestWithSelectedAttacks(WatermarkType.DWT, component, watermark, testName, embedParams);

        } catch (Exception e) {
            Logger.error("Error running DWT tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runSVDTests() {
        System.out.println("\n=== SVD Watermarking Tests ===");

        try {
            // Get watermark
            BufferedImage watermark = getWatermarkImage();

            // Select component
            QualityType component = selectComponent();

            // Select alpha value
            double alpha = getDoubleInput("Enter alpha value (0.5-5.0): ");
            if (alpha < 0.1 || alpha > 10) {
                System.out.println("Invalid alpha. Using default value 1.0.");
                alpha = 1.0;
            }

            // Set up parameters
            String testName = String.format("SVD_%s_A%.1f", component, alpha);
            Object[] embedParams = {alpha};

            // Run tests with selected attacks
            runTestWithSelectedAttacks(WatermarkType.SVD, component, watermark, testName, embedParams);

        } catch (Exception e) {
            Logger.error("Error running SVD tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runCustomTest() {
        System.out.println("\n=== Custom Watermarking Test ===");

        try {
            // Select watermarking method
            System.out.println("Select watermarking method:");
            System.out.println("1. LSB (Spatial Domain)");
            System.out.println("2. DCT (Frequency Domain)");
            System.out.println("3. DWT (Wavelet Domain)");
            System.out.println("4. SVD (Singular Value Decomposition)");
            int methodChoice = getIntInput("Enter choice: ");

            WatermarkType method;
            switch (methodChoice) {
                case 2: method = WatermarkType.DCT; break;
                case 3: method = WatermarkType.DWT; break;
                case 4: method = WatermarkType.SVD; break;
                default: method = WatermarkType.LSB; break;
            }

            // Based on the selected method, run the appropriate test
            switch (method) {
                case LSB:
                    runLSBTests();
                    break;
                case DCT:
                    runDCTTests();
                    break;
                case DWT:
                    runDWTTests();
                    break;
                case SVD:
                    runSVDTests();
                    break;
            }

        } catch (Exception e) {
            Logger.error("Error running custom test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runTestWithSelectedAttacks(
            WatermarkType method,
            QualityType component,
            BufferedImage watermark,
            String testName,
            Object[] embedParams) {

        try {
            System.out.println("\n=== Select Attacks to Run ===");
            System.out.println("0. No Attack (Baseline)");
            System.out.println("1. JPEG Compression (25%, 50%, 75%, 90%)");
            System.out.println("2. PNG Compression (levels 1, 5, 9)");
            System.out.println("3. Rotation (45°, 90°)");
            System.out.println("4. Resize (50%, 75%)");
            System.out.println("5. Mirroring");
            System.out.println("6. Cropping (10%, 20%)");
            System.out.println("7. All Attacks");

            int attackChoice = getIntInput("Enter choice: ");

            // Access the testWatermarkWithAttacks method using reflection
            java.lang.reflect.Method testMethod = WatermarkTestingAutomation.class.getDeclaredMethod(
                    "testWatermarkWithAttacks",
                    WatermarkType.class, QualityType.class, BufferedImage.class,
                    String.class, Object[].class);
            testMethod.setAccessible(true);

            // Invoke the test method with the selected parameters
            testMethod.invoke(tester, method, component, watermark, testName, embedParams);

            System.out.println("Test completed. Results saved in the test-results directory.");

        } catch (Exception e) {
            Logger.error("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper methods

    private static BufferedImage getWatermarkImage() throws Exception {
        System.out.println("Select watermark:");
        System.out.println("1. Checkerboard pattern");
        System.out.println("2. Logo");
        System.out.println("3. Text");
        int watermarkChoice = getIntInput("Enter choice: ");

        String watermarkFile;
        switch (watermarkChoice) {
            case 2: watermarkFile = "logo.png"; break;
            case 3: watermarkFile = "text.png"; break;
            default: watermarkFile = "checkerboard.png"; break;
        }

        return ImageIO.read(new File("test-watermarks/" + watermarkFile));
    }

    private static QualityType selectComponent() {
        System.out.println("Select component:");
        System.out.println("1. Y (Luminance)");
        System.out.println("2. Cb (Chrominance Blue)");
        System.out.println("3. Cr (Chrominance Red)");
        int componentChoice = getIntInput("Enter choice: ");

        switch (componentChoice) {
            case 2: return QualityType.CB;
            case 3: return QualityType.CR;
            default: return QualityType.Y;
        }
    }

    private static int getIntInput(String prompt) {
        int value;
        while (true) {
            try {
                System.out.print(prompt);
                value = Integer.parseInt(scanner.nextLine().trim());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        return value;
    }

    private static double getDoubleInput(String prompt) {
        double value;
        while (true) {
            try {
                System.out.print(prompt);
                value = Double.parseDouble(scanner.nextLine().trim());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        return value;
    }

    private static boolean getBooleanInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.startsWith("y")) {
                return true;
            } else if (input.startsWith("n")) {
                return false;
            } else {
                System.out.println("Invalid input. Please enter 'y' or 'n'.");
            }
        }
    }
}
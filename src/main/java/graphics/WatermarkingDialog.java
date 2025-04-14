package graphics;

import Jama.Matrix;
import enums.QualityType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import jpeg.Process;
import utils.Logger;
import watermarking.WatermarkEvaluation;
import watermarking.attacks.WatermarkAttacks;
import watermarking.frequency.DCTWatermarking;
import watermarking.spatial.LSBWatermarking;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Controller for the watermarking dialog window.
 */
public class WatermarkingDialog extends Stage {

    private enum WatermarkMethod {
        LSB("LSB (Spatial Domain)"),
        DCT("DCT (Frequency Domain)");

        private final String displayName;

        WatermarkMethod(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private Process originalProcess;
    private BufferedImage watermarkImage;
    private BufferedImage embeddedWatermark;
    private BufferedImage extractedWatermark;
    private Process watermarkedProcess;

    private ComboBox<WatermarkMethod> methodComboBox;
    private ComboBox<QualityType> componentComboBox;
    private Spinner<Integer> bitPlaneSpinner;
    private Spinner<Integer> blockSizeSpinner;
    private Spinner<Integer> coef1XSpinner;
    private Spinner<Integer> coef1YSpinner;
    private Spinner<Integer> coef2XSpinner;
    private Spinner<Integer> coef2YSpinner;
    private Spinner<Double> strengthSpinner;
    private CheckBox permuteCheckBox;
    private TextField keyTextField;
    private TextField watermarkWidthField;
    private TextField watermarkHeightField;

    private ImageView originalImageView;
    private ImageView watermarkImageView;
    private ImageView watermarkedImageView;
    private ImageView extractedWatermarkView;

    private Label berLabel;
    private Label ncLabel;

    private VBox lsbOptions;
    private VBox dctOptions;

    /**
     * Creates a new watermarking dialog.
     * @param parentStage The parent stage
     * @param process The image process to watermark
     */
    public WatermarkingDialog(Stage parentStage, Process process) {
        this.originalProcess = process;

        // Configure stage
        initModality(Modality.APPLICATION_MODAL);
        initOwner(parentStage);
        setTitle("Image Watermarking");
        setMinWidth(900);
        setMinHeight(700);

        // Create the UI
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Create the layout
        createControls(root);

        // Set the scene
        setScene(new Scene(root));

        // Create default watermark if none exists yet
        if (watermarkImage == null) {
            createDefaultWatermark();
        }

        // Update UI
        updateImageViews();
    }

    private void createControls(BorderPane root) {
        // Create left control panel
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setPrefWidth(300);

        // Method selection
        Label methodLabel = new Label("Watermarking Method:");
        methodComboBox = new ComboBox<>();
        methodComboBox.getItems().addAll(WatermarkMethod.values());
        methodComboBox.setValue(WatermarkMethod.LSB);
        methodComboBox.setMaxWidth(Double.MAX_VALUE);

        // Component selection
        Label componentLabel = new Label("Image Component:");
        componentComboBox = new ComboBox<>();
        componentComboBox.getItems().addAll(QualityType.Y, QualityType.CB, QualityType.CR);
        componentComboBox.setValue(QualityType.Y);
        componentComboBox.setMaxWidth(Double.MAX_VALUE);

        // Create LSB options panel
        lsbOptions = createLSBOptions();

        // Create DCT options panel
        dctOptions = createDCTOptions();

        // Show/hide the appropriate options panel based on selected method
        methodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            lsbOptions.setVisible(newVal == WatermarkMethod.LSB);
            lsbOptions.setManaged(newVal == WatermarkMethod.LSB);
            dctOptions.setVisible(newVal == WatermarkMethod.DCT);
            dctOptions.setManaged(newVal == WatermarkMethod.DCT);
        });

        // Initial visibility
        lsbOptions.setVisible(true);
        lsbOptions.setManaged(true);
        dctOptions.setVisible(false);
        dctOptions.setManaged(false);

        // Watermark buttons
        Button loadWatermarkButton = new Button("Load Watermark");
        loadWatermarkButton.setMaxWidth(Double.MAX_VALUE);
        loadWatermarkButton.setOnAction(e -> loadWatermark());

        Button createWatermarkButton = new Button("Create Watermark");
        createWatermarkButton.setMaxWidth(Double.MAX_VALUE);
        createWatermarkButton.setOnAction(e -> createWatermark());

        // Action buttons
        Button embedButton = new Button("Embed Watermark");
        embedButton.setMaxWidth(Double.MAX_VALUE);
        embedButton.setOnAction(e -> embedWatermark());

        Button extractButton = new Button("Extract Watermark");
        extractButton.setMaxWidth(Double.MAX_VALUE);
        extractButton.setOnAction(e -> extractWatermark());

        Button evaluateButton = new Button("Evaluate Quality");
        evaluateButton.setMaxWidth(Double.MAX_VALUE);
        evaluateButton.setOnAction(e -> evaluateWatermark());

        // Attack simulation
        TitledPane attackPane = createAttackControls();

        // Add everything to the control panel
        controlPanel.getChildren().addAll(
                methodLabel, methodComboBox,
                componentLabel, componentComboBox,
                new Separator(),
                lsbOptions,
                dctOptions,
                new Separator(),
                loadWatermarkButton,
                createWatermarkButton,
                new Separator(),
                embedButton,
                extractButton,
                evaluateButton,
                new Separator(),
                attackPane
        );

        // Create results panel on the right
        BorderPane resultsPanel = createResultsPanel();

        // Add panels to root
        root.setLeft(controlPanel);
        root.setCenter(resultsPanel);
    }

    private VBox createLSBOptions() {
        VBox options = new VBox(10);
        options.setPadding(new Insets(5, 0, 5, 0));

        // Bit plane selection
        Label bitPlaneLabel = new Label("Bit Plane (0-7):");
        bitPlaneSpinner = new Spinner<>(0, 7, 3);
        bitPlaneSpinner.setEditable(true);
        bitPlaneSpinner.setPrefWidth(80);

        HBox bitPlaneBox = new HBox(10, bitPlaneLabel, bitPlaneSpinner);

        // Permutation options
        permuteCheckBox = new CheckBox("Permute Watermark");
        permuteCheckBox.setSelected(true);

        Label keyLabel = new Label("Permutation Key:");
        keyTextField = new TextField("watermark-key");
        keyTextField.setPromptText("Enter key for permutation");

        VBox keyBox = new VBox(5, keyLabel, keyTextField);

        // Add to options
        options.getChildren().addAll(
                bitPlaneBox,
                permuteCheckBox,
                keyBox
        );

        return options;
    }

    private VBox createDCTOptions() {
        VBox options = new VBox(10);
        options.setPadding(new Insets(5, 0, 5, 0));

        // Block size
        Label blockSizeLabel = new Label("Block Size:");
        blockSizeSpinner = new Spinner<>(4, 16, 8, 4);
        blockSizeSpinner.setEditable(true);
        blockSizeSpinner.setPrefWidth(80);
        HBox blockSizeBox = new HBox(10, blockSizeLabel, blockSizeSpinner);

        // Coefficient pairs
        Label coefPairsLabel = new Label("Coefficient Pairs:");

        Label coef1Label = new Label("Coef 1 (x,y):");
        coef1XSpinner = new Spinner<>(0, 7, 3);
        coef1YSpinner = new Spinner<>(0, 7, 1);
        HBox coef1Box = new HBox(5, coef1Label, coef1XSpinner, coef1YSpinner);

        Label coef2Label = new Label("Coef 2 (x,y):");
        coef2XSpinner = new Spinner<>(0, 7, 4);
        coef2YSpinner = new Spinner<>(0, 7, 1);
        HBox coef2Box = new HBox(5, coef2Label, coef2XSpinner, coef2YSpinner);

        // Strength
        Label strengthLabel = new Label("Embedding Strength:");
        strengthSpinner = new Spinner<>(1.0, 50.0, 10.0, 1.0);
        strengthSpinner.setEditable(true);
        HBox strengthBox = new HBox(10, strengthLabel, strengthSpinner);

        // Add to options
        options.getChildren().addAll(
                blockSizeBox,
                coefPairsLabel,
                coef1Box,
                coef2Box,
                strengthBox
        );

        return options;
    }

    private TitledPane createAttackControls() {
        VBox attackBox = new VBox(10);
        attackBox.setPadding(new Insets(10));

        // JPEG Compression attack
        Label jpegQualityLabel = new Label("JPEG Quality (1-100):");
        Spinner<Integer> jpegQualitySpinner = new Spinner<>(1, 100, 75);
        jpegQualitySpinner.setEditable(true);
        jpegQualitySpinner.setPrefWidth(80);
        Button jpegAttackButton = new Button("Apply JPEG Attack");
        jpegAttackButton.setOnAction(e -> applyAttack("jpeg", jpegQualitySpinner.getValue()));
        HBox jpegBox = new HBox(10, jpegQualityLabel, jpegQualitySpinner, jpegAttackButton);

        // Resize attack
        Label resizeLabel = new Label("Resize Scale (0.1-1.0):");
        Spinner<Double> resizeSpinner = new Spinner<>(0.1, 1.0, 0.5, 0.1);
        resizeSpinner.setEditable(true);
        resizeSpinner.setPrefWidth(80);
        Button resizeAttackButton = new Button("Apply Resize Attack");
        resizeAttackButton.setOnAction(e -> applyAttack("resize", resizeSpinner.getValue()));
        HBox resizeBox = new HBox(10, resizeLabel, resizeSpinner, resizeAttackButton);

        // Rotation attack
        Label rotationLabel = new Label("Rotation Angle (degrees):");
        Spinner<Integer> rotationSpinner = new Spinner<>(0, 360, 45);
        rotationSpinner.setEditable(true);
        rotationSpinner.setPrefWidth(80);
        Button rotationAttackButton = new Button("Apply Rotation Attack");
        rotationAttackButton.setOnAction(e -> applyAttack("rotation", rotationSpinner.getValue()));
        HBox rotationBox = new HBox(10, rotationLabel, rotationSpinner, rotationAttackButton);

        // Crop attack
        Label cropLabel = new Label("Crop Percentage (0.0-0.5):");
        Spinner<Double> cropSpinner = new Spinner<>(0.0, 0.5, 0.1, 0.05);
        cropSpinner.setEditable(true);
        cropSpinner.setPrefWidth(80);
        Button cropAttackButton = new Button("Apply Crop Attack");
        cropAttackButton.setOnAction(e -> applyAttack("crop", cropSpinner.getValue()));
        HBox cropBox = new HBox(10, cropLabel, cropSpinner, cropAttackButton);

        attackBox.getChildren().addAll(jpegBox, resizeBox, rotationBox, cropBox);

        TitledPane attackPane = new TitledPane("Attack Simulation", attackBox);
        attackPane.setExpanded(false);

        return attackPane;
    }

    private BorderPane createResultsPanel() {
        BorderPane panel = new BorderPane();

        // Create grid for images
        GridPane imageGrid = new GridPane();
        imageGrid.setHgap(10);
        imageGrid.setVgap(10);
        imageGrid.setPadding(new Insets(10));

        // Original image view
        Label originalLabel = new Label("Original Image:");
        originalImageView = new ImageView();
        originalImageView.setFitWidth(200);
        originalImageView.setFitHeight(200);
        originalImageView.setPreserveRatio(true);

        // Watermark image view
        Label watermarkLabel = new Label("Watermark:");
        watermarkImageView = new ImageView();
        watermarkImageView.setFitWidth(200);
        watermarkImageView.setFitHeight(200);
        watermarkImageView.setPreserveRatio(true);

        // Watermarked image view
        Label watermarkedLabel = new Label("Watermarked Image:");
        watermarkedImageView = new ImageView();
        watermarkedImageView.setFitWidth(200);
        watermarkedImageView.setFitHeight(200);
        watermarkedImageView.setPreserveRatio(true);

        // Extracted watermark view
        Label extractedLabel = new Label("Extracted Watermark:");
        extractedWatermarkView = new ImageView();
        extractedWatermarkView.setFitWidth(200);
        extractedWatermarkView.setFitHeight(200);
        extractedWatermarkView.setPreserveRatio(true);

        // Add to grid
        imageGrid.add(originalLabel, 0, 0);
        imageGrid.add(originalImageView, 0, 1);
        imageGrid.add(watermarkLabel, 1, 0);
        imageGrid.add(watermarkImageView, 1, 1);
        imageGrid.add(watermarkedLabel, 0, 2);
        imageGrid.add(watermarkedImageView, 0, 3);
        imageGrid.add(extractedLabel, 1, 2);
        imageGrid.add(extractedWatermarkView, 1, 3);

        // Results panel
        VBox resultsBox = new VBox(10);
        resultsBox.setPadding(new Insets(10));

        // Watermark dimension input (for extraction)
        Label dimensionsLabel = new Label("Watermark Dimensions for Extraction:");
        HBox dimensionsBox = new HBox(10);

        Label widthLabel = new Label("Width:");
        watermarkWidthField = new TextField("64");

        Label heightLabel = new Label("Height:");
        watermarkHeightField = new TextField("64");

        dimensionsBox.getChildren().addAll(widthLabel, watermarkWidthField, heightLabel, watermarkHeightField);

        // Evaluation results
        Label evaluationLabel = new Label("Watermark Evaluation:");

        Label berTitleLabel = new Label("Bit Error Rate (BER):");
        berLabel = new Label("N/A");
        HBox berBox = new HBox(10, berTitleLabel, berLabel);

        Label ncTitleLabel = new Label("Normalized Correlation (NC):");
        ncLabel = new Label("N/A");
        HBox ncBox = new HBox(10, ncTitleLabel, ncLabel);

        // Add to results box
        resultsBox.getChildren().addAll(
                dimensionsLabel,
                dimensionsBox,
                new Separator(),
                evaluationLabel,
                berBox,
                ncBox
        );

        // Add to panel
        panel.setCenter(imageGrid);
        panel.setBottom(resultsBox);

        return panel;
    }

    private void loadWatermark() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Watermark Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.bmp")
        );

        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            try {
                BufferedImage loadedImage = ImageIO.read(file);

                // Force to black and white
                watermarkImage = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = watermarkImage.createGraphics();
                g2d.drawImage(loadedImage, 0, 0, null);
                g2d.dispose();

                // Update watermark dimensions fields
                watermarkWidthField.setText(String.valueOf(watermarkImage.getWidth()));
                watermarkHeightField.setText(String.valueOf(watermarkImage.getHeight()));

                // Update image view
                updateImageViews();

                Logger.info("Watermark image loaded: " + file.getAbsolutePath());
            } catch (IOException e) {
                Logger.error("Error loading watermark: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading watermark image: " + e.getMessage());
                alert.show();
            }
        }
    }

    private void createWatermark() {
        TextInputDialog dialog = new TextInputDialog("64");
        dialog.setTitle("Create Watermark");
        dialog.setHeaderText("Enter watermark size (width in pixels):");
        dialog.setContentText("Size:");

        dialog.showAndWait().ifPresent(result -> {
            try {
                int size = Integer.parseInt(result);
                if (size <= 0) {
                    throw new NumberFormatException("Size must be positive");
                }

                createCustomWatermark(size, size);

                // Update watermark dimensions fields
                watermarkWidthField.setText(String.valueOf(size));
                watermarkHeightField.setText(String.valueOf(size));

                Logger.info("Custom watermark created with size " + size + "x" + size);
            } catch (NumberFormatException e) {
                Logger.error("Invalid watermark size: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid size. Please enter a positive integer.");
                alert.show();
            }
        });
    }

    private void createDefaultWatermark() {
        createCustomWatermark(64, 64);
        watermarkWidthField.setText("64");
        watermarkHeightField.setText("64");
        Logger.info("Default watermark created (64x64)");
    }

    private void createCustomWatermark(int width, int height) {
        watermarkImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = watermarkImage.createGraphics();

        // Fill with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw black pattern (example: checkerboard)
        g2d.setColor(Color.BLACK);
        int squareSize = Math.max(8, width / 8);

        for (int y = 0; y < height; y += squareSize) {
            for (int x = 0; x < width; x += squareSize) {
                if ((x / squareSize + y / squareSize) % 2 == 0) {
                    g2d.fillRect(x, y, squareSize, squareSize);
                }
            }
        }

        // Draw a simple shape in the center
        int centerSize = Math.min(width, height) / 2;
        g2d.setColor(Color.BLACK);
        g2d.fillOval(width/2 - centerSize/2, height/2 - centerSize/2, centerSize, centerSize);

        g2d.dispose();

        // Update the image view
        updateImageViews();
    }

    private void embedWatermark() {
        if (watermarkImage == null) {
            Logger.error("No watermark image available");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please load or create a watermark image first.");
            alert.show();
            return;
        }

        // Check if YCbCr conversion is performed
        if (!originalProcess.isYCbCrConverted()) {
            Logger.error("YCbCr conversion required");
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Please convert the image to YCbCr before embedding a watermark.");
            alert.show();
            return;
        }

        try {
            // Get selected component
            QualityType component = componentComboBox.getValue();
            Matrix componentMatrix = null;

            switch (component) {
                case Y:
                    componentMatrix = originalProcess.getY();
                    break;
                case CB:
                    componentMatrix = originalProcess.getCb();
                    break;
                case CR:
                    componentMatrix = originalProcess.getCr();
                    break;
            }

            if (componentMatrix == null) {
                throw new IllegalStateException("Selected component is not available");
            }

            // Store original watermark for evaluation
            embeddedWatermark = watermarkImage;

            // Create a copy of the process
            watermarkedProcess = new Process(originalProcess.getImage());
            watermarkedProcess.convertToYCbCr();

            // Embed watermark
            WatermarkMethod method = methodComboBox.getValue();
            Matrix watermarkedMatrix = null;

            if (method == WatermarkMethod.LSB) {
                int bitPlane = bitPlaneSpinner.getValue();
                boolean permute = permuteCheckBox.isSelected();
                String key = keyTextField.getText();

                watermarkedMatrix = LSBWatermarking.embed(componentMatrix, watermarkImage, bitPlane, permute, key);
            } else if (method == WatermarkMethod.DCT) {
                int blockSize = blockSizeSpinner.getValue();
                int[] coef1 = new int[]{coef1XSpinner.getValue(), coef1YSpinner.getValue()};
                int[] coef2 = new int[]{coef2XSpinner.getValue(), coef2YSpinner.getValue()};
                double strength = strengthSpinner.getValue();

                watermarkedMatrix = DCTWatermarking.embed(componentMatrix, watermarkImage, blockSize, coef1, coef2, strength);
            }

            if (watermarkedMatrix == null) {
                throw new IllegalStateException("Failed to embed watermark");
            }

            // Update the component in the watermarked process
            switch (component) {
                case Y:
                    watermarkedProcess.setY(watermarkedMatrix);
                    break;
                case CB:
                    watermarkedProcess.setCb(watermarkedMatrix);
                    break;
                case CR:
                    watermarkedProcess.setCr(watermarkedMatrix);
                    break;
            }

            // Convert back to RGB for display
            watermarkedProcess.convertToRGB();

            // Update the image view
            updateImageViews();

            Logger.info("Watermark embedded successfully using " + method + " method");
        } catch (Exception e) {
            Logger.error("Error embedding watermark: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error embedding watermark: " + e.getMessage());
            alert.show();
        }
    }

    private void extractWatermark() {
        if (watermarkedProcess == null) {
            Logger.error("No watermarked image available");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please embed a watermark first.");
            alert.show();
            return;
        }

        try {
            // Get watermark dimensions
            int width = Integer.parseInt(watermarkWidthField.getText());
            int height = Integer.parseInt(watermarkHeightField.getText());

            // Check if YCbCr conversion is performed
            if (!watermarkedProcess.isYCbCrConverted()) {
                watermarkedProcess.convertToYCbCr();
            }

            // Get selected component
            QualityType component = componentComboBox.getValue();
            Matrix componentMatrix = null;

            switch (component) {
                case Y:
                    componentMatrix = watermarkedProcess.getY();
                    break;
                case CB:
                    componentMatrix = watermarkedProcess.getCb();
                    break;
                case CR:
                    componentMatrix = watermarkedProcess.getCr();
                    break;
            }

            if (componentMatrix == null) {
                throw new IllegalStateException("Selected component is not available");
            }

            // Extract watermark
            WatermarkMethod method = methodComboBox.getValue();

            if (method == WatermarkMethod.LSB) {
                int bitPlane = bitPlaneSpinner.getValue();
                boolean permute = permuteCheckBox.isSelected();
                String key = keyTextField.getText();

                extractedWatermark = LSBWatermarking.extract(componentMatrix, bitPlane, permute, key, width, height);
            } else if (method == WatermarkMethod.DCT) {
                int blockSize = blockSizeSpinner.getValue();
                int[] coef1 = new int[]{coef1XSpinner.getValue(), coef1YSpinner.getValue()};
                int[] coef2 = new int[]{coef2XSpinner.getValue(), coef2YSpinner.getValue()};

                extractedWatermark = DCTWatermarking.extract(componentMatrix, blockSize, coef1, coef2, width, height);
            }

            if (extractedWatermark == null) {
                throw new IllegalStateException("Failed to extract watermark");
            }

            // Update the image view
            updateImageViews();

            // Evaluate the watermark if possible
            if (embeddedWatermark != null) {
                evaluateWatermark();
            }

            Logger.info("Watermark extracted successfully using " + method + " method");
        } catch (Exception e) {
            Logger.error("Error extracting watermark: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error extracting watermark: " + e.getMessage());
            alert.show();
        }
    }

    private void evaluateWatermark() {
        if (embeddedWatermark == null || extractedWatermark == null) {
            Logger.error("Cannot evaluate: original or extracted watermark missing");
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Both embedded and extracted watermarks must be available for evaluation.");
            alert.show();
            return;
        }

        try {
            // Calculate BER
            double ber = WatermarkEvaluation.calculateBER(embeddedWatermark, extractedWatermark);
            berLabel.setText(String.format("%.4f", ber));

            // Calculate NC
            double nc = WatermarkEvaluation.calculateNC(embeddedWatermark, extractedWatermark);
            ncLabel.setText(String.format("%.4f", nc));

            Logger.info("Watermark evaluation completed: BER=" + ber + ", NC=" + nc);
        } catch (Exception e) {
            Logger.error("Error evaluating watermark: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error evaluating watermark: " + e.getMessage());
            alert.show();
        }
    }

    private void applyAttack(String attackType, Object parameter) {
        if (watermarkedProcess == null) {
            Logger.error("No watermarked image available for attack");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please embed a watermark first.");
            alert.show();
            return;
        }

        try {
            // Get the current RGB image
            BufferedImage currentImage = watermarkedProcess.getRGBImage();
            BufferedImage attackedImage = null;

            switch (attackType) {
                case "jpeg":
                    int quality = (Integer) parameter;
                    attackedImage = WatermarkAttacks.jpegCompressionAttack(currentImage, quality);
                    Logger.info("Applied JPEG compression attack with quality: " + quality);
                    break;

                case "resize":
                    double scale = (Double) parameter;
                    attackedImage = WatermarkAttacks.resizeAttack(currentImage, scale);
                    Logger.info("Applied resize attack with scale: " + scale);
                    break;

                case "rotation":
                    int angle = (Integer) parameter;
                    attackedImage = WatermarkAttacks.rotationAttack(currentImage, angle);
                    Logger.info("Applied rotation attack with angle: " + angle);
                    break;

                case "crop":
                    double cropPercent = (Double) parameter;
                    attackedImage = WatermarkAttacks.croppingAttack(currentImage, cropPercent);
                    Logger.info("Applied cropping attack with percentage: " + cropPercent);
                    break;
            }

            if (attackedImage != null) {
                // Create a new process with the attacked image
                watermarkedProcess = new Process(attackedImage);

                // Update the image view
                updateImageViews();

                // Clear the extraction results
                extractedWatermark = null;
                extractedWatermarkView.setImage(null);
                berLabel.setText("N/A");
                ncLabel.setText("N/A");
            }
        } catch (Exception e) {
            Logger.error("Error applying attack: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error applying attack: " + e.getMessage());
            alert.show();
        }
    }

    private void updateImageViews() {
        // Update original image view
        if (originalProcess != null) {
            originalImageView.setImage(convertToFXImage(originalProcess.getRGBImage()));
        }

        // Update watermark image view
        if (watermarkImage != null) {
            watermarkImageView.setImage(convertToFXImage(watermarkImage));
        }

        // Update watermarked image view
        if (watermarkedProcess != null) {
            watermarkedImageView.setImage(convertToFXImage(watermarkedProcess.getRGBImage()));
        }

        // Update extracted watermark view
        if (extractedWatermark != null) {
            extractedWatermarkView.setImage(convertToFXImage(extractedWatermark));
        }
    }

    private Image convertToFXImage(BufferedImage image) {
        if (image == null) return null;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            return new Image(in);
        } catch (IOException e) {
            Logger.error("Error converting BufferedImage to JavaFX Image: " + e.getMessage());
            return null;
        }
    }
}
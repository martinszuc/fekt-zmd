package graphics;

import Jama.Matrix;
import enums.QualityType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
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
        setMinWidth(1000);  // Increased from 900 to 1000
        setMinHeight(750);  // Increased from 700 to 750

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

    /**
     * Creates the attack simulation panel with improved layout
     */
    private TitledPane createAttackControls() {
        VBox attackBox = new VBox(12);
        attackBox.setPadding(new Insets(10));
        attackBox.setFillWidth(true);

        // JPEG Attack controls (vertical layout)
        VBox jpegBox = new VBox(5);
        jpegBox.setAlignment(Pos.CENTER_LEFT);

        HBox jpegLabelBox = new HBox(10);
        jpegLabelBox.setAlignment(Pos.CENTER_LEFT);
        Label jpegLabel = new Label("JPEG Quality:");
        Spinner<Integer> jpegSpinner = new Spinner<>(1, 100, 75);
        jpegSpinner.setEditable(true);
        jpegSpinner.setPrefWidth(80);
        jpegSpinner.setMaxWidth(80);
        jpegLabelBox.getChildren().addAll(jpegLabel, jpegSpinner);

        Button jpegButton = new Button("Apply JPEG Compression");
        jpegButton.setMaxWidth(Double.MAX_VALUE);
        jpegButton.setPrefHeight(30);
        jpegButton.setOnAction(e -> applyAttack("jpeg", jpegSpinner.getValue()));

        jpegBox.getChildren().addAll(jpegLabelBox, jpegButton);

        // Resize Attack controls
        VBox resizeBox = new VBox(5);
        resizeBox.setAlignment(Pos.CENTER_LEFT);

        HBox resizeLabelBox = new HBox(10);
        resizeLabelBox.setAlignment(Pos.CENTER_LEFT);
        Label resizeLabel = new Label("Resize Scale:");
        Spinner<Double> resizeSpinner = new Spinner<>(0.1, 1.0, 0.5, 0.1);
        resizeSpinner.setEditable(true);
        resizeSpinner.setPrefWidth(80);
        resizeSpinner.setMaxWidth(80);
        resizeLabelBox.getChildren().addAll(resizeLabel, resizeSpinner);

        Button resizeButton = new Button("Apply Resize");
        resizeButton.setMaxWidth(Double.MAX_VALUE);
        resizeButton.setPrefHeight(30);
        resizeButton.setOnAction(e -> applyAttack("resize", resizeSpinner.getValue()));

        resizeBox.getChildren().addAll(resizeLabelBox, resizeButton);

        // Rotation Attack controls
        VBox rotationBox = new VBox(5);
        rotationBox.setAlignment(Pos.CENTER_LEFT);

        HBox rotationLabelBox = new HBox(10);
        rotationLabelBox.setAlignment(Pos.CENTER_LEFT);
        Label rotationLabel = new Label("Rotation Angle:");
        Spinner<Integer> rotationSpinner = new Spinner<>(0, 360, 45);
        rotationSpinner.setEditable(true);
        rotationSpinner.setPrefWidth(80);
        rotationSpinner.setMaxWidth(80);
        rotationLabelBox.getChildren().addAll(rotationLabel, rotationSpinner);

        Button rotationButton = new Button("Apply Rotation");
        rotationButton.setMaxWidth(Double.MAX_VALUE);
        rotationButton.setPrefHeight(30);
        rotationButton.setOnAction(e -> applyAttack("rotation", rotationSpinner.getValue()));

        rotationBox.getChildren().addAll(rotationLabelBox, rotationButton);

        // Crop Attack controls
        VBox cropBox = new VBox(5);
        cropBox.setAlignment(Pos.CENTER_LEFT);

        HBox cropLabelBox = new HBox(10);
        cropLabelBox.setAlignment(Pos.CENTER_LEFT);
        Label cropLabel = new Label("Crop Percentage:");
        Spinner<Double> cropSpinner = new Spinner<>(0.0, 0.5, 0.1, 0.05);
        cropSpinner.setEditable(true);
        cropSpinner.setPrefWidth(80);
        cropSpinner.setMaxWidth(80);
        cropLabelBox.getChildren().addAll(cropLabel, cropSpinner);

        Button cropButton = new Button("Apply Crop");
        cropButton.setMaxWidth(Double.MAX_VALUE);
        cropButton.setPrefHeight(30);
        cropButton.setOnAction(e -> applyAttack("crop", cropSpinner.getValue()));

        cropBox.getChildren().addAll(cropLabelBox, cropButton);

        // Add all controls to the main box with separators
        attackBox.getChildren().addAll(
                jpegBox,
                new Separator(),
                resizeBox,
                new Separator(),
                rotationBox,
                new Separator(),
                cropBox
        );

        TitledPane attackPane = new TitledPane("Attack Simulation", attackBox);
        attackPane.setExpanded(false);
        attackPane.setMaxHeight(Double.MAX_VALUE);
        return attackPane;
    }

    private void createControls(BorderPane root) {
        // Create left control panel
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setPrefWidth(350); // Increased from original 300px

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

        // Wrap control panel in a scroll pane for better usability
        ScrollPane scrollPane = new ScrollPane(controlPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

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
        root.setLeft(scrollPane);
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

    /**
     * Creates improved DCT options panel with better layout
     */
    private VBox createDCTOptions() {
        VBox options = new VBox(15);
        options.setPadding(new Insets(10, 0, 10, 0));

        // Block size - using a grid for better alignment
        GridPane blockSizeGrid = new GridPane();
        blockSizeGrid.setHgap(10);
        blockSizeGrid.setVgap(5);
        blockSizeGrid.setAlignment(Pos.CENTER_LEFT);

        Label blockSizeLabel = new Label("Block Size:");
        blockSizeSpinner = new Spinner<>(4, 16, 8, 4);
        blockSizeSpinner.setEditable(true);
        blockSizeSpinner.setPrefWidth(80);
        blockSizeSpinner.setMaxWidth(80);

        blockSizeGrid.add(blockSizeLabel, 0, 0);
        blockSizeGrid.add(blockSizeSpinner, 1, 0);

        // Coefficient pairs section with heading
        Label coefPairsHeading = new Label("Coefficient Pairs:");
        coefPairsHeading.setStyle("-fx-font-weight: bold;");

        // First coefficient using a proper grid
        GridPane coef1Grid = new GridPane();
        coef1Grid.setHgap(10);
        coef1Grid.setVgap(5);
        coef1Grid.setPadding(new Insets(0, 0, 0, 15)); // Indent from the heading

        Label coef1Label = new Label("Coefficient 1:");
        coef1XSpinner = new Spinner<>(0, 7, 3);
        coef1XSpinner.setPrefWidth(60);
        coef1YSpinner = new Spinner<>(0, 7, 1);
        coef1YSpinner.setPrefWidth(60);

        Label coef1XLabel = new Label("X:");
        Label coef1YLabel = new Label("Y:");

        coef1Grid.add(coef1Label, 0, 0);
        coef1Grid.add(coef1XLabel, 1, 0);
        coef1Grid.add(coef1XSpinner, 2, 0);
        coef1Grid.add(coef1YLabel, 3, 0);
        coef1Grid.add(coef1YSpinner, 4, 0);

        // Second coefficient using a proper grid
        GridPane coef2Grid = new GridPane();
        coef2Grid.setHgap(10);
        coef2Grid.setVgap(5);
        coef2Grid.setPadding(new Insets(0, 0, 0, 15)); // Indent from the heading

        Label coef2Label = new Label("Coefficient 2:");
        coef2XSpinner = new Spinner<>(0, 7, 4);
        coef2XSpinner.setPrefWidth(60);
        coef2YSpinner = new Spinner<>(0, 7, 1);
        coef2YSpinner.setPrefWidth(60);

        Label coef2XLabel = new Label("X:");
        Label coef2YLabel = new Label("Y:");

        coef2Grid.add(coef2Label, 0, 0);
        coef2Grid.add(coef2XLabel, 1, 0);
        coef2Grid.add(coef2XSpinner, 2, 0);
        coef2Grid.add(coef2YLabel, 3, 0);
        coef2Grid.add(coef2YSpinner, 4, 0);

        // Strength - using a grid for better alignment
        GridPane strengthGrid = new GridPane();
        strengthGrid.setHgap(10);
        strengthGrid.setVgap(5);
        strengthGrid.setAlignment(Pos.CENTER_LEFT);

        Label strengthLabel = new Label("Embedding Strength:");
        strengthSpinner = new Spinner<>(1.0, 50.0, 10.0, 1.0);
        strengthSpinner.setEditable(true);
        strengthSpinner.setPrefWidth(100);
        strengthSpinner.setMaxWidth(120);

        strengthGrid.add(strengthLabel, 0, 0);
        strengthGrid.add(strengthSpinner, 1, 0);

        // Add everything to options with proper spacing and separators
        options.getChildren().addAll(
                blockSizeGrid,
                new Separator(),
                coefPairsHeading,
                coef1Grid,
                coef2Grid,
                new Separator(),
                strengthGrid
        );

        return options;
    }

    private BorderPane createResultsPanel() {
        BorderPane panel = new BorderPane();

        // Create grid for images
        GridPane imageGrid = new GridPane();
        imageGrid.setHgap(15);
        imageGrid.setVgap(15);
        imageGrid.setPadding(new Insets(10));

        // Original image view
        Label originalLabel = new Label("Original Image:");
        originalImageView = new ImageView();
        originalImageView.setFitWidth(220);
        originalImageView.setFitHeight(220);
        originalImageView.setPreserveRatio(true);

        // Watermark image view
        Label watermarkLabel = new Label("Watermark:");
        watermarkImageView = new ImageView();
        watermarkImageView.setFitWidth(220);
        watermarkImageView.setFitHeight(220);
        watermarkImageView.setPreserveRatio(true);

        // Watermarked image view
        Label watermarkedLabel = new Label("Watermarked Image:");
        watermarkedImageView = new ImageView();
        watermarkedImageView.setFitWidth(220);
        watermarkedImageView.setFitHeight(220);
        watermarkedImageView.setPreserveRatio(true);

        // Extracted watermark view
        Label extractedLabel = new Label("Extracted Watermark:");
        extractedWatermarkView = new ImageView();
        extractedWatermarkView.setFitWidth(220);
        extractedWatermarkView.setFitHeight(220);
        extractedWatermarkView.setPreserveRatio(true);

        // Create VBoxes for better layout
        VBox originalBox = new VBox(5, originalLabel, originalImageView);
        VBox watermarkBox = new VBox(5, watermarkLabel, watermarkImageView);
        VBox watermarkedBox = new VBox(5, watermarkedLabel, watermarkedImageView);
        VBox extractedBox = new VBox(5, extractedLabel, extractedWatermarkView);

        // Add to grid
        imageGrid.add(originalBox, 0, 0);
        imageGrid.add(watermarkBox, 1, 0);
        imageGrid.add(watermarkedBox, 0, 1);
        imageGrid.add(extractedBox, 1, 1);

        // Results panel
        VBox resultsBox = new VBox(10);
        resultsBox.setPadding(new Insets(10));

        // Watermark dimension input (for extraction)
        TitledPane dimensionsPane = new TitledPane();
        dimensionsPane.setText("Watermark Dimensions for Extraction");

        GridPane dimensionsGrid = new GridPane();
        dimensionsGrid.setHgap(10);
        dimensionsGrid.setVgap(5);
        dimensionsGrid.setPadding(new Insets(10));

        Label widthLabel = new Label("Width:");
        watermarkWidthField = new TextField("64");
        watermarkWidthField.setPrefWidth(100);

        Label heightLabel = new Label("Height:");
        watermarkHeightField = new TextField("64");
        watermarkHeightField.setPrefWidth(100);

        dimensionsGrid.add(widthLabel, 0, 0);
        dimensionsGrid.add(watermarkWidthField, 1, 0);
        dimensionsGrid.add(heightLabel, 2, 0);
        dimensionsGrid.add(watermarkHeightField, 3, 0);

        dimensionsPane.setContent(dimensionsGrid);
        dimensionsPane.setCollapsible(false);

        // Evaluation results
        TitledPane evaluationPane = new TitledPane();
        evaluationPane.setText("Watermark Evaluation");

        GridPane evaluationGrid = new GridPane();
        evaluationGrid.setHgap(10);
        evaluationGrid.setVgap(5);
        evaluationGrid.setPadding(new Insets(10));

        Label berTitleLabel = new Label("Bit Error Rate (BER):");
        berLabel = new Label("N/A");
        berLabel.setStyle("-fx-font-weight: bold;");

        Label ncTitleLabel = new Label("Normalized Correlation (NC):");
        ncLabel = new Label("N/A");
        ncLabel.setStyle("-fx-font-weight: bold;");

        evaluationGrid.add(berTitleLabel, 0, 0);
        evaluationGrid.add(berLabel, 1, 0);
        evaluationGrid.add(ncTitleLabel, 0, 1);
        evaluationGrid.add(ncLabel, 1, 1);

        evaluationPane.setContent(evaluationGrid);
        evaluationPane.setCollapsible(false);

        // Add to results box
        resultsBox.getChildren().addAll(
                dimensionsPane,
                evaluationPane
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
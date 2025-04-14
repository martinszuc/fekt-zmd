package graphics;

import Jama.Matrix;
import enums.QualityType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import watermarking.WatermarkResult;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced dialog for watermarking operations with improved attack simulation UI.
 */
public class WatermarkingDialog extends Stage {

    // Enumeration for watermarking methods
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

    // Enumeration for attack types
    private enum AttackType {
        JPEG_COMPRESSION("JPEG Compression", "Applies JPEG compression with specified quality"),
        JPEG_COMPRESSION_INTERNAL("Internal JPEG Compression", "Uses application's compression pipeline"),
        PNG_COMPRESSION("PNG Compression", "Applies PNG compression with specified level"),
        ROTATION_45("Rotation 45°", "Rotates image by 45 degrees"),
        ROTATION_90("Rotation 90°", "Rotates image by 90 degrees"),
        RESIZE_75("Resize 75%", "Resizes image to 75% and back"),
        RESIZE_50("Resize 50%", "Resizes image to 50% and back"),
        MIRRORING("Mirroring", "Flips image horizontally"),
        CROPPING("Cropping", "Crops image edges and resizes back");

        private final String displayName;
        private final String description;

        AttackType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    // Instance variables for storing process state
    private Process originalProcess;
    private BufferedImage watermarkImage;
    private BufferedImage embeddedWatermark;
    private BufferedImage extractedWatermark;
    private Process watermarkedProcess;
    private List<WatermarkResult> results = new ArrayList<>();

    // UI components for watermarking
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

    // UI components for images
    private ImageView originalImageView;
    private ImageView watermarkImageView;
    private ImageView watermarkedImageView;
    private ImageView extractedWatermarkView;

    // UI components for results
    private Label berLabel;
    private Label ncLabel;
    private TableView<WatermarkResult> resultsTable;

    private VBox lsbOptions;
    private VBox dctOptions;

    /**
     * Creates a new watermarking dialog.
     * @param parentStage The parent stage
     * @param process The image process to watermark
     */
    public WatermarkingDialog(Stage parentStage, Process process) {
        this.originalProcess = process;

        // Auto-convert to YCbCr if not already converted
        if (!originalProcess.isYCbCrConverted()) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("YCbCr Conversion");
            confirmDialog.setHeaderText("Image is not in YCbCr format");
            confirmDialog.setContentText("Watermarking requires YCbCr format. Convert now?");

            if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                originalProcess.convertToYCbCr();
                Logger.info("Image automatically converted to YCbCr for watermarking");
            }
        }

        // Configure stage
        initModality(Modality.APPLICATION_MODAL);
        initOwner(parentStage);
        setTitle("Image Watermarking");
        setMinWidth(1200);
        setMinHeight(900);

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
        VBox attackBox = new VBox(16);
        attackBox.setPadding(new Insets(10));
        attackBox.setFillWidth(true);

        // ComboBox for selecting attack type
        ComboBox<AttackType> attackTypeCombo = new ComboBox<>();
        attackTypeCombo.getItems().addAll(AttackType.values());
        attackTypeCombo.getSelectionModel().select(AttackType.JPEG_COMPRESSION);
        attackTypeCombo.setMaxWidth(Double.MAX_VALUE);

        // Attack description label
        Label descriptionLabel = new Label(AttackType.JPEG_COMPRESSION.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-style: italic;");

        // Update description when attack type changes
        attackTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                descriptionLabel.setText(newVal.getDescription());
            }
        });

        // Parameter controls
        VBox parameterBox = new VBox(10);
        parameterBox.setPadding(new Insets(10, 0, 10, 0));

        // JPEG quality parameter
        Label jpegQualityLabel = new Label("JPEG Quality (1-100):");
        Spinner<Integer> jpegQualitySpinner = new Spinner<>(1, 100, 75);
        jpegQualitySpinner.setEditable(true);
        jpegQualitySpinner.setPrefWidth(120);
        HBox jpegQualityBox = new HBox(10, jpegQualityLabel, jpegQualitySpinner);
        jpegQualityBox.setAlignment(Pos.CENTER_LEFT);

        // PNG compression level
        Label pngLevelLabel = new Label("PNG Compression (1-9):");
        Spinner<Integer> pngLevelSpinner = new Spinner<>(1, 9, 5);
        pngLevelSpinner.setEditable(true);
        pngLevelSpinner.setPrefWidth(120);
        HBox pngLevelBox = new HBox(10, pngLevelLabel, pngLevelSpinner);
        pngLevelBox.setAlignment(Pos.CENTER_LEFT);

        // Crop percentage
        Label cropPercentLabel = new Label("Crop Percentage (0.0-0.5):");
        Spinner<Double> cropPercentSpinner = new Spinner<>(0.0, 0.5, 0.2, 0.05);
        cropPercentSpinner.setEditable(true);
        cropPercentSpinner.setPrefWidth(120);
        HBox cropPercentBox = new HBox(10, cropPercentLabel, cropPercentSpinner);
        cropPercentBox.setAlignment(Pos.CENTER_LEFT);

        // Add parameter controls to parameter box
        parameterBox.getChildren().addAll(jpegQualityBox, pngLevelBox, cropPercentBox);

        // Apply attack button
        Button applyAttackButton = new Button("Apply Attack");
        applyAttackButton.setMaxWidth(Double.MAX_VALUE);
        applyAttackButton.setPrefHeight(40);
        applyAttackButton.setStyle("-fx-font-weight: bold;");

        // Apply attack action
        applyAttackButton.setOnAction(e -> {
            if (watermarkedProcess == null) {
                showAlert(Alert.AlertType.WARNING, "No Image", "Please embed a watermark first.");
                return;
            }

            // Get current RGB image
            BufferedImage currentImage = watermarkedProcess.getRGBImage();
            BufferedImage attackedImage = null;

            AttackType selectedAttack = attackTypeCombo.getValue();
            switch (selectedAttack) {
                case JPEG_COMPRESSION:
                    attackedImage = WatermarkAttacks.jpegCompressionAttack(currentImage, jpegQualitySpinner.getValue());
                    break;

                case JPEG_COMPRESSION_INTERNAL:
                    attackedImage = WatermarkAttacks.jpegCompressionAttackInternal(currentImage, jpegQualitySpinner.getValue());
                    break;

                case PNG_COMPRESSION:
                    attackedImage = WatermarkAttacks.pngCompressionAttack(currentImage, pngLevelSpinner.getValue());
                    break;

                case ROTATION_45:
                    attackedImage = WatermarkAttacks.rotationAttack(currentImage, 45);
                    break;

                case ROTATION_90:
                    attackedImage = WatermarkAttacks.rotationAttack(currentImage, 90);
                    break;

                case RESIZE_75:
                    attackedImage = WatermarkAttacks.resizeAttack(currentImage, 0.75);
                    break;

                case RESIZE_50:
                    attackedImage = WatermarkAttacks.resizeAttack(currentImage, 0.50);
                    break;

                case MIRRORING:
                    attackedImage = WatermarkAttacks.mirroringAttack(currentImage);
                    break;

                case CROPPING:
                    attackedImage = WatermarkAttacks.croppingAttack(currentImage, cropPercentSpinner.getValue());
                    break;
            }

            if (attackedImage != null) {
                // Create a new process with the attacked image
                watermarkedProcess = new Process(attackedImage);

                // Update the image view
                updateImageViews();

                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Attack Applied",
                        "The " + selectedAttack + " attack was applied successfully.\n" +
                                "You can now extract the watermark to evaluate the result.");
            }
        });

        // Add everything to attack box
        attackBox.getChildren().addAll(
                new Label("Select Attack Type:"),
                attackTypeCombo,
                descriptionLabel,
                new Separator(),
                new Label("Attack Parameters:"),
                parameterBox,
                applyAttackButton
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
        controlPanel.setPrefWidth(350);

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
        embedButton.setStyle("-fx-font-weight: bold;");
        embedButton.setOnAction(e -> embedWatermark());

        Button extractButton = new Button("Extract Watermark");
        extractButton.setMaxWidth(Double.MAX_VALUE);
        extractButton.setStyle("-fx-font-weight: bold;");
        extractButton.setOnAction(e -> extractWatermark());

        Button evaluateButton = new Button("Evaluate Quality");
        evaluateButton.setMaxWidth(Double.MAX_VALUE);
        evaluateButton.setOnAction(e -> evaluateWatermark());

        // Results table for tracking multiple evaluations
        resultsTable = createResultsTable();

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
                evaluateButton
        );

        // Add attack pane at the end
        controlPanel.getChildren().add(new Separator());
        controlPanel.getChildren().add(attackPane);

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
        originalImageView.setFitWidth(250);
        originalImageView.setFitHeight(250);
        originalImageView.setPreserveRatio(true);

        // Watermark image view
        Label watermarkLabel = new Label("Watermark:");
        watermarkImageView = new ImageView();
        watermarkImageView.setFitWidth(250);
        watermarkImageView.setFitHeight(250);
        watermarkImageView.setPreserveRatio(true);

        // Watermarked image view
        Label watermarkedLabel = new Label("Watermarked Image:");
        watermarkedImageView = new ImageView();
        watermarkedImageView.setFitWidth(250);
        watermarkedImageView.setFitHeight(250);
        watermarkedImageView.setPreserveRatio(true);

        // Extracted watermark view
        Label extractedLabel = new Label("Extracted Watermark:");
        extractedWatermarkView = new ImageView();
        extractedWatermarkView.setFitWidth(250);
        extractedWatermarkView.setFitHeight(250);
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

        // Results table
        VBox tableContainer = new VBox(5);
        Label tableLabel = new Label("Test Results:");
        tableLabel.setStyle("-fx-font-weight: bold;");
        tableContainer.getChildren().addAll(tableLabel, resultsTable);
        tableContainer.setPadding(new Insets(10, 0, 0, 0));

        // Add to results box
        resultsBox.getChildren().addAll(
                dimensionsPane,
                evaluationPane,
                tableContainer
        );

        // Add to panel
        panel.setCenter(imageGrid);
        panel.setBottom(resultsBox);

        return panel;
    }

    /**
     * Creates the results table for tracking attack performance
     */
    private TableView<WatermarkResult> createResultsTable() {
        TableView<WatermarkResult> table = new TableView<>();

        // Create columns
        TableColumn<WatermarkResult, String> attackCol = new TableColumn<>("Attack");
        attackCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAttackName()));

        TableColumn<WatermarkResult, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMethod()));

        TableColumn<WatermarkResult, String> berCol = new TableColumn<>("BER");
        berCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.4f", cellData.getValue().getBer())));

        TableColumn<WatermarkResult, String> ncCol = new TableColumn<>("NC");
        ncCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.4f", cellData.getValue().getNc())));

        // Add columns to table
        table.getColumns().addAll(attackCol, methodCol, berCol, ncCol);

        // Adjust column widths
        attackCol.prefWidthProperty().bind(table.widthProperty().multiply(0.35));
        methodCol.prefWidthProperty().bind(table.widthProperty().multiply(0.25));
        berCol.prefWidthProperty().bind(table.widthProperty().multiply(0.2));
        ncCol.prefWidthProperty().bind(table.widthProperty().multiply(0.2));

        // Set height
        table.setPrefHeight(200);

        return table;
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
                showAlert(Alert.AlertType.ERROR, "Error", "Error loading watermark image: " + e.getMessage());
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
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid size. Please enter a positive integer.");
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
            showAlert(Alert.AlertType.ERROR, "Error", "Please load or create a watermark image first.");
            return;
        }

        // Check if YCbCr conversion is performed
        if (!originalProcess.isYCbCrConverted()) {
            Logger.error("YCbCr conversion required");

            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("YCbCr Conversion Required");
            confirmDialog.setHeaderText("Image is not in YCbCr format");
            confirmDialog.setContentText("Watermarking requires YCbCr format. Convert now?");

            if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                originalProcess.convertToYCbCr();
                Logger.info("Image converted to YCbCr for watermarking");
            } else {
                return;
            }
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

            // Reset extraction results
            extractedWatermark = null;
            extractedWatermarkView.setImage(null);
            berLabel.setText("N/A");
            ncLabel.setText("N/A");

            Logger.info("Watermark embedded successfully using " + method + " method");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Watermark embedded successfully");

        } catch (Exception e) {
            Logger.error("Error embedding watermark: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Error embedding watermark: " + e.getMessage());
        }
    }

    private void extractWatermark() {
        if (watermarkedProcess == null) {
            Logger.error("No watermarked image available");
            showAlert(Alert.AlertType.ERROR, "Error", "Please embed a watermark first.");
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
            showAlert(Alert.AlertType.INFORMATION, "Success", "Watermark extracted successfully");

        } catch (Exception e) {
            Logger.error("Error extracting watermark: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Error extracting watermark: " + e.getMessage());
        }
    }

    private void evaluateWatermark() {
        if (embeddedWatermark == null || extractedWatermark == null) {
            Logger.error("Cannot evaluate: original or extracted watermark missing");
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Both embedded and extracted watermarks must be available for evaluation.");
            return;
        }

        try {
            // Calculate BER
            double ber = WatermarkEvaluation.calculateBER(embeddedWatermark, extractedWatermark);
            berLabel.setText(String.format("%.4f", ber));

            // Calculate NC
            double nc = WatermarkEvaluation.calculateNC(embeddedWatermark, extractedWatermark);
            ncLabel.setText(String.format("%.4f", nc));

            // Add to results table
            String attackName = "None";
            WatermarkMethod method = methodComboBox.getValue();

            // Create a new result and add to table
            WatermarkResult result = new WatermarkResult(
                    attackName,
                    method.toString(),
                    componentComboBox.getValue().toString(),
                    (method == WatermarkMethod.LSB) ? String.valueOf(bitPlaneSpinner.getValue()) :
                            String.valueOf(blockSizeSpinner.getValue()),
                    ber,
                    nc
            );

            results.add(result);
            resultsTable.getItems().add(result);
            resultsTable.refresh();

            Logger.info("Watermark evaluation completed: BER=" + ber + ", NC=" + nc);
        } catch (Exception e) {
            Logger.error("Error evaluating watermark: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Error evaluating watermark: " + e.getMessage());
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
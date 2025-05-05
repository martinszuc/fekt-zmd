package graphics;

import Jama.Matrix;
import enums.QualityType;
import enums.WatermarkType;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jpeg.Process;
import utils.Logger;
import watermarking.attacks.WatermarkAttacks;
import watermarking.core.AbstractWatermarking;
import watermarking.core.WatermarkEvaluation;
import watermarking.core.WatermarkResult;
import watermarking.core.WatermarkingFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced dialog for watermarking operations with improved attack simulation UI.
 * This dialog provides functionality for embedding and extracting watermarks using
 * both spatial and frequency domain techniques, as well as testing their robustness
 * against various attacks.
 */
public class WatermarkingDialog extends Stage {

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

    // Status label for notifications
    private Label statusLabel;

    // Instance variables for storing process state
    private Process originalProcess;
    private BufferedImage watermarkImage;
    private BufferedImage embeddedWatermark;
    private BufferedImage extractedWatermark;
    private Process watermarkedProcess;
    private List<WatermarkResult> results = new ArrayList<>();

    // UI components for watermarking
    private ComboBox<WatermarkType> methodComboBox;
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
        setMinWidth(1000);
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

    private void createControls(BorderPane root) {
        // Create left control panel for watermarking methods
        VBox watermarkControlPanel = createWatermarkControlPanel();
        watermarkControlPanel.setPrefWidth(250);

        // Create content area with images and results
        BorderPane contentPanel = createContentPanel();

        // Create attack controls panel
        VBox attackControlPanel = createAttackControls();
        attackControlPanel.setPrefWidth(250);

        // Status label for notifications
        statusLabel = new Label("");
        statusLabel.setTextAlignment(TextAlignment.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setPadding(new Insets(5));
        statusLabel.setMinHeight(30);
        root.setBottom(statusLabel);

        // Create vertical separators
        Separator leftSeparator = new Separator(Orientation.VERTICAL);
        leftSeparator.setPadding(new Insets(0, 5, 0, 5));

        Separator rightSeparator = new Separator(Orientation.VERTICAL);
        rightSeparator.setPadding(new Insets(0, 5, 0, 5));

        // Combine everything in horizontal layout with separators
        HBox mainContent = new HBox(5);
        mainContent.getChildren().addAll(
                watermarkControlPanel,
                leftSeparator,
                contentPanel,
                rightSeparator,
                attackControlPanel);
        HBox.setHgrow(contentPanel, Priority.ALWAYS);

        root.setCenter(mainContent);
    }

    private VBox createWatermarkControlPanel() {
        VBox watermarkControlPanel = new VBox(10);
        watermarkControlPanel.setPadding(new Insets(10));
        watermarkControlPanel.setPrefWidth(280);

        Label WatermarkTypeTitle = new Label("Watermarking Configuration");
        WatermarkTypeTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        watermarkControlPanel.getChildren().add(WatermarkTypeTitle);

        // Method selection
        Label methodLabel = new Label("Watermarking Method:");
        methodComboBox = new ComboBox<>();
        methodComboBox.getItems().addAll(WatermarkType.values());
        methodComboBox.setValue(WatermarkType.LSB);
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
            lsbOptions.setVisible(newVal == WatermarkType.LSB);
            lsbOptions.setManaged(newVal == WatermarkType.LSB);
            dctOptions.setVisible(newVal == WatermarkType.DCT);
            dctOptions.setManaged(newVal == WatermarkType.DCT);
        });

        // Initial visibility
        lsbOptions.setVisible(true);
        lsbOptions.setManaged(true);
        dctOptions.setVisible(false);
        dctOptions.setManaged(false);

        // Watermark dimensions
        HBox dimensionsBox = new HBox(10);
        dimensionsBox.setAlignment(Pos.CENTER_LEFT);

        Label dimensionsLabel = new Label("Dimensions:");
        watermarkWidthField = new TextField("64");
        watermarkWidthField.setPrefWidth(50);
        Label xLabel = new Label("x");
        watermarkHeightField = new TextField("64");
        watermarkHeightField.setPrefWidth(50);

        dimensionsBox.getChildren().addAll(dimensionsLabel, watermarkWidthField, xLabel, watermarkHeightField);

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

        // Add everything to the watermark control panel
        watermarkControlPanel.getChildren().addAll(
                methodLabel, methodComboBox,
                componentLabel, componentComboBox,
                new Separator(),
                lsbOptions,
                dctOptions,
                new Separator(),
                dimensionsBox,
                loadWatermarkButton,
                createWatermarkButton,
                new Separator(),
                embedButton,
                extractButton,
                evaluateButton
        );

        return watermarkControlPanel;
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

        // Create a GridPane for coefficient controls
        GridPane coefGrid = new GridPane();
        coefGrid.setHgap(5);
        coefGrid.setVgap(5);

        // First coefficient (x,y)
        Label coef1Label = new Label("Coef 1:");
        coef1XSpinner = new Spinner<>(0, 7, 3);
        coef1XSpinner.setPrefWidth(60);
        coef1XSpinner.setEditable(true);
        Label xLabel1 = new Label("x:");

        coef1YSpinner = new Spinner<>(0, 7, 1);
        coef1YSpinner.setPrefWidth(60);
        coef1YSpinner.setEditable(true);
        Label yLabel1 = new Label("y:");

        // Second coefficient (x,y)
        Label coef2Label = new Label("Coef 2:");
        coef2XSpinner = new Spinner<>(0, 7, 4);
        coef2XSpinner.setPrefWidth(60);
        coef2XSpinner.setEditable(true);
        Label xLabel2 = new Label("x:");

        coef2YSpinner = new Spinner<>(0, 7, 1);
        coef2YSpinner.setPrefWidth(60);
        coef2YSpinner.setEditable(true);
        Label yLabel2 = new Label("y:");

        // Add to grid
        coefGrid.add(coef1Label, 0, 0);
        coefGrid.add(xLabel1, 1, 0);
        coefGrid.add(coef1XSpinner, 2, 0);
        coefGrid.add(yLabel1, 3, 0);
        coefGrid.add(coef1YSpinner, 4, 0);

        coefGrid.add(coef2Label, 0, 1);
        coefGrid.add(xLabel2, 1, 1);
        coefGrid.add(coef2XSpinner, 2, 1);
        coefGrid.add(yLabel2, 3, 1);
        coefGrid.add(coef2YSpinner, 4, 1);

        // Strength
        Label strengthLabel = new Label("Embedding Strength:");
        strengthSpinner = new Spinner<>(1.0, 50.0, 10.0, 1.0);
        strengthSpinner.setEditable(true);
        strengthSpinner.setPrefWidth(80);
        strengthSpinner.setMaxWidth(80);
        HBox strengthBox = new HBox(10, strengthLabel, strengthSpinner);
        strengthBox.setAlignment(Pos.CENTER_LEFT);

        // Add to options
        options.getChildren().addAll(
                blockSizeBox,
                coefPairsLabel,
                coefGrid,
                strengthBox
        );

        return options;
    }

    private BorderPane createContentPanel() {
        BorderPane panel = new BorderPane();

        // Create grid for images
        GridPane imageGrid = new GridPane();
        imageGrid.setHgap(15);
        imageGrid.setVgap(15);
        imageGrid.setPadding(new Insets(10));

        // Original image view
        Label originalLabel = new Label("Original Image:");
        originalLabel.setStyle("-fx-font-weight: bold;");
        originalImageView = new ImageView();
        originalImageView.setFitWidth(250);
        originalImageView.setFitHeight(250);
        originalImageView.setPreserveRatio(true);

        // Watermark image view
        Label watermarkLabel = new Label("Watermark:");
        watermarkLabel.setStyle("-fx-font-weight: bold;");
        watermarkImageView = new ImageView();
        watermarkImageView.setFitWidth(250);
        watermarkImageView.setFitHeight(250);
        watermarkImageView.setPreserveRatio(true);

        // Watermarked image view
        Label watermarkedLabel = new Label("Watermarked Image:");
        watermarkedLabel.setStyle("-fx-font-weight: bold;");
        watermarkedImageView = new ImageView();
        watermarkedImageView.setFitWidth(250);
        watermarkedImageView.setFitHeight(250);
        watermarkedImageView.setPreserveRatio(true);

        // Extracted watermark view
        Label extractedLabel = new Label("Extracted Watermark:");
        extractedLabel.setStyle("-fx-font-weight: bold;");
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


        // Create evaluation results panel
        HBox evaluationPane = new HBox(10);
        evaluationPane.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID,
                new CornerRadii(5), BorderWidths.DEFAULT)));
        evaluationPane.setPadding(new Insets(10));

        Label evaluationTitle = new Label("Watermark Evaluation");
        evaluationTitle.setStyle("-fx-font-weight: bold;");

        GridPane evaluationGrid = new GridPane();
        evaluationGrid.setHgap(10);
        evaluationGrid.setVgap(5);

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

        VBox evalContentBox = new VBox(5, evaluationTitle, evaluationGrid);
        evaluationPane.getChildren().add(evalContentBox);
        HBox.setHgrow(evalContentBox, Priority.ALWAYS);

        // Results table with scrolling
        Label tableLabel = new Label("Test Results:");
        tableLabel.setStyle("-fx-font-weight: bold;");

        resultsTable = createResultsTable();

        // Create ScrollPane for the results table
        ScrollPane scrollPane = new ScrollPane(resultsTable);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setMinHeight(100);
        scrollPane.setMaxHeight(300);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Create VBox for evaluation results and table
        VBox resultsBox = new VBox(10);
        resultsBox.setPadding(new Insets(10));
        resultsBox.getChildren().addAll(evaluationPane, tableLabel, scrollPane);

        // Add to panel
        panel.setCenter(imageGrid);
        panel.setBottom(resultsBox);

        return panel;
    }

    /**
     * Creates the attack simulation panel with improved layout
     */
    private VBox createAttackControls() {
        VBox attackBox = new VBox(16);
        attackBox.setPadding(new Insets(10));
        attackBox.setFillWidth(true);

        Label titleLabel = new Label("Attack Simulation");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

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
                showStatus("Please embed a watermark first", "error");
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
                showStatus("Attack applied: " + selectedAttack, "success");
            }
        });

        // Add everything to attack box
        attackBox.getChildren().addAll(
                titleLabel,
                new Label("Select Attack Type:"),
                attackTypeCombo,
                descriptionLabel,
                new Separator(),
                new Label("Attack Parameters:"),
                parameterBox,
                applyAttackButton
        );

        return attackBox;
    }

    /**
     * Creates the results table for tracking attack performance
     */
    private TableView<WatermarkResult> createResultsTable() {
        TableView<WatermarkResult> table = new TableView<>();

        // Make table columns resize to fit content width
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        // Add a quality rating column
        TableColumn<WatermarkResult, String> qualityCol = new TableColumn<>("Quality");
        qualityCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getQualityRating()));

        // Add columns to table
        table.getColumns().addAll(attackCol, methodCol, berCol, ncCol, qualityCol);

        // Adjust column widths
        attackCol.prefWidthProperty().bind(table.widthProperty().multiply(0.30));
        methodCol.prefWidthProperty().bind(table.widthProperty().multiply(0.20));
        berCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        ncCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        qualityCol.prefWidthProperty().bind(table.widthProperty().multiply(0.20));

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

                showStatus("Watermark loaded: " + file.getName(), "success");
                Logger.info("Watermark image loaded: " + file.getAbsolutePath());
            } catch (IOException e) {
                Logger.error("Error loading watermark: " + e.getMessage());
                showStatus("Error loading watermark", "error");
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

                showStatus("Custom watermark created (" + size + "x" + size + ")", "success");
                Logger.info("Custom watermark created with size " + size + "x" + size);
            } catch (NumberFormatException e) {
                Logger.error("Invalid watermark size: " + e.getMessage());
                showStatus("Invalid size. Please enter a positive integer", "error");
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
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw black pattern (example: checkerboard)
        g2d.setColor(java.awt.Color.BLACK);
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
        g2d.setColor(java.awt.Color.BLACK);
        g2d.fillOval(width/2 - centerSize/2, height/2 - centerSize/2, centerSize, centerSize);

        g2d.dispose();

        // Update the image view
        updateImageViews();
    }

    private void embedWatermark() {
        if (watermarkImage == null) {
            Logger.error("No watermark image available");
            showStatus("Please load or create a watermark image first", "error");
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

            // Get selected watermarking method
            WatermarkType method = methodComboBox.getValue();
            AbstractWatermarking watermarking = WatermarkingFactory.createWatermarking(method);

            // Embed watermark
            Matrix watermarkedMatrix = null;

            if (method == WatermarkType.LSB) {
                int bitPlane = bitPlaneSpinner.getValue();
                boolean permute = permuteCheckBox.isSelected();
                String key = keyTextField.getText();

                watermarkedMatrix = watermarking.embed(
                        componentMatrix,
                        watermarkImage,
                        bitPlane,
                        permute,
                        key
                );
            } else if (method == WatermarkType.DCT) {
                int blockSize = blockSizeSpinner.getValue();
                int[] coef1 = new int[]{coef1XSpinner.getValue(), coef1YSpinner.getValue()};
                int[] coef2 = new int[]{coef2XSpinner.getValue(), coef2YSpinner.getValue()};
                double strength = strengthSpinner.getValue();

                watermarkedMatrix = watermarking.embed(
                        componentMatrix,
                        watermarkImage,
                        blockSize,
                        coef1,
                        coef2,
                        strength
                );
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
            showStatus("Watermark embedded successfully", "success");

        } catch (Exception e) {
            Logger.error("Error embedding watermark: " + e.getMessage());
            showStatus("Error embedding watermark: " + e.getMessage(), "error");
        }
    }

    private void extractWatermark() {
        if (watermarkedProcess == null) {
            Logger.error("No watermarked image available");
            showStatus("Please embed a watermark first", "error");
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

            // Get selected watermarking method
            WatermarkType method = methodComboBox.getValue();
            AbstractWatermarking watermarking = WatermarkingFactory.createWatermarking(method);

            // Extract watermark
            if (method == WatermarkType.LSB) {
                int bitPlane = bitPlaneSpinner.getValue();
                boolean permute = permuteCheckBox.isSelected();
                String key = keyTextField.getText();

                extractedWatermark = watermarking.extract(
                        componentMatrix,
                        width,
                        height,
                        bitPlane,
                        permute,
                        key
                );
            } else if (method == WatermarkType.DCT) {
                int blockSize = blockSizeSpinner.getValue();
                int[] coef1 = new int[]{coef1XSpinner.getValue(), coef1YSpinner.getValue()};
                int[] coef2 = new int[]{coef2XSpinner.getValue(), coef2YSpinner.getValue()};

                extractedWatermark = watermarking.extract(
                        componentMatrix,
                        width,
                        height,
                        blockSize,
                        coef1,
                        coef2
                );
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
            showStatus("Watermark extracted successfully", "success");

        } catch (Exception e) {
            Logger.error("Error extracting watermark: " + e.getMessage());
            showStatus("Error extracting watermark: " + e.getMessage(), "error");
        }
    }

    private void evaluateWatermark() {
        if (embeddedWatermark == null || extractedWatermark == null) {
            Logger.error("Cannot evaluate: original or extracted watermark missing");
            showStatus("Both embedded and extracted watermarks must be available for evaluation", "error");
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
            WatermarkType method = methodComboBox.getValue();

            // Create a new result and add to table
            WatermarkResult result = new WatermarkResult(
                    attackName,
                    method.toString(),
                    componentComboBox.getValue().toString(),
                    (method == WatermarkType.LSB) ? String.valueOf(bitPlaneSpinner.getValue()) :
                            String.valueOf(blockSizeSpinner.getValue()),
                    ber,
                    nc
            );

            results.add(result);
            resultsTable.getItems().add(result);
            resultsTable.refresh();

            showStatus("Evaluation complete: BER=" + String.format("%.4f", ber) + ", NC=" + String.format("%.4f", nc), "success");
            Logger.info("Watermark evaluation completed: BER=" + ber + ", NC=" + nc);
        } catch (Exception e) {
            Logger.error("Error evaluating watermark: " + e.getMessage());
            showStatus("Error evaluating watermark: " + e.getMessage(), "error");
        }
    }

    /**
     * Displays a status message at the bottom of the window.
     * @param message The message to display
     * @param type The type of message (success, error, info)
     */
    private void showStatus(String message, String type) {
        statusLabel.setText(message);

        // Set style based on message type
        switch (type) {
            case "success":
                statusLabel.setStyle("-fx-background-color: #e7f3e7; -fx-text-fill: #2e7d32; -fx-padding: 5px;");
                break;
            case "error":
                statusLabel.setStyle("-fx-background-color: #fdecea; -fx-text-fill: #d32f2f; -fx-padding: 5px;");
                break;
            default: // info
                statusLabel.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #0277bd; -fx-padding: 5px;");
                break;
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
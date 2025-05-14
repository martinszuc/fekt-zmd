package graphics;

import Jama.Matrix;
import enums.AttackType;
import enums.QualityType;
import enums.WatermarkType;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jpeg.Process;
import utils.Logger;
import watermarking.attacks.AbstractWatermarkAttack;
import watermarking.attacks.WatermarkAttackFactory;
import watermarking.core.*;
import watermarking.core.WatermarkingFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * Enhanced dialog for watermarking operations with improved attack simulation UI.
 * This dialog provides functionality for embedding and extracting watermarks using
 * both spatial and frequency domain techniques, as well as testing their robustness
 * against various attacks.
 */
public class WatermarkingDialog extends Stage {

    // Status label for notifications
    private Label statusLabel;

    // Instance variables for storing process state
    private Process originalProcess;
    private BufferedImage watermarkImage;
    private BufferedImage embeddedWatermark;
    private BufferedImage extractedWatermark;
    private Process watermarkedProcess;
    private List<WatermarkResult> results = new ArrayList<>();
    private String watermarkConfigDesc = "Default"; // Description of current watermark

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

    // UI components for attacks
    private ComboBox<AttackType> attackTypeCombo;
    private VBox attackParametersBox;
    private Map<String, Control> attackParameterControls = new HashMap<>();

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
    private VBox dwtOptions;
    private VBox svdOptions;

    // DWT controls
    private Spinner<Double> dwtStrengthSpinner;
    private ComboBox<String> dwtSubbandComboBox;

    // SVD controls
    private Spinner<Double> svdAlphaSpinner;

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

        // Create menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

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
     * Creates the menu bar for the watermarking dialog,
     * now including “Save Watermarked Image...” functionality.
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");

        // *** NEW: Save the current watermarked image to disk ***
        MenuItem saveWatermarkedItem = new MenuItem("Save Watermarked Image...");
        saveWatermarkedItem.setOnAction(e -> saveWatermarkedImage());
        fileMenu.getItems().add(saveWatermarkedItem);

        MenuItem saveReportItem = new MenuItem("Export Test Report...");
        saveReportItem.setOnAction(e -> exportTestReport());

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setOnAction(e -> close());

        fileMenu.getItems().addAll(
                new SeparatorMenuItem(),
                saveReportItem,
                new SeparatorMenuItem(),
                closeItem
        );

        // Watermark menu
        Menu watermarkMenu = new Menu("Watermark");
        MenuItem configureWatermarkItem = new MenuItem("Configure Watermark...");
        configureWatermarkItem.setOnAction(e -> openWatermarkConfigDialog());
        MenuItem loadWatermarkItem = new MenuItem("Load Watermark...");
        loadWatermarkItem.setOnAction(e -> loadWatermark());
        MenuItem createDefaultItem = new MenuItem("Create Default");
        createDefaultItem.setOnAction(e -> createDefaultWatermark());
        watermarkMenu.getItems().addAll(configureWatermarkItem, loadWatermarkItem, createDefaultItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        // Add menus to menu bar
        menuBar.getMenus().addAll(fileMenu, watermarkMenu, helpMenu);

        return menuBar;
    }

    /**
     * Opens a FileChooser and writes out the currently displayed
     * watermarked image (if any) as a PNG.
     */
    private void saveWatermarkedImage() {
        if (watermarkedProcess == null) {
            showStatus("No watermarked image to save", "error");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Watermarked Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Image (*.jpg;*.jpeg)", "*.jpg"),
                new FileChooser.ExtensionFilter("BMP Image (*.bmp)", "*.bmp")
        );
        fileChooser.setInitialFileName("watermarked_image.png");

        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try {
                String fname = file.getName().toLowerCase();
                String fmt = "png";
                if (fname.endsWith(".jpg") || fname.endsWith(".jpeg")) fmt = "jpg";
                else if (fname.endsWith(".bmp")) fmt = "bmp";
                BufferedImage img = watermarkedProcess.getRGBImage();
                ImageIO.write(img, fmt, file);
                showStatus("Watermarked image saved: " + file.getAbsolutePath(), "success");
            } catch (IOException ex) {
                Logger.error("Error saving watermarked image: " + ex.getMessage());
                showStatus("Error saving image: " + ex.getMessage(), "error");
            }
        }
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
                attackControlPanel
        );
        HBox.setHgrow(contentPanel, Priority.ALWAYS);

        root.setCenter(mainContent);
    }

    private VBox createWatermarkControlPanel() {
        VBox watermarkControlPanel = new VBox(10);
        watermarkControlPanel.setPadding(new Insets(10));
        watermarkControlPanel.setPrefWidth(280);

        Label WatermarkTypeTitle = new Label("Watermarking Config");
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

        // Create DWT options panel
        dwtOptions = createDWTOptions();

        // Create SVD options panel
        svdOptions = createSVDOptions();

        // Show/hide the appropriate options panel based on selected method
        methodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            lsbOptions.setVisible(newVal == WatermarkType.LSB);
            lsbOptions.setManaged(newVal == WatermarkType.LSB);
            dctOptions.setVisible(newVal == WatermarkType.DCT);
            dctOptions.setManaged(newVal == WatermarkType.DCT);
            dwtOptions.setVisible(newVal == WatermarkType.DWT);
            dwtOptions.setManaged(newVal == WatermarkType.DWT);
            svdOptions.setVisible(newVal == WatermarkType.SVD);
            svdOptions.setManaged(newVal == WatermarkType.SVD);
        });

        // Initial visibility
        lsbOptions.setVisible(true);
        lsbOptions.setManaged(true);
        dctOptions.setVisible(false);
        dctOptions.setManaged(false);
        dwtOptions.setVisible(false);
        dwtOptions.setManaged(false);
        svdOptions.setVisible(false);
        svdOptions.setManaged(false);

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

        // Configure Watermark button
        Button configureWatermarkButton = new Button("Configure Watermark");
        configureWatermarkButton.setMaxWidth(Double.MAX_VALUE);
        configureWatermarkButton.setOnAction(e -> openWatermarkConfigDialog());

        // Action buttons
        Button embedButton = new Button("Embed Watermark");
        embedButton.setMaxWidth(Double.MAX_VALUE);
        embedButton.setPrefHeight(50);
        embedButton.setStyle("-fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        embedButton.setOnAction(e -> embedWatermark());

        Button extractButton = new Button("Extract Watermark");
        extractButton.setMaxWidth(Double.MAX_VALUE);
        extractButton.setPrefHeight(50);
        extractButton.setStyle("-fx-font-weight: bold; -fx-background-color: #FFC107; -fx-text-fill: black;");
        extractButton.setOnAction(e -> extractWatermark());

        // Add everything to the watermark control panel
        watermarkControlPanel.getChildren().addAll(
                methodLabel, methodComboBox,
                componentLabel, componentComboBox,
                new Separator(),
                lsbOptions,
                dctOptions,
                dwtOptions,
                svdOptions,
                new Separator(),
                dimensionsBox,
                configureWatermarkButton,
                new Separator()
        );

        // Create a VBox for the action buttons at the bottom
        VBox actionButtonsBox = new VBox(10);
        actionButtonsBox.getChildren().addAll(embedButton, extractButton);

        // Add a spring to push buttons to the bottom
        VBox.setVgrow(new Region(), Priority.ALWAYS);
        watermarkControlPanel.getChildren().addAll(
                new Region(),  // This region will expand to fill space
                new Separator(),
                actionButtonsBox
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
        VBox blockSizeBox = new VBox(5, blockSizeLabel, blockSizeSpinner);

        // Separator between block size and coefficient pairs
        Separator blockCoefSeparator = new Separator(Orientation.HORIZONTAL);
        blockCoefSeparator.setPadding(new Insets(5, 0, 5, 0));

        // Coefficient pairs
        Label coefPairsLabel = new Label("Coefficient Pairs:");
        coefPairsLabel.setStyle("-fx-font-weight: bold;");

        // First coefficient (x,y) - using VBox and HBox for better layout control
        Label coef1Label = new Label("Coefficient 1:");
        HBox coef1Controls = new HBox(10);

        Label xLabel1 = new Label("x:");
        coef1XSpinner = new Spinner<>(0, 7, 3);
        coef1XSpinner.setPrefWidth(60);
        coef1XSpinner.setEditable(true);

        Label yLabel1 = new Label("y:");
        coef1YSpinner = new Spinner<>(0, 7, 1);
        coef1YSpinner.setPrefWidth(60);
        coef1YSpinner.setEditable(true);

        coef1Controls.getChildren().addAll(xLabel1, coef1XSpinner, yLabel1, coef1YSpinner);
        VBox coef1Box = new VBox(5, coef1Label, coef1Controls);

        // Small spacer
        Region coefSpacer = new Region();
        coefSpacer.setPrefHeight(5);

        // Second coefficient (x,y) - using VBox and HBox for better layout control
        Label coef2Label = new Label("Coefficient 2:");
        HBox coef2Controls = new HBox(10);

        Label xLabel2 = new Label("x:");
        coef2XSpinner = new Spinner<>(0, 7, 4);
        coef2XSpinner.setPrefWidth(60);
        coef2XSpinner.setEditable(true);

        Label yLabel2 = new Label("y:");
        coef2YSpinner = new Spinner<>(0, 7, 1);
        coef2YSpinner.setPrefWidth(60);
        coef2YSpinner.setEditable(true);

        coef2Controls.getChildren().addAll(xLabel2, coef2XSpinner, yLabel2, coef2YSpinner);
        VBox coef2Box = new VBox(5, coef2Label, coef2Controls);

        // Separator before strength
        Separator strengthSeparator = new Separator(Orientation.HORIZONTAL);
        strengthSeparator.setPadding(new Insets(5, 0, 5, 0));

        // Strength - vertical layout with label above
        Label strengthLabel = new Label("Embedding Strength:");
        strengthSpinner = new Spinner<>(1.0, 50.0, 10.0, 1.0);
        strengthSpinner.setEditable(true);
        strengthSpinner.setPrefWidth(120);
        VBox strengthBox = new VBox(5, strengthLabel, strengthSpinner);

        // Add to options
        options.getChildren().addAll(
                blockSizeBox,
                blockCoefSeparator, // Separator between block size and coefficient pairs
                coefPairsLabel,
                coef1Box,
                coefSpacer,
                coef2Box,
                strengthSeparator,
                strengthBox
        );

        return options;
    }

    /**
     * Options panel for DWT watermarking.
     */
    private VBox createDWTOptions() {
        VBox options = new VBox(10);
        options.setPadding(new Insets(5, 0, 5, 0));

        // Strength parameter
        Label strengthLabel = new Label("Embedding Strength:");
        dwtStrengthSpinner = new Spinner<>(0.1, 100.0, 5.0, 0.5);
        dwtStrengthSpinner.setEditable(true);
        dwtStrengthSpinner.setPrefWidth(120);
        VBox strengthBox = new VBox(5, strengthLabel, dwtStrengthSpinner);

        // Subband selection
        Label subbandLabel = new Label("Subband:");
        dwtSubbandComboBox = new ComboBox<>();
        dwtSubbandComboBox.getItems().addAll("LL", "LH", "HL", "HH");
        dwtSubbandComboBox.setValue("LL");
        dwtSubbandComboBox.setMaxWidth(Double.MAX_VALUE);
        VBox subbandBox = new VBox(5, subbandLabel, dwtSubbandComboBox);

        // Description for subbands
        Label subbandDescLabel = new Label(
                "LL: Low-Low (Approximation)\n" +
                        "LH: Low-High (Horizontal detail)\n" +
                        "HL: High-Low (Vertical detail)\n" +
                        "HH: High-High (Diagonal detail)");
        subbandDescLabel.setStyle("-fx-font-size: 10px; -fx-font-style: italic;");
        subbandDescLabel.setWrapText(true);

        options.getChildren().addAll(strengthBox, subbandBox, subbandDescLabel);
        return options;
    }

    /**
     * Options panel for SVD watermarking.
     */
    private VBox createSVDOptions() {
        VBox options = new VBox(10);
        options.setPadding(new Insets(5, 0, 5, 0));

        // Alpha parameter (embedding strength)
        Label alphaLabel = new Label("Embedding Strength (α):");
        svdAlphaSpinner = new Spinner<>(0.1, 50.0, 1.0, 0.1);
        svdAlphaSpinner.setEditable(true);
        svdAlphaSpinner.setPrefWidth(120);
        VBox alphaBox = new VBox(5, alphaLabel, svdAlphaSpinner);

        // Description for SVD
        Label svdDescLabel = new Label(
                "SVD embeds watermark in singular values.\n" +
                        "Higher α means stronger watermark but\n" +
                        "may affect image quality.");
        svdDescLabel.setStyle("-fx-font-size: 10px; -fx-font-style: italic;");
        svdDescLabel.setWrapText(true);

        options.getChildren().addAll(alphaBox, svdDescLabel);
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
        Label tableLabel = new Label("Results:");
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
     * Creates the attack simulation panel with improved layout and parameter handling
     */
    private VBox createAttackControls() {
        VBox attackBox = new VBox(16);
        attackBox.setPadding(new Insets(10));
        attackBox.setFillWidth(true);

        Label titleLabel = new Label("Attack Simulation");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // ComboBox for selecting attack type
        Label attackTypeLabel = new Label("Select Attack Type:");
        attackTypeCombo = new ComboBox<>();
        attackTypeCombo.getItems().addAll(AttackType.values());
        attackTypeCombo.getSelectionModel().select(AttackType.JPEG_COMPRESSION);
        attackTypeCombo.setMaxWidth(Double.MAX_VALUE);

        VBox attackTypeBox = new VBox(5, attackTypeLabel, attackTypeCombo);

        // Attack description label
        Label descriptionLabel = new Label(AttackType.JPEG_COMPRESSION.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-style: italic;");

        // Update description when attack type changes
        attackTypeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                descriptionLabel.setText(newVal.getDescription());
                updateAttackParameters();
            }
        });

        // Parameter controls header
        Label parametersHeaderLabel = new Label("Attack Parameters:");
        parametersHeaderLabel.setStyle("-fx-font-weight: bold;");

        // Small separator
        Separator parametersSeparator = new Separator(Orientation.HORIZONTAL);
        parametersSeparator.setPadding(new Insets(5, 0, 5, 0));

        // Parameter controls container
        attackParametersBox = new VBox(15);
        attackParametersBox.setPadding(new Insets(5, 0, 10, 0));

        // Create initial parameter controls
        updateAttackParameters();

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

            // Get the selected attack
            AttackType selectedAttackType = attackTypeCombo.getValue();
            AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(selectedAttackType);

            // Get current RGB image
            BufferedImage currentImage = watermarkedProcess.getRGBImage();

            // Gather parameters from UI
            Map<String, Object> attackParams = getAttackParameters(selectedAttackType);

            try {
                // Apply the attack
                showStatus("Applying attack: " + selectedAttackType.getDisplayName(), "info");
                BufferedImage attackedImage = attack.apply(currentImage, attackParams);

                // Create a new process with the attacked image
                watermarkedProcess = new Process(attackedImage);

                // Update the image view
                updateImageViews();

                // Show success message
                showStatus("Attack applied: " + selectedAttackType.getDisplayName(), "success");
            } catch (Exception ex) {
                Logger.error("Error applying attack: " + ex.getMessage());
                showStatus("Error applying attack: " + ex.getMessage(), "error");
            }
        });

        // Main separator
        Separator mainSeparator = new Separator(Orientation.HORIZONTAL);
        mainSeparator.setPadding(new Insets(10, 0, 10, 0));

        // Add everything to attack box
        attackBox.getChildren().addAll(
                titleLabel,
                attackTypeBox,
                descriptionLabel,
                mainSeparator,
                parametersHeaderLabel,
                parametersSeparator,
                attackParametersBox,
                applyAttackButton
        );

        return attackBox;
    }

    /**
     * Updates the attack parameter controls based on the selected attack type.
     */
    private void updateAttackParameters() {
        // Clear existing controls
        attackParametersBox.getChildren().clear();
        attackParameterControls.clear();

        // Get the selected attack type
        AttackType selectedAttackType = attackTypeCombo.getValue();
        if (selectedAttackType == null) {
            return;
        }

        // Get the attack instance
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(selectedAttackType);

        // Get default parameters
        Map<String, Object> defaultParams = attack.getDefaultParameters();

        // Create controls for each parameter
        for (Map.Entry<String, Object> param : defaultParams.entrySet()) {
            String paramName = param.getKey();
            Object paramValue = param.getValue();

            // Create parameter label with proper capitalization
            String displayName = paramName.substring(0, 1).toUpperCase() + paramName.substring(1);
            Label paramLabel = new Label(displayName + ":");

            // Create appropriate control based on parameter type
            Control paramControl = null;

            if (paramValue instanceof Number) {
                // Numeric parameter (create spinner)
                if (paramValue instanceof Integer) {
                    // Integer parameter
                    int value = (Integer) paramValue;
                    Spinner<Integer> spinner;

                    // Set appropriate range based on parameter name
                    if (paramName.contains("radius")) {
                        spinner = new Spinner<>(1, 5, value);
                    } else if (paramName.contains("level")) {
                        spinner = new Spinner<>(1, 9, value);
                    } else if (paramName.contains("quality")) {
                        spinner = new Spinner<>(1, 100, value);
                    } else {
                        spinner = new Spinner<>(0, 100, value);
                    }

                    spinner.setEditable(true);
                    spinner.setPrefWidth(120);
                    paramControl = spinner;

                } else if (paramValue instanceof Double || paramValue instanceof Float) {
                    // Double/Float parameter
                    double value = ((Number) paramValue).doubleValue();
                    Spinner<Double> spinner;

                    // Set appropriate range based on parameter name
                    if (paramName.contains("percentage")) {
                        spinner = new Spinner<>(0.0, 0.5, value, 0.05);
                    } else if (paramName.contains("scale")) {
                        spinner = new Spinner<>(0.1, 1.0, value, 0.05);
                    } else if (paramName.contains("amount")) {
                        spinner = new Spinner<>(0.0, 2.0, value, 0.1);
                    } else if (paramName.contains("stddev")) {
                        spinner = new Spinner<>(0.0, 50.0, value, 1.0);
                    } else {
                        spinner = new Spinner<>(0.0, 100.0, value, 1.0);
                    }

                    spinner.setEditable(true);
                    spinner.setPrefWidth(120);
                    paramControl = spinner;
                }

            } else if (paramValue instanceof Boolean) {
                // Boolean parameter (create checkbox)
                boolean value = (Boolean) paramValue;
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(value);
                paramControl = checkBox;

            } else if (paramValue instanceof String) {
                // String parameter (create combobox or text field)
                String value = (String) paramValue;

                if (paramName.contains("direction")) {
                    // For direction parameters, create a combo box
                    ComboBox<String> comboBox = new ComboBox<>();
                    comboBox.getItems().addAll("horizontal", "vertical");
                    comboBox.setValue(value);
                    comboBox.setMaxWidth(Double.MAX_VALUE);
                    paramControl = comboBox;
                } else {
                    // Generic string parameter
                    TextField textField = new TextField(value);
                    paramControl = textField;
                }
            }

            // If we created a control, add it to the UI
            if (paramControl != null) {
                // Store control for later access
                attackParameterControls.put(paramName, paramControl);

                // Create layout
                HBox paramBox = new HBox(10, paramLabel, paramControl);
                paramBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(paramControl, Priority.ALWAYS);

                // Add to container
                attackParametersBox.getChildren().add(paramBox);
            }
        }

        // If no parameters, show a message
        if (attackParametersBox.getChildren().isEmpty()) {
            Label noParamsLabel = new Label("No parameters for this attack");
            noParamsLabel.setStyle("-fx-font-style: italic;");
            attackParametersBox.getChildren().add(noParamsLabel);
        }
    }

    /**
     * Gets the current attack parameters from the UI controls.
     *
     * @param attackType The selected attack type
     * @return Map of parameter names to values
     */
    private Map<String, Object> getAttackParameters(AttackType attackType) {
        Map<String, Object> params = new HashMap<>();

        // Get the default parameters to know the types
        AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(attackType);
        Map<String, Object> defaultParams = attack.getDefaultParameters();

        // Populate parameters from UI controls
        for (Map.Entry<String, Object> param : defaultParams.entrySet()) {
            String paramName = param.getKey();
            Object defaultValue = param.getValue();
            Control control = attackParameterControls.get(paramName);

            if (control != null) {
                if (defaultValue instanceof Integer && control instanceof Spinner) {
                    @SuppressWarnings("unchecked")
                    Spinner<Integer> spinner = (Spinner<Integer>) control;
                    params.put(paramName, spinner.getValue());

                } else if ((defaultValue instanceof Double || defaultValue instanceof Float) &&
                        control instanceof Spinner) {
                    @SuppressWarnings("unchecked")
                    Spinner<Double> spinner = (Spinner<Double>) control;
                    params.put(paramName, spinner.getValue());

                } else if (defaultValue instanceof Boolean && control instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) control;
                    params.put(paramName, checkBox.isSelected());

                } else if (defaultValue instanceof String) {
                    if (control instanceof ComboBox) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> comboBox = (ComboBox<String>) control;
                        params.put(paramName, comboBox.getValue());
                    } else if (control instanceof TextField) {
                        TextField textField = (TextField) control;
                        params.put(paramName, textField.getText());
                    }
                }
            } else {
                // Fall back to default value if no control exists
                params.put(paramName, defaultValue);
            }
        }

        return params;
    }

    /**
     * Opens the watermark configuration dialog to create or customize watermarks
     */
    private void openWatermarkConfigDialog() {
        try {
            // Get current watermark dimensions
            int width = watermarkImage != null ? watermarkImage.getWidth() : 64;
            int height = watermarkImage != null ? watermarkImage.getHeight() : 64;

            try {
                width = Integer.parseInt(watermarkWidthField.getText());
                height = Integer.parseInt(watermarkHeightField.getText());
            } catch (NumberFormatException e) {
                // Use defaults if fields contain invalid values
            }

            // Create a final array to store the dialog reference
            final WatermarkConfigurationDialog[] dialogRef = new WatermarkConfigurationDialog[1];

            // Create and show the configuration dialog
            WatermarkConfigurationDialog dialog = new WatermarkConfigurationDialog(
                    this, watermarkImage, width, height,
                    (newWatermark, newWidth, newHeight) -> {
                        // Handle selected watermark
                        watermarkImage = newWatermark;
                        watermarkWidthField.setText(String.valueOf(newWidth));
                        watermarkHeightField.setText(String.valueOf(newHeight));

                        // Update the configuration description based on the last selection
                        if (newWatermark != null) {
                            // Get the selected preset from the dialog
                            WatermarkConfigurationDialog.WatermarkPreset selectedPreset = dialogRef[0].getLastSelectedPreset();
                            if (selectedPreset != null) {
                                watermarkConfigDesc = selectedPreset.toString();
                            } else {
                                watermarkConfigDesc = "Custom " + newWidth + "x" + newHeight;
                            }
                        }

                        // Update the UI
                        updateImageViews();
                    }
            );

            // Store the dialog reference
            dialogRef[0] = dialog;

            dialog.showAndWait();

        } catch (Exception e) {
            Logger.error("Error opening watermark configuration dialog: " + e.getMessage());
            showStatus("Error opening configuration dialog: " + e.getMessage(), "error");
        }
    }

    /**
     * Shows a simple about dialog with information about the application
     */
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Watermarking Tool");
        alert.setHeaderText("Image Watermarking Application");
        alert.setContentText("This application demonstrates various watermarking techniques and their " +
                "robustness against common attacks.\n\n" +
                "Supported methods: LSB, DCT, DWT, SVD\n" +
                "Created for ZMD course project at BUT FEEC");
        alert.showAndWait();
    }

    /**
     * Creates the results table for tracking attack performance
     */
    private TableView<WatermarkResult> createResultsTable() {
        TableView<WatermarkResult> table = new TableView<>();

        // Make table columns resize to fit content width
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create columns
        TableColumn<WatermarkResult, Integer> idCol = new TableColumn<>("Test #");
        idCol.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getTestId()).asObject());
        idCol.setPrefWidth(50);

        TableColumn<WatermarkResult, String> attackCol = new TableColumn<>("Attack");
        attackCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAttackName()));
        attackCol.setPrefWidth(120);

        TableColumn<WatermarkResult, String> paramsCol = new TableColumn<>("Parameters");
        paramsCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAttackParameters()));
        paramsCol.setPrefWidth(100);

        TableColumn<WatermarkResult, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMethod()));
        methodCol.setPrefWidth(80);

        TableColumn<WatermarkResult, String> compCol = new TableColumn<>("Component");
        compCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getComponent()));
        compCol.setPrefWidth(80);

        // Add new watermark config column
        TableColumn<WatermarkResult, String> configCol = new TableColumn<>("Watermark Config");
        configCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getWatermarkConfig()));
        configCol.setPrefWidth(120);

        TableColumn<WatermarkResult, String> berCol = new TableColumn<>("BER");
        berCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.4f", cellData.getValue().getBer())));
        berCol.setPrefWidth(60);

        // Set cell factory to color cells based on BER value
        berCol.setCellFactory(column -> new TableCell<WatermarkResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        double ber = Double.parseDouble(item);
                        if (ber < 0.05) {
                            setStyle("-fx-background-color: lightgreen;");
                        } else if (ber < 0.15) {
                            setStyle("-fx-background-color: lightyellow;");
                        } else if (ber < 0.30) {
                            setStyle("-fx-background-color: #FFCC80;"); // Light orange
                        } else {
                            setStyle("-fx-background-color: #FFCDD2;"); // Light red
                        }
                    } catch (NumberFormatException e) {
                        setStyle("");
                    }
                }
            }
        });

        TableColumn<WatermarkResult, String> ncCol = new TableColumn<>("NC");
        ncCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.4f", cellData.getValue().getNc())));
        ncCol.setPrefWidth(60);

        // Set cell factory to color cells based on NC value
        ncCol.setCellFactory(column -> new TableCell<WatermarkResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        double nc = Double.parseDouble(item);
                        if (nc > 0.95) {
                            setStyle("-fx-background-color: lightgreen;");
                        } else if (nc > 0.85) {
                            setStyle("-fx-background-color: lightyellow;");
                        } else if (nc > 0.75) {
                            setStyle("-fx-background-color: #FFCC80;"); // Light orange
                        } else {
                            setStyle("-fx-background-color: #FFCDD2;"); // Light red
                        }
                    } catch (NumberFormatException e) {
                        setStyle("");
                    }
                }
            }
        });

        // Add a quality rating column
        TableColumn<WatermarkResult, String> qualityCol = new TableColumn<>("Quality");
        qualityCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getQualityRating()));
        qualityCol.setPrefWidth(80);

        // Color code quality ratings
        qualityCol.setCellFactory(column -> new TableCell<WatermarkResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Excellent":
                            setStyle("-fx-background-color: #C8E6C9;"); // Light green
                            break;
                        case "Very Good":
                            setStyle("-fx-background-color: #DCEDC8;"); // Lighter green
                            break;
                        case "Good":
                            setStyle("-fx-background-color: #FFF9C4;"); // Light yellow
                            break;
                        case "Fair":
                            setStyle("-fx-background-color: #FFE0B2;"); // Light orange
                            break;
                        case "Poor":
                            setStyle("-fx-background-color: #FFCCBC;"); // Light red-orange
                            break;
                        case "Failed":
                            setStyle("-fx-background-color: #FFCDD2;"); // Light red
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });

        // Add a robustness rating column
        TableColumn<WatermarkResult, String> robustnessCol = new TableColumn<>("Robustness");
        robustnessCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRobustnessLevel()));
        robustnessCol.setPrefWidth(100);

        // Add columns to table
        table.getColumns().addAll(idCol, attackCol, paramsCol, methodCol, compCol, configCol,
                berCol, ncCol, qualityCol, robustnessCol);

        // Add right-click context menu
        ContextMenu contextMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Delete Selected");
        deleteItem.setOnAction(e -> {
            WatermarkResult selectedResult = table.getSelectionModel().getSelectedItem();
            if (selectedResult != null) {
                results.remove(selectedResult);
                table.getItems().remove(selectedResult);
            }
        });

        MenuItem deleteAllItem = new MenuItem("Delete All");
        deleteAllItem.setOnAction(e -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete all test results?",
                    ButtonType.YES, ButtonType.NO);
            confirmation.setTitle("Confirm Delete All");
            confirmation.setHeaderText("Delete All Test Results");
            confirmation.showAndWait()
                    .filter(response -> response == ButtonType.YES)
                    .ifPresent(response -> {
                        results.clear();
                        table.getItems().clear();
                    });
        });

        MenuItem repeatAttackItem = new MenuItem("Repeat Attack");
        repeatAttackItem.setOnAction(e -> {
            WatermarkResult selectedResult = table.getSelectionModel().getSelectedItem();
            if (selectedResult != null && watermarkedProcess != null) {
                // Set attack type in the UI
                AttackType attackType = selectedResult.getAttackType();
                attackTypeCombo.setValue(attackType);

                // Scroll to ensure the attack type is visible
                showStatus("Attack type set to: " + attackType.getDisplayName() +
                        ". Click 'Apply Attack' to repeat", "info");
            }
        });

        MenuItem setWatermarkingItem = new MenuItem("Set Watermarking Method");
        setWatermarkingItem.setOnAction(e -> {
            WatermarkResult selectedResult = table.getSelectionModel().getSelectedItem();
            if (selectedResult != null) {
                // Set method in the UI
                methodComboBox.setValue(selectedResult.getWatermarkType());

                // Set component
                try {
                    QualityType component = QualityType.valueOf(selectedResult.getComponent());
                    componentComboBox.setValue(component);
                } catch (IllegalArgumentException ex) {
                    // Component not found, ignore
                }

                showStatus("Watermarking method set to: " + selectedResult.getMethod(), "info");
            }
        });

        contextMenu.getItems().addAll(deleteItem, deleteAllItem, new SeparatorMenuItem(),
                repeatAttackItem, setWatermarkingItem);

        table.setContextMenu(contextMenu);

        // Double-click handler to repeat attack
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                WatermarkResult selectedResult = table.getSelectionModel().getSelectedItem();
                if (selectedResult != null) {
                    // Show details dialog
                    Alert details = new Alert(Alert.AlertType.INFORMATION);
                    details.setTitle("Test Result Details");
                    details.setHeaderText("Test #" + selectedResult.getTestId() + " - " + selectedResult.getAttackName());

                    // Create formatted content
                    StringBuilder content = new StringBuilder();
                    content.append("Time: ").append(selectedResult.getTimestamp()).append("\n\n");
                    content.append("Attack: ").append(selectedResult.getAttackName()).append("\n");
                    content.append("Parameters: ").append(selectedResult.getAttackParameters()).append("\n\n");
                    content.append("Method: ").append(selectedResult.getMethod()).append("\n");
                    content.append("Component: ").append(selectedResult.getComponent()).append("\n");
                    content.append("Parameter: ").append(selectedResult.getParameter()).append("\n");
                    content.append("Watermark Config: ").append(selectedResult.getWatermarkConfig()).append("\n\n");
                    content.append("BER: ").append(String.format("%.6f", selectedResult.getBer())).append("\n");
                    content.append("NC: ").append(String.format("%.6f", selectedResult.getNc())).append("\n");
                    content.append("Quality Rating: ").append(selectedResult.getQualityRating()).append("\n");
                    content.append("Robustness: ").append(selectedResult.getRobustnessLevel()).append("\n");

                    details.setContentText(content.toString());
                    details.showAndWait();
                }
            }
        });

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

                // Update watermark config description
                watermarkConfigDesc = "Custom " + file.getName();

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

                // Update watermark config
                watermarkConfigDesc = "Checkerboard " + size + "x" + size;

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
        watermarkConfigDesc = "Default Checkerboard 64x64";
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
                default:
                    throw new IllegalStateException("Unsupported component: " + component);
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
            } else if (method == WatermarkType.DWT) {
                double strength = dwtStrengthSpinner.getValue();
                String subband = dwtSubbandComboBox.getValue();

                watermarkedMatrix = watermarking.embed(
                        componentMatrix,
                        watermarkImage,
                        strength,
                        subband
                );
            } else if (method == WatermarkType.SVD) {
                double alpha = svdAlphaSpinner.getValue();

                watermarkedMatrix = watermarking.embed(
                        componentMatrix,
                        watermarkImage,
                        alpha
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
            } else if (method == WatermarkType.DWT) {
                double strength = dwtStrengthSpinner.getValue();
                String subband = dwtSubbandComboBox.getValue();

                extractedWatermark = watermarking.extract(
                        componentMatrix,
                        width,
                        height,
                        strength,
                        subband
                );
            } else if (method == WatermarkType.SVD) {
                double alpha = svdAlphaSpinner.getValue();

                extractedWatermark = watermarking.extract(
                        componentMatrix,
                        width,
                        height,
                        alpha
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

            // Get attack information
            AttackType attackType = attackTypeCombo.getValue();
            Map<String, Object> attackParams = getAttackParameters(attackType);
            AbstractWatermarkAttack attack = WatermarkAttackFactory.getAttack(attackType);
            String paramDescription = attack.getParametersDescription(attackParams);

            // Create a new result
            WatermarkResult result = new WatermarkResult(
                    attackType,
                    methodComboBox.getValue(),
                    componentComboBox.getValue().toString(),
                    (methodComboBox.getValue() == WatermarkType.LSB) ?
                            String.valueOf(bitPlaneSpinner.getValue()) :
                            String.valueOf(blockSizeSpinner.getValue()),
                    ber,
                    nc,
                    0.0, // PSNR (not calculated here)
                    0.0, // WNR (not calculated here)
                    paramDescription,
                    watermarkConfigDesc // Add watermark configuration description
            );

            // Add to table
            results.add(result);

            // Refresh table with sorted data
            ObservableList<WatermarkResult> items = FXCollections.observableArrayList(results);
            resultsTable.setItems(items);
            resultsTable.refresh();

            showStatus("Evaluation complete: BER=" + String.format("%.4f", ber) + ", NC=" + String.format("%.4f", nc), "success");
            Logger.info("Watermark evaluation completed: BER=" + ber + ", NC=" + nc);
        } catch (Exception e) {
            Logger.error("Error evaluating watermark: " + e.getMessage());
            showStatus("Error evaluating watermark: " + e.getMessage(), "error");
        }
    }

    /**
     * Exports test results to an Excel report or CSV file.
     */
    private void exportTestReport() {
        if (results.isEmpty()) {
            showStatus("No test results to export", "error");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Watermark Test Report");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv")
        );
        fileChooser.setInitialFileName("watermark_test_report.xlsx");

        File outputFile = fileChooser.showSaveDialog(this);
        if (outputFile != null) {
            try {
                String filePath = outputFile.getAbsolutePath();
                if (filePath.toLowerCase().endsWith(".csv")) {
                    // Export as CSV
                    WatermarkTestReport.exportToCsv(results, filePath);
                } else {
                    // Export as Excel
                    if (!filePath.toLowerCase().endsWith(".xlsx")) {
                        filePath += ".xlsx";
                    }
                    WatermarkTestReport.generateReport(results, filePath);
                }

                showStatus("Test report exported successfully to: " + filePath, "success");

            } catch (IOException e) {
                Logger.error("Error exporting test report: " + e.getMessage());
                showStatus("Error exporting test report: " + e.getMessage(), "error");
            }
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
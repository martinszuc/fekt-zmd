package graphics;
import Core.FileBindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.Logger;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Dialog for configuring watermarks used in the watermarking process.
 * Allows selection from predefined watermarks or custom watermark creation.
 */
public class WatermarkConfigurationDialog extends Stage {

    // Watermark dimensions
    private int watermarkWidth = 64;
    private int watermarkHeight = 64;

    // Current watermark image
    private BufferedImage currentWatermark;

    // UI components
    private ImageView previewImageView;
    private TextField widthField;
    private TextField heightField;
    private ComboBox<String> presetSelector;

    // Callback for when a watermark is selected
    private WatermarkSelectedCallback callback;

    // Predefined watermark types
    public enum WatermarkPreset {
        CHECKERBOARD("Checkerboard"),
        BUT_FEEC("BUT FEEC Text"),
        TRIANGLE("Triangle"),
        CUSTOM("Custom");

        private final String displayName;

        WatermarkPreset(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Interface for watermark selection callback
     */
    public interface WatermarkSelectedCallback {
        void onWatermarkSelected(BufferedImage watermark, int width, int height);
    }

    /**
     * Creates a new watermark configuration dialog.
     *
     * @param parentStage The parent stage
     * @param initialWatermark Initial watermark image (can be null)
     * @param initialWidth Initial watermark width
     * @param initialHeight Initial watermark height
     * @param callback Callback for watermark selection
     */
    public WatermarkConfigurationDialog(
            Stage parentStage,
            BufferedImage initialWatermark,
            int initialWidth,
            int initialHeight,
            WatermarkSelectedCallback callback) {

        this.watermarkWidth = initialWidth;
        this.watermarkHeight = initialHeight;
        this.currentWatermark = initialWatermark;
        this.callback = callback;

        // Configure stage
        initModality(Modality.APPLICATION_MODAL);
        initOwner(parentStage);
        setTitle("Watermark Configuration");
        setMinWidth(500);
        setMinHeight(500);

        // Create the layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Add menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // Main content area
        VBox content = createContent();
        root.setCenter(content);

        // Create the scene
        setScene(new Scene(root));

        // Update preview with initial watermark
        if (initialWatermark != null) {
            updatePreview(initialWatermark);
        } else {
            // Create default checkerboard pattern if no initial watermark
            createCheckerboardWatermark();
        }
    }

    /**
     * Creates the menu bar for the dialog
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");

        MenuItem importItem = new MenuItem("Import Watermark...");
        importItem.setOnAction(e -> importWatermark());

        MenuItem saveItem = new MenuItem("Save Watermark...");
        saveItem.setOnAction(e -> saveWatermark());

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setOnAction(e -> close());

        fileMenu.getItems().addAll(importItem, saveItem, new SeparatorMenuItem(), closeItem);

        // Add menus to menu bar
        menuBar.getMenus().add(fileMenu);

        return menuBar;
    }

    /**
     * Creates the main content area of the dialog
     */
    private VBox createContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // Title
        Text titleText = new Text("Watermark Configuration");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 16));

        // Preset selector
        Label presetLabel = new Label("Select Watermark Preset:");
        presetSelector = new ComboBox<>(FXCollections.observableArrayList(
                WatermarkPreset.CHECKERBOARD.toString(),
                WatermarkPreset.BUT_FEEC.toString(),
                WatermarkPreset.TRIANGLE.toString(),
                WatermarkPreset.CUSTOM.toString()
        ));
        presetSelector.setValue(WatermarkPreset.CHECKERBOARD.toString());
        presetSelector.setMaxWidth(Double.MAX_VALUE);

        // Add listener for preset selection
        presetSelector.setOnAction(e -> {
            String selected = presetSelector.getValue();
            if (selected.equals(WatermarkPreset.CHECKERBOARD.toString())) {
                createCheckerboardWatermark();
            } else if (selected.equals(WatermarkPreset.BUT_FEEC.toString())) {
                createButFeecWatermark();
            } else if (selected.equals(WatermarkPreset.TRIANGLE.toString())) {
                createTriangleWatermark();
            }
            // CUSTOM doesn't create a new watermark, it's for importing
        });

        // Dimensions
        HBox dimensionsBox = new HBox(10);
        dimensionsBox.setAlignment(Pos.CENTER_LEFT);

        Label dimensionsLabel = new Label("Dimensions:");
        widthField = new TextField(String.valueOf(watermarkWidth));
        widthField.setPrefWidth(70);
        widthField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                watermarkWidth = Integer.parseInt(newVal);
            } catch (NumberFormatException ex) {
                widthField.setText(oldVal);
            }
        });

        Label xLabel = new Label("×");

        heightField = new TextField(String.valueOf(watermarkHeight));
        heightField.setPrefWidth(70);
        heightField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                watermarkHeight = Integer.parseInt(newVal);
            } catch (NumberFormatException ex) {
                heightField.setText(oldVal);
            }
        });

        Button updateSizeButton = new Button("Update Size");
        updateSizeButton.setOnAction(e -> updateWatermarkSize());

        dimensionsBox.getChildren().addAll(dimensionsLabel, widthField, xLabel, heightField, updateSizeButton);

        // Preview section
        VBox previewBox = new VBox(10);
        previewBox.setAlignment(Pos.CENTER);

        Label previewLabel = new Label("Preview:");
        previewLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        previewImageView = new ImageView();
        previewImageView.setFitWidth(200);
        previewImageView.setFitHeight(200);
        previewImageView.setPreserveRatio(true);

        // Add a border to the preview image
        StackPane previewPane = new StackPane();
        previewPane.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1px; -fx-padding: 5px; -fx-background-color: #F8F8F8;");
        previewPane.getChildren().add(previewImageView);

        previewBox.getChildren().addAll(previewLabel, previewPane);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button importButton = new Button("Import Watermark");
        importButton.setOnAction(e -> importWatermark());

        Button acceptButton = new Button("Accept Watermark");
        acceptButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        acceptButton.setPrefWidth(150);
        acceptButton.setOnAction(e -> {
            if (currentWatermark != null && callback != null) {
                callback.onWatermarkSelected(currentWatermark, watermarkWidth, watermarkHeight);
                close();
            }
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(importButton, acceptButton, cancelButton);

        // Add all to content
        content.getChildren().addAll(
                titleText,
                new Separator(),
                presetLabel,
                presetSelector,
                new Separator(),
                dimensionsBox,
                previewBox,
                new Separator(),
                buttonBox
        );

        return content;
    }

    /**
     * Updates the watermark preview
     */
    private void updatePreview(BufferedImage watermark) {
        if (watermark == null) return;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(watermark, "png", out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            Image image = new Image(in);
            previewImageView.setImage(image);

            // Update dimensions fields
            widthField.setText(String.valueOf(watermark.getWidth()));
            heightField.setText(String.valueOf(watermark.getHeight()));

            // Store current watermark
            currentWatermark = watermark;
        } catch (IOException e) {
            Logger.error("Error creating preview: " + e.getMessage());
        }
    }

    /**
     * Updates the watermark size based on current dimensions
     */
    private void updateWatermarkSize() {
        if (currentWatermark == null) return;

        try {
            int width = Integer.parseInt(widthField.getText());
            int height = Integer.parseInt(heightField.getText());

            if (width <= 0 || height <= 0 || width > 512 || height > 512) {
                showAlert("Invalid Dimensions",
                        "Watermark dimensions must be between 1x1 and 512x512.");
                return;
            }

            watermarkWidth = width;
            watermarkHeight = height;

            // Create a resized version of the current watermark
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(currentWatermark, 0, 0, width, height, null);
            g2d.dispose();

            updatePreview(resized);

        } catch (NumberFormatException e) {
            showAlert("Invalid Dimensions", "Please enter valid numbers for width and height.");
        }
    }

    /**
     * Creates the default checkerboard watermark pattern
     */
    private void createCheckerboardWatermark() {
        try {
            int width = Integer.parseInt(widthField.getText());
            int height = Integer.parseInt(heightField.getText());

            BufferedImage watermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = watermark.createGraphics();

            // Fill with white
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Draw black checkboard pattern
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
            g2d.fillOval(width/2 - centerSize/2, height/2 - centerSize/2, centerSize, centerSize);

            g2d.dispose();

            // Update preview
            updatePreview(watermark);

            // Set selection to Checkerboard
            presetSelector.setValue(WatermarkPreset.CHECKERBOARD.toString());

        } catch (NumberFormatException e) {
            showAlert("Invalid Dimensions", "Please enter valid numbers for width and height.");
        }
    }

    /**
     * Creates the "BUT FEEC" text watermark
     */
    private void createButFeecWatermark() {
        try {
            int width = Integer.parseInt(widthField.getText());
            int height = Integer.parseInt(heightField.getText());

            // Create a JavaFX Canvas for text rendering
            Canvas canvas = new Canvas(width, height);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // Fill background with white
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, width, height);

            // Set text properties
            gc.setFill(Color.BLACK);
            gc.setTextAlign(TextAlignment.CENTER);

            // Calculate font size based on dimensions
            double fontSize = Math.min(width * 0.25, height / 8);
            gc.setFont(new Font("Arial", fontSize));

            // Draw vertical text - one letter per line
            String text = "BUT FEEC";
            double lineHeight = height / text.length();

            for (int i = 0; i < text.length(); i++) {
                String letter = text.substring(i, i+1);
                double y = i * lineHeight + lineHeight * 0.75;
                gc.fillText(letter, width/2, y);
            }

            // Convert JavaFX canvas to BufferedImage
            BufferedImage watermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // Save the canvas content to a PNG and then read it back
            // (This is a workaround since we can't directly convert JavaFX Canvas to BufferedImage)
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try {
                javafx.embed.swing.SwingFXUtils.fromFXImage(canvas.snapshot(null, null), watermark);
            } catch (Exception e) {
                // If SwingFXUtils fails, use a fallback method
                java.awt.Graphics2D g2d = watermark.createGraphics();
                g2d.setColor(java.awt.Color.WHITE);
                g2d.fillRect(0, 0, width, height);
                g2d.setColor(java.awt.Color.BLACK);

                java.awt.Font font = new java.awt.Font("Arial", java.awt.Font.BOLD, (int)fontSize);
                g2d.setFont(font);

                java.awt.FontMetrics metrics = g2d.getFontMetrics(font);
                for (int i = 0; i < text.length(); i++) {
                    String letter = text.substring(i, i+1);
                    int x = (width - metrics.stringWidth(letter)) / 2;
                    int y = (int)(i * lineHeight + lineHeight * 0.75);
                    g2d.drawString(letter, x, y);
                }
                g2d.dispose();
            }

            // Update preview
            updatePreview(watermark);

            // Set selection to BUT FEEC
            presetSelector.setValue(WatermarkPreset.BUT_FEEC.toString());

        } catch (NumberFormatException e) {
            showAlert("Invalid Dimensions", "Please enter valid numbers for width and height.");
        }
    }

    /**
     * Creates a black triangle watermark
     */
    private void createTriangleWatermark() {
        try {
            int width = Integer.parseInt(widthField.getText());
            int height = Integer.parseInt(heightField.getText());

            BufferedImage watermark = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = watermark.createGraphics();

            // Fill with white
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            // Draw black triangle
            g2d.setColor(java.awt.Color.BLACK);
            int[] xPoints = {width / 2, width / 4, 3 * width / 4};
            int[] yPoints = {height / 4, 3 * height / 4, 3 * height / 4};
            g2d.fillPolygon(xPoints, yPoints, 3);

            g2d.dispose();

            // Update preview
            updatePreview(watermark);

            // Set selection to Triangle
            presetSelector.setValue(WatermarkPreset.TRIANGLE.toString());

        } catch (NumberFormatException e) {
            showAlert("Invalid Dimensions", "Please enter valid numbers for width and height.");
        }
    }

    /**
     * Import a watermark from file
     */
    private void importWatermark() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Watermark Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(this);
        if (selectedFile != null) {
            try {
                BufferedImage imported = ImageIO.read(selectedFile);

                // Check if dimensions are too large
                if (imported.getWidth() > 512 || imported.getHeight() > 512) {
                    showAlert("Image Too Large",
                            "The imported image is too large. Maximum dimensions are 512x512 pixels.");
                    return;
                }

                // Update dimensions
                watermarkWidth = imported.getWidth();
                watermarkHeight = imported.getHeight();

                // Convert to black and white if needed
                BufferedImage bwImage = new BufferedImage(watermarkWidth, watermarkHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = bwImage.createGraphics();
                g2d.drawImage(imported, 0, 0, null);
                g2d.dispose();

                // Update preview
                updatePreview(bwImage);

                // Set selection to Custom
                presetSelector.setValue(WatermarkPreset.CUSTOM.toString());

                Logger.info("Imported watermark: " + selectedFile.getName() + " (" + watermarkWidth + "x" + watermarkHeight + ")");

            } catch (IOException e) {
                Logger.error("Error importing watermark: " + e.getMessage());
                showAlert("Import Error", "Failed to import the watermark image: " + e.getMessage());
            }
        }
    }

    /**
     * Save the current watermark to file
     */
    private void saveWatermark() {
        if (currentWatermark == null) {
            showAlert("No Watermark", "No watermark to save.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Watermark Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg"),
                new FileChooser.ExtensionFilter("BMP Files", "*.bmp")
        );
        fileChooser.setInitialFileName("watermark.png");

        File selectedFile = fileChooser.showSaveDialog(this);
        if (selectedFile != null) {
            try {
                String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf('.') + 1);
                ImageIO.write(currentWatermark, extension, selectedFile);
                Logger.info("Saved watermark to: " + selectedFile.getAbsolutePath());
            } catch (IOException e) {
                Logger.error("Error saving watermark: " + e.getMessage());
                showAlert("Save Error", "Failed to save the watermark image: " + e.getMessage());
            }
        }
    }

    /**
     * Shows an alert dialog with the given title and message
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
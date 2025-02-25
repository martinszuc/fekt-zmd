package graphics;

import Core.FileBindings;
import Core.Helper;
import enums.SamplingType;
import enums.TransformType;
import jpeg.Process;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {
    private Process processOriginal;
    private Process processModified;

    @FXML
    Button buttonInverseQuantize, buttonInverseToRGB, buttonInverseSample, buttonInverseTransform;
    @FXML
    Button buttonQuantize, buttonSample, buttonToYCbCr, buttonTransform;

    @FXML
    TextField qualityMSE, qualityPSNR, quantizeQualityField;
    @FXML
    Slider quantizeQuality;
    @FXML
    CheckBox shadesOfGrey, showSteps;
    @FXML
    Spinner<Integer> transformBlock;
    @FXML
    ComboBox<TransformType> transformType;
    @FXML
    ComboBox<SamplingType> sampling;

    /**
     * Initializes the window, sets default values, and fills UI elements.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Fill ComboBoxes with enum values
        sampling.getItems().setAll(SamplingType.values());
        transformType.getItems().setAll(TransformType.values());

        // Set default values
        sampling.getSelectionModel().select(SamplingType.S_4_4_4);
        transformType.getSelectionModel().select(TransformType.DCT);
        quantizeQuality.setValue(50);

        // Populate Spinner with block sizes
        ObservableList<Integer> blocks = FXCollections.observableArrayList(2, 4, 8, 16, 32, 64, 128, 256, 512);
        SpinnerValueFactory<Integer> spinnerValues = new SpinnerValueFactory.ListSpinnerValueFactory<>(blocks);
        spinnerValues.setValue(8);
        transformBlock.setValueFactory(spinnerValues);

        // Allow only numeric input in quantizeQualityField
        quantizeQualityField.setTextFormatter(new TextFormatter<>(Helper.NUMBER_FORMATTER));
        // Bind Slider to TextField
        quantizeQualityField.textProperty().bindBidirectional(quantizeQuality.valueProperty(), NumberFormat.getIntegerInstance());

        // Load default image
        try {
            File defaultImage = new File(FileBindings.defaultImage);
            processOriginal = new Process(ImageIO.read(defaultImage));
            processModified = new Process(ImageIO.read(defaultImage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the application window.
     */
    public void close() {
        Stage stage = ((Stage) buttonSample.getScene().getWindow());
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    /**
     * Close all open image windows.
     */
    public void closeWindows() {
        Dialogs.closeAllWindows();
    }

    /**
     * Show the original image.
     */
    public void showOriginal() {
        if (processOriginal != null) {
            Dialogs.showImageInWindow(processOriginal.getImage(), "Original");
        }
    }

    /**
     * Change the current image by selecting one from the file chooser.
     */
    public void changeImage() {
        File imageFile = Dialogs.openFile();
        if (imageFile != null) {
            try {
                processOriginal = new Process(ImageIO.read(imageFile));
                processModified = new Process(ImageIO.read(imageFile));
                showOriginal();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reset the modified image to the original.
     */
    public void reset() {
        if (processOriginal != null) {
            processModified = new Process(processOriginal.getImage());
        }
    }

    /**
     * Convert the modified image from RGB to YCbCr.
     * (Does not display an image; it just stores the conversion results.)
     * Also converts the original so that its Y, Cb, Cr values become available.
     */
    public void convertToYCbCr() {
        if (processModified != null) {
            processOriginal.convertToYCbCr();
            processModified.convertToYCbCr();
        }
    }

    /**
     * Convert the modified image from YCbCr back to RGB.
     * (Does not display an image; it just updates the stored RGB values.)
     */
    public void convertToRGB() {
        if (processModified != null) {
            processModified.convertToRGB();
        }
    }

    // --- ORIGINAL IMAGE CHANNELS ---
    public void showRedOriginal() {
        if (processOriginal != null) {
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getRed(), "RED"), "Red - Original");
        }
    }

    public void showGreenOriginal() {
        if (processOriginal != null) {
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getGreen(), "GREEN"), "Green - Original");
        }
    }

    public void showBlueOriginal() {
        if (processOriginal != null) {
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getBlue(), "BLUE"), "Blue - Original");
        }
    }

    public void showYOriginal() {
        if (processOriginal != null && processOriginal.isYCbCrConverted()) {
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getY()), "Y - Original");
        }
    }

    public void showCbOriginal() {
        if (processOriginal != null && processOriginal.isYCbCrConverted()) {
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getCb()), "Cb - Original");
        }
    }

    public void showCrOriginal() {
        if (processOriginal != null && processOriginal.isYCbCrConverted()) {
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getCr()), "Cr - Original");
        }
    }

    // --- MODIFIED IMAGE CHANNELS ---
    public void showRedModified() {
        if (processModified != null) {
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getRed(), "RED"), "Red - Modified");
        }
    }

    public void showGreenModified() {
        if (processModified != null) {
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getGreen(), "GREEN"), "Green - Modified");
        }
    }

    public void showBlueModified() {
        if (processModified != null) {
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getBlue(), "BLUE"), "Blue - Modified");
        }
    }

    public void showYModified() {
        if (processModified != null && processModified.isYCbCrConverted()) {
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getY()), "Y - Modified");
        }
    }

    public void showCbModified() {
        if (processModified != null && processModified.isYCbCrConverted()) {
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCb()), "Cb - Modified");
        }
    }

    public void showCrModified() {
        if (processModified != null && processModified.isYCbCrConverted()) {
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCr()), "Cr - Modified");
        }
    }

    public void showRGBModified() {
        if (processModified != null) {
            Dialogs.showImageInWindow(processModified.getRGBImage(), "RGB - Modified");
        }
    }

    // --- PLACEHOLDERS FOR FUTURE FEATURES ---
    public void sample() {}
    public void inverseSample() {}
    public void transform() {}
    public void inverseTransform() {}
    public void quantize() {}
    public void inverseQuantize() {}
    public void countQuality() {}
}

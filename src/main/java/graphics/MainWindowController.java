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
import utils.Logger;

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
     * Initializes the window and sets default values.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize logger
        Logger.init();
        Logger.info("Application started");

        sampling.getItems().setAll(SamplingType.values());
        transformType.getItems().setAll(TransformType.values());
        sampling.getSelectionModel().select(SamplingType.S_4_4_4);
        transformType.getSelectionModel().select(TransformType.DCT);
        quantizeQuality.setValue(50);

        ObservableList<Integer> blocks = FXCollections.observableArrayList(2, 4, 8, 16, 32, 64, 128, 256, 512);
        SpinnerValueFactory<Integer> spinnerValues = new SpinnerValueFactory.ListSpinnerValueFactory<>(blocks);
        spinnerValues.setValue(8);
        transformBlock.setValueFactory(spinnerValues);

        quantizeQualityField.setTextFormatter(new TextFormatter<>(Helper.NUMBER_FORMATTER));
        quantizeQualityField.textProperty().bindBidirectional(quantizeQuality.valueProperty(), NumberFormat.getIntegerInstance());

        try {
            File defaultImage = new File(FileBindings.defaultImage);
            Logger.info("Loading default image: " + defaultImage.getAbsolutePath());
            processOriginal = new Process(ImageIO.read(defaultImage));
            processModified = new Process(ImageIO.read(defaultImage));
        } catch (IOException e) {
            Logger.error("Error loading default image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        Logger.info("Closing application");
        Stage stage = ((Stage) buttonSample.getScene().getWindow());
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    public void closeWindows() {
        Logger.info("Closing all image windows");
        Dialogs.closeAllWindows();
    }

    public void showOriginal() {
        if (processOriginal != null) {
            Logger.info("Displaying original image");
            Dialogs.showImageInWindow(processOriginal.getImage(), "Original");
        }
    }

    public void changeImage() {
        Logger.info("Opening file chooser dialog for new image");
        File imageFile = Dialogs.openFile();
        if (imageFile != null) {
            try {
                Logger.info("Loading new image: " + imageFile.getAbsolutePath());
                processOriginal = new Process(ImageIO.read(imageFile));
                processModified = new Process(ImageIO.read(imageFile));
                showOriginal();
            } catch (IOException e) {
                Logger.error("Error loading new image: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Logger.info("No image selected");
        }
    }

    public void reset() {
        if (processOriginal != null) {
            Logger.info("Resetting modified image to original state");
            processModified = new Process(processOriginal.getImage());
        }
    }

    public void convertToYCbCr() {
        if (processModified != null) {
            Logger.info("Converting images to YCbCr");
            processOriginal.convertToYCbCr();
            processModified.convertToYCbCr();
        }
    }

    public void convertToRGB() {
        if (processModified != null) {
            Logger.info("Converting modified image back to RGB");
            processModified.convertToRGB();
        }
    }

    // --- Original Image Channels ---
    public void showRedOriginal() {
        if (processOriginal != null) {
            Logger.info("Displaying Red channel of original image");
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getRed(), "RED"), "Red - Original");
        }
    }

    public void showGreenOriginal() {
        if (processOriginal != null) {
            Logger.info("Displaying Green channel of original image");
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getGreen(), "GREEN"), "Green - Original");
        }
    }

    public void showBlueOriginal() {
        if (processOriginal != null) {
            Logger.info("Displaying Blue channel of original image");
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getBlue(), "BLUE"), "Blue - Original");
        }
    }

    public void showYOriginal() {
        if (processOriginal != null && processOriginal.isYCbCrConverted()) {
            Logger.info("Displaying Y channel of original image");
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getY()), "Y - Original");
        }
    }

    public void showCbOriginal() {
        if (processOriginal != null && processOriginal.isYCbCrConverted()) {
            Logger.info("Displaying Cb channel of original image");
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getCb()), "Cb - Original");
        }
    }

    public void showCrOriginal() {
        if (processOriginal != null && processOriginal.isYCbCrConverted()) {
            Logger.info("Displaying Cr channel of original image");
            Dialogs.showImageInWindow(processOriginal.getChannelImage(processOriginal.getCr()), "Cr - Original");
        }
    }

    // --- Modified Image Channels ---
    public void showRedModified() {
        if (processModified != null) {
            Logger.info("Displaying Red channel of modified image");
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getRed(), "RED"), "Red - Modified");
        }
    }

    public void showGreenModified() {
        if (processModified != null) {
            Logger.info("Displaying Green channel of modified image");
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getGreen(), "GREEN"), "Green - Modified");
        }
    }

    public void showBlueModified() {
        if (processModified != null) {
            Logger.info("Displaying Blue channel of modified image");
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getBlue(), "BLUE"), "Blue - Modified");
        }
    }

    public void showYModified() {
        if (processModified != null && processModified.isYCbCrConverted()) {
            Logger.info("Displaying Y channel of modified image");
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getY()), "Y - Modified");
        }
    }

    public void showCbModified() {
        if (processModified != null && processModified.isYCbCrConverted()) {
            Logger.info("Displaying Cb channel of modified image");
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCb()), "Cb - Modified");
        }
    }

    public void showCrModified() {
        if (processModified != null && processModified.isYCbCrConverted()) {
            Logger.info("Displaying Cr channel of modified image");
            Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCr()), "Cr - Modified");
        }
    }

    public void showRGBModified() {
        if (processModified != null) {
            Logger.info("Displaying full RGB modified image");
            Dialogs.showImageInWindow(processModified.getRGBImage(), "RGB - Modified");
        }
    }

    public void sample() {
        if (processModified != null && processModified.isYCbCrConverted()) {
            SamplingType selectedSampling = sampling.getSelectionModel().getSelectedItem();
            Logger.info("Performing downsampling with pattern: " + selectedSampling);

            processModified.downSample(selectedSampling);

            if (showSteps.isSelected()) {
                Logger.info("Displaying downsampled channels (show steps enabled)");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCb()), "Cb - Downsampled");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCr()), "Cr - Downsampled");
            }
        } else {
            Logger.warning("Cannot downsample - YCbCr conversion not performed");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Downsample");
            alert.setHeaderText(null);
            alert.setContentText("You must convert to YCbCr first before downsampling.");
            alert.showAndWait();
        }
    }

    public void inverseSample() {
        if (processModified != null && processModified.isYCbCrConverted()) {
            SamplingType selectedSampling = sampling.getSelectionModel().getSelectedItem();
            Logger.info("Performing upsampling with pattern: " + selectedSampling);

            processModified.upSample(selectedSampling);

            if (showSteps.isSelected()) {
                Logger.info("Displaying upsampled channels (show steps enabled)");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCb()), "Cb - Upsampled");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCr()), "Cr - Upsampled");
            }
        } else {
            Logger.warning("Cannot upsample - YCbCr conversion not performed");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Upsample");
            alert.setHeaderText(null);
            alert.setContentText("You must convert to YCbCr first before upsampling.");
            alert.showAndWait();
        }
    }

    // Placeholders for future features
    public void transform() {}
    public void inverseTransform() {}
    public void quantize() {}
    public void inverseQuantize() {}
    public void countQuality() {}
}

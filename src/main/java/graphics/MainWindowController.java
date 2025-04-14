package graphics;

import Core.FileBindings;
import Core.Helper;
import enums.QualityType;
import enums.SamplingType;
import enums.TransformType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import jpeg.Process;
import jpeg.Quality;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jpeg.Transform;
import utils.Logger;
import Jama.Matrix;

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
    TextField quantizeQualityField;
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
    @FXML
    ComboBox<QualityType> qualityType;
    @FXML
    TextField qualityMSE, qualityMAE, qualitySAE, qualityPSNR;
    @FXML
    TextField qualitySSIM, qualityMSSIM;
    @FXML
    ComboBox<QualityType> ssimQualityType;
    @FXML
    Button countPSNRButton, countSSIMButton;

    /**
     * Initializes the window and sets default values.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize logger
        Logger.init();
        Logger.info("Application started");

        // Initialize sampling and transform types
        sampling.getItems().setAll(SamplingType.values());
        transformType.getItems().setAll(TransformType.values());
        sampling.getSelectionModel().select(SamplingType.S_4_4_4);
        transformType.getSelectionModel().select(TransformType.DCT);
        quantizeQuality.setValue(50);

        // Initialize quality type ComboBox for PSNR
        qualityType.getItems().setAll(
                QualityType.RGB,
                QualityType.RED,
                QualityType.GREEN,
                QualityType.BLUE,
                QualityType.Y,
                QualityType.CB,
                QualityType.CR,
                QualityType.YCBCR
        );
        qualityType.getSelectionModel().select(QualityType.RGB);

        // Initialize quality type ComboBox for SSIM
        ssimQualityType.getItems().setAll(
                QualityType.Y,
                QualityType.CB,
                QualityType.CR
        );
        ssimQualityType.getSelectionModel().select(QualityType.Y);

        // Initialize transform block sizes
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

    // Original Image Channels
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

    // Modified Image Channels
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

    /**
     * Calculates quality metrics based on the selected component
     */
    @FXML
    public void countQuality() {
        Logger.info("Calculating quality metrics");

        if (processOriginal == null || processModified == null) {
            Logger.warning("Cannot calculate quality: no images loaded");
            showAlert("Cannot Calculate Quality",
                    "Please load an image first before calculating quality metrics.");
            return;
        }

        QualityType selectedType = qualityType.getSelectionModel().getSelectedItem();

        if (selectedType == null) {
            Logger.warning("No quality component selected");
            return;
        }

        Logger.info("Selected quality component: " + selectedType);

        try {
            double mse = 0, mae = 0, sae = 0, psnr = 0;

            switch (selectedType) {
                case RGB:
                    if (processOriginal.getRed() == null || processModified.getRed() == null) {
                        Logger.warning("RGB components not available");
                        showAlert("Quality Calculation Error", "RGB components not available");
                        return;
                    }

                    double[][] origRed = convertIntToDouble(processOriginal.getRed());
                    double[][] modRed = convertIntToDouble(processModified.getRed());
                    double mseRed = Quality.countMSE(origRed, modRed);

                    double[][] origGreen = convertIntToDouble(processOriginal.getGreen());
                    double[][] modGreen = convertIntToDouble(processModified.getGreen());
                    double mseGreen = Quality.countMSE(origGreen, modGreen);

                    double[][] origBlue = convertIntToDouble(processOriginal.getBlue());
                    double[][] modBlue = convertIntToDouble(processModified.getBlue());
                    double mseBlue = Quality.countMSE(origBlue, modBlue);

                    mse = (mseRed + mseGreen + mseBlue) / 3.0;
                    mae = (Quality.countMAE(origRed, modRed) + Quality.countMAE(origGreen, modGreen) +
                            Quality.countMAE(origBlue, modBlue)) / 3.0;
                    sae = Quality.countSAE(origRed, modRed) + Quality.countSAE(origGreen, modGreen) +
                            Quality.countSAE(origBlue, modBlue);
                    psnr = Quality.countPSNRforRGB(mseRed, mseGreen, mseBlue);
                    break;

                case RED:
                    if (processOriginal.getRed() == null || processModified.getRed() == null) {
                        Logger.warning("Red component not available");
                        showAlert("Quality Calculation Error", "Red component not available");
                        return;
                    }

                    double[][] origRedOnly = convertIntToDouble(processOriginal.getRed());
                    double[][] modRedOnly = convertIntToDouble(processModified.getRed());
                    mse = Quality.countMSE(origRedOnly, modRedOnly);
                    mae = Quality.countMAE(origRedOnly, modRedOnly);
                    sae = Quality.countSAE(origRedOnly, modRedOnly);
                    psnr = Quality.countPSNR(mse);
                    break;

                case GREEN:
                    if (processOriginal.getGreen() == null || processModified.getGreen() == null) {
                        Logger.warning("Green component not available");
                        showAlert("Quality Calculation Error", "Green component not available");
                        return;
                    }

                    double[][] origGreenOnly = convertIntToDouble(processOriginal.getGreen());
                    double[][] modGreenOnly = convertIntToDouble(processModified.getGreen());
                    mse = Quality.countMSE(origGreenOnly, modGreenOnly);
                    mae = Quality.countMAE(origGreenOnly, modGreenOnly);
                    sae = Quality.countSAE(origGreenOnly, modGreenOnly);
                    psnr = Quality.countPSNR(mse);
                    break;

                case BLUE:
                    if (processOriginal.getBlue() == null || processModified.getBlue() == null) {
                        Logger.warning("Blue component not available");
                        showAlert("Quality Calculation Error", "Blue component not available");
                        return;
                    }

                    double[][] origBlueOnly = convertIntToDouble(processOriginal.getBlue());
                    double[][] modBlueOnly = convertIntToDouble(processModified.getBlue());
                    mse = Quality.countMSE(origBlueOnly, modBlueOnly);
                    mae = Quality.countMAE(origBlueOnly, modBlueOnly);
                    sae = Quality.countSAE(origBlueOnly, modBlueOnly);
                    psnr = Quality.countPSNR(mse);
                    break;

                case Y:
                    if (processOriginal.getY() == null || processModified.getY() == null) {
                        Logger.warning("Y component not available");
                        showAlert("Quality Calculation Error", "Y component not available. Convert to YCbCr first.");
                        return;
                    }

                    double[][] origY = processOriginal.getY().getArray();
                    double[][] modY = processModified.getY().getArray();
                    mse = Quality.countMSE(origY, modY);
                    mae = Quality.countMAE(origY, modY);
                    sae = Quality.countSAE(origY, modY);
                    psnr = Quality.countPSNR(mse);
                    break;

                case CB:
                    if (processOriginal.getCb() == null || processModified.getCb() == null) {
                        Logger.warning("Cb component not available");
                        showAlert("Quality Calculation Error", "Cb component not available. Convert to YCbCr first.");
                        return;
                    }

                    double[][] origCb = processOriginal.getCb().getArray();
                    double[][] modCb = processModified.getCb().getArray();
                    mse = Quality.countMSE(origCb, modCb);
                    mae = Quality.countMAE(origCb, modCb);
                    sae = Quality.countSAE(origCb, modCb);
                    psnr = Quality.countPSNR(mse);
                    break;

                case CR:
                    if (processOriginal.getCr() == null || processModified.getCr() == null) {
                        Logger.warning("Cr component not available");
                        showAlert("Quality Calculation Error", "Cr component not available. Convert to YCbCr first.");
                        return;
                    }

                    double[][] origCr = processOriginal.getCr().getArray();
                    double[][] modCr = processModified.getCr().getArray();
                    mse = Quality.countMSE(origCr, modCr);
                    mae = Quality.countMAE(origCr, modCr);
                    sae = Quality.countSAE(origCr, modCr);
                    psnr = Quality.countPSNR(mse);
                    break;

                case YCBCR:
                    if (processOriginal.getY() == null || processModified.getY() == null) {
                        Logger.warning("YCbCr components not available");
                        showAlert("Quality Calculation Error", "YCbCr components not available. Convert to YCbCr first.");
                        return;
                    }

                    double[][] origYComp = processOriginal.getY().getArray();
                    double[][] modYComp = processModified.getY().getArray();
                    double mseY = Quality.countMSE(origYComp, modYComp);

                    double[][] origCbComp = processOriginal.getCb().getArray();
                    double[][] modCbComp = processModified.getCb().getArray();
                    double mseCb = Quality.countMSE(origCbComp, modCbComp);

                    double[][] origCrComp = processOriginal.getCr().getArray();
                    double[][] modCrComp = processModified.getCr().getArray();
                    double mseCr = Quality.countMSE(origCrComp, modCrComp);

                    mse = (mseY + mseCb + mseCr) / 3.0;
                    mae = (Quality.countMAE(origYComp, modYComp) + Quality.countMAE(origCbComp, modCbComp) +
                            Quality.countMAE(origCrComp, modCrComp)) / 3.0;
                    sae = Quality.countSAE(origYComp, modYComp) + Quality.countSAE(origCbComp, modCbComp) +
                            Quality.countSAE(origCrComp, modCrComp);
                    psnr = Quality.countPSNR(mse);
                    break;
            }

            // Display results in text fields
            qualityMSE.setText(String.format("%.4f", mse));
            qualityMAE.setText(String.format("%.4f", mae));
            qualitySAE.setText(String.format("%.4f", sae));
            qualityPSNR.setText(String.format("%.4f", psnr));

            Logger.info("Quality metrics calculated successfully");
            Logger.info(String.format("MSE: %.4f, MAE: %.4f, SAE: %.4f, PSNR: %.4f", mse, mae, sae, psnr));

        } catch (Exception e) {
            Logger.error("Error calculating quality metrics: " + e.getMessage());
            e.printStackTrace();
            showAlert("Quality Calculation Error", "An error occurred: " + e.getMessage());
        }
    }

    @FXML
    public void countSSIM() {
        Logger.info("Calculating SSIM metrics");

        if (processOriginal == null || processModified == null) {
            Logger.warning("Cannot calculate SSIM: no images loaded");
            showAlert("Cannot Calculate SSIM",
                    "Please load an image first before calculating SSIM metrics.");
            return;
        }

        // Ensure images have been converted to YCbCr
        if (!processOriginal.isYCbCrConverted() || !processModified.isYCbCrConverted()) {
            Logger.warning("Cannot calculate SSIM: images not converted to YCbCr");
            showAlert("Cannot Calculate SSIM",
                    "Please convert the images to YCbCr first using the 'RGB -> YCbCr' button.");
            return;
        }

        QualityType selectedType = ssimQualityType.getSelectionModel().getSelectedItem();

        if (selectedType == null) {
            Logger.warning("No SSIM component selected");
            return;
        }

        Logger.info("Selected SSIM component: " + selectedType);

        try {
            double ssim = 0, mssim = 0;

            switch (selectedType) {
                case Y:
                    if (processOriginal.getY() == null || processModified.getY() == null) {
                        Logger.warning("Y component not available");
                        showAlert("SSIM Calculation Error", "Y component not available");
                        return;
                    }

                    ssim = Quality.countSSIM(processOriginal.getY(), processModified.getY());
                    mssim = Quality.countMSSIM(processOriginal.getY(), processModified.getY());
                    break;

                case CB:
                    if (processOriginal.getCb() == null || processModified.getCb() == null) {
                        Logger.warning("Cb component not available");
                        showAlert("SSIM Calculation Error", "Cb component not available");
                        return;
                    }

                    ssim = Quality.countSSIM(processOriginal.getCb(), processModified.getCb());
                    mssim = Quality.countMSSIM(processOriginal.getCb(), processModified.getCb());
                    break;

                case CR:
                    if (processOriginal.getCr() == null || processModified.getCr() == null) {
                        Logger.warning("Cr component not available");
                        showAlert("SSIM Calculation Error", "Cr component not available");
                        return;
                    }

                    ssim = Quality.countSSIM(processOriginal.getCr(), processModified.getCr());
                    mssim = Quality.countMSSIM(processOriginal.getCr(), processModified.getCr());
                    break;

                default:
                    Logger.warning("Invalid component selected for SSIM");
                    showAlert("SSIM Calculation Error", "Invalid component selected");
                    return;
            }

            // Display results in text fields
            qualitySSIM.setText(String.format("%.6f", ssim));
            qualityMSSIM.setText(String.format("%.6f", mssim));

            Logger.info("SSIM metrics calculated successfully");
            Logger.info(String.format("SSIM: %.6f, MSSIM: %.6f", ssim, mssim));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Not implemented yet")) {
                Logger.warning("SSIM calculation not implemented yet");
                showAlert("SSIM Calculation", "SSIM calculation is not implemented yet.");
            } else {
                Logger.error("Error calculating SSIM metrics: " + e.getMessage());
                e.printStackTrace();
                showAlert("SSIM Calculation Error", "An error occurred: " + e.getMessage());
            }
        } catch (Exception e) {
            Logger.error("Error calculating SSIM metrics: " + e.getMessage());
            e.printStackTrace();
            showAlert("SSIM Calculation Error", "An error occurred: " + e.getMessage());
        }
    }

    /**
     * Helper method to convert int array to double array
     */
    private double[][] convertIntToDouble(int[][] intArray) {
        if (intArray == null) return null;

        int height = intArray.length;
        int width = intArray[0].length;
        double[][] doubleArray = new double[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                doubleArray[i][j] = intArray[i][j];
            }
        }

        return doubleArray;
    }

    /**
     * Helper method to show alert dialogs
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void transform() {
        Logger.info("Transform button clicked");

        if (processModified == null) {
            Logger.warning("Cannot transform - no image loaded");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Transform");
            alert.setHeaderText(null);
            alert.setContentText("No image loaded. Please load an image first.");
            alert.showAndWait();
            return;
        }

        if (!processModified.isYCbCrConverted()) {
            Logger.warning("Cannot transform - YCbCr conversion not performed");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Transform");
            alert.setHeaderText(null);
            alert.setContentText("You must convert to YCbCr first before applying transforms.");
            alert.showAndWait();
            return;
        }

        TransformType selectedType = transformType.getSelectionModel().getSelectedItem();
        int blockSize = transformBlock.getValue();

        Logger.info("Starting " + selectedType + " transform with block size " + blockSize);
        long startTime = System.currentTimeMillis();

        try {
            // Get Y, Cb, and Cr channels
            Matrix y = processModified.getY();
            Matrix cb = processModified.getCb();
            Matrix cr = processModified.getCr();

            Logger.info("Transforming Y channel (" + y.getRowDimension() + "x" + y.getColumnDimension() + ")");
            y = Transform.transform(y, selectedType, blockSize);
            Logger.info("Transforming Cb channel (" + cb.getRowDimension() + "x" + cb.getColumnDimension() + ")");
            cb = Transform.transform(cb, selectedType, blockSize);
            Logger.info("Transforming Cr channel (" + cr.getRowDimension() + "x" + cr.getColumnDimension() + ")");
            cr = Transform.transform(cr, selectedType, blockSize);

            // Store transformed matrices back in process
            processModified.setY(y);
            processModified.setCb(cb);
            processModified.setCr(cr);

            long endTime = System.currentTimeMillis();
            Logger.info("Complete transform operation finished in " + (endTime - startTime) + "ms");

            if (showSteps.isSelected()) {
                Logger.info("Displaying transformed channels (show steps enabled)");
                Dialogs.showImageInWindow(processModified.getChannelImage(y), "Y - " + selectedType + " Transformed (Block Size: " + blockSize + ")");
                Dialogs.showImageInWindow(processModified.getChannelImage(cb), "Cb - " + selectedType + " Transformed (Block Size: " + blockSize + ")");
                Dialogs.showImageInWindow(processModified.getChannelImage(cr), "Cr - " + selectedType + " Transformed (Block Size: " + blockSize + ")");
            }
        } catch (Exception e) {
            Logger.error("Error during transform: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Transform Error");
            alert.setHeaderText(null);
            alert.setContentText("An error occurred during transformation: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void inverseTransform() {
        Logger.info("Inverse Transform button clicked");

        if (processModified == null) {
            Logger.warning("Cannot inverse transform - no image loaded");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Inverse Transform");
            alert.setHeaderText(null);
            alert.setContentText("No image loaded. Please load an image first.");
            alert.showAndWait();
            return;
        }

        if (!processModified.isYCbCrConverted()) {
            Logger.warning("Cannot inverse transform - YCbCr conversion not performed");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Inverse Transform");
            alert.setHeaderText(null);
            alert.setContentText("You must convert to YCbCr first before applying inverse transforms.");
            alert.showAndWait();
            return;
        }

        TransformType selectedType = transformType.getSelectionModel().getSelectedItem();
        int blockSize = transformBlock.getValue();

        Logger.info("Starting inverse " + selectedType + " transform with block size " + blockSize);
        long startTime = System.currentTimeMillis();

        try {
            // Get Y, Cb, and Cr channels
            Matrix y = processModified.getY();
            Matrix cb = processModified.getCb();
            Matrix cr = processModified.getCr();

            Logger.info("Inverse transforming Y channel (" + y.getRowDimension() + "x" + y.getColumnDimension() + ")");
            y = Transform.inverseTransform(y, selectedType, blockSize);

            Logger.info("Inverse transforming Cb channel (" + cb.getRowDimension() + "x" + cb.getColumnDimension() + ")");
            cb = Transform.inverseTransform(cb, selectedType, blockSize);

            Logger.info("Inverse transforming Cr channel (" + cr.getRowDimension() + "x" + cr.getColumnDimension() + ")");
            cr = Transform.inverseTransform(cr, selectedType, blockSize);

            // Store inverse transformed matrices back in process
            processModified.setY(y);
            processModified.setCb(cb);
            processModified.setCr(cr);

            long endTime = System.currentTimeMillis();
            Logger.info("Complete inverse transform operation finished in " + (endTime - startTime) + "ms");

            if (showSteps.isSelected()) {
                Logger.info("Displaying inverse transformed channels (show steps enabled)");
                Dialogs.showImageInWindow(processModified.getChannelImage(y), "Y - " + selectedType + " Inverse Transformed (Block Size: " + blockSize + ")");
                Dialogs.showImageInWindow(processModified.getChannelImage(cb), "Cb - " + selectedType + " Inverse Transformed (Block Size: " + blockSize + ")");
                Dialogs.showImageInWindow(processModified.getChannelImage(cr), "Cr - " + selectedType + " Inverse Transformed (Block Size: " + blockSize + ")");
            }
        } catch (Exception e) {
            Logger.error("Error during inverse transform: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Inverse Transform Error");
            alert.setHeaderText(null);
            alert.setContentText("An error occurred during inverse transformation: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Performs quantization on the image data
     */
    public void quantize() {
        Logger.info("Quantize button clicked");

        if (processModified == null) {
            Logger.warning("Cannot quantize - no image loaded");
            showAlert("Cannot Quantize", "No image loaded. Please load an image first.");
            return;
        }

        if (!processModified.isYCbCrConverted()) {
            Logger.warning("Cannot quantize - YCbCr conversion not performed");
            showAlert("Cannot Quantize", "You must convert to YCbCr first before quantizing.");
            return;
        }

        // Get quality value from the slider
        double quality = quantizeQuality.getValue();

        // Get block size from the spinner
        int blockSize = transformBlock.getValue();

        Logger.info("Starting quantization with quality " + quality + " and block size " + blockSize);
        long startTime = System.currentTimeMillis();

        try {
            // Perform quantization
            processModified.quantize(quality, blockSize);

            long endTime = System.currentTimeMillis();
            Logger.info("Quantization completed in " + (endTime - startTime) + "ms");

            if (showSteps.isSelected()) {
                Logger.info("Displaying quantized channels (show steps enabled)");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getY()),
                        "Y - Quantized (Quality: " + quality + ", Block Size: " + blockSize + ")");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCb()),
                        "Cb - Quantized (Quality: " + quality + ", Block Size: " + blockSize + ")");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCr()),
                        "Cr - Quantized (Quality: " + quality + ", Block Size: " + blockSize + ")");
            }
        } catch (Exception e) {
            Logger.error("Error during quantization: " + e.getMessage());
            e.printStackTrace();
            showAlert("Quantization Error", "An error occurred during quantization: " + e.getMessage());
        }
    }

    /**
     * Performs inverse quantization on the image data
     */
    public void inverseQuantize() {
        Logger.info("Inverse Quantize button clicked");

        if (processModified == null) {
            Logger.warning("Cannot inverse quantize - no image loaded");
            showAlert("Cannot Inverse Quantize", "No image loaded. Please load an image first.");
            return;
        }

        if (!processModified.isYCbCrConverted()) {
            Logger.warning("Cannot inverse quantize - YCbCr conversion not performed");
            showAlert("Cannot Inverse Quantize", "You must convert to YCbCr first.");
            return;
        }

        if (!processModified.isQuantized()) {
            Logger.warning("Cannot inverse quantize - image not quantized");
            showAlert("Cannot Inverse Quantize", "The image has not been quantized yet.");
            return;
        }

        Logger.info("Starting inverse quantization with quality " + processModified.getQuantizationQuality() +
                " and block size " + processModified.getQuantizationBlockSize());
        long startTime = System.currentTimeMillis();

        try {
            // Perform inverse quantization
            processModified.inverseQuantize();

            long endTime = System.currentTimeMillis();
            Logger.info("Inverse quantization completed in " + (endTime - startTime) + "ms");

            if (showSteps.isSelected()) {
                Logger.info("Displaying inverse quantized channels (show steps enabled)");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getY()),
                        "Y - Inverse Quantized");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCb()),
                        "Cb - Inverse Quantized");
                Dialogs.showImageInWindow(processModified.getChannelImage(processModified.getCr()),
                        "Cr - Inverse Quantized");
            }
        } catch (Exception e) {
            Logger.error("Error during inverse quantization: " + e.getMessage());
            e.printStackTrace();
            showAlert("Inverse Quantization Error", "An error occurred during inverse quantization: " + e.getMessage());
        }
    }

    @FXML
    public void openComparisonWindow() {
        Logger.info("Opening comparison window");

        if (processOriginal == null || processModified == null) {
            showAlert("Cannot Compare", "Please load an image first.");
            return;
        }

        try {
            // Load the comparison window FXML
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("graphics/ComparisonWindow.fxml"));
            Parent root = loader.load();

            // Get the controller and set the processes
            ComparisonWindowController controller = loader.getController();
            controller.setProcesses(processOriginal, processModified);

            // Create and show the stage
            Stage stage = new Stage();
            stage.setTitle("Image Comparison");
            stage.getIcons().add(FileBindings.favicon);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            Logger.error("Error opening comparison window: " + e.getMessage());
            showAlert("Error", "Failed to open comparison window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Opens an enhanced watermarking dialog with automatic YCbCr conversion.
     */
    @FXML
    public void openWatermarkingDialog() {
        Logger.info("Opening watermarking dialog");

        if (processOriginal == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Open Watermarking");
            alert.setHeaderText(null);
            alert.setContentText("Please load an image first before opening the watermarking tool.");
            alert.showAndWait();
            return;
        }

        try {
            WatermarkingDialog dialog = new WatermarkingDialog(
                    ((Stage) buttonSample.getScene().getWindow()),
                    processOriginal
            );
            dialog.show();
        } catch (Exception e) {
            Logger.error("Error opening watermarking dialog: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to open watermarking dialog: " + e.getMessage());
            alert.showAndWait();
        }
    }

}
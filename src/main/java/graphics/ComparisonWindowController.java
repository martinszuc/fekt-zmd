package graphics;

import Core.FileBindings;
import enums.QualityType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import jpeg.Process;
import utils.Logger;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

public class ComparisonWindowController implements Initializable {

    @FXML
    private ComboBox<QualityType> componentSelector;

    @FXML
    private ImageView originalImageView;

    @FXML
    private ImageView modifiedImageView;

    @FXML
    private Label infoLabel;

    private Process originalProcess;
    private Process modifiedProcess;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize the component selector dropdown
        componentSelector.getItems().setAll(
                QualityType.RGB,
                QualityType.RED,
                QualityType.GREEN,
                QualityType.BLUE,
                QualityType.Y,
                QualityType.CB,
                QualityType.CR
        );
        componentSelector.getSelectionModel().select(QualityType.RGB);

        // Set up listener for component changes
        componentSelector.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> refreshComparison());
    }

    public void setProcesses(Process original, Process modified) {
        this.originalProcess = original;
        this.modifiedProcess = modified;
        refreshComparison();
    }

    @FXML
    public void refreshComparison() {
        if (originalProcess == null || modifiedProcess == null) {
            infoLabel.setText("No images loaded");
            return;
        }

        try {
            QualityType selectedComponent = componentSelector.getSelectionModel().getSelectedItem();

            // Check component availability based on selection
            if ((selectedComponent == QualityType.Y || selectedComponent == QualityType.CB ||
                    selectedComponent == QualityType.CR) &&
                    (!originalProcess.isYCbCrConverted() || !modifiedProcess.isYCbCrConverted())) {
                infoLabel.setText("YCbCr components not available. Convert to YCbCr first.");
                return;
            }

            // Get the appropriate images based on the selected component
            BufferedImage originalImg = getComponentImage(originalProcess, selectedComponent);
            BufferedImage modifiedImg = getComponentImage(modifiedProcess, selectedComponent);

            if (originalImg == null || modifiedImg == null) {
                infoLabel.setText("Selected component not available");
                return;
            }

            // Convert BufferedImage to JavaFX Image
            originalImageView.setImage(convertToFXImage(originalImg));
            modifiedImageView.setImage(convertToFXImage(modifiedImg));

            // Update info label
            infoLabel.setText("Comparing " + selectedComponent.toString() + " component");

            Logger.info("Displayed comparison of " + selectedComponent + " component");

        } catch (Exception e) {
            Logger.error("Error updating comparison: " + e.getMessage());
            infoLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private BufferedImage getComponentImage(Process process, QualityType component) {
        switch (component) {
            case RGB:
                return process.getRGBImage();
            case RED:
                return process.getChannelImage(process.getRed(), "RED");
            case GREEN:
                return process.getChannelImage(process.getGreen(), "GREEN");
            case BLUE:
                return process.getChannelImage(process.getBlue(), "BLUE");
            case Y:
                if (process.isYCbCrConverted()) {
                    return process.getChannelImage(process.getY());
                }
                break;
            case CB:
                if (process.isYCbCrConverted()) {
                    return process.getChannelImage(process.getCb());
                }
                break;
            case CR:
                if (process.isYCbCrConverted()) {
                    return process.getChannelImage(process.getCr());
                }
                break;
        }
        return null;
    }

    private Image convertToFXImage(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            return new Image(in);
        } catch (Exception e) {
            Logger.error("Error converting BufferedImage to FX Image: " + e.getMessage());
            return null;
        }
    }

    @FXML
    public void closeWindow() {
        Stage stage = (Stage) componentSelector.getScene().getWindow();
        stage.close();
    }
}
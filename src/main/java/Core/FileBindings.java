package Core;

import javafx.scene.image.Image;

import java.net.URL;

public class FileBindings {

    // Cesta k výchozímu obrázku
    public static final String defaultImage = "Images/Lenna.png";

    // Cesta k souboru z rozhraním aplikace
    public static final URL GUIMain = FileBindings.class.getClassLoader().getResource("graphics/" + "MainWindow" + ".fxml");

    // Ikona aplikace
    public static Image favicon = new Image(FileBindings.class.getClassLoader().getResourceAsStream("favicon.png"));

}

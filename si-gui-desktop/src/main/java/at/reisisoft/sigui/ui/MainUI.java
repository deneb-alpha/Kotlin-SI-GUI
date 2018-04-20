package at.reisisoft.sigui.ui;

import at.reisisoft.sigui.settings.SettingsKt;
import at.reisisoft.sigui.settings.SiGuiSetting;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainUI extends Application {

    private MainUIController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        SiGuiSetting settings = SettingsKt.loadSettings();
        Locale.setDefault(settings.getUiLanguage());
        FXMLLoader loader = new FXMLLoader();
        URL uiDescription = MainUI.class.getClassLoader().getResource("mainUI.fxml");
        ResourceBundle languageSupport = ResourceBundle.getBundle("uistrings.sigui-desktop");
        loader.setResources(languageSupport);
        loader.setLocation(uiDescription);
        Parent mainUi = loader.load();
        controller = loader.getController();
        controller.internalInitialize(settings);
        primaryStage.setScene(new Scene(mainUi));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        Objects.requireNonNull(controller).close();
        System.exit(0);
    }
}
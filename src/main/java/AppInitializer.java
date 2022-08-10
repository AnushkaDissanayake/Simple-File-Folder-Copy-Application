import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AppInitializer extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL resources = this.getClass().getResource("/view/MainForm.fxml");
        Parent container = FXMLLoader.load(resources);
        Scene scene = new Scene(container);

        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.centerOnScreen();
        primaryStage.setTitle("Copy File");

    }
}

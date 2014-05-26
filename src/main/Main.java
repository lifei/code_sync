package main;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	public static void main(final String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(final Stage stage) throws IOException {

		final Parent root = FXMLLoader.load(this.getClass().getResource(
				"/code_sync.fxml"));

		root.getChildrenUnmodifiable();

		final Scene scene = new Scene(root, 600, 400);

		stage.setTitle("Code Sync v1.1");
		stage.setScene(scene);
		stage.show();
	}
}
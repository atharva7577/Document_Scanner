package com.documentscanner;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.documentscanner.view.ScannerView;
import com.documentscanner.controller.ScannerController;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        ScannerView scannerView = new ScannerView();
        ScannerController scannerController = new ScannerController(scannerView.getScannedImageView());

        Scene scene = new Scene(scannerView, 800, 600);
        primaryStage.setTitle("Document Scanner");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

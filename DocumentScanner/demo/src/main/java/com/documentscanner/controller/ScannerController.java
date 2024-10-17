package com.documentscanner.controller;

import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils; // Correct import for SwingFXUtils
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ScannerController {
    private ImageView scannedImageView;

    public ScannerController(ImageView scannedImageView) {
        this.scannedImageView = scannedImageView;
    }

    public void scanDocument() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                BufferedImage image = ImageIO.read(file);
                Image fxImage = SwingFXUtils.toFXImage(image, null);
                scannedImageView.setImage(fxImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveDocument() {
        Image image = scannedImageView.getImage();
        if (image != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Document");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );

            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try {
                    // Save as image
                    if (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")) {
                        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
                        ImageIO.write(bImage, file.getName().endsWith(".png") ? "png" : "jpg", file);
                    }
                    // Save as PDF
                    else if (file.getName().endsWith(".pdf")) {
                        PDDocument document = new PDDocument();
                        PDPage page = new PDPage();
                        document.addPage(page);
                        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageToByteArray(SwingFXUtils.fromFXImage(image, null)), "image");
                        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                            contentStream.drawImage(pdImage, 100, 100);
                        }
                        document.save(file);
                        document.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("No image scanned to save.");
        }
    }

    // Utility method to convert BufferedImage to byte array
    private byte[] imageToByteArray(BufferedImage bImage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bImage, "png", baos);
        return baos.toByteArray();
    }
}

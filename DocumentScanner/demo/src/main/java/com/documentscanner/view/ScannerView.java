package com.documentscanner.view;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import javafx.embed.swing.SwingFXUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class ScannerView extends VBox {
    private Button scanButton;
    private Button saveButton;
    private Button verifyWithResourcesButton;
    private ImageView scannedImageView;
    private Label statusLabel;

    public ScannerView() {
        setPadding(new Insets(20));
        setSpacing(15);
        setAlignment(Pos.CENTER);

        // Initialize UI Components
        scanButton = new Button("Scan Document");
        saveButton = new Button("Save Document");
        verifyWithResourcesButton = new Button("Verify with Resources");
        scannedImageView = new ImageView();
        statusLabel = new Label();

        // Style buttons
        scanButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        saveButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        verifyWithResourcesButton.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");

        // Set button sizes
        scanButton.setMinWidth(150);
        saveButton.setMinWidth(150);
        verifyWithResourcesButton.setMinWidth(200);

        // Create a container for the buttons
        HBox buttonContainer = new HBox(10, scanButton, saveButton, verifyWithResourcesButton);
        buttonContainer.setAlignment(Pos.CENTER);

        // Add components to layout
        getChildren().addAll(buttonContainer, scannedImageView, statusLabel);

        // Add actions to buttons
        scanButton.setOnAction(e -> scanDocument());
        saveButton.setOnAction(e -> saveDocument());
        verifyWithResourcesButton.setOnAction(e -> verifyWithResources());
    }

    public ImageView getScannedImageView() {
        return scannedImageView; // Getter for scannedImageView
    }

    private void scanDocument() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                BufferedImage image = ImageIO.read(file);
                Image fxImage = SwingFXUtils.toFXImage(image, null);
                scannedImageView.setImage(fxImage);
                statusLabel.setText("Document scanned successfully. Ready for verification.");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to scan document: " + e.getMessage());
            }
        }
    }

    private void saveDocument() {
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
                        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
                        PDDocument document = new PDDocument();
                        PDPage page = new PDPage();
                        document.addPage(page);
                        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageToByteArray(bImage), "image");
                        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                            contentStream.drawImage(pdImage, 100, 100, bImage.getWidth(), bImage.getHeight());
                        }
                        document.save(file);
                        document.close();
                    }
                    showAlert("Success", "Document saved successfully.");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to save document: " + e.getMessage());
                }
            }
        } else {
            showAlert("Error", "No image scanned to save.");
        }
    }

    private void verifyWithResources() {
        Image image = scannedImageView.getImage();
        if (image != null) {
            try {
                BufferedImage scannedImage = SwingFXUtils.fromFXImage(image, null);
                String scannedText = extractTextFromImage(scannedImage);

                if (scannedText != null && !scannedText.isEmpty()) {
                    // Load all images from the resources/images folder
                    URL resourceFolderUrl = getClass().getResource("/images");
                    File resourceFolder = new File(resourceFolderUrl.toURI());
                    boolean foundMatch = false;

                    // Print the resource folder path for debugging
                    System.out.println("Resource folder path: " + resourceFolder.getAbsolutePath());

                    for (File resourceFile : Objects.requireNonNull(resourceFolder.listFiles())) {
                        if (resourceFile.isFile() && isImageFile(resourceFile)) {
                            BufferedImage resourceImage = ImageIO.read(resourceFile);
                            String resourceText = extractTextFromImage(resourceImage);

                            // Print the texts for debugging
                            System.out.println("Scanned Text: " + scannedText);
                            System.out.println("Resource Text (" + resourceFile.getName() + "): " + resourceText);

                            // Compare the cleaned texts
                            if (scannedText.trim().equalsIgnoreCase(resourceText.trim())) {
                                foundMatch = true;
                                showAlert("Verification Passed", "Scanned image matches the image: " + resourceFile.getName());
                                break;
                            }
                        }
                    }

                    if (!foundMatch) {
                        showAlert("Verification Failed", "Scanned image does not match any image in the resources folder.");
                    }
                } else {
                    showAlert("Error", "No text extracted from the scanned document.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to verify document: " + e.getMessage());
            }
        } else {
            showAlert("Error", "No document scanned to verify.");
        }
    }

    // Function to extract text from an image using Tesseract OCR
    private String extractTextFromImage(BufferedImage image) {
        try {
            // Preprocess the image to improve OCR results
            BufferedImage processedImage = preprocessImage(image);
            
            // Print to debug the processed image (optional)
            System.out.println("Processed Image Size: " + processedImage.getWidth() + "x" + processedImage.getHeight());

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("E:\\zip files\\tessdata-main"); // Adjust the path as per your system
            tesseract.setLanguage("eng");
            String text = tesseract.doOCR(processedImage);
            System.out.println("Extracted Text: " + text); // Debug print
            return text;
        } catch (TesseractException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Preprocess the image to improve OCR results
    private BufferedImage preprocessImage(BufferedImage originalImage) {
        // Convert to grayscale
        BufferedImage grayImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for (int i = 0; i < originalImage.getWidth(); i++) {
            for (int j = 0; j < originalImage.getHeight(); j++) {
                grayImage.setRGB(i, j, originalImage.getRGB(i, j));
            }
        }

        // Optional: Apply image filtering to reduce noise
        // This is a simple example; you may want to use a more sophisticated approach.
        // You can apply more advanced techniques using libraries like OpenCV if needed.

        return grayImage; // Return the processed image
    }

    // Helper function to check if the file is an image
    private boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Utility method to convert BufferedImage to byte array
    private byte[] imageToByteArray(BufferedImage bImage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bImage, "png", baos);
        return baos.toByteArray();
    }
}

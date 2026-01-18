import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;


/**
 * Handles exporting traffic simulation data to CSV files.
 * 
 * <p>This class provides functionality to export current simulation state including:
 * <ul>
 *   <li>Timestamp and simulation time</li>
 *   <li>Total vehicle count</li>
 *   <li>Vehicle breakdown by type (cars, trucks, buses, motorcycles, emergency)</li>
 *   <li>Average vehicle speed</li>
 * </ul>
 * 
 * @author M A T^2 H Team
 * @version 1.0
 */
public class TrafficDataExporter {
    /**
     * Exports current simulation data to a CSV file with file chooser dialog.
     * Data is already filtered by SimulationRunner based on VehicleFilterPanel settings.
     * Shows success or error alerts after export attempt.
     * 
     * @param runner The simulation runner to get current simulation data
     */
    public static void exportToCSV(SimulationRunner runner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Simulation Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("Simulation_data.csv");
        
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                String filePath = file.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                }
                
                // Write CSV file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    writer.write("simulation_time, total_vehicles, cars, trucks, buses, motorcycles, emergency, avg_speed\n ");
                    //Get current simulation data 
                    double simTime;
                    if (runner != null){
                        simTime = runner.getSimulationTime();
                    }
                    else { simTime = 0.0;}

                    //Get vehicle speeds from runner

                    // Get vehicle data (already filtered by SimulationRunner)
                    java.util.Map<String, Double> speeds = runner.getVehicleSpeeds();
                    java.util.Map<String, Integer> counts = runner.getVehicleCountsByType();
                    
                    int totalVehicles = speeds.size();
                    int cars = counts.getOrDefault("car", 0);
                    int trucks = counts.getOrDefault("truck", 0);
                    int buses = counts.getOrDefault("bus", 0);
                    int motorcycles = counts.getOrDefault("moto", 0);
                    int emergency = counts.getOrDefault("emergency", 0);
                    
                    // Calculate average speed
                    double totalSpeed = speeds.values().stream().mapToDouble(Double::doubleValue).sum();
                    double avgSpeed = totalVehicles > 0 ? totalSpeed / totalVehicles : 0.0;
                    
                    // Write data row
                    writer.write(String.format("%.2f,%d,%d,%d,%d,%d,%d,%.2f\n",
                        simTime, totalVehicles, cars, trucks, buses, motorcycles, emergency, avgSpeed));
                    
                    writer.flush();
                }
                
                System.out.println("Simulation data exported to: " + filePath);
                
                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("CSV exported successfully!");
                alert.showAndWait();
            } catch (IOException ex) {
                System.err.println("Error exporting CSV: " + ex.getMessage());
                ex.printStackTrace();
                
                // Show error alert
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export CSV:\n" + ex.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    /**
     * Exports a BufferedImage to a PDF file with a title.
     * The image is scaled to fit A4 portrait page while maintaining aspect ratio.
     * 
     * @param image The BufferedImage to export (from JavaFX snapshot)
     * @param filePath The destination file path for the PDF
     * @throws IOException if PDF creation or writing fails
     */
    public static void exportPDF(BufferedImage image, String filePath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Create A4 portrait page
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            // Get page dimensions
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;
            
            // Title settings
            float titleHeight = 60;
            String titleText = "Statistics Report";
            
            // Calculate scaling to fit image on page while maintaining aspect ratio
            // Reserve space for title at the top
            float availableHeight = pageHeight - titleHeight - (margin * 2);
            float availableWidth = pageWidth - (margin * 2);
            
            float imageWidth = image.getWidth();
            float imageHeight = image.getHeight();
            
            float scaleX = availableWidth / imageWidth;
            float scaleY = availableHeight / imageHeight;
            float scale = Math.min(scaleX, scaleY);
            
            float scaledWidth = imageWidth * scale;
            float scaledHeight = imageHeight * scale;
            
            // Center the image horizontally, place below title
            float imageX = (pageWidth - scaledWidth) / 2;
            float imageY = pageHeight - titleHeight - margin - scaledHeight;
            
            // Create image object and draw it
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                document, 
                imageToBytes(image), 
                "chart"
            );
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Draw title
                contentStream.beginText();
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 24);
                contentStream.newLineAtOffset(margin, pageHeight - margin - 20);
                contentStream.showText(titleText);
                contentStream.endText();
                
                // Draw the image
                contentStream.drawImage(pdImage, imageX, imageY, scaledWidth, scaledHeight);
            }
            
            // Save the document
            document.save(new File(filePath));
        }
    }
    
    /**
     * Exports statistics window snapshot to PDF with file chooser dialog.
     * Shows success or error alert after export attempt.
     * 
     * @param bufferedImage The BufferedImage from the statistics window snapshot
     * @param owner The parent stage for the file chooser dialog
     */
    public static void exportToPDF(BufferedImage bufferedImage, Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Statistics as PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        fileChooser.setInitialFileName("traffic_statistics.pdf");
        
        File file = fileChooser.showSaveDialog(owner);
        
        if (file != null) {
            try {
                exportPDF(bufferedImage, file.getAbsolutePath());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("PDF exported successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    /**
     * Converts BufferedImage to byte array for PDFBox.
     * 
     * @param image The BufferedImage to convert
     * @return Byte array representation of the image
     * @throws IOException if conversion fails
     */
    private static byte[] imageToBytes(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
    
}

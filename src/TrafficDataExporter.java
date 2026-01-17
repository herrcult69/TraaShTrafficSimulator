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
     * Exports current simulation data to a CSV file with optional filtering.
     * 
     * @param csvPath The file path where CSV should be saved
     * @param runner The simulation runner to get current simulation time
     * @param trafficManager The traffic manager containing vehicle data
     * @param vehicleFilter Optional filter panel to apply vehicle type filtering (can be null)
     * @throws IOException if file writing fails
     */
    public static void exportToCSV(String csvPath, SimulationRunner runner, TrafficManager trafficManager, VehicleFilterPanel vehicleFilter) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath))) {
            writer.write("simulation_time, total_vehicles, cars, trucks, buses, motorcycles, emergency, avg_speed\n ");
            //Get current simulation data 
            double simTime;
            if (runner != null){
                simTime = runner.getSimulationTime();
            }
            else { simTime = 0.0;}

            //Get vehicle speeds from runner

            java.util.Map<String, Double> speeds = runner.getVehicleSpeeds();
        // int totalVehicles = speeds.size();

        int cars = 0, trucks = 0, buses = 0, motorcycles = 0, emergency = 0;
        double totalSpeed = 0;
        int filteredVehicleCount = 0;
        
        for (java.util.Map.Entry<String, Double> entry : speeds.entrySet()) {
            String id = entry.getKey();
            
            // Determine vehicle type
            String type = null;
            if (id.startsWith("car")) type = "car";
            else if (id.startsWith("truck")) type = "truck";
            else if (id.startsWith("bus")) type = "bus";
            else if (id.startsWith("moto")) type = "moto";
            else if (id.startsWith("ambu")) type = "emergency";
            
            // Apply filter if provided
            if (vehicleFilter != null && type != null && !vehicleFilter.isTypeVisible(type)) {
                continue; // Skip this vehicle if filtered out
            }
            
            // Count and accumulate speed for filtered vehicles
            filteredVehicleCount++;
            totalSpeed += entry.getValue();
            
            if (type != null) {
                if (type.equals("car")) cars++;
                else if (type.equals("truck")) trucks++;
                else if (type.equals("bus")) buses++;
                else if (type.equals("moto")) motorcycles++;
                else if (type.equals("emergency")) emergency++;
            }
        }
            double avgSpeed;
            if (filteredVehicleCount > 0) {
                avgSpeed = totalSpeed / filteredVehicleCount;
            }
            else {
                avgSpeed = 0.0;
            }
            // Write data row
            writer.write(String.format("%.2f,%d,%d,%d,%d,%d,%d,%.2f\n",
                simTime, filteredVehicleCount, cars, trucks, buses, motorcycles, emergency, avgSpeed));
            
            writer.flush();
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

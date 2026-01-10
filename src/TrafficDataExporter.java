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
     * Exports current simulation data to a CSV file.
     * 
     * @param csvPath The file path where CSV should be saved
     * @param runner The simulation runner to get current simulation time
     * @param trafficManager The traffic manager containing vehicle data
     * @throws IOException if file writing fails
     */
    public static class Snapshot {
        public double simulationTime;
        public int totalVehicles;
        public int cars;
        public int trucks;
        public int buses;
        public int motorcycles;
        public int emergency;
        public double avgSpeed;

        public String toCSVRow() {
            return String.format("%.2f, %d, %d, %d, %d, %d, %d, %.2f\n", simulationTime, totalVehicles, cars, trucks, buses, motorcycles, emergency, avgSpeed);
        }
        @Override
        public String toString() {
            return String.format("Time: %.2fs - Total: %d (Cars: %d, Trucks: %d, Buses: %d, Motorcycles: %d, Emergency: %d) - Avg Speed: %.2f", simulationTime, totalVehicles, cars, trucks, buses, motorcycles, emergency, avgSpeed);
        }
    }
    
    public static void exportToCSV(String csvPath, SimulationRunner runner, TrafficManager trafficManager) throws IOException {
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
        int totalVehicles = speeds.size();

        int cars = 0, trucks = 0, buses = 0, motorcycles = 0, emergency = 0;
        double totalSpeed = 0;
        
        for (java.util.Map.Entry<String, Double> entry : speeds.entrySet()) {
            String id = entry.getKey();
            totalSpeed += entry.getValue();
            
            // Count by vehicle ID prefix (same as TrafficSimulatorApp)
            if (id.startsWith("car")) cars++;
            else if (id.startsWith("truck")) trucks++;
            else if (id.startsWith("bus")) buses++;
            else if (id.startsWith("moto")) motorcycles++;
            else if (id.startsWith("ambu")) emergency++;
        }
            double avgSpeed;
            if (totalVehicles > 0) {
                avgSpeed = totalSpeed / totalVehicles;
            }
            else {
                avgSpeed = 0.0;
            }
            // Write data row
            writer.write(String.format("%.2f,%d,%d,%d,%d,%d,%d,%.2f\n",
                simTime, totalVehicles, cars, trucks, buses, motorcycles, emergency, avgSpeed));
            
            writer.flush();
        }
    }
    
    /**
     * Exports a BufferedImage to a PDF file.
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
            
            // Calculate scaling to fit image on page while maintaining aspect ratio
            float imageWidth = image.getWidth();
            float imageHeight = image.getHeight();
            
            float scaleX = pageWidth / imageWidth;
            float scaleY = pageHeight / imageHeight;
            float scale = Math.min(scaleX, scaleY) * 0.95f; // 95% to leave small margin
            
            float scaledWidth = imageWidth * scale;
            float scaledHeight = imageHeight * scale;
            
            // Center the image on the page
            float x = (pageWidth - scaledWidth) / 2;
            float y = (pageHeight - scaledHeight) / 2;
            
            // Create image object and draw it
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                document, 
                imageToBytes(image), 
                "chart"
            );
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight);
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

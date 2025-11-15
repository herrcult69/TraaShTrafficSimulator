package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class TrafficSimulatorApp extends Application {
    private static final String NETWORK_FILE = "resource/network.net.xml";
    private static final String CONFIG_FILE = "resource/simulation.sumocfg";

    private NetworkParser.NetworkData network;
    private SimulationRunner runner;
    private ExecutorService exec;
    private Canvas canvas;

    private double scaleX, scaleY, offsetX, offsetY;

    @Override
    public void start(Stage stage) throws Exception {
        network = NetworkParser.parse(NETWORK_FILE);
        canvas = new Canvas(1000, 800);
        computeTransform();

        BorderPane root = new BorderPane(canvas);
        stage.setTitle("TraaSh Traffic Simulator (JavaFX)");
        stage.setScene(new Scene(root));
        stage.show();

        runner = new SimulationRunner(CONFIG_FILE, false); // headless SUMO
        exec = Executors.newSingleThreadExecutor();
        exec.submit(runner);

        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                draw();
            }
        };
        timer.start();

        stage.setOnCloseRequest(e -> {
            runner.stop();
            exec.shutdownNow();
            Platform.exit();
        });
    }

    private void computeTransform(){
        double margin = 40;
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double netW = network.maxX - network.minX;
        double netH = network.maxY - network.minY;
        scaleX = (width - 2*margin) / netW;
        scaleY = (height - 2*margin) / netH;
        offsetX = margin - network.minX * scaleX;
        offsetY = margin - network.minY * scaleY;
    }

    private double tx(double x){ return x * scaleX + offsetX; }
    private double ty(double y){ // invert Y for screen coordinates if desired
        return (y * scaleY + offsetY); // keep as is for now
    }

    private void draw(){
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        // Draw edges as lines
        g.setStroke(Color.DARKGRAY);
        g.setLineWidth(2);
        for(NetworkParser.Edge e : network.edges){
            NetworkParser.Junction from = findJunction(e.from);
            NetworkParser.Junction to = findJunction(e.to);
            if(from==null || to==null) continue;
            g.strokeLine(tx(from.x), ty(from.y), tx(to.x), ty(to.y));
        }

        // Draw junctions
        for(NetworkParser.Junction j : network.junctions){
            g.setFill(Color.GRAY);
            g.fillOval(tx(j.x)-3, ty(j.y)-3, 6,6);
        }

        // Traffic lights (color circles)
        Map<String,String> tlColors = runner.getTrafficLightColors();
        for(Map.Entry<String,String> tl : tlColors.entrySet()){
            NetworkParser.Junction j = findJunction(tl.getKey());
            if(j==null) continue;
            Color c = switch(tl.getValue()){
                case "GREEN" -> Color.LIME;
                case "YELLOW" -> Color.GOLD;
                default -> Color.RED;
            };
            g.setFill(c);
            g.fillOval(tx(j.x)-6, ty(j.y)-6, 12,12);
        }

        // Vehicles
        Map<String,double[]> vehicles = runner.getVehiclePositions();
        for(Map.Entry<String,double[]> v : vehicles.entrySet()){
            String id = v.getKey();
            double[] pos = v.getValue();
            Color c = vehicleColor(id);
            g.setFill(c);
            double px = tx(pos[0]);
            double py = ty(pos[1]);
            double size = vehicleSize(id);
            g.fillOval(px - size/2, py - size/2, size, size);
        }
    }

    private Color vehicleColor(String id){
        if(id.startsWith("car")) return Color.RED;
        if(id.startsWith("truck")) return Color.BLUE;
        if(id.startsWith("moto")) return Color.BLACK;
        if(id.startsWith("bus")) return Color.GREEN;
        if(id.startsWith("ambulance")) return Color.YELLOW;
        return Color.WHITE;
    }

    private double vehicleSize(String id){
        if(id.startsWith("truck")) return 14;
        if(id.startsWith("bus")) return 16;
        if(id.startsWith("ambulance")) return 12;
        if(id.startsWith("moto")) return 6;
        return 10; // car default
    }

    private NetworkParser.Junction findJunction(String id){
        for(NetworkParser.Junction j : network.junctions){
            if(j.id.equals(id)) return j;
        }
        return null;
    }

    public static void main(String[] args) { launch(args); }
}

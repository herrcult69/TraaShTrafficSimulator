package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class TrafficSimulatorApp extends Application {
    private static final String NETWORK_FILE = "resource/network.net.xml";
    private static final String CONFIG_FILE = "resource/simulation.sumocfg";

    private NetworkParser.NetworkData network;
    private SimulationRunner runner;
    private ExecutorService exec;
    private Canvas canvas;

    // View transform (uniform scale for cohesive look)
    private double baseScale, scaleX, scaleY, offsetX, offsetY;
    private double baseOffsetX, baseOffsetY;
    private double zoom = 1.0;
    private double viewOffsetX = 0.0, viewOffsetY = 0.0;

    // Interaction state
    private double dragStartScreenX, dragStartScreenY;
    private double dragStartViewOffsetX, dragStartViewOffsetY;

    // Fast junction lookup
    private Map<String, NetworkParser.Junction> junctionIndex;

    @Override
    public void start(Stage stage) throws Exception {
        network = NetworkParser.parse(NETWORK_FILE);
        canvas = new Canvas(1000, 800);
        buildJunctionIndex();
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

        // Simple pan (drag) and zoom (scroll) interactions
        canvas.setOnScroll(e -> {
            double mouseX = e.getX();
            double mouseY = e.getY();
            double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            zoomAt(mouseX, mouseY, zoomFactor);
            e.consume();
        });

        canvas.setOnMousePressed(e -> {
            dragStartScreenX = e.getX();
            dragStartScreenY = e.getY();
            dragStartViewOffsetX = viewOffsetX;
            dragStartViewOffsetY = viewOffsetY;
        });

        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartScreenX;
            double dy = e.getY() - dragStartScreenY;
            viewOffsetX = dragStartViewOffsetX + dx;
            viewOffsetY = dragStartViewOffsetY + dy;
            updateTransformFromView();
        });

        stage.setOnCloseRequest(e -> {
            runner.stop();
            exec.shutdownNow();
            Platform.exit();
        });
    }

    private void buildJunctionIndex(){
        junctionIndex = new HashMap<>();
        for(NetworkParser.Junction j : network.junctions){
            junctionIndex.put(j.id, j);
        }
    }

    private void computeTransform(){
        double margin = 40;
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double netW = network.maxX - network.minX;
        double netH = network.maxY - network.minY;
        baseScale = Math.min((width - 2*margin) / netW, (height - 2*margin) / netH);
        // Center the network in the available area
        double drawW = netW * baseScale;
        double drawH = netH * baseScale;
        baseOffsetX = (width - drawW) / 2 - network.minX * baseScale;
        baseOffsetY = (height - drawH) / 2 - network.minY * baseScale;
        updateTransformFromView();
    }

    private void updateTransformFromView(){
        double s = baseScale * zoom;
        scaleX = s; // uniform scale for X and Y
        scaleY = s;
        offsetX = baseOffsetX + viewOffsetX;
        offsetY = baseOffsetY + viewOffsetY;
    }

    private void zoomAt(double screenX, double screenY, double factor){
        double oldS = baseScale * zoom;
        double worldX = (screenX - offsetX) / oldS;
        double worldY = (screenY - offsetY) / oldS;

        zoom *= factor;
        zoom = Math.max(0.2, Math.min(5.0, zoom));

        double newS = baseScale * zoom;
        // Reposition offset so the point under the cursor stays fixed
        offsetX = screenX - worldX * newS;
        offsetY = screenY - worldY * newS;
        // Reflect new offsets into view deltas from base
        viewOffsetX = offsetX - baseOffsetX;
        viewOffsetY = offsetY - baseOffsetY;
        scaleX = newS;
        scaleY = newS;
    }

    private double tx(double x){ return x * scaleX + offsetX; }
    private double ty(double y){ return y * scaleY + offsetY; }

    private void draw(){
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.rgb(20, 24, 28));
        g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        // Draw roads: asphalt base, then dashed centerline (thicker for readability)
        for(NetworkParser.Edge e : network.edges){
            NetworkParser.Junction from = findJunction(e.from);
            NetworkParser.Junction to = findJunction(e.to);
            if(from==null || to==null) continue;
            double x1 = tx(from.x), y1 = ty(from.y);
            double x2 = tx(to.x), y2 = ty(to.y);

            // Asphalt base
            g.setStroke(Color.rgb(60, 66, 72));
            g.setLineWidth(40);
            g.setLineDashes();
            g.strokeLine(x1, y1, x2, y2);

            // Edge highlights
            g.setStroke(Color.rgb(90, 96, 104));
            g.setLineWidth(12);
            g.strokeLine(x1, y1, x2, y2);

            // Center dashed line
            g.setStroke(Color.rgb(255, 220, 50));
            g.setLineWidth(4);
            g.setLineDashes(16, 14);
            g.strokeLine(x1, y1, x2, y2);
        }
        g.setLineDashes(); // reset

        // Draw junctions
        for(NetworkParser.Junction j : network.junctions){
            double sx = tx(j.x), sy = ty(j.y);
            g.setFill(Color.rgb(150, 150, 150));
            g.fillOval(sx-3, sy-3, 6,6);
            g.setStroke(Color.rgb(30,30,30));
            g.setLineWidth(1.5);
            g.strokeOval(sx-3, sy-3, 6,6);
        }

        // Traffic lights (color circles)
        Map<String,String> tlColors = runner.getTrafficLightColors();
        for(Map.Entry<String,String> tl : tlColors.entrySet()){
            NetworkParser.Junction j = findJunction(tl.getKey());
            if(j==null) continue;
            Color c = switch(tl.getValue()){
                case "GREEN" -> Color.rgb(50, 220, 90);
                case "YELLOW" -> Color.rgb(255, 210, 70);
                default -> Color.rgb(240, 60, 60);
            };
            double sx = tx(j.x), sy = ty(j.y);
            g.setFill(c);
            g.fillOval(sx-6, sy-6, 12,12);
            g.setStroke(Color.BLACK);
            g.setLineWidth(1.5);
            g.strokeOval(sx-6, sy-6, 12,12);
        }

        // Vehicles
        Map<String,double[]> vehicles = runner.getVehiclePositions();
        int carCount = 0, truckCount = 0, busCount = 0, motoCount = 0, ambCount = 0;
        for(Map.Entry<String,double[]> v : vehicles.entrySet()){
            String id = v.getKey();
            double[] pos = v.getValue();
            double px = tx(pos[0]);
            double py = ty(pos[1]);
            // SUMO: 0° = North, 90° = East (clockwise). Map to canvas rotation.
            // Convert to standard math angle (0° = East, CCW positive): angle = 90 - sumoAngle
            double ang = (pos.length >= 3) ? (90.0 - pos[2]) : 0.0;
            drawVehicle(g, id, px, py, ang);
            if(id.startsWith("truck")) truckCount++;
            else if(id.startsWith("bus")) busCount++;
            else if(id.startsWith("moto")) motoCount++;
            else if(id.startsWith("ambulance")) ambCount++;
            else if(id.startsWith("car")) carCount++;
        }

        drawLegend(g, carCount, truckCount, busCount, motoCount, ambCount, tlColors.size());
    }

    private Color vehicleColor(String id){
        if(id.startsWith("car")) return Color.rgb(220, 70, 70);
        if(id.startsWith("truck")) return Color.rgb(70, 120, 230);
        if(id.startsWith("moto")) return Color.rgb(230, 230, 230);
        if(id.startsWith("bus")) return Color.rgb(80, 200, 120);
        if(id.startsWith("ambulance")) return Color.rgb(250, 240, 120);
        return Color.WHITE;
    }

    // Deprecated: kept for reference during transition to sprite rendering
    // private double vehicleSize(String id){
    //     if(id.startsWith("truck")) return 16;
    //     if(id.startsWith("bus")) return 18;
    //     if(id.startsWith("ambulance")) return 12;
    //     if(id.startsWith("moto")) return 6;
    //     return 10; // car default
    // }

    private void drawVehicle(GraphicsContext g, String id, double px, double py, double angleDeg){
        String type = vehicleType(id);
        // Dimensions in screen pixels
        double len, wid;
        switch (type){
            case "truck" -> { len = 26; wid = 8; }
            case "bus" -> { len = 24; wid = 8; }
            case "ambulance" -> { len = 14; wid = 7; }
            case "moto" -> { len = 10; wid = 3; }
            default -> { len = 12; wid = 6; } // car
        }

        // Shadow
        g.save();
        g.translate(px + 1.5, py + 1.5);
        g.rotate(angleDeg);
        g.setFill(Color.rgb(0,0,0,0.35));
        g.fillRoundRect(-len/2, -wid/2, len, wid, 3, 3);
        g.restore();

        // Body
        g.save();
        g.translate(px, py);
        g.rotate(angleDeg);
        g.setFill(vehicleColor(id));
        g.fillRoundRect(-len/2, -wid/2, len, wid, 3, 3);
        g.setStroke(Color.rgb(20,20,20));
        g.setLineWidth(1.25);
        g.strokeRoundRect(-len/2, -wid/2, len, wid, 3, 3);

        // Details by type
        switch (type){
            case "car" -> drawCarDetails(g, len, wid);
            case "truck" -> drawTruckDetails(g, len, wid);
            case "bus" -> drawBusDetails(g, len, wid);
            case "moto" -> drawMotoDetails(g, len, wid);
            case "ambulance" -> drawAmbulanceDetails(g, len, wid);
        }
        g.restore();
    }

    private String vehicleType(String id){
        if(id.startsWith("truck")) return "truck";
        if(id.startsWith("bus")) return "bus";
        if(id.startsWith("moto")) return "moto";
        if(id.startsWith("ambulance")) return "ambulance";
        if(id.startsWith("car")) return "car";
        return "car";
    }

    private void drawCarDetails(GraphicsContext g, double len, double wid){
        g.setFill(Color.rgb(240,240,255,0.9));
        g.fillRoundRect(-len/4, -wid/2 + 1, len/2, wid/2.2, 2, 2); // cabin windows
        g.setStroke(Color.rgb(30,30,30));
        g.setLineWidth(1);
        // wheels
        g.strokeLine(-len/3, -wid/2, -len/3, wid/2);
        g.strokeLine(len/3, -wid/2, len/3, wid/2);
    }

    private void drawTruckDetails(GraphicsContext g, double len, double wid){
        // Trailer shading
        g.setFill(Color.rgb(255,255,255,0.15));
        g.fillRect(-len/2 + 2, -wid/2 + 1, len*0.65, wid-2);
        // Cab separation
        g.setStroke(Color.rgb(30,30,30));
        g.setLineWidth(1);
        g.strokeLine(len*0.15 - len/2, -wid/2, len*0.15 - len/2, wid/2);
        // wheels
        g.strokeLine(-len/3, -wid/2, -len/3, wid/2);
        g.strokeLine(len/3, -wid/2, len/3, wid/2);
    }

    private void drawBusDetails(GraphicsContext g, double len, double wid){
        // Window band
        g.setFill(Color.rgb(230, 240, 255, 0.9));
        g.fillRect(-len/2 + 2, -wid/3, len-4, wid/1.7);
        // Doors/segments lines
        g.setStroke(Color.rgb(40,40,40));
        g.setLineWidth(1);
        g.strokeLine(-len/6, -wid/2, -len/6, wid/2);
        g.strokeLine(len/6, -wid/2, len/6, wid/2);
        // wheels
        g.strokeLine(-len/3, -wid/2, -len/3, wid/2);
        g.strokeLine(len/3, -wid/2, len/3, wid/2);
    }

    private void drawMotoDetails(GraphicsContext g, double len, double wid){
        g.setStroke(Color.rgb(230,230,230));
        g.setLineWidth(2);
        g.strokeLine(-len/2 + 1, 0, len/2 - 1, 0); // bike frame
    }

    private void drawAmbulanceDetails(GraphicsContext g, double len, double wid){
        // Red cross
        g.setFill(Color.rgb(210, 30, 30));
        double cx = 0, cy = 0;
        g.fillRect(cx - 2, cy - wid/4, 4, wid/2);
        g.fillRect(cx - len/10, cy - 2, len/5, 4);
        // light bar
        g.setFill(Color.rgb(80, 180, 255));
        g.fillRect(-len/6, -wid/2 + 1, len/3, 3);
    }

    private void drawLegend(GraphicsContext g, int cars, int trucks, int buses, int motos, int ambulances, int tlCount){
        double pad = 10;
        double boxW = 240;
        double boxH = 130;
        double x = pad;
        double y = canvas.getHeight() - boxH - pad;

        g.setFill(Color.rgb(30, 34, 38, 0.9));
        g.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g.setStroke(Color.rgb(70, 76, 84));
        g.setLineWidth(1.5);
        g.strokeRoundRect(x, y, boxW, boxH, 10, 10);

        g.setFill(Color.WHITE);
        g.setFont(Font.font(13));

        double rowY = y + 20;
        drawLegendRow(g, x + 12, rowY, Color.rgb(220,70,70), "Cars: " + cars);
        rowY += 20;
        drawLegendRow(g, x + 12, rowY, Color.rgb(70,120,230), "Trucks: " + trucks);
        rowY += 20;
        drawLegendRow(g, x + 12, rowY, Color.rgb(80,200,120), "Buses: " + buses);
        rowY += 20;
        drawLegendRow(g, x + 12, rowY, Color.rgb(230,230,230), "Motorcycles: " + motos);
        rowY += 20;
        drawLegendRow(g, x + 12, rowY, Color.rgb(250,240,120), "Ambulances: " + ambulances);
        rowY += 24;
        g.setFill(Color.rgb(255, 220, 50));
        g.fillRect(x + 12, rowY - 8, 20, 3);
        g.setFill(Color.WHITE);
        g.fillText("Road centerline • TLs: " + tlCount, x + 40, rowY);
    }

    private void drawLegendRow(GraphicsContext g, double sx, double sy, Color color, String label){
        g.setFill(color);
        g.fillOval(sx, sy - 6, 12, 12);
        g.setFill(Color.WHITE);
        g.fillText(label, sx + 20, sy + 3);
    }

    private NetworkParser.Junction findJunction(String id){
        return junctionIndex.get(id);
    }

    public static void main(String[] args) { launch(args); }
}

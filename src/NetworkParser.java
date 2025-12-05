import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.*;

/*
 * This class parses the network.net.xml file to extract the road network.
 */

public class NetworkParser {
    public static class Junction {
        public final String id;
        public final double x;
        public final double y;
        public final String type;
        public final String shape;

        public Junction(String id, double x, double y, String type, String shape) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.type = type;
            this.shape = shape;
        }
    }

    public static class Lane {
        public final String id;
        public final int index;
        public final double speed; // Max speed in m/s
        public final double length; // Lane length in meters
        public final double width; // Lane width in meters (default 3.2m in SUMO)

        public Lane(String id, int index, double speed, double length, double width) {
            this.id = id;
            this.index = index;
            this.speed = speed;
            this.length = length;
            this.width = width;
        }
    }

    public static class Edge {
        public final String id;
        public final String from;
        public final String to;
        public final List<Lane> lanes; // List of lanes in this edge

        public Edge(String id, String from, String to, List<Lane> lanes) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.lanes = lanes;
        }

        // Helper method to get total number of lanes
        public int getNumLanes() {
            return lanes.size();
        }

        // Helper method to get total edge width
        public double getTotalWidth() {
            return lanes.stream().mapToDouble(l -> l.width).sum();
        }
    }
    public static class Connection {
        public final String from;
        public final String to;
        public final int fromLane;
        public final int toLane;
        public final String dir;
        public final String tl; // for Traffic Light ID
        public final int linkIndex;

        public Connection(String from, String to, int fromLane, int toLane, String dir, String tl, int linkIndex) {
            this.from = from;
            this.to = to;
            this.fromLane = fromLane;
            this.toLane = toLane;
            this.dir = dir;
            this.tl = tl;
            this.linkIndex = linkIndex;
        }
    }
    public static class TrafficLightLogic {
        public final String id;
        public final String type;
        public final int programID;
        public final List<TrafficLightPhase> phases;
        
        public TrafficLightLogic(String id, String type, int programID, List<TrafficLightPhase> phases) {
            this.id = id;
            this.type = type;
            this.programID = programID;
            this.phases = phases;
        }
        
        public static class TrafficLightPhase {
            public final double duration;
            public final String state;
            
            public TrafficLightPhase(double duration, String state) {
                this.duration = duration;
                this.state = state;
            }
        }
    }

    public static class NetworkData {
        public final List<Junction> junctions;
        public final List<Edge> edges;
        public final List<TrafficLightLogic> trafficLights;
        public final List<Connection> connections;
        public final double minX, maxX, minY, maxY;

        public NetworkData(List<Junction> js, List<Edge> es, List<TrafficLightLogic> tls, List<Connection> conns, double minX, double maxX, double minY, double maxY) {
            this.junctions = js;
            this.edges = es;
            this.trafficLights = tls;
            this.connections = conns;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
    }

    public static NetworkData parse(String path) throws Exception {
        File f = new File(path);
        if (!f.exists())
            throw new IllegalArgumentException("Network file not found: " + path);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(f);

        dbf.setIgnoringComments(true);
        doc.getDocumentElement().normalize();

        // Parse junctions
        NodeList junctionNodes = doc.getElementsByTagName("junction");
        List<Junction> junctions = new ArrayList<>();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < junctionNodes.getLength(); i++) {
            Element e = (Element) junctionNodes.item(i);

            if (e.getAttribute("type").equals("internal")) {
                continue;
            }
            if (e.hasAttribute("x") && e.hasAttribute("y")) {
                String id = e.getAttribute("id");
                double x = Double.parseDouble(e.getAttribute("x"));
                double y = Double.parseDouble(e.getAttribute("y"));
                String type = e.hasAttribute("type") ? e.getAttribute("type") : "priority";
                String shape = e.hasAttribute("shape") ? e.getAttribute("shape") : null;
                junctions.add(new Junction(id, x, y, type, shape));
                if (x < minX)
                    minX = x;
                if (x > maxX)
                    maxX = x;
                if (y < minY)
                    minY = y;
                if (y > maxY)
                    maxY = y;
            }
        }

        // Parse edges with lanes
        NodeList edgeNodes = doc.getElementsByTagName("edge");
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < edgeNodes.getLength(); i++) {
            Element edgeElem = (Element) edgeNodes.item(i);
            if (!edgeElem.hasAttribute("from") || !edgeElem.hasAttribute("to"))
                continue;
            // Skip internal edges (intersection connectors)
            if (edgeElem.hasAttribute("function") && edgeElem.getAttribute("function").equals("internal"))
                continue;

            String edgeId = edgeElem.getAttribute("id");
            String from = edgeElem.getAttribute("from");
            String to = edgeElem.getAttribute("to");

            // Parse lanes within this edge
            NodeList laneNodes = edgeElem.getElementsByTagName("lane");
            List<Lane> lanes = new ArrayList<>();
            for (int j = 0; j < laneNodes.getLength(); j++) {
                Element laneElem = (Element) laneNodes.item(j);
                String laneId = laneElem.getAttribute("id");

                // Parse lane attributes with defaults
                int index = laneElem.hasAttribute("index")
                        ? Integer.parseInt(laneElem.getAttribute("index"))
                        : j;

                double speed = laneElem.hasAttribute("speed")
                        ? Double.parseDouble(laneElem.getAttribute("speed"))
                        : 13.89; // Default ~50 km/h in m/s

                double length = laneElem.hasAttribute("length")
                        ? Double.parseDouble(laneElem.getAttribute("length"))
                        : 0.0;

                double width = laneElem.hasAttribute("width")
                        ? Double.parseDouble(laneElem.getAttribute("width"))
                        : 3.2; // SUMO default lane width

                lanes.add(new Lane(laneId, index, speed, length, width));
            }

            // Only add edges that have lanes
            if (!lanes.isEmpty()) {
                edges.add(new Edge(edgeId, from, to, lanes));
            }
        }
        
        // Parse traffic lights
        NodeList tlLogicNodes = doc.getElementsByTagName("tlLogic");
        List<TrafficLightLogic> trafficLights = new ArrayList<>();
        
        System.out.println("\n=== Parsing Traffic Lights from XML ===");
        for (int i = 0; i < tlLogicNodes.getLength(); i++) {
            Element tlElem = (Element) tlLogicNodes.item(i);
            
            String tlId = tlElem.getAttribute("id");
            String type = tlElem.hasAttribute("type") ? tlElem.getAttribute("type") : "static";
            int programID = tlElem.hasAttribute("programID") 
                ? Integer.parseInt(tlElem.getAttribute("programID")) 
                : 0;
            
            // Parse phases
            NodeList phaseNodes = tlElem.getElementsByTagName("phase");
            List<TrafficLightLogic.TrafficLightPhase> phases = new ArrayList<>();
            
            for (int j = 0; j < phaseNodes.getLength(); j++) {
                Element phaseElem = (Element) phaseNodes.item(j);
                
                double duration = Double.parseDouble(phaseElem.getAttribute("duration"));
                String state = phaseElem.getAttribute("state");
                
                phases.add(new TrafficLightLogic.TrafficLightPhase(duration, state));
            }
            
            if (!phases.isEmpty()) {
                trafficLights.add(new TrafficLightLogic(tlId, type, programID, phases));
                System.out.println("Parsed TL [" + tlId + "] with " + phases.size() + " phases, initial state: " + phases.get(0).state);
            }
        }
        
        System.out.println("Total traffic lights parsed: " + trafficLights.size());

        // Parse connections
        NodeList connectionNodes = doc.getElementsByTagName("connection");
        List<Connection> connections = new ArrayList<>();
        
        for (int i = 0; i < connectionNodes.getLength(); i++) {
            Element connElem = (Element) connectionNodes.item(i);
            
            String from = connElem.getAttribute("from");
            String to = connElem.getAttribute("to");
            int fromLane = Integer.parseInt(connElem.getAttribute("fromLane"));
            int toLane = Integer.parseInt(connElem.getAttribute("toLane"));
            String dir = connElem.getAttribute("dir");
            String tl = connElem.hasAttribute("tl") ? connElem.getAttribute("tl") : null;
            int linkIndex = connElem.hasAttribute("linkIndex") 
                ? Integer.parseInt(connElem.getAttribute("linkIndex")) 
                : -1;
            
            connections.add(new Connection(from, to, fromLane, toLane, dir, tl, linkIndex));
        }
        
        System.out.println("Total connections parsed: " + connections.size());

        return new NetworkData(junctions, edges, trafficLights, connections, minX, maxX, minY, maxY);
    }
}
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

        public Junction(String id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    public static class Lane {
        public final String id;
        public final int index;
        public final double speed;    // Max speed in m/s
        public final double length;   // Lane length in meters
        public final double width;    // Lane width in meters (default 3.2m in SUMO)

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
        public final List<Lane> lanes;  // List of lanes in this edge

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

    public static class NetworkData {
        public final List<Junction> junctions;
        public final List<Edge> edges;
        public final double minX, maxX, minY, maxY;

        public NetworkData(List<Junction> js, List<Edge> es, double minX, double maxX, double minY, double maxY) {
            this.junctions = js;
            this.edges = es;
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
            if (e.hasAttribute("x") && e.hasAttribute("y")) {
                String id = e.getAttribute("id");
                double x = Double.parseDouble(e.getAttribute("x"));
                double y = Double.parseDouble(e.getAttribute("y"));
                junctions.add(new Junction(id, x, y));
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
                    : 13.89;  // Default ~50 km/h in m/s
                
                double length = laneElem.hasAttribute("length") 
                    ? Double.parseDouble(laneElem.getAttribute("length")) 
                    : 0.0;
                
                double width = laneElem.hasAttribute("width") 
                    ? Double.parseDouble(laneElem.getAttribute("width")) 
                    : 3.2;  // SUMO default lane width
                
                lanes.add(new Lane(laneId, index, speed, length, width));
            }
            
            // Only add edges that have lanes
            if (!lanes.isEmpty()) {
                edges.add(new Edge(edgeId, from, to, lanes));
            }
        }
        
        return new NetworkData(junctions, edges, minX, maxX, minY, maxY);
    }
}
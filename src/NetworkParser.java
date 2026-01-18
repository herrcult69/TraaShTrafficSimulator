import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses SUMO network files (.net.xml) to extract road network topology.
 * Reads junctions, edges, lanes, and connections from XML.
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see TrafficManager
 */
public class NetworkParser {
    private static final Logger logger = Logger.getLogger(NetworkParser.class.getName());
    
    /** Junction data from SUMO network. */
    public static class Junction {
        public final String id;
        public final double x;  // meters
        public final double y;  // meters
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

    /** Lane data from SUMO network. */
    public static class Lane {
        public final String id;
        public final int index;
        public final double speed;  // m/s
        public final double length; // meters
        public final double width;  // meters

        public Lane(String id, int index, double speed, double length, double width) {
            this.id = id;
            this.index = index;
            this.speed = speed;
            this.length = length;
            this.width = width;
        }
    }

    /** Connection between edges at a junction. */
    public static class Connection {
        public final String from;
        public final String to;
        /** Source lane index */
        public final int fromLane;
        /** Destination lane index */
        public final int toLane;
        /** Internal edge ID (junction crossing segment) */
        public final String via;
        /** Traffic light ID controlling this connection (null if uncontrolled) */
        public final String tl;
        /** Link index in the traffic light state string */
        public final int linkIndex;
        /**
         * Direction code: 's'=straight, 'l'/'L'=left, 'r'/'R'=right, 't'=turn around
         */
        public final String dir;
        /** Initial state character */
        public final String state;

        /**
         * Constructs a new Connection.
         * 
         * @param from      Source edge ID
         * @param to        Destination edge ID
         * @param fromLane  Source lane index
         * @param toLane    Destination lane index
         * @param via       Internal edge ID
         * @param tl        Traffic light ID
         * @param linkIndex Link index in TLS state
         * @param dir       Direction code
         * @param state     Initial state
         */
        public Connection(String from, String to, int fromLane, int toLane,
                String via, String tl, int linkIndex, String dir, String state) {
            this.from = from;
            this.to = to;
            this.fromLane = fromLane;
            this.toLane = toLane;
            this.via = via;
            this.tl = tl;
            this.linkIndex = linkIndex;
            this.dir = dir;
            this.state = state;
        }
    }

    /**
     * Data class representing an edge (road segment) with its lanes.
     */
    public static class Edge {
        /** The unique edge identifier */
        public final String id;
        /** The source junction ID */
        public final String from;
        /** The destination junction ID */
        public final String to;
        /** List of lanes in this edge */
        public final List<Lane> lanes;

        /**
         * Constructs a new Edge.
         * 
         * @param id    The edge identifier
         * @param from  Source junction ID
         * @param to    Destination junction ID
         * @param lanes List of lanes
         */
        public Edge(String id, String from, String to, List<Lane> lanes) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.lanes = lanes;
        }

        /**
         * Returns the number of lanes in this edge.
         * 
         * @return The lane count
         */
        public int getNumLanes() {
            return lanes.size();
        }

        /**
         * Returns the total width of this edge.
         * 
         * @return Total width in meters
         */
        public double getTotalWidth() {
            return lanes.stream().mapToDouble(l -> l.width).sum();
        }
    }

    /**
     * Container class for the complete network data including bounding box.
     */
    public static class NetworkData {
        /** List of all junctions */
        public final List<Junction> junctions;
        /** List of all edges */
        public final List<Edge> edges;
        /** List of all connections */
        public final List<Connection> connections;
        /** Minimum X coordinate of the network */
        public final double minX;
        /** Maximum X coordinate of the network */
        public final double maxX;
        /** Minimum Y coordinate of the network */
        public final double minY;
        /** Maximum Y coordinate of the network */
        public final double maxY;

        /**
         * Constructs a new NetworkData container.
         * 
         * @param js    List of junctions
         * @param es    List of edges
         * @param conns List of connections
         * @param minX  Minimum X coordinate
         * @param maxX  Maximum X coordinate
         * @param minY  Minimum Y coordinate
         * @param maxY  Maximum Y coordinate
         */
        public NetworkData(List<Junction> js, List<Edge> es, List<Connection> conns,
                double minX, double maxX, double minY, double maxY) {
            this.junctions = js;
            this.edges = es;
            this.connections = conns;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
    }

    /**
     * Parses a SUMO network XML file.
     * 
     * @param path Path to the .net.xml file
     * @return NetworkData containing all network information
     * @throws Exception if file not found or parsing fails
     */
    public static NetworkData parse(String path) throws Exception {
        logger.info("Parsing network file: " + path);
        File f = new File(path);
        if (!f.exists())
            throw new IllegalArgumentException("Network file not found: " + path);

        try {
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

            // Parse connections
            NodeList connectionNodes = doc.getElementsByTagName("connection");
            List<Connection> connections = new ArrayList<>();
            for (int i = 0; i < connectionNodes.getLength(); i++) {
                Element connElem = (Element) connectionNodes.item(i);

                String from = connElem.getAttribute("from");
                String to = connElem.getAttribute("to");
                int fromLane = connElem.hasAttribute("fromLane")
                        ? Integer.parseInt(connElem.getAttribute("fromLane"))
                        : 0;
                int toLane = connElem.hasAttribute("toLane")
                        ? Integer.parseInt(connElem.getAttribute("toLane"))
                        : 0;
                String via = connElem.hasAttribute("via") ? connElem.getAttribute("via") : "";
                String tl = connElem.hasAttribute("tl") ? connElem.getAttribute("tl") : null;
                int linkIndex = connElem.hasAttribute("linkIndex")
                        ? Integer.parseInt(connElem.getAttribute("linkIndex"))
                        : -1;
                String dir = connElem.hasAttribute("dir") ? connElem.getAttribute("dir") : "";
                String state = connElem.hasAttribute("state") ? connElem.getAttribute("state") : "";

                connections.add(new Connection(from, to, fromLane, toLane, via, tl, linkIndex, dir, state));
            }

            return new NetworkData(junctions, edges, connections, minX, maxX, minY, maxY);
        } catch (java.io.FileNotFoundException e) {
            System.err.println("ERROR: Network file not found: " + path);
            throw new Exception("Network file not found: " + path, e);
        } catch (org.xml.sax.SAXException e) {
            System.err.println("ERROR: Invalid XML format in network file");
            throw new Exception("Failed to parse network XML: Invalid format", e);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Invalid numeric value in network file");
            throw new Exception("Failed to parse network data: Invalid number format", e);
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error parsing network: " + e.getMessage());
            throw new Exception("Failed to parse network file", e);
        }
    }
}
package ui;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.*;

/** Parses a SUMO network.net.xml file extracting junction coordinates and edges. */
public class NetworkParser {
    public static class Junction {
        public final String id; public final double x; public final double y;
        public Junction(String id, double x, double y){this.id=id;this.x=x;this.y=y;}
    }
    public static class Edge {
        public final String id; public final String from; public final String to;
        public Edge(String id, String from, String to){this.id=id;this.from=from;this.to=to;}
    }
    public static class NetworkData {
        public final List<Junction> junctions; public final List<Edge> edges;
        public final double minX,maxX,minY,maxY;
        public NetworkData(List<Junction> js, List<Edge> es, double minX,double maxX,double minY,double maxY){
            this.junctions=js; this.edges=es; this.minX=minX; this.maxX=maxX; this.minY=minY; this.maxY=maxY;
        }
    }

    public static NetworkData parse(String path) throws Exception {
        File f = new File(path);
        if(!f.exists()) throw new IllegalArgumentException("Network file not found: "+path);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(f);
        doc.getDocumentElement().normalize();
        NodeList junctionNodes = doc.getElementsByTagName("junction");
        List<Junction> junctions = new ArrayList<>();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for(int i=0;i<junctionNodes.getLength();i++){
            Element e = (Element) junctionNodes.item(i);
            if(e.hasAttribute("x") && e.hasAttribute("y")) {
                String id = e.getAttribute("id");
                double x = Double.parseDouble(e.getAttribute("x"));
                double y = Double.parseDouble(e.getAttribute("y"));
                junctions.add(new Junction(id,x,y));
                if(x<minX) minX=x; if(x>maxX) maxX=x; if(y<minY) minY=y; if(y>maxY) maxY=y;
            }
        }
        NodeList edgeNodes = doc.getElementsByTagName("edge");
        List<Edge> edges = new ArrayList<>();
        for(int i=0;i<edgeNodes.getLength();i++){
            Element e = (Element) edgeNodes.item(i);
            if(!e.hasAttribute("from") || !e.hasAttribute("to")) continue;
            // skip internal edges often having function="internal"
            if(e.hasAttribute("function") && e.getAttribute("function").equals("internal")) continue;
            String id = e.getAttribute("id");
            edges.add(new Edge(id, e.getAttribute("from"), e.getAttribute("to")));
        }
        return new NetworkData(junctions, edges, minX, maxX, minY, maxY);
    }
}

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;


/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    Map<Long, Node> nodeList;
    Map<Long, List<Long>> neighbors;

    private class Node {
        long id;
        double lon;
        double lat;

        Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
        }
    }

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            nodeList = new HashMap<>();
            neighbors = new HashMap<>();

            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputFile, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        ArrayList<Long> removedItems = new ArrayList<>();
        for (long id: nodeList.keySet()) {
            if (neighbors.get(id).size() == 0) {
                removedItems.add(id);
            }
        }
        for (long l: removedItems) {
            nodeList.remove(l);
            neighbors.remove(l);
        }
    }

    /** Returns an iterable of all vertex IDs in the graph. */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return nodeList.keySet();
    }

    /** Returns ids of all vertices adjacent to v. */
    Iterable<Long> adjacent(long v) {
        return neighbors.get(v);
    }

    /** Returns the Euclidean distance between vertices v and w, where Euclidean distance
     *  is defined as sqrt( (lonV - lonV)^2 + (latV - latV)^2 ). */
    double distance(long v, long w) {
        double lonV = nodeList.get(v).lon;
        double latV = nodeList.get(v).lat;
        double lonW = nodeList.get(w).lon;
        double latW = nodeList.get(w).lat;
        return Math.sqrt(Math.pow((lonV - lonW), 2) + Math.pow((latV - latW), 2));
    }

    /** Returns the vertex id closest to the given longitude and latitude. */
    long closest(double lon, double lat) {
        Node closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Node n: nodeList.values()) {
            double d = helperDistance(lon, lat, n);
            if (d < closestDist) {
                closest = n;
                closestDist = d;
            }
        }
        return closest.id;
    }

    private double helperDistance(double lon, double lat, Node n) {
        double lonN = n.lon;
        double latN = n.lat;
        return Math.sqrt(Math.pow((lon - lonN), 2) + Math.pow((lat - latN), 2));
    }

    /** Longitude of vertex v. */
    double lon(long v) {
        return nodeList.get(v).lon;
    }

    /** Latitude of vertex v. */
    double lat(long v) {
        return nodeList.get(v).lat;
    }

    public void addNode(long id, double lon, double lat) {
        Node n = new Node(id, lon, lat);
        nodeList.put(id, n);
        neighbors.put(id, new ArrayList<>());
    }

    public void connectNodes(LinkedList<Long> adjacencies) {
        Long[] a = adjacencies.toArray(new Long[0]);
        for (int i = 0; i < a.length - 1; i++) {
            neighbors.get(a[i]).add(a[i + 1]);
            neighbors.get(a[i + 1]).add(a[i]);
        }
    }
}

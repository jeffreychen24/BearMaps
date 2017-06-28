import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a LinkedList of <code>Long</code>s representing the shortest path from st to dest, 
     * where the longs are node IDs.
     */

    private static class FringeObj implements Comparator<FringeObj> {
        long id;
        double distance;

        FringeObj(long id, double distance) {
            this.id = id;
            this.distance = distance;
        }

        @Override
        public int compare(FringeObj o1, FringeObj o2) {
            double d = o1.distance - o2.distance;
            if (d < 0) {
                return -1;
            } else if (d > 0) {
                return 1;
            }
            return 0;
        }
    }

    public static LinkedList<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                                double destlon, double destlat) {
        long source = g.closest(stlon, stlat);
        long dest = g.closest(destlon, destlat);

        HashMap<Long, Long> edgeTo = new HashMap<>();
        HashMap<Long, Double> distTo = new HashMap<>();

        PriorityQueue<FringeObj> pq = new PriorityQueue<>(new FringeObj(source, 0));
        pq.add(new FringeObj(source, g.distance(source, dest)));
        distTo.put(source, 0.0);
        boolean found = false;
        while (!found) {
            FringeObj v = pq.peek();
            for (long l: g.adjacent(v.id)) {
                if (v.id == l) {
                    continue;
                }
                double newDistance = distTo.get(v.id) + g.distance(l, v.id);
                if (distTo.containsKey(l)) {
                    if (distTo.get(l) <= newDistance) {
                        continue;
                    }
                }
                distTo.put(l, newDistance);
                edgeTo.put(l, v.id);
                pq.add(new FringeObj(l, newDistance + g.distance(l, dest)));
                if (l == dest) {
                    found = true;
                }
            }
            pq.remove();
        }
        Stack<Long> s = new Stack<>();
        LinkedList<Long> rv = new LinkedList<>();
        s.add(dest);
        long n = edgeTo.get(dest);
        while (n != source) {
            s.add(n);
            n = edgeTo.get(n);

        }
        s.add(source);
        while (!s.isEmpty()) {
            rv.add(s.pop());
        }
        return rv;
    }
}

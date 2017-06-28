import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.LinkedList;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    QuadTree images;
    double[] ovLonDPP;

    /**
     * info is a double array containing 4 elements, ul_lon, ul_lat, lr_lon.
     * and lr_lat in this order.
     */
    private class QuadTree {
        String name;
        int depth;
        double[] info;
        QuadTree upperLeft;
        QuadTree upperRight;
        QuadTree lowerLeft;
        QuadTree lowerRight;

        QuadTree(String name, int depth, double[] info) {
            this.name = name;
            this.depth = depth;
            this.info = info;
        }

        public String toString() {
            return name;
        }

    }

    /** imgRoot is the name of the directory containing the images.
     *  You may not actually need this for your class. */
    public Rasterer(String imgRoot) {
        double[] info = {-122.2998046875, 37.892195547244356, -122.2119140625, 37.82280243352756};
        images = new QuadTree("img/root.png", 0, info);
        instantiateQT(images, "", 1);
        ovLonDPP = new double[8];
        ovLonDPP[0] = 0.00034332275390625;
        ovLonDPP[1] = 0.000171661376953125;
        ovLonDPP[2] = 0.0000858306884765625;
        ovLonDPP[3] = 0.00004291534423828125;
        ovLonDPP[4] = 0.000021457672119140625;
        ovLonDPP[5] = 0.000010728836059570312;
        ovLonDPP[6] = 0.000005364418029785156;
        ovLonDPP[7] = 0.000002682209014892578;
    }

    private void instantiateQT(QuadTree root, String imgNum, int depth) {
        if (depth > 7) {
            return;
        }
        String name = "img/" + imgNum;
        root.upperLeft = new QuadTree(name + "1.png", depth, makeInfo(root.info, "ul"));
        root.upperRight = new QuadTree(name + "2.png", depth, makeInfo(root.info, "ur"));
        root.lowerLeft = new QuadTree(name + "3.png", depth, makeInfo(root.info, "ll"));
        root.lowerRight = new QuadTree(name + "4.png", depth, makeInfo(root.info, "lr"));
        instantiateQT(root.upperLeft, imgNum + "1", depth + 1);
        instantiateQT(root.upperRight, imgNum + "2", depth + 1);
        instantiateQT(root.lowerLeft, imgNum + "3", depth + 1);
        instantiateQT(root.lowerRight, imgNum + "4", depth + 1);

    }

    private double[] makeInfo(double[] rootInfo, String pos) {
        double[] info = new double[4];
        System.arraycopy(rootInfo, 0, info, 0, 4);
        double halfLon = (rootInfo[0] + rootInfo[2]) / 2;
        double halfLat = (rootInfo[1] + rootInfo[3]) / 2;
        if (pos.equals("ul")) {
            info[2] = halfLon;
            info[3] = halfLat;
        } else if (pos.equals("ur")) {
            info[0] = halfLon;
            info[3] = halfLat;
        } else if (pos.equals("ll")) {
            info[1] = halfLat;
            info[2] = halfLon;
        } else {
            info[0] = halfLon;
            info[1] = halfLat;

        }
        return info;
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     * <p>
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified:
     * "render_grid"   -> String[][], the files to display
     * "raster_ul_lon" -> Number, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Number, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Number, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Number, the bounding lower right latitude of the rastered image <br>
     * "depth"         -> Number, the 1-indexed quadtree depth of the nodes of the rastered image.
     *                    Can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" -> Boolean, whether the query was able to successfully complete. Don't
     *                    forget to set this to true! <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        LinkedList<QuadTree> intersections = new LinkedList<>();


        Map<String, Object> results = new HashMap<>();

        double lonDPP = (params.get("lrlon") - params.get("ullon")) / params.get("w");

        int depth = 7;
        for (int i = 0; i < ovLonDPP.length; i++) {
            if (ovLonDPP[i] <= lonDPP) {
                depth = i;
                break;
            }
        }
        readerGrid(images, depth, params, intersections);
        results.put("render_grid", makeGrid(intersections));
        results.put("raster_ul_lon", intersections.get(0).info[0]);
        results.put("raster_ul_lat", intersections.get(0).info[1]);
        results.put("raster_lr_lon", intersections.get(intersections.size() - 1).info[2]);
        results.put("raster_lr_lat", intersections.get(intersections.size() - 1).info[3]);
        results.put("depth", depth);
        results.put("query_success", true);
        return results;
    }

    private String[][] makeGrid(LinkedList<QuadTree> intersections) {
        TreeMap<Double, LinkedList<String>> sorted = new TreeMap<>();
        for (QuadTree qt: intersections) {
            if (!sorted.containsKey(qt.info[1])) {
                LinkedList<String> ll = new LinkedList<>();
                ll.add(qt.name);
                sorted.put(qt.info[1], ll);
            } else {
                sorted.get(qt.info[1]).add(qt.name);
            }
        }
        String[][] rv = new String[sorted.size()][intersections.size() / sorted.size()];
        int counter = sorted.size() - 1;
        for (double d: sorted.keySet()) {
            rv[counter] = sorted.get(d).toArray(new String[0]);
            counter--;
        }
        return rv;
    }

    private void readerGrid(QuadTree qt, int depth, Map<String, Double> params,
                            LinkedList<QuadTree> intersections) {
        if (depth == 0) {
            intersections.add(qt);
        } else {
            if (intersects(qt.upperLeft, params)) {
                readerGrid(qt.upperLeft, depth - 1, params, intersections);
            }
            if (intersects(qt.upperRight, params)) {
                readerGrid(qt.upperRight, depth - 1, params, intersections);
            }
            if (intersects(qt.lowerLeft, params)) {
                readerGrid(qt.lowerLeft, depth - 1, params, intersections);
            }
            if (intersects(qt.lowerRight, params)) {
                readerGrid(qt.lowerRight, depth - 1, params, intersections);
            }
        }
    }

    private boolean intersects(QuadTree qt, Map<String, Double> params) {
        return !(qt.info[2] <= params.get("ullon") || qt.info[0] >= params.get("lrlon")
                || qt.info[3] >= params.get("ullat") || qt.info[1] <= params.get("lrlat"));
    }

}

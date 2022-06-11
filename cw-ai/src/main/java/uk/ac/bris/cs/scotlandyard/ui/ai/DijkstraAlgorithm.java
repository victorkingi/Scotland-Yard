package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;

/*
    It only finds shortest distance from node 1 to 98 accessible nodes since we
    used a directed graph. Hence, if mrX location is a node that isn't part of the 98,
    getPath(); will return 1 node(mrXLocation).
    Credit to : https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
*/
public class DijkstraAlgorithm {
    private final List<Edge> edges;
    private Set<Node> visitedNodes;
    private Set<Node> unvisitedNodes;
    private Map<Node, Node> previousNodes;
    private Map<Node, Integer> distance;

    DijkstraAlgorithm(final Graph graph) {
        // create a copy of the array to use
        this.edges = new ArrayList<>(graph.getEdges());
    }
    /*
        finds minimum distance from source node which is node 1
        to 98 accessible nodes and stores the values in distance field
    */
    public void execute(final Node source) {
        visitedNodes = new HashSet<>();
        unvisitedNodes = new HashSet<>();
        distance = new HashMap<>();
        previousNodes = new HashMap<>();
        distance.put(source, 0);
        unvisitedNodes.add(source);
        while (unvisitedNodes.size() > 0) {
            Node node = getMinimum(unvisitedNodes);
            visitedNodes.add(node);
            unvisitedNodes.remove(node);
            findMinimalDistances(node);
        }
    }

    private void findMinimalDistances(final Node node) {
        List<Node> adjacentNodes = getAdjacentNodes(node);
        for (final Node target : adjacentNodes) {
            if (getShortestDistance(target) > getShortestDistance(node)
                    + getDistance(node, target)) {
                distance.put(target, getShortestDistance(node)
                        + getDistance(node, target));
                previousNodes.put(target, node);
                unvisitedNodes.add(target);
            }
        }

    }

    private int getDistance(final Node node, final Node target) {
        for (final Edge edge : edges) {
            if (edge.getSource().equals(node)
                    && edge.getDestination().equals(target)) {
                return edge.getWeight();
            }
        }
        throw new RuntimeException();
    }

    private List<Node> getAdjacentNodes(final Node node) {
        List<Node> adjacent = new ArrayList<>();
        for (final Edge edge : edges) {
            if (edge.getSource().equals(node)
                    && !isVisited(edge.getDestination())) {
                adjacent.add(edge.getDestination());
            }
        }
        return adjacent;
    }

    private Node getMinimum(final Set<Node> nodes) {
        Node minimum = null;
        for (final Node node : nodes) {
            if (minimum == null) {
                minimum = node;
            } else {
                if (getShortestDistance(node) < getShortestDistance(minimum)) {
                    minimum = node;
                }
            }
        }
        return minimum;
    }

    private boolean isVisited(final Node node) {
        return visitedNodes.contains(node);
    }

    private int getShortestDistance(final Node destination) {
        Integer d = distance.get(destination);
        return Objects.requireNonNullElse(d, Integer.MAX_VALUE);
    }

    public LinkedList<Node> getPath(final Node target) {
        LinkedList<Node> path = new LinkedList<>();
        if(!previousNodes.entrySet().isEmpty()) {
            Node step = target;
            path.add(step);
            for (final var p : previousNodes.entrySet()) {
                if (p.getKey().equals(step)) {
                    step = previousNodes.get(step);
                    path.add(step);

                }
            }
            Collections.reverse(path);
        }
        // Put it into the correct order
        return path;
    }

}
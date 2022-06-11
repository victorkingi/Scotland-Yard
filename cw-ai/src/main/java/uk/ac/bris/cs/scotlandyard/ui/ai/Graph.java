package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.List;

public class Graph {
    private final List<Edge> edges;

    protected Graph(final List<Edge> edges) { this.edges = edges; }

    public final List<Edge> getEdges() {
        return edges;
    }
}
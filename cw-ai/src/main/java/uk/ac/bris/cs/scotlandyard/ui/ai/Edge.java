package uk.ac.bris.cs.scotlandyard.ui.ai;

public class Edge  {
    private final String id;
    private final Node source;
    private final Node destination;
    private final int weight;

    protected Edge(final String id, final Node source,
                   final Node destination, final int weight) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    public final String getId() { return id; }
    public final Node getDestination() { return destination; }
    public final Node getSource() { return source; }
    public final int getWeight() { return weight; }
    @Override
    public final String toString() { return source + " " + destination; }
}
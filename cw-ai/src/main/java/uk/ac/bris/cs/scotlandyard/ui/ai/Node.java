package uk.ac.bris.cs.scotlandyard.ui.ai;

public class Node {
    private final Integer id;
    private final String name;

    protected Node(final Integer id, final String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    @Override
    public String toString() { return name; }
}

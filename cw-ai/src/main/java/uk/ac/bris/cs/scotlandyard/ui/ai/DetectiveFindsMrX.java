package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;
import java.util.*;

public class DetectiveFindsMrX {
    private List<Node> nodes;
    private List<Edge> edges;
    private final List<Integer> mrXReveal;
    private final Map<Move, Integer> score;

    protected DetectiveFindsMrX(final List<Integer> mrXReveal,
                                final Map<Move, Integer> score) {
        this.mrXReveal = mrXReveal;
        this.score = score;
    }

    public final Map<Move, Integer> getScore() { return score; }
    /*
        Only updates the detective's move score if mrX has revealed his location
    */
    protected void updateDetectiveScore(@Nonnull final Board board, final Move move,
                                        final LinkedList<Node> shortestPath) {
        checkIfMrXRevealedLocation(board);
        if(!mrXReveal.isEmpty()) {
            //Nodes adjacent to move's source
            final var nodesA = board.getSetup().graph.adjacentNodes(move.source());
            //nodes adjacent to move's destination
            final Set<Integer> nodesB = board.getSetup()
                    .graph.adjacentNodes(move.visit(new Move.Visitor<>() {
                        @Override
                        public Integer visit(Move.SingleMove move) {
                            return move.destination;
                        }

                        @Override
                        public Integer visit(Move.DoubleMove move) {
                            return move.destination2;
                        }
                    }));

            checkShortestPath(move, shortestPath);
            checkIfNodesAdjacentHaveMrX(move, nodesA);
            checkIfNodesAdjacentHaveMrX(move, nodesB);
        }
    }
    /*
        Finds shortest path from node 1 to 98 nodes in the new graph
    */
    public LinkedList<Node> executeDijkstra(@Nonnull final Board board,
                                               final Integer destination) {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        LinkedList<Node> shortestPath = new LinkedList<>();

        var nodesNo = board.getSetup().graph.nodes().size();
        createNodes(nodesNo);
        createEdges(board, nodesNo);

        Graph graph = new Graph(edges);
        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
        //if the source node isn't node 1, null pointer exception may be returned hence, we used node 1
        dijkstra.execute(nodes.get(0));

        for(final Node dest : nodes) {
            if (dest.getId().equals(destination)) {
                shortestPath = dijkstra.getPath(dest);
                break;
            }

        }
        return shortestPath;
    }

    private void createEdges(@Nonnull final Board board, final int nodesNo) {
        for(int i = 1; i < nodesNo+1; i++) {
            for(int j = i+1; j < nodesNo+1; j++) {
                if(!board.getSetup().graph.hasEdgeConnecting(i, j)) continue;
                addEdges("Edge_" + i, i-1, j-1);
            }
        }
    }

    private void createNodes(final int nodesNo) {
        for (int i = 1; i < nodesNo+1; i++) {
            Node location = new Node(i, "Node_" + i);
            nodes.add(location);
        }
    }
    /*
        adds edge values to a List of edges and initialise distance between nodes
        as 1
    */
    private void addEdges(final String id, final int source,
                          final int dest) {
        Edge a = new Edge(id, nodes.get(source), nodes.get(dest), 1);
        edges.add(a);
    }

    protected void checkIfMrXRevealedLocation(@Nonnull final Board board) {
        if (!board.getMrXTravelLog().isEmpty()) {
            for (final LogEntry reveal : board.getMrXTravelLog()) {
                if(reveal.location().isPresent()
                        && !mrXReveal.contains(reveal.location().get())) {
                    mrXReveal.add(reveal.location().get());
                }
            }
        }
    }
    /*
        checks if a move's destination node is equal to any node in
        the returned linkedList of shortestPath, if so, score for the move is increased
        by 1
    */
    private void checkShortestPath(final Move m,
                                   final LinkedList<Node> shortestPath) {
        if(!shortestPath.isEmpty()) {
            for (final Node node : shortestPath) {
                if (node.getId() == m.source()) continue;
                if (m.visit(new Move.Visitor<>() {
                    @Override
                    public Integer visit(Move.SingleMove move) {
                        return move.destination;
                    }

                    @Override
                    public Integer visit(Move.DoubleMove move) {
                        return move.destination2;
                    }
                }) == node.getId()) {
                    increaseScoreValue(m);
                }
            }
        }
    }

    private void increaseScoreValue(final Move move) {
        Integer value = score.getOrDefault(move, 1);
        if (score.containsKey(move)) value++;
        score.put(move, value);
    }
    /*
        this will check nodes adjacent to the source and nodes adjacent to the
        move's destination if mrX is there
    */
    private void checkIfNodesAdjacentHaveMrX(final Move move, final Set<Integer> n) {
        Integer value = score.getOrDefault(move, 1);
        for (final int a : n) {
            Integer mrXLocation = mrXReveal.get(mrXReveal.size() - 1);
            if(mrXLocation == a) {
                if(score.containsKey(move)) {
                    value++;
                }
                score.put(move, value);
            }
        }
    }
}

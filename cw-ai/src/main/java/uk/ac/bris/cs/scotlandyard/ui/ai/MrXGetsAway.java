package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MrXGetsAway {

    private final Map<Move, Integer> score;

    MrXGetsAway(final Map<Move, Integer> score) { this.score = score; }

    public final Map<Move, Integer> getScore() { return score; }

    protected void updateMrXScore(@Nonnull final Board board, final Move move) {
        //nodes adjacent to move's source
        final var nodesA = board.getSetup().graph.adjacentNodes(move.source());
        //nodes adjacent to move's destination
        final var nodesB = board.getSetup()
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
        final ImmutableSet<Piece> detectives = board.getPlayers().stream()
                .dropWhile(Piece::isMrX).collect(ImmutableSet.toImmutableSet());

        detectives.stream().forEach(d -> checkVariousConditions(board, move, nodesA, nodesB, (Detective) d));
    }

    private void checkVariousConditions(@Nonnull final Board board, final Move move,
                                        final Set<Integer> a, final Set<Integer> b,
                                        final Detective d) {
        final Integer location = board.getDetectiveLocation(d).get();
        final var moves = board.getAvailableMoves().asList();

        checkDetectiveLocation(move, a, location);
        checkDetectiveLocation(move, b, location);
        moves.stream().forEach(x -> checkNumberOfAdjacentNodes(board, move, a, x));
        moves.stream().forEach(x -> checkNumberOfAdjacentNodes(board, move, b, x));
    }
    /*
        If mrX move source or destination node has more adjacent nodes, then the score for that move
        is increased by 1 (checkNumberOfAdjacentNodes();), however if any of the nodes has a detective
        (checkDetectiveLocation();), the value for that move is
        decreased by 1
    */
    private void checkNumberOfAdjacentNodes(@Nonnull final Board board, final Move move,
                                            final Set<Integer> set, final Move x) {
        //contains second iteration of moves, adjacent nodes to its destination
        final var nodesA = board.getSetup().graph.adjacentNodes( x.visit(new Move.Visitor<>() {
            @Override
            public Integer visit(Move.SingleMove move) {
                return move.destination;
            }

            @Override
            public Integer visit(Move.DoubleMove move) {
                return move.destination2;
            }
        }));
        if (set.size() > board.getSetup()
                .graph.adjacentNodes(x.source()).size()) increaseScoreValue(move);
        else if (set.size() < board.getSetup()
                .graph.adjacentNodes(x.source()).size()) increaseScoreValue(x);
        if(set.size() > nodesA.size()) increaseScoreValue(move);
        else if (set.size() < nodesA.size()) increaseScoreValue(x);
    }

    private void increaseScoreValue(final Move move) {
       Integer value = score.getOrDefault(move, 1);
        if (score.containsKey(move)) value++;
        score.put(move, value);
    }
    /*
        add to score value only if a detective isn't in that position
    */
    private void checkDetectiveLocation(Move move, Set<Integer> set, Integer location) {
        Integer value = score.getOrDefault(move, 1);
        for(final Integer w : set) {
            if(location != w) increaseScoreValue(move);
            else {
                if(score.containsKey(move)) value--;
                score.put(move, value);
            }
        }
    }
}

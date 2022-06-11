package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import uk.ac.bris.cs.scotlandyard.model.Move.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import static uk.ac.bris.cs.scotlandyard.model.MyGameStateFactory.numberOfRoundsLeft;
import static uk.ac.bris.cs.scotlandyard.model.MyGameStateFactory.remainingDetectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
    Helper methods to make various moves, perform checks on who should make the move and returns the moves
*/
public class Moves {

    protected static ImmutableSet<Move.SingleMove> makeSingleMoves(final GameSetup setup,
                                                                   final List<Player> detectives,
                                                                   final Player player,
                                                                   final int source) {
        // Creates all possible single moves from the source node
        final var singleMoves = new ArrayList<Move.SingleMove>();
        for (int destination : setup.graph.adjacentNodes(source)) {
            var occupied = false;
            for (Player detective : detectives)	{
                if (detective.location() == destination) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;
            for (Transport t : Objects.requireNonNull(setup.graph
                    .edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
                if (player.has(t.requiredTicket())) {
                    singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
                }
                if (player.has(Ticket.SECRET) && player.isMrX()) {
                    singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
                }
            }
        }
        return ImmutableSet.copyOf(singleMoves);
    }

    protected static ImmutableSet<DoubleMove> makeDoubleMove(final GameSetup setup,
                                                             final List<Player> detectives,
                                                             final Player player,
                                                             final int source) {
        // Creates all possible double moves from the source node
        final var doubleMove = new ArrayList<Move.DoubleMove>();
        for (int firstDestination : setup.graph.adjacentNodes(source)) {
            var occupied = false;
            for (Player detective : detectives) {
                if (detective.location() == firstDestination) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;
            for (Transport t : Objects.requireNonNull(setup.graph
                    .edgeValueOrDefault(source, firstDestination, ImmutableSet.of()))) {
                if (player.has(t.requiredTicket())) {
                    ImmutableSet<SingleMove> singleMoveSet = (makeSingleMoves
                            (setup, detectives, player, firstDestination));
                    for (SingleMove move : singleMoveSet) {
                        boolean valid = true;
                        // ensures mrX has enough tickets if he wants to do a double move
                        if ((t.requiredTicket() == move.ticket)
                                && !(player.hasAtLeast(move.ticket, 2))) valid = false;
                        if (valid) doubleMove.add(new DoubleMove(player.piece(), source,
                                t.requiredTicket(), firstDestination, move.ticket, move.destination));
                    }
                }
            }
            //generate double moves with a secret ticket
            if (player.has(Ticket.SECRET)) {
                ImmutableSet<SingleMove> singleMoveSet = (makeSingleMoves(setup,
                        detectives, player, firstDestination));
                for (SingleMove move : singleMoveSet) {
                    doubleMove.add(new Move.DoubleMove(player.piece(), source,
                            Ticket.SECRET, firstDestination, move.ticket, move.destination));
                }
            }
        }
        return ImmutableSet.copyOf(doubleMove);
    }
    /*
        Returns all of the detective moves, given a list of detectives
    */
    protected static ImmutableSet<Move> getDetectiveMoves(final GameSetup setup,
                                                          final List<Player> detectives,
                                                          final Player d) {
        ImmutableSet<SingleMove> singleMoveSet = (makeSingleMoves(setup, detectives, d, d.location()));
        List<SingleMove> singleMoves = new ArrayList<>(singleMoveSet);

        return ImmutableSet.<Move>builder().addAll(singleMoves).build();
    }
    /*
       Only returns detective moves for detectives in remaining set
    */
    protected static List<Move> getRemainingDetectivesMovesList(final GameSetup setup,
                                                                final List<Player> detectives,
                                                                final ImmutableSet<Piece> remaining,
                                                                final Piece piece) {
        List<Move> detectiveMoves = new ArrayList<>();
        if (piece.isDetective()) {
            List<Player> remainingDetectiveList;
            remainingDetectiveList = remainingDetectives(remaining, detectives);

            for(final var p : remainingDetectiveList) {
                ImmutableSet<Move> detectiveMoveSet = getDetectiveMoves(setup, detectives, p);
                detectiveMoves.addAll(detectiveMoveSet);
            }
        }
        return detectiveMoves;
    }

    protected static ImmutableSet<Move> getMrXMoves(final GameSetup setup,
                                                    final List<Player> detectives,
                                                    final Player mrX,
                                                    final ImmutableList<LogEntry> log) {
        // Returns an individual list of only mrX's possible moves including double moves
        List<DoubleMove> doubleMoves = new ArrayList<>();
        ImmutableSet<SingleMove> singleMoveSet = (makeSingleMoves(setup, detectives, mrX, mrX.location()));
        List<SingleMove> singleMoves = new ArrayList<>(singleMoveSet);
        if (mrX.has(Ticket.DOUBLE) && (numberOfRoundsLeft(setup, log) > 1)) {
            ImmutableSet<Move.DoubleMove> doubleMoveSet = (makeDoubleMove(setup,
                    detectives, mrX, mrX.location()));
            doubleMoves.addAll(doubleMoveSet);
        }
        return ImmutableSet.<Move>builder().addAll(singleMoves).addAll(doubleMoves).build();
    }
    /*
        if remaining detectives all don't have tickets or piece selected is mrX, mrX moves are returned
    */
    protected static List<Move> checkIfAllDetectivesStillHaveTickets(final ImmutableSet<Move> mrXMoves,
                                                                     final boolean b) {
        List<Move> newMoves = new ArrayList<>();
        if(b) {
            newMoves.addAll(mrXMoves);
        }
        return newMoves;
    }

    protected static boolean isDoubleMove(final Move move) {
        // Checks a move to see if its a double move
        return move.visit(new Move.Visitor<>() {
            @Override
            public Boolean visit(SingleMove move1) {
                return false;
            }
            @Override
            public Boolean visit(DoubleMove move1) {
                return true;
            }
        });
    }
}

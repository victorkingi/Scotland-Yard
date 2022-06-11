package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Board.TicketBoard;
import uk.ac.bris.cs.scotlandyard.model.Move.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.Visitor;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.ac.bris.cs.scotlandyard.model.Moves.*;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private static final class MyGameState implements GameState {
		private final GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		private final Ticket[] mrXFirstTicket = new Ticket[1];

		private MyGameState(final GameSetup setup,
							final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log,
							final Player mrX,
							final List<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			winner = ImmutableSet.<Piece>builder().build();
			gameProcess(setup, remaining, getMrXMoves(setup, detectives, mrX, log), detectives);
		}
		/*
			stages from initialising the game to selecting a winner
		*/
		private void gameProcess(final GameSetup setup,
								 final ImmutableSet<Piece> remaining,
								 final ImmutableSet<Move> mrXMoves,
								 final List<Player> detectives) {
			checkExceptionsDuringInitialisation(setup, detectives);
			//for each remaining, moves field is updated to consist of all possible moves by each piece
			remaining.forEach(piece -> updateMovesField(setup, remaining,
					mrXMoves, detectives, piece));
			checkWinner();
		}

		@Nonnull
		@Override public final GameSetup getSetup() { return setup; }
		@Nonnull
		@Override public final ImmutableSet<Piece> getPlayers() {
			final List<Piece> otherPlayers = this.detectives.stream()
					.map(Player::piece).collect(Collectors.toList());
			return ImmutableSet.<Piece>builder().add(mrX.piece()).addAll(otherPlayers).build();
		}
		@Nonnull
		@Override public final Optional<Integer> getDetectiveLocation(final Detective detective) {
			for (final var p : detectives) {
				if (p.piece().equals(detective)) return Optional.of(p.location());
			}
			return Optional.empty();
		}
		@Nonnull
		@Override public final Optional<TicketBoard> getPlayerTickets(final Piece piece) {
			if(piece.isMrX()) return Optional.of(new StoreTickets(mrX.tickets()));
			for(final var p : detectives) {
				if (p.piece().equals(piece)) return Optional.of(new StoreTickets(p.tickets()));
			}
			return Optional.empty();
		}
		@Nonnull
		@Override public final ImmutableList<LogEntry> getMrXTravelLog() { return log; }
		@Nonnull
		@Override public final ImmutableSet<Piece> getWinner() { return winner; }
		@Nonnull
		@Override public final ImmutableSet<Move> getAvailableMoves() { return moves; }
		@Override public final GameState advance(final Move move) {
			if (winner.isEmpty()) {
				final List<Piece> det = this.detectives.stream().map(Player::piece).collect(Collectors.toList());
				// Checks move given if it's possible
				if (!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
				remaining.forEach(p -> moveAPiece(move, det, p));
				if (remaining.isEmpty()) remaining = ImmutableSet.<Piece>builder().add(mrX.piece()).build();
			}
			return new MyGameState(setup, remaining, log, mrX, detectives);
		}

		private void updateMovesField(final GameSetup setup,
									  final ImmutableSet<Piece> remaining,
									  final ImmutableSet<Move> mrXMoves1,
									  final List<Player> detectives,
									  final Piece piece) {
			// Creates movesSet for detectives and mrX if are remaining
			final List<Move> detectiveMoves = getRemainingDetectivesMovesList(setup, detectives, remaining, piece);
			final List<Move> mrXMoves = checkIfAllDetectivesStillHaveTickets(mrXMoves1,
					remainingDetectives(remaining, detectives).isEmpty() || piece.isMrX());

			moves = ImmutableSet.<Move>builder().addAll(detectiveMoves).addAll(mrXMoves).build();
		}

		private Ticket getMoveTicket(final Move move) {
			// Returns the move's corresponding ticket using double dispatch
			return move.visit(new Visitor<>() {
				@Override
				public Ticket visit(SingleMove move1) {
					return move1.ticket;
				}
				@Override
				public Ticket visit(DoubleMove move1) {
					mrXFirstTicket[0] = move1.ticket1;
					return move1.ticket2;
				}
			});
		}

		private void moveAPiece(final Move move, final List<Piece> detectives2, final Piece p) {
			if (move.commencedBy().equals(p) && p.isMrX()) moveMrX(move, detectives2, p);
			else if (move.commencedBy().equals(p) && p.isDetective())
				detectives.forEach(d -> moveDetective(move, p, d));
		}

		private void moveDetective(final Move move, final Piece p, Player d) {
				if (move.commencedBy().equals(d.piece())) {
					final List<Player> newDetectives = new ArrayList<>(List.copyOf(detectives));
					int indexOf = newDetectives.indexOf(d);
					d = changePlayerLocation(d, move);
					d = changeTicketNumber(d, move);
					newDetectives.set(indexOf, d);
					detectives = ImmutableList.<Player>builder().addAll(newDetectives).build();
				}
			remaining = Sets.difference(remaining, ImmutableSet.of(p)).immutableCopy();
		}

		private void moveMrX(final Move move, final List<Piece> detectives2, final Piece p) {
			mrX = changePlayerLocation(mrX, move);
			mrX = changeTicketNumber(mrX, move);
			final Ticket mrXTicket = getMoveTicket(move);
			remaining = Sets.difference(remaining, ImmutableSet.of(p)).immutableCopy();
			if (remaining.isEmpty()) remaining = ImmutableSet.<Piece>builder().addAll(detectives2).build();

			logUpdate(move, mrXTicket);
		}

		private Player changePlayerLocation(Player player, final Move move) {
			// Changes a player's location based upon a given move
			final Player finalPlayer = player;
			final Integer dest = move.visit(new Visitor<>() {
				@Override
				public final Integer visit(SingleMove move) { return move.destination; }
				@Override
				public final Integer visit(DoubleMove move) {
					if(move.commencedBy().equals(finalPlayer.piece()) && finalPlayer.isDetective())
						throw new IllegalArgumentException("Detective cannot make a Double Move: " + move);
					return move.destination2;
				}
			});
			player = player.at(dest);
			return player;
		}

		private Player changeTicketNumber(Player player, final Move move) {
			// Updates tickets accordingly from the given move
			final Ticket ticket = getMoveTicket(move);
			player = player.use(ticket);
			if (player.isMrX() && isDoubleMove(move)) {
				player = player.use(mrXFirstTicket[0]);
				player = player.use(Ticket.DOUBLE);
			}
			if(player.isDetective()) mrX = mrX.give(ticket);

			return player;
		}

		private void logUpdate(final Move move, final Ticket mrXTicket) {
			final List<LogEntry> newLog = new ArrayList<>(List.copyOf(log));

			if (setup.rounds.get(log.size())) newLog.add(LogEntry.reveal(mrXTicket, mrX.location()));
			else newLog.add(LogEntry.hidden(mrXTicket));
			if (isDoubleMove(move)) {
				if (setup.rounds.get(log.size() + 1)) newLog
						.add(LogEntry.reveal(mrXTicket, mrX.location()));
				else newLog.add(LogEntry.hidden(mrXTicket));
			}
			log = ImmutableList.<LogEntry>builder().addAll(newLog).build();
		}

		private void checkWinner() {
			final List<Piece> playerThatWon = new ArrayList<>();
			final List<Player> d = new ArrayList<>();

            for(final var j : detectives) {
                final var a = new StoreTickets(j.tickets());
                if (a.getCount(Ticket.TAXI) == 0 && a.getCount(Ticket.UNDERGROUND) == 0 && a.getCount(Ticket.BUS) == 0)
                	d.add(j);
            }
            if(d.size() == detectives.size()) {
				// If all rounds are exhausted or all detectives have no tickets left, mrx automatically wins
                playerThatWon.add(mrX.piece());
                moves = ImmutableSet.<Move>builder().build();
            }
            else if (numberOfRoundsLeft(setup, log) == 0 && remaining.contains(mrX.piece())) {
            	playerThatWon.add(mrX.piece());
            	moves = ImmutableSet.<Move>builder().build();
			}
			else if (detectives.stream().map(Player::location).anyMatch(x -> x == mrX.location())) {
				playerThatWon.addAll(detectives.stream().map(Player::piece).collect(Collectors.toList()));
				moves = ImmutableSet.<Move>builder().build();
			}
			else if (moves.isEmpty()) {
				playerThatWon.addAll(detectives.stream().map(Player::piece).collect(Collectors.toList()));
			}
			if (winner.isEmpty() && !playerThatWon.isEmpty()) {
				winner = ImmutableSet.<Piece>builder().addAll(playerThatWon).build();
			}
        }

	}
	protected static List<Player> remainingDetectives(final ImmutableSet<Piece> remaining,
													  final List<Player> detectives) {
		// Calculates the remaining number of detectives in the turn
		final ArrayList<Player> newPlayerList = new ArrayList<>();
		for (final Player p : detectives)	{
			if(!(p.hasAtLeast(Ticket.TAXI, 1) || p.hasAtLeast(Ticket.UNDERGROUND, 1)
					|| p.hasAtLeast(Ticket.BUS, 1))) continue;
			if (remaining.contains(p.piece())) newPlayerList.add(p);
		}
		return newPlayerList;
	}

	private static void checkExceptionsDuringInitialisation(final GameSetup setup,
															final List<Player> detectives)
			throws IllegalArgumentException {

		// Ensures detectives don't have access to mrX's special tickets
		for (final Player detective : detectives) {
			if (detective.has(Ticket.SECRET) || detective.has(Ticket.DOUBLE))
				throw new IllegalArgumentException("Detective " + detective
						+ " can't have a secret or double ticket.");
		}
		// Checks for no duplicate detectives or starting in the same location
		for (int i = 0; i < detectives.size(); i++) {
			for (int j = i + 1; j < detectives.size(); j++) {
				if(Objects.equals(detectives.get(i), detectives.get(j)))
					throw new IllegalArgumentException("Cannot have two equal detectives: "
							+ detectives.get(i) + " " + detectives.get(j) + " are equal.");
				if(Objects.equals(detectives.get(i).location(), detectives.get(j).location()))
					throw new IllegalArgumentException("Cannot have detective "
							+ detectives.get(i) + " and " + detectives.get(j) + " in same location.");
			}
		}

		if(setup.rounds.isEmpty())
			throw new IllegalArgumentException("game cannot start with 0 rounds");
	}

	private static final class StoreTickets implements TicketBoard {
		private final ImmutableMap<Ticket, Integer> storeTickets;

		private StoreTickets(final ImmutableMap<Ticket, Integer> tickets) { storeTickets = tickets; }
		@Override public final int getCount(@Nonnull final Ticket ticket) { return storeTickets.get(ticket); }
	}

	protected static int numberOfRoundsLeft(final GameSetup setup, final ImmutableList<LogEntry> log)	{
		return (setup.rounds.size()) - (log.size());
	}

	@Nonnull @Override public final GameState build(final GameSetup setup,
													final Player mrX,
													final ImmutableList<Player> detectives) {

		return new MyGameState(setup, ImmutableSet.of(MRX), ImmutableList.of(), mrX, detectives);
	}
}

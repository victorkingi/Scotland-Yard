package uk.ac.bris.cs.scotlandyard.ui.ai;

import javafx.application.Platform;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
/*
	MrX is max and detective is min. If the score is empty and Dijkstra's Algorithm
	didn't return a specific move, a random move is chosen
*/
public class Rick implements Ai {
	private final List<Integer> mrXReveal = new ArrayList<>();

	@Nonnull
	@Override
	public String name() { return "Rick"; }

	@Nonnull
	@Override
	public Move pickMove(
			@Nonnull final Board board,
			@Nonnull final AtomicBoolean terminate) {
		final var moves = board.getAvailableMoves().asList();
		final Move move = moves.get(new Random().nextInt(moves.size()));
		Map<Move, Integer> score = new HashMap<>();

		for (final Move m : moves) {
			score = mrXMayWin(board, m, score);
			score = detectiveMayWin(board, m, score);
		}
		/*
			if some moves have a map value, the move with the maximum value is returned
			as it guarantees, mrX gets away from detectives if move.commencedby MRX
			or a detective gets closer to MRX if move.commencedby Detective
		*/
		if(score.isEmpty()) return move;
		final Entry<Move, Integer> maxEntry = getMaxEntry(score);
		return maxEntry.getKey();
	}
	public void onStart() {
		try {
			final String[] frames = {"|", "/", "-", "\\"};
			System.out.println("Hello :)\n");
			Thread.sleep(1000);
			System.out.println("We are setting up the UI\n");
			for (int ctr = 0; ctr <= 20; ctr++) {
				System.out.print(ctr * 5 + "% complete");
				System.out.print(frames[ctr % frames.length]);
				Thread.sleep(300);
				System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
			}
			System.out.println("100% complete");
			System.out.println("done! Enjoy Scotland Yard :)");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void onTerminate() {
		System.out.println("Cleaning up");
		try {
			final String[] frames = {"|", "/", "-", "\\"};
			for (int ctr = 0; ctr < 20; ctr++) {
				System.out.print(frames[ctr % frames.length]);
				Thread.sleep(150);
				System.out.print("\b");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Complete\tGoodbye");

	}

	private Entry<Move, Integer> getMaxEntry(final Map<Move, Integer> score) {
		Entry<Move, Integer> maxEntry = null;
		for (final Entry<Move, Integer> entry : score.entrySet()) {
			if (maxEntry == null || entry.getValue()
					.compareTo(maxEntry.getValue()) > 0) maxEntry = entry;
		}
		return maxEntry;
	}

	private Map<Move, Integer> mrXMayWin(@Nonnull final Board board, final Move m,
										 final Map<Move, Integer> score) {
		final MrXGetsAway max = new MrXGetsAway(score);
		if(m.commencedBy().equals(MRX)) {
			max.updateMrXScore(board, m);
		}
		return max.getScore();
	}

	private Map<Move, Integer> detectiveMayWin(@Nonnull final Board board, final Move m,
											   final Map<Move, Integer> score) {
		final DetectiveFindsMrX min = new DetectiveFindsMrX(mrXReveal, score);
		min.checkIfMrXRevealedLocation(board);
		if (!m.commencedBy().equals(MRX)) {
			if (!mrXReveal.isEmpty()) {
				LinkedList<Node> shortestPath;
				final Integer mrXLocation = mrXReveal.get(mrXReveal.size() - 1);
				shortestPath = min.executeDijkstra(board, mrXLocation);
				min.updateDetectiveScore(board, m, shortestPath);
				return min.getScore();
			}
		}
		return min.getScore();
	}
}


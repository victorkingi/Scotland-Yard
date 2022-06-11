package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sun.webkit.Timer;
import javafx.print.PageLayout;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer.Event;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private static final class MyModel implements Model {
		private GameState modelState;
		private ImmutableSet<Observer> observers;
		List<Observer> o = new ArrayList<>();

		private MyModel(final GameSetup setup,
						final ImmutableSet<Observer> observers,
						final Player mrX,
						final ImmutableList<Player> detectives) {
			this.observers = observers;
			modelState = new MyGameStateFactory().build(setup, mrX, detectives);

		}
		/**
		 * @return the current game board
		 */
		@Nonnull
		@Override
		public Board getCurrentBoard() { return modelState; }

		/**
		 * Registers an observer to the model. It is an error to register the same observer more than
		 * once.
		 *
		 * @param observer the observer to register
		 */
		@Override
		public void registerObserver(@Nonnull final Observer observer) {
			if(observer.equals(null)) throw new NullPointerException();
			if(o.contains(observer)) throw new IllegalArgumentException();
			o.add(observer);
		}

		/**
		 * Unregisters an observer to the model. It is an error to unregister an observer not
		 * previously registered with {@link #registerObserver(Observer)}.
		 *
		 * @param observer the observer to register
		 */
		@Override
		public void unregisterObserver(@Nonnull final Observer observer) {
			if(observer.equals(null)) throw new NullPointerException();
			if(!o.contains(observer)) throw new IllegalArgumentException();
			o.remove(observer);
		}

		/**
		 * @return all currently registered observers of the model
		 */
		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			observers = ImmutableSet.<Observer>builder().addAll(o).build();
			return observers;
		}

		/**
		 * @param move delegates the move to the underlying
		 *             {@link GameState}
		 */
		@Override
		public void chooseMove(@Nonnull final Move move) {
			observers = ImmutableSet.<Observer>builder().addAll(o).build();
            modelState = modelState.advance(move);
			var event = modelState.getWinner().isEmpty() ? Event.MOVE_MADE : Event.GAME_OVER;
            for (Observer o : observers) o.onModelChanged(modelState, event);
		}
	}

	@Nonnull @Override public Model build(final GameSetup setup,
	                                      final Player mrX,
	                                      final ImmutableList<Player> detectives) {

		return new MyModel(setup, ImmutableSet.of(), mrX, detectives);
	}
}

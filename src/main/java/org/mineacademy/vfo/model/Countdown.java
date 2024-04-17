package org.mineacademy.vfo.model;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.plugin.SimplePlugin;

import com.velocitypowered.api.scheduler.ScheduledTask;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Represents a runnable timer task that counts down to 0 and stops
 */
public abstract class Countdown implements Runnable {

	/**
	 * How long to wait before starting this countdown (in ticks)?
	 * <p>
	 * Set to 1 second.
	 */
	private static final int START_DELAY = 20;

	/**
	 * How long to wait before ticking the next count (in ticks)?
	 * <p>
	 * Set to 1 second.
	 */
	private static final int TICK_PERIOD = 20;

	/**
	 * The time in seconds we are counting down from
	 */
	@Getter
	private final int countdownSeconds;

	/**
	 * How many seconds have passed since the start ?
	 */
	@Getter(AccessLevel.PROTECTED)
	private int secondsSinceStart = 0;

	/**
	 * The internal task from Bukkit associated with this countdown
	 */
	private ScheduledTask task = null;

	/**
	 * Create new countdown from the given time
	 *
	 * @param time
	 */
	protected Countdown(final SimpleTime time) {
		this((int) time.getTimeSeconds());
	}

	/**
	 * Create new countdown
	 *
	 * @param countdownSeconds
	 */
	protected Countdown(final int countdownSeconds) {
		this.countdownSeconds = countdownSeconds;
	}

	@Override
	public final void run() {
		this.secondsSinceStart++;

		if (this.secondsSinceStart < this.countdownSeconds)
			try {
				this.onTick();

			} catch (final Throwable t) {
				try {
					this.onTickError(t);
				} catch (final Throwable tt) {
					Common.log("Unable to handle onTickError, got " + t + ": " + tt.getMessage());
				}

				Common.error(t,
						"Error in countdown!",
						"Seconds since start: " + this.secondsSinceStart,
						"Counting till: " + this.countdownSeconds,
						"%error");
			}
		else {
			this.cancel();
			this.onEnd();
		}
	}

	/**
	 * Called when this countdown is launched
	 */
	protected void onStart() {
	}

	/**
	 * Called on each tick (by default each second) till we count down to 0
	 */
	protected abstract void onTick();

	/**
	 * Called when the clock hits the final 0 and stops.
	 */
	protected abstract void onEnd();

	/**
	 * Called when the {@link #onTick()} method throws an error (we already log the error)
	 *
	 * @param t
	 */
	protected void onTickError(final Throwable t) {
	}

	/**
	 * Return the time left in seconds
	 *
	 * @return
	 */
	public int getTimeLeft() {
		return this.countdownSeconds - this.secondsSinceStart;
	}

	/**
	 * Starts this countdown failing if it is already running
	 */
	public final void launch() {
		Valid.checkBoolean(!this.isRunning(), "Task " + this + " already scheduled!");

		this.task = SimplePlugin.getServer().getScheduler().buildTask(SimplePlugin.getInstance(), this).delay(START_DELAY * 50, TimeUnit.MILLISECONDS).repeat(TICK_PERIOD * 50, TimeUnit.MILLISECONDS).schedule();

		this.onStart();
	}

	/**
	 * Cancels this countdown, failing if it is not scheduled (use {@link #isRunning()})
	 */
	public final void cancel() {
		final Collection<ScheduledTask> tasks = SimplePlugin.getServer().getScheduler().tasksByPlugin(SimplePlugin.getInstance());

		for (final ScheduledTask other : tasks)
			if (other.equals(this.task))
				this.task.cancel();

		this.secondsSinceStart = 0;
	}

	/**
	 * Return true if this countdown is running
	 *
	 * @return
	 */
	public final boolean isRunning() {
		return this.task != null;
	}

	@Override
	public final String toString() {
		return this.getClass().getSimpleName() + "{" + this.countdownSeconds + ", task=" + this.task + "}";
	}
}

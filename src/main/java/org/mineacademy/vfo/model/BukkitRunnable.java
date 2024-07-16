package org.mineacademy.vfo.model;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.mineacademy.vfo.plugin.SimplePlugin;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;

import lombok.NonNull;

/**
 * This class is provided as an easy way to handle scheduling tasks.
 */
public abstract class BukkitRunnable implements Runnable {

	private ScheduledTask task;

	/**
	 * Attempts to cancel this task.
	 *
	 * @throws IllegalStateException if task was not scheduled yet
	 */
	public synchronized void cancel() throws IllegalStateException {
		final Collection<ScheduledTask> tasks = this.getScheduler().tasksByPlugin(SimplePlugin.getInstance());

		for (final ScheduledTask other : tasks)
			if (other.equals(this.task))
				this.task.cancel();
	}

	/**
	 * Schedules this in the Bukkit scheduler to run on next tick.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 *
	 * @deprecated all tasks on proxy are run async
	 */
	@Deprecated
	@NonNull
	public synchronized ScheduledTask runTask(@NonNull SimplePlugin plugin) throws IllegalArgumentException, IllegalStateException {
		return this.runTaskAsynchronously(plugin);
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit. Great care
	 * should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <p>
	 * Schedules this in the Bukkit scheduler to run asynchronously.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 */
	@NonNull
	public synchronized ScheduledTask runTaskAsynchronously(@NonNull SimplePlugin plugin) throws IllegalArgumentException, IllegalStateException {
		this.checkNotYetScheduled();

		return this.setupTask(this.getScheduler().buildTask(plugin, this).schedule());
	}

	/**
	 * Schedules this to run after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 *
	 * @deprecated all tasks on proxy are run async
	 */
	@Deprecated
	@NonNull
	public synchronized ScheduledTask runTaskLater(@NonNull SimplePlugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		return this.runTaskLaterAsynchronously(plugin, delay);
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit. Great care
	 * should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <p>
	 * Schedules this to run asynchronously after the specified number of
	 * server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 */
	@NonNull
	public synchronized ScheduledTask runTaskLaterAsynchronously(@NonNull SimplePlugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		this.checkNotYetScheduled();

		return this.setupTask(this.getScheduler().buildTask(plugin, this).delay(delay * 50, TimeUnit.MILLISECONDS).schedule());
	}

	/**
	 * Schedules this to repeatedly run until cancelled, starting after the
	 * specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task
	 * @param period the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 *
	 * @deprecated all tasks on proxy are run async
	 */
	@Deprecated
	@NonNull
	public synchronized ScheduledTask runTaskTimer(@NonNull SimplePlugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		return this.runTaskTimerAsynchronously(plugin, delay, period);
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit. Great care
	 * should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <p>
	 * Schedules this to repeatedly run asynchronously until cancelled,
	 * starting after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task for the first
	 *     time
	 * @param period the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 */
	@NonNull
	public synchronized ScheduledTask runTaskTimerAsynchronously(@NonNull SimplePlugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		this.checkNotYetScheduled();

		return this.setupTask(this.getScheduler().buildTask(SimplePlugin.getInstance(), this).delay(delay * 50, TimeUnit.MILLISECONDS).repeat(period * 50, TimeUnit.MILLISECONDS).schedule());
	}

	private void checkNotYetScheduled() {
		if (this.task != null)
			throw new IllegalStateException("Already scheduled");
	}

	private Scheduler getScheduler() {
		return SimplePlugin.getServer().getScheduler();
	}

	@NonNull
	private ScheduledTask setupTask(@NonNull final ScheduledTask task) {
		this.task = task;

		return task;
	}
}

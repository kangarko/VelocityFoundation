/**
 * (c) 2013 - 2019 - All rights reserved.
 * <p>
 * Do not share, copy, reproduce or sell any part of this library
 * unless you have written permission from MineAcademy.org.
 * All infringements will be prosecuted.
 * <p>
 * If you are the personal owner of the MineAcademy.org End User License
 * then you may use it for your own use in plugins but not for any other purpose.
 */
package org.mineacademy.vfo.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.ReflectionUtil;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.annotation.AutoRegister;
import org.mineacademy.vfo.bungee.BungeeListener;
import org.mineacademy.vfo.collection.StrictList;
import org.mineacademy.vfo.command.SimpleCommand;
import org.mineacademy.vfo.command.SimpleCommandGroup;
import org.mineacademy.vfo.command.SimpleSubCommand;
import org.mineacademy.vfo.debug.Debugger;
import org.mineacademy.vfo.exception.FoException;
import org.mineacademy.vfo.metrics.Metrics;
import org.mineacademy.vfo.model.FolderWatcher;
import org.mineacademy.vfo.model.JavaScriptExecutor;
import org.mineacademy.vfo.remain.Remain;
import org.mineacademy.vfo.settings.FileConfig;
import org.mineacademy.vfo.settings.Lang;
import org.mineacademy.vfo.settings.SimpleLocalization;
import org.mineacademy.vfo.settings.SimpleSettings;
import org.slf4j.Logger;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;

import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar.Listener;

/**
 * Represents a basic Java plugin using enhanced library functionality.
 */
public abstract class SimplePlugin {

	// ----------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------

	/**
	 * The instance of this plugin
	 */
	private static SimplePlugin instance;

	/**
	 * Shortcut for getDescription().getVersion()
	 */
	@Getter
	private static String version;

	/**
	 * Shortcut for getName()
	 */
	@Getter
	private static String named;

	/**
	 * An internal flag to indicate that the plugin is being reloaded.
	 */
	@Getter
	private static boolean reloading = false;

	/**
	 * Returns the instance of {@link SimplePlugin}.
	 * <p>
	 * It is recommended to override this in your own {@link SimplePlugin}
	 * implementation so you will get the instance of that, directly.
	 *
	 * @return this instance
	 */
	public static SimplePlugin getInstance() {
		return instance;
	}

	/**
	 * Returns the server instance
	 *
	 * @return
	 */
	public static ProxyServer getServer() {
		return getInstance().proxy;
	}

	/**
	 *
	 * @return
	 */
	public static File getSource() {
		return getInstance().file;
	}

	public static File getData() {
		return getInstance().dataFolder;
	}

	/**
	 * Return the logger
	 *
	 * @return
	 */
	public static Logger getLogger() {
		return getInstance().logger;
	}

	/**
	 * Get if the instance that is used across the library has been set. Normally it
	 * is always set, except for testing.
	 *
	 * @return if the instance has been set.
	 */
	public static final boolean hasInstance() {
		return instance != null;
	}

	// ----------------------------------------------------------------------------------------
	// Instance specific
	// ----------------------------------------------------------------------------------------

	/**
	 * A list of currently enabled event listeners
	 */
	private final StrictList<Listener> listeners = new StrictList<>();

	/**
	 * The proxy server
	 */
	private final ProxyServer proxy;

	/**
	 * The proxy logger
	 */
	private final Logger logger;

	/**
	 * Shortcut for getFile()
	 */
	private File file;

	/**
	 * The path data
	 */
	private final File dataFolder;

	/**
	 * The plugin id
	 */
	private final String id;

	/**
	 * A temporary bungee listener, see {@link #setBungeeCord(BungeeListener)}
	 * set automatically by us.
	 */
	private BungeeListener bungeeListener;

	/**
	 * A temporary main command to be set in {@link #setMainCommand(SimpleCommandGroup)}
	 * automatically by us.
	 */
	private SimpleCommandGroup mainCommand;

	/**
	 * Shortcut to discover if the plugin was disabled (only used internally)
	 */
	private boolean enabled = true;

	// ----------------------------------------------------------------------------------------
	// Main methods
	// ----------------------------------------------------------------------------------------

	static {

		// Add console filters early - no reload support
		FoundationFilter.inject();
	}

	public SimplePlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
		instance = this;

		// Hacky due to lacking Velocity implementation
		final Plugin pluginAnnotation = this.getClass().getDeclaredAnnotation(Plugin.class);

		version = pluginAnnotation.version();
		named = pluginAnnotation.name();

		this.proxy = proxy;
		this.logger = logger;
		this.id = pluginAnnotation.id();
		this.dataFolder = new File(dataDirectory.toFile().getParentFile(), named); // Another hack to prevent lowercase folders

		// Init Nashorn
		final javax.script.ScriptEngineManager engineManager = new javax.script.ScriptEngineManager();
		final javax.script.ScriptEngineFactory engineFactory = new org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory();

		engineManager.registerEngineName("Nashorn", engineFactory);

		// Call parent
		this.onPluginInit();
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		this.file = proxy.getPluginManager().getPlugin(this.id).get().getDescription().getSource().get().toFile();

		// Before all, check if necessary libraries and the minimum required MC version
		if (!this.enabled)
			return;

		// Load debug mode early
		Debugger.detectDebugMode();

		// Print startup logo early before onPluginPreStart
		// Disable logging prefix if logo is set
		if (this.getStartupLogo() != null) {
			final String oldLogPrefix = Common.getLogPrefix();

			Common.setLogPrefix("");
			Common.log(this.getStartupLogo());
			Common.setLogPrefix(oldLogPrefix);
		}

		// Return if plugin pre start indicated a fatal problem
		if (!this.enabled)
			return;

		try {

			// --------------------------------------------
			// Call the main start method
			// --------------------------------------------

			Common.registerEvents(new BungeeListener.BungeeListenerImpl());
			this.proxy.getChannelRegistrar().register(new LegacyChannelIdentifier("BungeeCord"));

			// Hide plugin name before console messages
			final String oldLogPrefix = Common.getLogPrefix();
			Common.setLogPrefix("");

			try {
				AutoRegisterScanner.scanAndRegister();

			} catch (final Throwable t) {
				Remain.sneaky(t);

				return;
			}

			this.onReloadablesStart();
			this.onPluginStart();
			// --------------------------------------------

			// Return if plugin start indicated a fatal problem
			if (!this.enabled)
				return;

			// Register our listeners
			//this.registerEvents(this); - automatically registered

			// Prepare Nashorn engine
			JavaScriptExecutor.run("");

			// Finish off by starting metrics (currently bStats)
			if (this.getMetricsPluginId() != -1)
				new Metrics.Factory(this.proxy, this.logger, this.dataFolder.toPath()).make(this, getMetricsPluginId());

			// Set the logging and tell prefix
			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);

			// Finally, place plugin name before console messages after plugin has (re)loaded
			Common.runAsync(() -> Common.setLogPrefix(oldLogPrefix));

		} catch (final Throwable t) {
			this.displayError0(t);
		}
	}

	@Subscribe
	public void onProxyInitialization(ProxyShutdownEvent event) {
		try {
			this.onPluginStop();
		} catch (final Throwable t) {
			Common.log("&cPlugin might not shut down property. Got " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}

		this.unregisterReloadables();

		Objects.requireNonNull(instance, "Instance of " + this.getDataFolder().getName() + " already nulled!");
		instance = null;
	}

	/**
	 * Handles various startup problems
	 *
	 * @param throwable
	 */
	protected final void displayError0(Throwable throwable) {
		Debugger.printStackTrace(throwable);

		Common.log(
				"&4    ___                  _ ",
				"&4   / _ \\  ___  _ __  ___| |",
				"&4  | | | |/ _ \\| '_ \\/ __| |",
				"&4  | |_| | (_) | |_) \\__ \\_|",
				"&4   \\___/ \\___/| .__/|___(_)",
				"&4             |_|          ",
				"&4!-----------------------------------------------------!",
				" &cError loading " + getNamed() + " v" + getVersion() + ", plugin is disabled!",
				" &cRunning on " + this.proxy.getVersion().getVersion() + " and Java " + System.getProperty("java.version"),
				"&4!-----------------------------------------------------!");

		{
			while (throwable.getCause() != null)
				throwable = throwable.getCause();

			String error = "Unable to get the error message, search above.";
			if (throwable.getMessage() != null && !throwable.getMessage().isEmpty() && !throwable.getMessage().equals("null"))
				error = throwable.getMessage();

			Common.log(" &cError: " + error);
		}
		Common.log("&4!-----------------------------------------------------!");

		this.enabled = false;
	}

	// ----------------------------------------------------------------------------------------
	// Delegate methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Called before the plugin is started, see {@link JavaPlugin#onLoad()}
	 */
	protected void onPluginInit() {
	}

	/**
	 * The main loading method, called when we are ready to load
	 */
	protected abstract void onPluginStart();

	/**
	 * The main method called when we are about to shut down
	 */
	protected void onPluginStop() {
	}

	/**
	 * Invoked before settings were reloaded.
	 */
	protected void onPluginPreReload() {
	}

	/**
	 * Invoked after settings were reloaded.
	 */
	protected void onPluginReload() {
	}

	/**
	 * Register your commands, events, tasks and files here.
	 * <p>
	 * This is invoked when you start the plugin, call /reload, or the {@link #reload()}
	 * method.
	 */
	protected void onReloadablesStart() {
	}

	// ----------------------------------------------------------------------------------------
	// Reload
	// ----------------------------------------------------------------------------------------

	/**
	 * Attempts to reload the plugin
	 */
	public final void reload() {
		final String oldLogPrefix = Common.getLogPrefix();
		Common.setLogPrefix("");

		Common.log(Common.consoleLineSmooth());
		Common.log(" ");
		Common.log("Reloading plugin " + this.getDataFolder().getName() + " v" + getVersion());
		Common.log(" ");

		reloading = true;

		try {

			this.proxy.getEventManager().unregisterListeners(this);
			this.listeners.clear();

			Debugger.detectDebugMode();

			this.unregisterReloadables();

			FileConfig.clearLoadedSections();

			this.onPluginPreReload();

			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);
			this.onPluginReload();

			// Something went wrong in the reload pipeline
			if (!this.enabled)
				return;

			this.proxy.getChannelRegistrar().register(new LegacyChannelIdentifier("BungeeCord"));

			// Register classes
			AutoRegisterScanner.scanAndRegister();

			Lang.reloadLang();
			Lang.loadPrefixes();

			this.onReloadablesStart();

			Common.log(Common.consoleLineSmooth());

		} catch (final Throwable t) {
			Common.throwError(t, "Error reloading " + this.getDataFolder().getName() + " " + getVersion());

		} finally {
			Common.setLogPrefix(oldLogPrefix);

			reloading = false;
		}
	}

	private void unregisterReloadables() {
		SimpleSettings.resetSettingsCall();
		SimpleLocalization.resetLocalizationCall();

		FolderWatcher.stopThreads();

		this.proxy.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);
		this.mainCommand = null;
	}

	// ----------------------------------------------------------------------------------------
	// Methods
	// ----------------------------------------------------------------------------------------

	/**
	 * Convenience method for quickly registering events in all classes in your plugin that
	 * extend the given class.
	 *
	 * NB: You must have a no arguments constructor otherwise it will not be registered
	 *
	 * TIP: Set your Debug key in your settings.yml to ["auto-register"] to see what is registered.
	 *
	 * @param extendingClass
	 */
	protected final <T extends Listener> void registerAllEvents(final Class<T> extendingClass) {

		Valid.checkBoolean(!extendingClass.equals(Listener.class), "registerAllEvents does not support Listener.class due to conflicts, create your own middle class instead");

		classLookup:
		for (final Class<? extends T> pluginClass : ReflectionUtil.getClasses(instance, extendingClass)) {

			// AutoRegister means the class is already being registered
			if (pluginClass.isAnnotationPresent(AutoRegister.class))
				continue;

			for (final Constructor<?> con : pluginClass.getConstructors())
				if (con.getParameterCount() == 0) {
					final T instance = (T) ReflectionUtil.instantiate(con);

					Debugger.debug("auto-register", "Auto-registering events in " + pluginClass);
					this.registerEvents(instance);

					continue classLookup;
				}

			Debugger.debug("auto-register", "Skipping auto-registering events in " + pluginClass + " because it lacks at least one no arguments constructor");
		}
	}

	/**
	 * Convenience method for quickly registering bungecoord channel for this plugin
	 *
	 * @param listener
	 */
	protected final void registerBungeeCord(final BungeeListener listener) {
		this.proxy.getChannelRegistrar().register(new LegacyChannelIdentifier(listener.getChannel()));
		this.proxy.getEventManager().register(this, listener);
	}

	/**
	 * Convenience method for quickly registering events for this plugin
	 *
	 * @param listenerInstance
	 */
	protected final void registerEvents(final Object listenerInstance) {
		this.proxy.getEventManager().register(this, listenerInstance);
	}

	/**
	 * Convenience method for quickly registering all command classes in your plugin that
	 * extend the given class.
	 *
	 * NB: You must have a no arguments constructor otherwise it will not be registered
	 *
	 * TIP: Set your Debug key in your settings.yml to ["auto-register"] to see what is registered.
	 *
	 * @param extendingClass
	 */
	protected final <T extends SimpleCommand> void registerAllCommands(final Class<T> extendingClass) {
		Valid.checkBoolean(!extendingClass.equals(Command.class), "registerAllCommands does not support Command.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleCommand.class), "registerAllCommands does not support SimpleCommand.class due to conflicts, create your own middle class instead");
		Valid.checkBoolean(!extendingClass.equals(SimpleSubCommand.class), "registerAllCommands does not support SubCommand.class");

		classLookup:
		for (final Class<? extends T> pluginClass : ReflectionUtil.getClasses(instance, extendingClass)) {

			// AutoRegister means the class is already being registered
			if (pluginClass.isAnnotationPresent(AutoRegister.class))
				continue;

			if (SimpleSubCommand.class.isAssignableFrom(pluginClass)) {
				Debugger.debug("auto-register", "Skipping auto-registering command " + pluginClass + " because sub-commands cannot be registered");

				continue;
			}

			try {
				for (final Constructor<?> con : pluginClass.getConstructors())
					if (con.getParameterCount() == 0) {
						final T instance = (T) ReflectionUtil.instantiate(con);

						Debugger.debug("auto-register", "Auto-registering command " + pluginClass);

						if (instance instanceof SimpleCommand)
							this.registerCommand(instance);

						else
							this.registerCommand(instance);

						continue classLookup;
					}

			} catch (final LinkageError ex) {
				Common.log("Unable to register commands in '" + pluginClass.getSimpleName() + "' due to error: " + ex);
			}

			Debugger.debug("auto-register", "Skipping auto-registering command " + pluginClass + " because it lacks at least one no arguments constructor");
		}
	}

	/**
	 * Convenience method for registering a bukkit command
	 *
	 * @param command
	 */
	protected final void registerCommand(final SimpleCommand command) {
		if (command instanceof SimpleCommand)
			command.register();

		else
			this.proxy.getCommandManager().register(command.getLabel(), command);
	}

	/**
	 * Shortcut for calling {@link SimpleCommandGroup#register()}
	 *
	 * @param labelAndAliases
	 * @param group
	 */
	protected final void registerCommands(final SimpleCommandGroup group) {
		group.register();
	}

	// ----------------------------------------------------------------------------------------
	// Additional features
	// ----------------------------------------------------------------------------------------

	/**
	 * Return false if plugin was disabled during startup/reload
	 *
	 * @return
	 */
	public final boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * The start-up fancy logo
	 *
	 * @return null by default
	 */
	protected String[] getStartupLogo() {
		return null;
	}

	/**
	 * If you use \@AutoRegister on a command group that has a no args constructor,
	 * we use the label and aliases from {@link SimpleSettings#MAIN_COMMAND_ALIASES}
	 * and associate it here for the record.
	 *
	 * @return
	 */
	@Nullable
	public final SimpleCommandGroup getMainCommand() {
		return this.mainCommand;
	}

	/**
	 * @deprecated do not use, internal use only
	 * @param group
	 */
	@Deprecated
	public final void setMainCommand(SimpleCommandGroup group) {
		Valid.checkBoolean(this.mainCommand == null, "Main command has already been set to " + this.mainCommand);

		this.mainCommand = group;
	}

	/**
	 * If you want to use bStats.org metrics system,
	 * simply return the plugin ID (https://bstats.org/what-is-my-plugin-id)
	 * here and we will automatically start tracking it.
	 * <p>
	 * Defaults to -1 which means disabled
	 *
	 * @return
	 */
	public int getMetricsPluginId() {
		return -1;
	}

	/**
	 * Foundation automatically can filter console commands for you, including
	 * messages from other plugins or the server itself, preventing unnecessary console spam.
	 *
	 * You can return a list of messages that will be matched using "startsWith OR contains" method
	 * and will be filtered.
	 *
	 * @return
	 */
	public Set<String> getConsoleFilter() {
		return new HashSet<>();
	}

	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 *
	 * @return time limit in milliseconds for processing regular expression
	 */
	public int getRegexTimeout() {
		throw new FoException("Must override getRegexTimeout()");
	}

	/**
	 * Strip colors from checked message while checking it against a regex?
	 *
	 * @return
	 */
	public boolean regexStripColors() {
		return true;
	}

	/**
	 * Should Pattern.CASE_INSENSITIVE be applied when compiling regular expressions in {@link Common#compilePattern(String)}?
	 * <p>
	 * May impose a slight performance penalty but increases catches.
	 *
	 * @return
	 */
	public boolean regexCaseInsensitive() {
		return true;
	}

	/**
	 * Should Pattern.UNICODE_CASE be applied when compiling regular expressions in {@link Common#compilePattern(String)}?
	 * <p>
	 * May impose a slight performance penalty but useful for non-English servers.
	 *
	 * @return
	 */
	public boolean regexUnicode() {
		return true;
	}

	/**
	 * Should we remove diacritical marks before matching regex?
	 * Defaults to true
	 *
	 * @return
	 */
	public boolean regexStripAccents() {
		return true;
	}

	/**
	 * Should we replace accents with their non accented friends when
	 * checking two strings for similarity in ChatUtil?
	 *
	 * @return defaults to true
	 */
	public boolean similarityStripAccents() {
		return true;
	}

	/**
	 * Returns the default or "main" bungee listener you use. This is checked so that you won't
	 * have to pass in channel name each time and we use channel name from this listener instead.
	 *
	 * @deprecated only returns the first found bungee listener, if you have multiple, do not use, order not guaranteed
	 * @return
	 */
	@Deprecated
	public final BungeeListener getBungeeCord() {
		return this.bungeeListener;
	}

	/**
	 * Sets the first valid bungee listener
	 *
	 * @deprecated INTERNAL USE ONLY, DO NOT USE! can only set one bungee listener, if you have multiple, order not guaranteed
	 * @param bungeeListener
	 */
	@Deprecated
	public final void setBungeeCord(BungeeListener bungeeListener) {
		this.bungeeListener = bungeeListener;
	}

	/**
	 * Return the data folder
	 *
	 * @return
	 */
	public final File getDataFolder() {
		return this.dataFolder;
	}

	/**
	 *
	 * @return
	 */
	public final File getFile() {
		return this.file;
	}
}

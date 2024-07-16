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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.FileUtil;
import org.mineacademy.vfo.ReflectionUtil;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.annotation.AutoRegister;
import org.mineacademy.vfo.collection.StrictList;
import org.mineacademy.vfo.command.SimpleCommand;
import org.mineacademy.vfo.command.SimpleCommandGroup;
import org.mineacademy.vfo.command.SimpleSubCommand;
import org.mineacademy.vfo.debug.Debugger;
import org.mineacademy.vfo.exception.FoException;
import org.mineacademy.vfo.jsonsimple.JSONObject;
import org.mineacademy.vfo.jsonsimple.JSONParser;
import org.mineacademy.vfo.library.Library;
import org.mineacademy.vfo.library.LibraryManager;
import org.mineacademy.vfo.library.VelocityLibraryManager;
import org.mineacademy.vfo.metrics.Metrics;
import org.mineacademy.vfo.model.JavaScriptExecutor;
import org.mineacademy.vfo.proxy.ProxyListener;
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
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
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

	private static final LegacyChannelIdentifier LEGACY_BUNGEE_CHANNEL = new LegacyChannelIdentifier("BungeeCord");
	private static final MinecraftChannelIdentifier MODERN_BUNGEE_CHANNEL = MinecraftChannelIdentifier.create("bungeecord", "main");

	/**
	 * The instance of this plugin
	 */
	private static SimplePlugin instance;

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
	 * Get this plugin's jar file
	 *
	 * @return
	 */
	public static File getSource() {
		return getInstance().file;
	}

	/**
	 * Get this plugin's name
	 *
	 * @return
	 */
	public static String getNamed() {
		return getInstance().name;
	}

	/**
	 * Get this plugin's version
	 *
	 * @return
	 */
	public static String getVersion() {
		return getInstance().version;
	}

	/**
	 * Shortcut to retrieve this plugin's data folder
	 *
	 * @return
	 */
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
	 * The proxy server
	 */
	private final ProxyServer proxy;

	/**
	 * The proxy logger
	 */
	private final Logger logger;

	/**
	 * The path data
	 */
	private final File dataFolder;

	/**
	 * Shortcut for getFile()
	 */
	private final File file;

	/**
	 * The plugin version
	 */
	private final String version;

	/**
	 * The plugin name
	 */
	private final String name;

	/**
	 * A list of currently enabled event listeners
	 */
	private final StrictList<Listener> listeners = new StrictList<>();

	/**
	 * The library manager
	 */
	private LibraryManager libraryManager;

	/**
	 * A temporary proxy listener, see {@link #setProxyListener(ProxyListener)}
	 * set automatically by us.
	 */
	private ProxyListener proxyListener;

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

		try {
			this.file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

		} catch (final URISyntaxException ex) {
			throw new RuntimeException(ex);
		}

		// Hacky due to Velocity lacking simpler implementation
		final Plugin annotation = this.getClass().getDeclaredAnnotation(Plugin.class);

		if (annotation != null) {
			this.version = annotation.version();
			this.name = annotation.name();

		} else {

			// If annotation isn't used, try to load from velocity-plugin.json directly. You can place this file to your src/main/resources and use variables in it.
			final List<String> lines = FileUtil.getInternalFileContent("velocity-plugin.json");
			Valid.checkBoolean(lines != null, "Either place @Plugin annotation over your main class or write velocity-plugin.json to your resources folder!");

			JSONObject json;

			try {
				json = (JSONObject) JSONParser.deserialize(String.join("", lines));

			} catch (final Throwable t) {
				throw new FoException(t, "Failed to parse velocity-plugin.json: " + t.getMessage());
			}

			this.version = json.getString("version");
			this.name = json.getString("name");
		}

		Valid.checkBoolean(this.version != null && !this.version.contains("${project.version}"), "Invalid plugin version: " + this.version);
		Valid.checkBoolean(this.name != null && !this.name.contains("${project.name}"), "Invalid plugin name: " + this.name);

		this.proxy = proxy;
		this.logger = logger;
		this.dataFolder = new File(dataDirectory.toFile().getParentFile(), this.name); // Another hack to prevent lowercase folders

		// Init Nashorn library, must be shaded in the plugin's jar
		try {
			final javax.script.ScriptEngineManager engineManager = new javax.script.ScriptEngineManager();
			final javax.script.ScriptEngineFactory engineFactory = new org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory();
			engineManager.registerEngineName("Nashorn", engineFactory);

		} catch (final NoClassDefFoundError ex) {
			// Assume running on ant
		}

		// Call delegate
		this.onPluginLoad();
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		if (!this.enabled)
			return;

		if (getJavaVersion() >= 11)
			this.loadLibrary("org.openjdk.nashorn", "nashorn-core", "15.4");

		// Disable logging prefix if logo is set
		if (this.getStartupLogo() != null) {
			final String oldLogPrefix = Common.getLogPrefix();

			Common.setLogPrefix("");
			Common.log(this.getStartupLogo());
			Common.setLogPrefix(oldLogPrefix);
		}

		try {
			// --------------------------------------------
			// Call the main start method
			// --------------------------------------------

			this.registerDefaultProxyChannels();

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

			if (!this.enabled)
				return;

			// Prepare Nashorn engine
			JavaScriptExecutor.run("");

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
	public void onProxyShutdown(ProxyShutdownEvent event) {
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
	protected void onPluginLoad() {
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

			this.unregisterReloadables();

			FileConfig.clearLoadedSections();

			this.onPluginPreReload();

			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);
			this.onPluginReload();

			// Something went wrong in the reload pipeline
			if (!this.enabled)
				return;

			this.registerDefaultProxyChannels();

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

					this.registerEvents(instance);

					continue classLookup;
				}
		}
	}

	/*
	 * Register the default proxy channels
	 */
	private void registerDefaultProxyChannels() {
		this.proxy.getChannelRegistrar().register(LEGACY_BUNGEE_CHANNEL, MODERN_BUNGEE_CHANNEL);

		this.registerEvents(new ForwardingListener(this.proxy));
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

			if (SimpleSubCommand.class.isAssignableFrom(pluginClass))
				continue;

			try {
				for (final Constructor<?> con : pluginClass.getConstructors())
					if (con.getParameterCount() == 0) {
						final T instance = (T) ReflectionUtil.instantiate(con);

						if (instance instanceof SimpleCommand)
							this.registerCommand(instance);

						else
							this.registerCommand(instance);

						continue classLookup;
					}

			} catch (final LinkageError ex) {
				Common.log("Unable to register commands in '" + pluginClass.getSimpleName() + "' due to error: " + ex);
			}
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

	/**
	 * Return the proxy server (aka Bukkit#getServer())
	 *
	 * @return
	 */
	public final ProxyServer getProxy() {
		return proxy;
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
	 * VelocityFoundation automatically can filter console commands for you, including
	 * messages from other plugins or the server itself, preventing unnecessary console spam.
	 *
	 * You can return a list of messages that will be matched using "startsWith OR contains" method
	 * and will be filtered.
	 *
	 * @deprecated limited to System.out
	 *
	 * @return
	 */
	@Deprecated
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
	 * Returns the default proxy listener used when sending outgoing messages without explicitly having to pass in a listener to them
	 *
	 * @deprecated only returns the first found proxy listener
	 * @return
	 */
	@Deprecated
	public final ProxyListener getProxyListener() {
		return this.proxyListener;
	}

	/**
	 * Sets the default proxy listener
	 *
	 * @deprecated INTERNAL USE ONLY
	 * @param listener
	 */
	@Deprecated
	public final void setProxyListener(ProxyListener listener) {
		this.proxyListener = listener;
	}

	/**
	 * Loads a library jar into the classloader classpath. If the library jar
	 * doesn't exist locally, it will be downloaded.
	 * <p>
	 * If the provided library has any relocations, they will be applied to
	 * create a relocated jar and the relocated jar will be loaded instead.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	public void loadLibrary(String groupId, String artifactId, String version) {
		this.getLibraryManager().loadLibrary(Library.builder().groupId(groupId).artifactId(artifactId).version(version).resolveTransitiveDependencies(true).build());
	}

	/**
	 * Get the Libby library manager
	 *
	 * @return
	 */
	public final LibraryManager getLibraryManager() {
		if (this.libraryManager == null)
			this.libraryManager = new VelocityLibraryManager(this, this.dataFolder.toPath(), this.proxy.getPluginManager());

		return this.libraryManager;
	}

	/**
	 * Return the corresponding major Java version such as 8 for Java 1.8, or 11 for Java 11.
	 *
	 * @return
	 */
	public static int getJavaVersion() {
		String version = System.getProperty("java.version");

		if (version.startsWith("1."))
			version = version.substring(2, 3);

		else {
			final int dot = version.indexOf(".");

			if (dot != -1)
				version = version.substring(0, dot);
		}

		if (version.contains("-"))
			version = version.split("\\-")[0];

		return Integer.parseInt(version);
	}
}

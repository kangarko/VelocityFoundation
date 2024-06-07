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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.PlayerUtil;
import org.mineacademy.vfo.ReflectionUtil;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.annotation.AutoRegister;
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
import org.mineacademy.vfo.velocity.BungeeListener;
import org.mineacademy.vfo.velocity.BungeeMessageType;
import org.mineacademy.vfo.velocity.message.IncomingMessage;
import org.slf4j.Logger;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.util.UuidUtils;

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
		final Plugin annotation = this.getClass().getDeclaredAnnotation(Plugin.class);

		this.version = annotation.version();
		this.name = annotation.name();

		try {
			this.file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

		} catch (final URISyntaxException ex) {
			throw new RuntimeException(ex);
		}

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

		Debugger.detectDebugMode();

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

			this.registerInitBungee();

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
	 * Handle the received message automatically if it matches our tag
	 *
	 * @param event
	 */
	@Subscribe
	public void onPluginMessage(PluginMessageEvent event) {
		synchronized (BungeeListener.DEFAULT_CHANNEL) {
			final ChannelMessageSource sender = event.getSource();
			final ChannelMessageSink receiver = event.getTarget();
			final byte[] data = event.getData();

			if (event.getResult() == ForwardResult.handled())
				return;

			if (!event.getIdentifier().getId().equals("BungeeCord") && !event.getIdentifier().getId().equals("bungeecord:main"))
				return;

			// Check if a player is not trying to send us a fake message
			if (!(sender instanceof ServerConnection))
				return;

			final ServerConnection connection = (ServerConnection) event.getSource();
			final ByteArrayInputStream stream = new ByteArrayInputStream(data);
			final ByteArrayDataInput in = ByteStreams.newDataInput(stream);

			final String subChannel = in.readUTF();

			boolean handled = false;

			for (final BungeeListener listener : BungeeListener.getRegisteredListeners())
				if (subChannel.equals(listener.getChannel())) {

					final UUID senderUid = UUID.fromString(in.readUTF());
					final String serverName = in.readUTF();
					final String actionName = in.readUTF();

					final BungeeMessageType action = BungeeMessageType.getByName(listener, actionName);
					Valid.checkNotNull(action, "Unknown plugin action '" + actionName + "'. IF YOU UPDATED THE PLUGIN BY RELOADING, stop your entire network, ensure all servers were updated and start it again.");

					final IncomingMessage message = new IncomingMessage(listener, senderUid, serverName, action, data, in, stream);

					listener.setSender((ServerConnection) sender);
					listener.setReceiver(receiver);
					listener.setData(data);

					Debugger.debug("bungee-all", "Channel " + subChannel + " received " + message.getAction() + " message from " + message.getServerName() + " server.");
					listener.onMessageReceived(listener.getSender(), message);
					handled = true;
				}

			// Credits: https://github.com/VelocityPowered/BungeeQuack/blob/master/src/main/java/com/velocitypowered/bungeequack/BungeeQuack.java
			// The reason for this ugly patch is that the above listener is ignored completely when velocity handles bungee commands :/
			//
			// https://github.com/kangarko/ChatControl-Red/issues/2673
			final ByteArrayDataOutput out = ByteStreams.newDataOutput();
			boolean found = true;

			if (subChannel.equals("ForwardToPlayer")) {
				this.proxy.getPlayer(in.readUTF())
						.ifPresent(player -> player.sendPluginMessage(event.getIdentifier(), prepareForwardMessage(in)));

			} else if (subChannel.equals("Forward")) {
				final String target = in.readUTF();
				final byte[] toForward = prepareForwardMessage(in);

				if (target.equals("ALL")) {
					for (final RegisteredServer rs : this.proxy.getAllServers())
						rs.sendPluginMessage(event.getIdentifier(), toForward);

				} else
					this.proxy.getServer(target).ifPresent(conn -> conn.sendPluginMessage(event.getIdentifier(), toForward));

			} else if (subChannel.equals("Connect")) {
				final Optional<RegisteredServer> info = this.proxy.getServer(in.readUTF());
				info.ifPresent(serverInfo -> connection.getPlayer().createConnectionRequest(serverInfo).fireAndForget());

			} else if (subChannel.equals("ConnectOther"))
				this.proxy.getPlayer(in.readUTF()).ifPresent(player -> {
					final Optional<RegisteredServer> info = this.proxy.getServer(in.readUTF());
					info.ifPresent(serverInfo -> connection.getPlayer().createConnectionRequest(serverInfo).fireAndForget());
				});

			else if (subChannel.equals("IP")) {
				out.writeUTF("IP");
				out.writeUTF(connection.getPlayer().getRemoteAddress().getHostString());
				out.writeInt(connection.getPlayer().getRemoteAddress().getPort());

			} else if (subChannel.equals("PlayerCount")) {
				final String target = in.readUTF();

				if (target.equals("ALL")) {
					out.writeUTF("PlayerCount");
					out.writeUTF("ALL");
					out.writeInt(this.proxy.getPlayerCount());
				} else
					this.proxy.getServer(target).ifPresent(rs -> {
						final int playersOnServer = rs.getPlayersConnected().size();
						out.writeUTF("PlayerCount");
						out.writeUTF(rs.getServerInfo().getName());
						out.writeInt(playersOnServer);
					});

			} else if (subChannel.equals("PlayerList")) {
				final String target = in.readUTF();

				if (target.equals("ALL")) {
					out.writeUTF("PlayerList");
					out.writeUTF("ALL");
					out.writeUTF(this.proxy.getAllPlayers().stream().map(Player::getUsername).collect(Collectors.joining(", ")));

				} else
					this.proxy.getServer(target).ifPresent(info -> {
						final String playersOnServer = info.getPlayersConnected().stream().map(Player::getUsername).collect(Collectors.joining(", "));
						out.writeUTF("PlayerList");
						out.writeUTF(info.getServerInfo().getName());
						out.writeUTF(playersOnServer);
					});

			} else if (subChannel.equals("GetServers")) {
				out.writeUTF("GetServers");
				out.writeUTF(this.proxy.getAllServers().stream().map(s -> s.getServerInfo().getName()).collect(Collectors.joining(", ")));

			} else if (subChannel.equals("Message")) {
				final String target = in.readUTF();
				final String message = in.readUTF();

				if (target.equals("ALL"))
					for (final Player player : this.proxy.getAllPlayers())
						Common.tell(player, message);

				else
					this.proxy.getPlayer(target).ifPresent(player -> {
						Common.tell(player, message);
					});

			} else if (subChannel.equals("GetServer")) {
				out.writeUTF("GetServer");
				out.writeUTF(connection.getServerInfo().getName());

			} else if (subChannel.equals("UUID")) {
				out.writeUTF("UUID");
				out.writeUTF(UuidUtils.toUndashed(connection.getPlayer().getUniqueId()));

			} else if (subChannel.equals("UUIDOther"))
				this.proxy.getPlayer(in.readUTF()).ifPresent(player -> {
					out.writeUTF("UUIDOther");
					out.writeUTF(player.getUsername());
					out.writeUTF(UuidUtils.toUndashed(player.getUniqueId()));
				});

			else if (subChannel.equals("ServerIP"))
				this.proxy.getServer(in.readUTF()).ifPresent(info -> {
					out.writeUTF("ServerIP");
					out.writeUTF(info.getServerInfo().getName());
					out.writeUTF(info.getServerInfo().getAddress().getHostString());
					out.writeShort(info.getServerInfo().getAddress().getPort());
				});

			else if (subChannel.equals("KickPlayer"))
				this.proxy.getPlayer(in.readUTF()).ifPresent(player -> {
					final String kickReason = in.readUTF();

					PlayerUtil.kick(player, kickReason);
				});

			else
				found = false;

			if (found) {
				final byte[] outData = out.toByteArray();

				if (outData.length > 0)
					connection.sendPluginMessage(event.getIdentifier(), outData);

				handled = true;
			}

			if (handled)
				event.setResult(PluginMessageEvent.ForwardResult.handled());
		}
	}

	// Credits: https://github.com/VelocityPowered/BungeeQuack/blob/master/src/main/java/com/velocitypowered/bungeequack/BungeeQuack.java
	private byte[] prepareForwardMessage(ByteArrayDataInput in) {
		final String channel = in.readUTF();
		final short messageLength = in.readShort();
		final byte[] message = new byte[messageLength];
		in.readFully(message);

		final ByteArrayDataOutput forwarded = ByteStreams.newDataOutput();
		forwarded.writeUTF(channel);
		forwarded.writeShort(messageLength);
		forwarded.write(message);
		return forwarded.toByteArray();
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

			Debugger.detectDebugMode();

			this.unregisterReloadables();

			FileConfig.clearLoadedSections();

			this.onPluginPreReload();

			Common.setTellPrefix(SimpleSettings.PLUGIN_PREFIX);
			this.onPluginReload();

			// Something went wrong in the reload pipeline
			if (!this.enabled)
				return;

			this.registerInitBungee();

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
		//this.proxy.getChannelRegistrar().register(new LegacyChannelIdentifier(listener.getChannel()));
		this.proxy.getEventManager().register(this, listener);
	}

	/*
	 * Register the main bungeecord listener alwas
	 */
	private void registerInitBungee() {
		this.proxy.getChannelRegistrar().register(LEGACY_BUNGEE_CHANNEL, MODERN_BUNGEE_CHANNEL);
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
}

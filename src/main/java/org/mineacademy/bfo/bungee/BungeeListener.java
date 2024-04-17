package org.mineacademy.bfo.bungee;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.message.IncomingMessage;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.plugin.SimplePlugin;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a BungeeCord listener using a bungee channel
 * on which you can listen to receiving messages
 *
 * This class is also a Listener for Bukkit events for your convenience
 */
public abstract class BungeeListener {

	/**
	 * Holds registered bungee listeners
	 */
	private static final Set<BungeeListener> registeredListeners = new HashSet<>();

	/**
	 * The channel
	 */
	@Getter
	private final String channel;

	/**
	 * The actions
	 */
	@Getter
	private final BungeeMessageType[] actions;

	/**
	 * Temporary variable storing the senders connection
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private ServerConnection sender;

	/**
	 * Temporary variable storing the receiver
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private ChannelMessageSink receiver;

	/**
	 * Temporary variable for reading data
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private byte[] data;

	/**
	 * Create a new bungee suite with the given params
	 *
	 * @param channel
	 * @param listener
	 * @param actions
	 */
	protected BungeeListener(@NonNull String channel, Class<? extends BungeeMessageType> actionEnum) {
		this.channel = channel;
		this.actions = toActions(actionEnum);

		for (final BungeeListener listener : registeredListeners)
			if (listener.getChannel().equals(this.getChannel()))
				return;

		registeredListeners.add(this);
	}

	private static BungeeMessageType[] toActions(@NonNull Class<? extends BungeeMessageType> actionEnum) {
		Valid.checkBoolean(actionEnum != BungeeMessageType.class, "When creating BungeeListener put your own class that extend BungeeMessageType there, not BungeeMessageType class itself!");
		Valid.checkBoolean(actionEnum.isEnum(), "BungeeListener expects BungeeMessageType to be an enum, given: " + actionEnum);

		try {
			return (BungeeMessageType[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to get values() of " + actionEnum + ", ensure it is an enum or has 'public static T[] values() method'!");

			return null;
		}
	}

	/**
	 * Called automatically when you receive a plugin message from Bungeecord,
	 * see https://spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel
	 *
	 * @param sender
	 * @param message
	 */
	public abstract void onMessageReceived(ServerConnection sender, IncomingMessage message);

	/**
	 * Shortcut for {@link ProxyServer#getInstance()}
	 *
	 * @return
	 */
	protected final ProxyServer getProxy() {
		return SimplePlugin.getServer();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BungeeListener && ((BungeeListener) obj).getChannel().equals(this.getChannel());
	}

	/**
	 * Distributes received plugin message across all {@link BungeeListener} classes
	 *
	 * @deprecated internal use only
	 */
	@Deprecated
	public static final class BungeeListenerImpl {

		private static boolean registered = false;

		public BungeeListenerImpl() {
			Valid.checkBoolean(!registered, "Already registered!");

			registered = true;
		}

		/**
		 * Handle the received message automatically if it matches our tag
		 *
		 * @param event
		 */
		@Subscribe
		public void onPluginMessage(PluginMessageEvent event) {

			final ChannelMessageSource sender = event.getSource();
			final ChannelMessageSink receiver = event.getTarget();
			final byte[] data = event.getData();

			if (event.getResult() == ForwardResult.handled())
				return;

			// Check if the message is for a server (ignore client messages)
			//if (!event.getTag().equals("BungeeCord"))
			//	return;

			// Check if a player is not trying to send us a fake message
			if (!(sender instanceof ServerConnection))
				return;

			final String channelName = event.getIdentifier().getId();
			boolean handled = false;

			for (final BungeeListener listener : registeredListeners)
				if (channelName.equals(listener.getChannel())) {

					// Read the plugin message
					final ByteArrayInputStream stream = new ByteArrayInputStream(data);
					ByteArrayDataInput input;

					try {
						input = ByteStreams.newDataInput(stream);

					} catch (final Throwable t) {
						input = ByteStreams.newDataInput(data);
					}

					input.readUTF(); // unused channel name
					final UUID senderUid = UUID.fromString(input.readUTF());
					final String serverName = input.readUTF();
					final String actionName = input.readUTF();

					final BungeeMessageType action = BungeeMessageType.getByName(listener, actionName);
					Valid.checkNotNull(action, "Unknown plugin action '" + actionName + "'. IF YOU UPDATED THE PLUGIN BY RELOADING, stop your entire network, ensure all servers were updated and start it again.");

					final IncomingMessage message = new IncomingMessage(listener, senderUid, serverName, action, data, input, stream);

					listener.sender = (ServerConnection) sender;
					listener.receiver = receiver;
					listener.data = data;

					Debugger.debug("bungee-all", "Channel " + channelName + " received " + message.getAction() + " message from " + message.getServerName() + " server.");
					listener.onMessageReceived(listener.sender, message);

					handled = true;
				}

			if (handled)
				event.setResult(PluginMessageEvent.ForwardResult.handled());
		}
	}
}

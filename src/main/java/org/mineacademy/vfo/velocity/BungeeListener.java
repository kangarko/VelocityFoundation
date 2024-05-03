package org.mineacademy.vfo.velocity;

import java.util.HashSet;
import java.util.Set;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.plugin.SimplePlugin;
import org.mineacademy.vfo.velocity.message.IncomingMessage;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;

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
	 * The default channel
	 */
	public static final ChannelIdentifier DEFAULT_CHANNEL = new LegacyChannelIdentifier("BungeeCord");

	/**
	 * Holds registered bungee listeners
	 */
	@Getter
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
	@Getter
	private ServerConnection sender;

	/**
	 * Temporary variable storing the receiver
	 */
	@Getter
	private ChannelMessageSink receiver;

	/**
	 * Temporary variable for reading data
	 */
	@Getter
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
	 * @deprecated internal use only
	 *
	 * @param sender
	 */
	@Deprecated
	public void setSender(ServerConnection sender) {
		this.sender = sender;
	}

	/**
	 * @deprecated internal use only
	 *
	 * @param receiver
	 */
	@Deprecated
	public void setReceiver(ChannelMessageSink receiver) {
		this.receiver = receiver;
	}

	/**
	 * @deprecated internal use only
	 *
	 * @param data
	 */
	@Deprecated
	public void setData(byte[] data) {
		this.data = data;
	}

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
}

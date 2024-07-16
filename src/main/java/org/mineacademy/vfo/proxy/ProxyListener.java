package org.mineacademy.vfo.proxy;

import java.util.HashSet;
import java.util.Set;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.plugin.SimplePlugin;
import org.mineacademy.vfo.proxy.message.IncomingMessage;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a proxy listener using a custom plugin message channel
 * on which you can listen to receiving messages
 */
public abstract class ProxyListener {

	/**
	 * The default channel
	 */
	public static final ChannelIdentifier DEFAULT_CHANNEL = new LegacyChannelIdentifier("BungeeCord");

	/**
	 * Holds registered listeners
	 */
	@Getter
	private static final Set<ProxyListener> registeredListeners = new HashSet<>();

	/**
	 * The channel
	 */
	@Getter
	private final String channel;

	/**
	 * The actions
	 */
	@Getter
	private final ProxyMessage[] actions;

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
	 * Create a new proxy listener with the given params
	 *
	 * @param channel
	 * @param listener
	 * @param actions
	 */
	protected ProxyListener(@NonNull String channel, Class<? extends ProxyMessage> actionEnum) {
		this.channel = channel;
		this.actions = toActions(actionEnum);

		for (final ProxyListener listener : registeredListeners)
			if (listener.getChannel().equals(this.getChannel()))
				return;

		registeredListeners.add(this);
	}

	private static ProxyMessage[] toActions(@NonNull Class<? extends ProxyMessage> actionEnum) {
		Valid.checkBoolean(actionEnum != ProxyMessage.class, "When creating a new proxy listener put your own class that extend ProxyMessage there, not ProxyMessage class itself!");
		Valid.checkBoolean(actionEnum.isEnum(), "Proxy listener expects ProxyMessage to be an enum, given: " + actionEnum);

		try {
			return (ProxyMessage[]) actionEnum.getMethod("values").invoke(null);

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex, "Unable to get values() of " + actionEnum + ", ensure it is an enum or has 'public static T[] values() method'!");

			return null;
		}
	}

	/**
	 * Called automatically when you receive a plugin message from Bukkit,
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
		return obj instanceof ProxyListener && ((ProxyListener) obj).getChannel().equals(this.getChannel());
	}
}
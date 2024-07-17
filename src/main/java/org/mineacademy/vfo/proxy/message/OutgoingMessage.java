package org.mineacademy.vfo.proxy.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.collection.SerializedMap;
import org.mineacademy.vfo.debug.Debugger;
import org.mineacademy.vfo.model.SimpleComponent;
import org.mineacademy.vfo.plugin.SimplePlugin;
import org.mineacademy.vfo.proxy.ProxyListener;
import org.mineacademy.vfo.proxy.ProxyMessage;
import org.mineacademy.vfo.remain.Remain;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;

/**
 * NB: This uses the standardized Foundation model where the first
 * String is the server name and the second String is the
 * {@link ProxyMessage} by its name *written automatically*.
 */
public final class OutgoingMessage extends Message {

	/**
	 * The pending queue to write the message
	 */
	private final List<Object> queue = new ArrayList<>();

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param action
	 */
	public OutgoingMessage(ProxyMessage action) {
		this(SimplePlugin.getInstance().getProxyListener(), action);
	}

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param listener
	 * @param action
	 */
	public OutgoingMessage(ProxyListener listener, ProxyMessage action) {
		super(listener, action);
	}

	/**
	 * Write the given strings into the message
	 *
	 * @param messages
	 */
	public void writeString(String... messages) {
		for (final String message : messages)
			this.write(message, String.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param component
	 */
	public void writeComponent(Component component) {
		this.write(component, Component.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param component
	 */
	public void writeSimpleComponent(SimpleComponent component) {
		this.write(component, SimpleComponent.class);
	}

	/**
	 * Write the map into the message
	 *
	 * @param map
	 */
	public void writeMap(SerializedMap map) {
		this.write(map, SerializedMap.class);
	}

	/**
	 * Write a boolean into the message
	 *
	 * @param bool
	 */
	public void writeBoolean(boolean bool) {
		this.write(bool, Boolean.class);
	}

	/**
	 * Write a byte into the message
	 *
	 * @param number
	 */
	public void writeByte(byte number) {
		this.write(number, Byte.class);
	}

	/**
	 * Write a double into the message
	 *
	 * @param number
	 */
	public void writeDouble(double number) {
		this.write(number, Double.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeFloat(float number) {
		this.write(number, Float.class);
	}

	/**
	 * Write an integer into the message
	 *
	 * @param number
	 */
	public void writeInt(int number) {
		this.write(number, Integer.class);
	}

	/**
	 * Write a float into the message
	 *
	 * @param number
	 */
	public void writeLong(long number) {
		this.write(number, Long.class);
	}

	/**
	 * Write a short into the message
	 *
	 * @param number
	 */
	public void writeShort(short number) {
		this.write(number, Short.class);
	}

	/**
	 * Write an uuid into the message
	 *
	 * @param uuid
	 */
	public void writeUUID(UUID uuid) {
		this.write(uuid, UUID.class);
	}

	/*
	 * Write an object of the given type into the message
	 */
	private void write(Object object, Class<?> typeOf) {
		Valid.checkNotNull(object, "Added object must not be null!");

		this.moveHead(typeOf);
		this.queue.add(object);
	}

	/**
	 * Convert the queue into byte array
	 *
	 * @param serverName
	 * @return
	 */
	public byte[] toByteArray(String serverName) {

		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF(this.getListener().getChannel());
		out.writeUTF(UUID.fromString("00000000-0000-0000-0000-000000000000").toString());
		out.writeUTF(serverName);
		out.writeUTF(this.getMessage().name());

		int head = 0;

		for (final Object data : this.queue)
			try {
				if (data == null)
					throw new NullPointerException("Null data");

				if (data instanceof Integer) {
					checkData(head, Integer.class);

					out.writeInt((Integer) data);

				} else if (data instanceof Double) {
					checkData(head, Double.class);

					out.writeDouble((Double) data);

				} else if (data instanceof Long) {
					checkData(head, Long.class);

					out.writeLong((Long) data);

				} else if (data instanceof Boolean) {
					checkData(head, Boolean.class);

					out.writeBoolean((Boolean) data);

				} else if (data instanceof String) {
					checkData(head, String.class);

					OutgoingMessage.writeCompressedString(out, (String) data);

				} else if (data instanceof Component) {
					checkData(head, Component.class);

					OutgoingMessage.writeCompressedString(out, Remain.convertAdventureToJson((Component) data));

				} else if (data instanceof SimpleComponent) {
					checkData(head, SimpleComponent.class);

					OutgoingMessage.writeCompressedString(out, ((SimpleComponent) data).serialize().toJson());

				} else if (data instanceof SerializedMap) {
					checkData(head, SerializedMap.class);

					OutgoingMessage.writeCompressedString(out, ((SerializedMap) data).toJson());

				} else if (data instanceof UUID) {
					checkData(head, UUID.class);

					out.writeUTF(((UUID) data).toString());

				} else if (data instanceof Enum) {
					checkData(head, Enum.class);

					out.writeUTF(((Enum<?>) data).toString());

				} else if (data instanceof byte[]) {
					checkData(head, String.class);

					out.write((byte[]) data);

				} else
					throw new IllegalArgumentException("Unknown type of data");

				head++;

			} catch (final Throwable t) {
				Common.throwError(t,
						"Error writing data in proxy plugin message!",
						"Message: " + this.getMessage(),
						"Channel: " + this.getListener().getChannel(),
						"Wrong data: " + data,
						"Error: %error%",
						"All data: " + this.queue);
			}

		return out.toByteArray();
	}

	/*
	 * Ensures we are reading in the correct order and the correct data type.
	 */
	private void checkData(int head, Class<?> requiredType) throws Throwable {
		final Class<?>[] content = this.getMessage().getContent();
		final Class<?> clazz = content[head];

		Valid.checkBoolean(requiredType.isAssignableFrom(clazz), "Expected " + requiredType.getSimpleName() + " at position " + head + " but got " + clazz.getSimpleName() + " for " + this.getMessage().name());
		Valid.checkBoolean(head < content.length, "Head out of bounds! Max data size for " + this.getMessage().name() + " is " + content.length);
	}

	/**
	 * Forwards this message to another server
	 *
	 * @param fromServer
	 * @param info
	 */
	public void sendToServer(String fromServer, RegisteredServer info) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			if (info.getPlayersConnected().isEmpty()) {
				Debugger.debug("proxy", "NOT sending data on " + this.getChannel() + " channel from " + this.getMessage() + " to " + info.getServerInfo().getName() + " server because it is empty.");

				return;
			}

			final byte[] data = this.toByteArray(fromServer);

			if (data.length > 32_700) { // Safety margin
				Common.log("[outgoing-sendToServer] Outgoing proxy message was oversized, not sending. Max length: 32766 bytes, got " + data.length + " bytes.");

				return;
			}

			info.sendPluginMessage(ProxyListener.DEFAULT_CHANNEL, data);
			Debugger.debug("proxy", "Forwarding data on " + this.getChannel() + " channel from " + this.getMessage() + " to " + info.getServerInfo().getName() + " server.");
		}
	}

	/**
	 * Broadcasts the message to all servers
	 *
	 */
	public void broadcast() {
		this.broadcastExcept(null);
	}

	/**
	 * Broadcasts the message to all servers except the one ignored
	 *
	 * @param ignoredServerName
	 */
	public void broadcastExcept(@Nullable String ignoredServerName) {
		synchronized (ProxyListener.DEFAULT_CHANNEL) {
			final String channel = this.getChannel();
			final byte[] data = this.toByteArray("");

			if (data.length > 32_700) { // Safety margin
				Common.log("[outgoing-broadcastExcept] Outgoing message was oversized, not sending. Max length: 32766 bytes, got " + data.length + " bytes. Channel: " + this.getListener().getChannel()
						+ ", action: " + this.getMessage().name() + ", queue: " + queue);

				return;
			}

			for (final RegisteredServer otherServer : Remain.getServers()) {
				if (otherServer.getPlayersConnected().isEmpty()) {
					Debugger.debug("proxy", "NOT sending data on " + channel + " channel from " + this.getMessage() + " to " + otherServer.getServerInfo().getName() + " server because it is empty.");

					continue;
				}

				if (ignoredServerName != null && otherServer.getServerInfo().getName().equalsIgnoreCase(ignoredServerName)) {
					Debugger.debug("proxy", "NOT sending data on " + channel + " channel from " + this.getMessage() + " to " + otherServer.getServerInfo().getName() + " server because it is ignored.");

					continue;
				}

				otherServer.sendPluginMessage(ProxyListener.DEFAULT_CHANNEL, data);
				Debugger.debug("proxy", "Sending data on " + channel + " channel from " + this.getMessage() + " to " + otherServer.getServerInfo().getName() + " server.");
			}
		}
	}

	/**
	 *
	 * @return
	 */
	protected String getChannel() {
		return this.getListener().getChannel();
	}

	/**
	 * Writes a compressed string to the output
	 *
	 * @param out
	 * @param data
	 */
	public static void writeCompressedString(ByteArrayDataOutput out, String data) {
		final byte[] compressed = Common.compress(data);

		out.writeInt(compressed.length);
		out.write(compressed);
	}
}
package org.mineacademy.vfo.velocity.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.collection.SerializedMap;
import org.mineacademy.vfo.debug.Debugger;
import org.mineacademy.vfo.exception.FoException;
import org.mineacademy.vfo.plugin.SimplePlugin;
import org.mineacademy.vfo.velocity.BungeeListener;
import org.mineacademy.vfo.velocity.BungeeMessageType;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * NB: This uses the standardized Foundation model where the first
 * String is the server name and the second String is the
 * {@link BungeeMessageType} by its name *written automatically*.
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
	public OutgoingMessage(BungeeMessageType action) {
		this(SimplePlugin.getInstance().getBungeeCord(), action);
	}

	/**
	 * Create a new outgoing message, see header of this class
	 *
	 * @param listener
	 * @param action
	 */
	public OutgoingMessage(BungeeListener listener, BungeeMessageType action) {
		super(listener, action);
	}

	/**
	 * Write the map into the message
	 *
	 * @param map
	 */
	public void writeMap(SerializedMap map) {
		this.write(map.toJson(), String.class);
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

	/**
	 * Write an object of the given type into the message
	 * <p>
	 * We move the head and ensure writing safety in accordance
	 * to the {@link BungeeMessageType#getContent()} length and
	 * data type at the given position
	 *
	 * @param object
	 * @param typeOf
	 */
	private void write(Object object, Class<?> typeOf) {
		Valid.checkNotNull(object, "Added object must not be null!");

		this.moveHead(typeOf);
		this.queue.add(object);
	}

	/**
	 * Delegate write methods for the byte array data output
	 * based on the queue
	 *
	 * @param serverName
	 * @return
	 */
	public byte[] getData(String serverName) {
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		// -----------------------------------------------------------------
		// We are automatically writing the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		out.writeUTF(this.getListener().getChannel());
		out.writeUTF(UUID.fromString("00000000-0000-0000-0000-000000000000").toString());
		out.writeUTF(serverName);
		out.writeUTF(this.getAction().name());

		for (final Object object : this.queue)
			if (object instanceof String)
				out.writeUTF((String) object);

			else if (object instanceof Boolean)
				out.writeBoolean((Boolean) object);

			else if (object instanceof Byte)
				out.writeByte((Byte) object);

			else if (object instanceof Double)
				out.writeDouble((Double) object);

			else if (object instanceof Float)
				out.writeFloat((Float) object);

			else if (object instanceof Integer)
				out.writeInt((Integer) object);

			else if (object instanceof Long)
				out.writeLong((Long) object);

			else if (object instanceof Short)
				out.writeShort((Short) object);

			else if (object instanceof byte[])
				out.write((byte[]) object);

			else if (object instanceof UUID)
				out.writeUTF(object.toString());

			else
				throw new FoException("Unsupported write of " + object.getClass().getSimpleName() + " to channel " + this.getChannel() + " with action " + this.getAction().toString());

		return out.toByteArray();
	}

	/**
	 * Forwards this message to another server
	 *
	 * @param fromServer
	 * @param info
	 */
	public void sendToServer(String fromServer, RegisteredServer info) {
		synchronized (BungeeListener.DEFAULT_CHANNEL) {
			if (info.getPlayersConnected().isEmpty()) {
				Debugger.debug("bungee", "NOT sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + info.getServerInfo().getName() + " server because it is empty.");

				return;
			}

			final byte[] data = this.getData(fromServer);

			if (data.length > 32_000) { // Safety margin
				Common.log("[outgoing-sendToServer] Outgoing bungee message was oversized, not sending. Max length: 32766 bytes, got " + data.length + " bytes.");

				return;
			}

			info.sendPluginMessage(BungeeListener.DEFAULT_CHANNEL, data);
			Debugger.debug("bungee", "Forwarding data on " + this.getChannel() + " channel from " + this.getAction() + " to " + info.getServerInfo().getName() + " server.");
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
		synchronized (BungeeListener.DEFAULT_CHANNEL) {
			final String channel = this.getChannel();
			final byte[] data = this.getData("");

			if (data.length > 32_000) { // Safety margin
				Common.log("[outgoing-broadcastExcept] Outgoing message was oversized, not sending. Max length: 32766 bytes, got " + data.length + " bytes. Channel: " + this.getListener().getChannel()
						+ ", action: " + this.getAction().name() + ", queue: " + queue);

				return;
			}

			for (final RegisteredServer otherServer : SimplePlugin.getServer().getAllServers()) {
				if (otherServer.getPlayersConnected().isEmpty()) {
					Debugger.debug("bungee", "NOT sending data on " + channel + " channel from " + this.getAction() + " to " + otherServer.getServerInfo().getName() + " server because it is empty.");

					continue;
				}

				if (ignoredServerName != null && otherServer.getServerInfo().getName().equalsIgnoreCase(ignoredServerName)) {
					Debugger.debug("bungee", "NOT sending data on " + channel + " channel from " + this.getAction() + " to " + otherServer.getServerInfo().getName() + " server because it is ignored.");

					continue;
				}

				otherServer.sendPluginMessage(BungeeListener.DEFAULT_CHANNEL, data);
				Debugger.debug("bungee", "Sending data on " + channel + " channel from " + this.getAction() + " to " + otherServer.getServerInfo().getName() + " server.");
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
}
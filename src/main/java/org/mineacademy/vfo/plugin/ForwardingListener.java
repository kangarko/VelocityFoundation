package org.mineacademy.vfo.plugin;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.PlayerUtil;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.debug.Debugger;
import org.mineacademy.vfo.remain.Remain;
import org.mineacademy.vfo.velocity.BungeeListener;
import org.mineacademy.vfo.velocity.BungeeMessageType;
import org.mineacademy.vfo.velocity.message.IncomingMessage;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.UuidUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class ForwardingListener {

	/**
	 * The proxy
	 */
	private final ProxyServer proxy;

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

					try {
						listener.onMessageReceived(listener.getSender(), message);

					} catch (final Throwable t) {
						Common.error(t,
								Common.consoleLine(),
								"ERROR COMMUNICATING WITH SPIGOT",
								Common.consoleLine(),
								"Ensure you are running latest version of",
								"both proxy and Spigot plugins!",
								"",
								"Server: " + connection.getServerInfo().getName(),
								"Error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
					}

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
					for (final RegisteredServer rs : Remain.getServers())
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
					out.writeUTF(Remain.getOnlinePlayers().stream().map(Player::getUsername).collect(Collectors.joining(", ")));

				} else
					this.proxy.getServer(target).ifPresent(info -> {
						final String playersOnServer = info.getPlayersConnected().stream().map(Player::getUsername).collect(Collectors.joining(", "));
						out.writeUTF("PlayerList");
						out.writeUTF(info.getServerInfo().getName());
						out.writeUTF(playersOnServer);
					});

			} else if (subChannel.equals("GetServers")) {
				out.writeUTF("GetServers");
				out.writeUTF(Remain.getServers().stream().map(s -> s.getServerInfo().getName()).collect(Collectors.joining(", ")));

			} else if (subChannel.equals("Message")) {
				final String target = in.readUTF();
				final String message = in.readUTF();

				if (target.equals("ALL"))
					for (final Player player : Remain.getOnlinePlayers())
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
}

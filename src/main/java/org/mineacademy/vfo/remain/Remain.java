package org.mineacademy.vfo.remain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.ReflectionUtil;
import org.mineacademy.vfo.ReflectionUtil.ReflectionException;
import org.mineacademy.vfo.exception.FoException;
import org.mineacademy.vfo.model.Variables;
import org.mineacademy.vfo.plugin.SimplePlugin;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;

/**
 * Our main cross-version compatibility class.
 * <p>
 * Look up for many methods enabling you to make your plugin
 * compatible with MC 1.8.8 up to the latest version.
 */
public final class Remain {

	/**
	 * The Google Json instance
	 */
	private final static Gson gson = new Gson();

	// ----------------------------------------------------------------------------------------------------
	// Flags below
	// ----------------------------------------------------------------------------------------------------
	/**
	 * The internal private section path data class
	 */
	private static Class<?> sectionPathDataClass = null;

	// Singleton
	private Remain() {
	}

	/**
	 * Initialize all fields and methods automatically when we set the plugin
	 */
	static {

		try {
			sectionPathDataClass = ReflectionUtil.lookupClass("org.bukkit.configuration.SectionPathData");

		} catch (final ReflectionException ex) {
			// unsupported
		}
	}

	// ----------------------------------------------------------------------------------------------------
	// Compatibility methods below
	// ----------------------------------------------------------------------------------------------------

	/**
	 * The server getter, used to change for Redis compatibility
	 */
	@Setter
	private static Supplier<Collection<RegisteredServer>> serverGetter = () -> SimplePlugin.getServer().getAllServers();

	/**
	 * Return the server by the given name
	 *
	 * @param name
	 * @return
	 */
	public static RegisteredServer getServer(String name) {
		for (final RegisteredServer server : Remain.getServers())
			if (server.getServerInfo().getName().equalsIgnoreCase(name))
				return server;

		return null;
	}

	/**
	 * Returns all servers
	 *
	 * @return
	 */
	public static Collection<RegisteredServer> getServers() {
		return serverGetter.get();
	}

	/**
	 * Returns all online players
	 *
	 * @return the online players
	 */
	public static Collection<Player> getOnlinePlayers() {
		final Collection<Player> players = new ArrayList<>();

		for (final RegisteredServer serverInfo : getServers())
			for (final Player player : serverInfo.getPlayersConnected())
				if (player != null && !players.contains(player))
					players.add(player);

		return players;
	}

	/**
	 * Get the player or null if he is not online
	 *
	 * @param name
	 * @return
	 */
	public static Player getPlayer(String name) {
		for (final Player player : getOnlinePlayers())
			if (player.getUsername().equalsIgnoreCase(name))
				return player;

		return null;
	}

	/**
	 * Get the player or null if he is not online
	 *
	 * @param uuid
	 * @return
	 */
	public static Player getPlayer(UUID uuid) {
		for (final Player player : getOnlinePlayers())
			if (player.getUniqueId().equals(uuid))
				return player;

		return null;
	}

	/**
	 * Return the given list as JSON
	 *
	 * @param list
	 * @return
	 */
	public static String toJson(final Collection<String> list) {
		return gson.toJson(list);
	}

	/**
	 * Convert the given json into list
	 *
	 * @param json
	 * @return
	 */
	public static List<String> fromJsonList(String json) {
		return gson.fromJson(json, List.class);
	}

	/**
	 * Converts a component to JSON
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToJson(Component component) {
		return GsonComponentSerializer.gson().serialize(component);
	}

	/**
	 * Serializes the component into legacy text
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToLegacy(Component component) {
		return LegacyComponentSerializer.legacySection().serialize(component);
	}

	/**
	 * Serializes the component into plain text
	 *
	 * @param component
	 * @return
	 */
	public static String convertAdventureToPlain(Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	/**
	 * Converts a json string to Adventure component
	 *
	 * @param json
	 * @return
	 */
	public static Component convertJsonToAdventure(String json) {
		return GsonComponentSerializer.gson().deserialize(json);
	}

	/**
	 *
	 * @param componentJson
	 * @return
	 */
	public static String convertJsonToLegacy(String componentJson) {
		return convertAdventureToLegacy(convertJsonToAdventure(componentJson));
	}

	/**
	 * Creates a new adventure component from legacy text with {@link CompChatColor#COLOR_CHAR} colors replaced
	 *
	 * @param legacyText
	 * @return
	 */
	public static Component convertLegacyToAdventure(String legacyText) {
		return LegacyComponentSerializer.legacySection().deserialize(legacyText);
	}

	/**
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 *
	 * @param message
	 * @return
	 */
	public static String convertLegacyToJson(final String message) {
		return GsonComponentSerializer.gson().serialize(convertLegacyToAdventure(message));
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(final Audience sender, final String json) {
		try {
			sender.sendMessage(convertJsonToAdventure(json));

		} catch (final Throwable t) {

			// Silence a bug in md_5's library
			if (t.toString().contains("missing 'text' property"))
				return;

			throw new RuntimeException("Malformed JSON when sending message to " + sender + " with JSON: " + json, t);
		}
	}

	/**
	 * Send the sender a component, ignoring it if it is empty
	 *
	 * @param sender
	 * @param component
	 */
	public static void tell(Audience sender, Component component) {
		tell(sender, component, true);
	}

	/**
	 * Send the sender a component, ignoring it if it is empty
	 *
	 * @param sender
	 * @param component
	 * @param skipEmpty
	 */
	public static void tell(@NonNull Audience sender, Component component, boolean skipEmpty) {
		if (Remain.convertAdventureToPlain(component).trim().isEmpty() && skipEmpty)
			return;

		sender.sendMessage(component);
	}

	/**
	 * Sends a title to the player (1.8+) for three seconds
	 *
	 * @param player
	 * @param title
	 * @param subtitle
	 */
	public static void sendTitle(final Audience player, final String title, final String subtitle) {
		sendTitle(player, 20, 3 * 20, 20, title, subtitle);
	}

	/**
	 * Sends a title to the player (1.8+) Texts will be colorized.
	 *
	 * @param player   the player
	 * @param fadeIn   how long to fade in the title (in ticks)
	 * @param stay     how long to make the title stay (in ticks)
	 * @param fadeOut  how long to fade out (in ticks)
	 * @param title    the title, will be colorized
	 * @param subtitle the subtitle, will be colorized
	 */
	public static void sendTitle(final Audience player, final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
		player.showTitle(Title.title(
				convertLegacyToAdventure(Variables.replace(title, player)),
				convertLegacyToAdventure(Variables.replace(subtitle, player)),
				Times.times(Duration.ofSeconds(fadeIn * 50), Duration.ofSeconds(stay * 50), Duration.ofMillis(fadeOut * 50))));
	}

	/**
	 * Resets the title that is being displayed to the player (1.8+)
	 *
	 * @param player the player
	 */
	public static void resetTitle(final Audience player) {
		player.resetTitle();
	}

	/**
	 * Sets tab-list header and/or footer. Header or footer can be null. (1.8+)
	 * Texts will be colorized.
	 *
	 * @param player the player
	 * @param header the header
	 * @param footer the footer
	 */
	public static void sendTablist(final Audience player, final String header, final String footer) {
		player.sendPlayerListHeaderAndFooter(convertLegacyToAdventure(Variables.replace(header, player)), convertLegacyToAdventure(Variables.replace(footer, player)));
	}

	/**
	 * Displays message above player's health and hunger bar. (1.8+) Text will be
	 * colorized.
	 *
	 * @param player the player
	 * @param text   the text
	 */
	public static void sendActionBar(final Audience player, final String text) {
		player.sendActionBar(convertLegacyToAdventure(Variables.replace(text, player)));
	}

	/**
	 * Shows a boss bar that is then hidden after the given period
	 *
	 * @param player
	 * @param message
	 * @param secondsToShow
	 */
	public static void sendBossbarTimed(Audience player, String message, int secondsToShow) {
		final BossBar bar = BossBar.bossBar(convertLegacyToAdventure(Variables.replace(message, player)), 1F, Color.WHITE, Overlay.PROGRESS);
		player.showBossBar(bar);

		Common.runLaterAsync(secondsToShow * 20, (Runnable) () -> removeBossBar(player, bar));
	}

	/**
	 * Send boss bar as percent
	 *
	 * @param player
	 * @param message
	 * @param percent
	 *
	 * @return
	 */
	public static BossBar sendBossbar(final Audience player, final String message, final float percent) {
		return sendBossbar(player, message, percent, null, null);
	}

	/**
	 * Send boss bar as percent
	 *
	 * @param player
	 * @param message
	 * @param percent from 0.0 to 1.0
	 * @param color
	 * @param style
	 *
	 * return
	 * @return
	 */
	public static BossBar sendBossbar(final Audience player, final String message, final float percent, final Color color, final Overlay style) {
		final BossBar bar = BossBar.bossBar(convertLegacyToAdventure(Variables.replace(message, player)), percent, color, style);
		player.showBossBar(bar);

		return bar;
	}

	/**
	 * Attempts to remove a boss bar of the given UUID from player.
	 *
	 * @param player
	 * @param bar
	 */
	public static void removeBossBar(final Audience player, BossBar bar) {
		player.hideBossBar(bar);
	}

	/**
	 * Converts an unchecked exception into checked
	 *
	 * @param throwable
	 */
	public static void sneaky(final Throwable throwable) {
		try {
			SneakyThrow.sneaky(throwable);

		} catch (final NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
			throw new FoException(throwable);
		}
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

	/**
	 * Converts the given object that may be a SectionPathData for MC 1.18 back into its root data
	 *
	 * @param objectOrSectionPathData
	 * @return
	 *
	 * @deprecated legacy code, will be removed
	 */
	@Deprecated
	public static Object getRootOfSectionPathData(Object objectOrSectionPathData) {
		if (objectOrSectionPathData != null && objectOrSectionPathData.getClass() == sectionPathDataClass)
			objectOrSectionPathData = ReflectionUtil.invoke("getData", objectOrSectionPathData);

		return objectOrSectionPathData;
	}

	/**
	 * Return true if the given object is a memory section
	 *
	 * @param obj
	 * @return
	 */
	public static boolean isMemorySection(Object obj) {
		return obj != null && sectionPathDataClass == obj.getClass();
	}

	// ----------------------------------------------------------------------------------------------------
	// Classes
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Thrown when message contains hover or click events which would otherwise got
	 * removed.
	 * <p>
	 * Such message is not checked.
	 */
	public static class InteractiveTextFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InteractiveTextFoundException() {
		}
	}
}

/**
 * A wrapper for Spigot
 */
class SneakyThrow {

	public static void sneaky(final Throwable t) {
		throw SneakyThrow.<RuntimeException>superSneaky(t);
	}

	private static <T extends Throwable> T superSneaky(final Throwable t) throws T {
		throw (T) t;
	}
}
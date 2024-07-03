package org.mineacademy.vfo.remain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.ReflectionUtil;
import org.mineacademy.vfo.ReflectionUtil.ReflectionException;
import org.mineacademy.vfo.collection.SerializedMap;
import org.mineacademy.vfo.exception.FoException;
import org.mineacademy.vfo.model.Variables;
import org.mineacademy.vfo.plugin.SimplePlugin;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.TitlePart;

/**
 * Our main cross-version compatibility class.
 * <p>
 * Look up for many methods enabling you to make your plugin
 * compatible with MC 1.8.8 up to the latest version.
 */
public final class Remain {

	/**
	 * Pattern used to match encoded HEX colors &x&F&F&F&F&F&F
	 */
	private static final Pattern RGB_HEX_ENCODED_REGEX = Pattern.compile("(?i)(§x)((§[0-9A-F]){6})");

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
			players.addAll(serverInfo.getPlayersConnected());

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
	 * Converts chat message with color codes to Json chat components e.g. &6Hello
	 * world converts to {text:"Hello world",color="gold"}
	 * @param message
	 * @return
	 */
	public static String toJson(final String message) {
		return toJson(JSONComponentSerializer.json().serialize(toComponentLegacy(message)));
	}

	/**
	 * Converts base components into json
	 *
	 * @param comps
	 * @return
	 */
	public static String toJson(final Component comps) {
		return JSONComponentSerializer.json().serialize(comps);
	}

	/**
	 * Converts legacy text into an array of components
	 *
	 * @param text
	 * @return
	 */
	public static TextComponent toComponentLegacy(final String text) {
		return LegacyComponentSerializer.legacySection().deserialize(Common.colorize(text));
	}

	/**
	 *
	 * @param componentJson
	 * @return
	 */
	public static String toLegacyText(String componentJson) {
		return toLegacyText(toComponent(componentJson));
	}

	/**
	 *
	 * @param component
	 * @return
	 */
	public static String toLegacyText(Component component) {
		return LegacyComponentSerializer.legacySection().serialize(component);
	}

	/**
	 * Converts json into base component array
	 *
	 * @param json
	 * @return
	 */
	public static Component toComponent(final String json) {
		try {
			return JSONComponentSerializer.json().deserialize(json);

		} catch (final Throwable t) {
			Common.throwError(t,
					"Failed to call toComponent!",
					"Json: " + json,
					"Error: %error%");

			return null;
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 * @param placeholders
	 */
	public static void sendJson(final Audience sender, final String json, final SerializedMap placeholders) {
		try {
			final Component component = toComponent(json);
			replaceHexPlaceholders(Arrays.asList(component), placeholders);

			sender.sendMessage(component);

		} catch (final RuntimeException ex) {
			Common.error(ex, "Malformed JSON when sending message to " + sender + " with JSON: " + json);
		}
	}

	/*
	 * A helper Method for MC 1.16+ to partially solve the issue of HEX colors in JSON
	 *
	 * BaseComponent does not support colors when in text, they must be set at the color level
	 */
	private static void replaceHexPlaceholders(final List<Component> components, final SerializedMap placeholders) {

		for (final Component component : components) {
			if (component instanceof TextComponent) {
				final TextComponent textComponent = (TextComponent) component;
				String text = textComponent.content();

				for (final Map.Entry<String, Object> entry : placeholders.entrySet()) {
					String key = entry.getKey();
					String value = Common.simplify(entry.getValue());

					// Detect HEX in placeholder
					final Matcher match = RGB_HEX_ENCODED_REGEX.matcher(text);

					while (match.find()) {

						// Find the color
						final String color = "#" + match.group(2).replace(CompChatColor.COLOR_CHAR + "", "");

						// Remove it from chat and bind it to TextComponent instead
						value = match.replaceAll("");
						textComponent.color(TextColor.fromHexString(color));
					}

					key = key.charAt(0) != '{' ? "{" + key : key;
					key = key.charAt(key.length() - 1) != '}' ? key + "}" : key;

					text = text.replace(key, value);
					textComponent.content(text);
				}
			}

			if (component.children() != null)
				replaceHexPlaceholders(component.children(), placeholders);
		}
	}

	/**
	 * Sends JSON component to sender
	 *
	 * @param sender
	 * @param json
	 */
	public static void sendJson(final Audience sender, final String json) {
		try {
			sender.sendMessage(toComponent(json));

		} catch (final Throwable t) {

			// Silence a bug in md_5's library
			if (t.toString().contains("missing 'text' property"))
				return;

			throw new RuntimeException("Malformed JSON when sending message to " + sender + " with JSON: " + json, t);
		}
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
		player.sendTitlePart(TitlePart.TITLE, toComponentLegacy(Variables.replace(title, player)));
		player.sendTitlePart(TitlePart.SUBTITLE, toComponentLegacy(Variables.replace(subtitle, player)));
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
		player.sendPlayerListHeaderAndFooter(toComponentLegacy(Variables.replace(header, player)), toComponentLegacy(Variables.replace(footer, player)));
	}

	/**
	 * Displays message above player's health and hunger bar. (1.8+) Text will be
	 * colorized.
	 *
	 * @param player the player
	 * @param text   the text
	 */
	public static void sendActionBar(final Audience player, final String text) {
		player.sendActionBar(toComponentLegacy(Variables.replace(text, player)));
	}

	/**
	 * Shows a boss bar that is then hidden after the given period
	 *
	 * @param player
	 * @param message
	 * @param secondsToShow
	 */
	public static void sendBossbarTimed(Audience player, String message, int secondsToShow) {
		final BossBar bar = BossBar.bossBar(toComponentLegacy(Variables.replace(message, player)), 1F, Color.WHITE, Overlay.PROGRESS);
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
		final BossBar bar = BossBar.bossBar(toComponentLegacy(Variables.replace(message, player)), percent, color, style);
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
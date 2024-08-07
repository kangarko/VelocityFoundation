package org.mineacademy.vfo.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.GeoAPI;
import org.mineacademy.vfo.GeoAPI.GeoResponse;
import org.mineacademy.vfo.Messenger;
import org.mineacademy.vfo.TimeUtil;
import org.mineacademy.vfo.collection.StrictList;
import org.mineacademy.vfo.collection.StrictMap;
import org.mineacademy.vfo.collection.expiringmap.ExpiringMap;
import org.mineacademy.vfo.plugin.SimplePlugin;
import org.mineacademy.vfo.remain.CompChatColor;
import org.mineacademy.vfo.settings.SimpleLocalization;

import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.audience.Audience;

/**
 * A simple engine that replaces variables in a message.
 */
public final class Variables {

	/**
	 * The pattern to find singular [syntax_name] variables
	 */
	public static final Pattern MESSAGE_PLACEHOLDER_PATTERN = Pattern.compile("[\\[]([^\\[\\]]+)[\\]]");

	/**
	 * The pattern to find simple {} placeholders
	 */
	public static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("[({|%)]([^{}]+)[(}|%)]");

	/**
	 * The patter to find simple {} placeholders starting with {rel_ (used for PlaceholderAPI)
	 */
	public static final Pattern BRACKET_REL_PLACEHOLDER_PATTERN = Pattern.compile("[({|%)](rel_)([^}]+)[(}|%)]");

	/**
	 * Player - [Original Message - Translated Message]
	 */
	private static final Map<String, Map<String, String>> cache = ExpiringMap.builder().expiration(500, TimeUnit.MILLISECONDS).build();

	/**
	 * Should we replace javascript placeholders from variables/ folder automatically?
	 * Used internally to prevent race condition
	 */
	static boolean REPLACE_JAVASCRIPT = true;

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * You take in a command sender (may/may not be a player) and output a replaced string.
	 * The variable name (the key) is automatically surrounded by {} brackets
	 */
	private static final StrictMap<String, Function<Audience, String>> customVariables = new StrictMap<>();

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * This is used to dynamically replace the variable based on its content, like
	 * PlaceholderAPI.
	 *
	 * We also hook into PlaceholderAPI, however, you'll have to use your plugin's prefix before
	 * all variables when called from there.
	 */
	private static final StrictList<SimpleExpansion> customExpansions = new StrictList<>();

	/**
	 * Return the variable for the given key that is a function of replacing
	 * itself for the player. Returns null if no such variable by key is present.
	 * @param key
	 *
	 * @return
	 */
	public static Function<Audience, String> getVariable(String key) {
		return customVariables.get(key);
	}

	/**
	 * Register a new variable. The variable will be found inside {} block so if you give the variable
	 * name player_health it will be {player_health}. The function takes in a command sender (can be player)
	 * and outputs the variable value.
	 * <p>
	 * Please keep in mind we replace your variables AFTER PlaceholderAPI and Javascript variables
	 *
	 * @param variable
	 * @param replacer
	 */
	public static void addVariable(String variable, Function<Audience, String> replacer) {
		customVariables.override(variable, replacer);
	}

	/**
	 * Removes an existing variable, only put the name here without brackets, e.g. player_name not {player_name}
	 * This fails when the variables does not exist
	 *
	 * @param variable
	 */
	public static void removeVariable(String variable) {
		customVariables.remove(variable);
	}

	/**
	 * Checks if the given variable exist. Warning: only put the name here without brackets,
	 * e.g. player_name not {player_name}
	 *
	 * @param variable
	 * @return
	 */
	public static boolean hasVariable(String variable) {
		return customVariables.containsKey(variable);
	}

	/**
	 * Return an immutable list of all currently loaded expansions
	 *
	 * @return
	 */
	public static List<SimpleExpansion> getExpansions() {
		return Collections.unmodifiableList(customExpansions.getSource());
	}

	/**
	 * Registers a new expansion if it was not already registered
	 *
	 * @param expansion
	 */
	public static void addExpansion(SimpleExpansion expansion) {
		customExpansions.addIfNotExist(expansion);
	}

	/**
	 * Unregisters an expansion if it was registered already
	 *
	 * @param expansion
	 */
	public static void removeExpansion(SimpleExpansion expansion) {
		customExpansions.remove(expansion);
	}

	/**
	 * Return true if the expansion has already been registered
	 *
	 * @param expansion
	 * @return
	 */
	public static boolean hasExpansion(SimpleExpansion expansion) {
		return customExpansions.contains(expansion);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces variables in the messages using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param messages
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public static List<String> replace(Iterable<String> messages, Audience sender, Map<String, Object> replacements) {

		// Trick: Join the lines to only parse variables at once -- performance++ -- then split again
		final String deliminer = "%FLVJ%";

		return Arrays.asList(replace(String.join(deliminer, messages), sender, replacements).split(deliminer));
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static String replace(String message, Audience sender) {
		return replace(message, sender, null);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @param replacements
	 * @return
	 */
	public static String replace(String message, Audience sender, Map<String, Object> replacements) {
		return replace(message, sender, replacements, true);
	}

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * We also support PlaceholderAPI and MvdvPlaceholderAPI (only if sender is a Player).
	 *
	 * @param message
	 * @param sender
	 * @param replacements
	 * @param colorize
	 * @return
	 */
	public static String replace(String message, Audience sender, Map<String, Object> replacements, boolean colorize) {
		if (message == null || message.isEmpty())
			return "";

		final String original = message;
		final boolean senderIsPlayer = sender instanceof Player;
		final String playerName = senderIsPlayer ? ((Player) sender).getUsername() : null;

		// Replace custom variables first
		if (replacements != null && !replacements.isEmpty())
			message = Replacer.replaceArray(message, replacements);

		if (senderIsPlayer) {

			// Already cached ? Return.
			final Map<String, String> cached = cache.get(playerName);
			final String cachedVar = cached != null ? cached.get(message) : null;

			if (cachedVar != null)
				return cachedVar;
		}

		// Custom placeholders
		if (REPLACE_JAVASCRIPT) {
			REPLACE_JAVASCRIPT = false;

			try {
				message = replaceJavascriptVariables0(message, sender, replacements);

			} finally {
				REPLACE_JAVASCRIPT = true;
			}
		}

		// Default
		message = replaceHardVariables0(sender, message);

		// Support the & color system
		if (!message.startsWith("[JSON]"))
			message = Common.colorize(message);

		if (senderIsPlayer) {
			final Map<String, String> map = cache.get(playerName);

			if (map != null)
				map.put(original, message);
			else
				cache.put(playerName, Common.newHashMap(original, message));
		}

		return message;
	}

	/*
	 * Replaces JavaScript variables in the message
	 */
	private static String replaceJavascriptVariables0(String message, Audience sender, Map<String, Object> replacements) {

		final Matcher matcher = BRACKET_PLACEHOLDER_PATTERN.matcher(message);

		while (matcher.find()) {
			final String variableKey = matcher.group();

			// Find the variable key without []
			final Variable variable = Variable.findVariable(variableKey.substring(1, variableKey.length() - 1));

			if (variable != null && variable.getType() == Variable.Type.FORMAT) {
				final SimpleComponent component = variable.build(sender, SimpleComponent.empty(), replacements);

				// We do not support interact chat elements in format variables,
				// so we just flatten the variable. Use formatting or chat variables instead.
				String plain = component.getPlainMessage();

				// And we remove the white prefix that is by default added in every component
				if (plain.startsWith(CompChatColor.COLOR_CHAR + "f" + CompChatColor.COLOR_CHAR + "f"))
					plain = plain.substring(4);

				message = message.replace(variableKey, plain);
			}
		}

		return message;
	}

	/*
	 * Replaces our hardcoded variables in the message, using a cache for better performance
	 */
	private static String replaceHardVariables0(Audience sender, String message) {
		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(message);
		final Player player = sender instanceof Player ? (Player) sender : null;

		while (matcher.find()) {
			String variable = matcher.group(1);
			boolean frontSpace = false;
			boolean backSpace = false;

			if (variable.startsWith("+")) {
				variable = variable.substring(1);

				frontSpace = true;
			}

			if (variable.endsWith("+")) {
				variable = variable.substring(0, variable.length() - 1);

				backSpace = true;
			}

			String value = lookupVariable0(player, sender, variable);

			if (value != null) {
				final boolean emptyColorless = Common.stripColors(value).isEmpty();
				value = value.isEmpty() ? "" : (frontSpace && !emptyColorless ? " " : "") + Common.colorize(value) + (backSpace && !emptyColorless ? " " : "");

				message = message.replace(matcher.group(), value);
			}
		}

		message = Messenger.replacePrefixes(message);

		return message;
	}

	/*
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 */
	private static String lookupVariable0(Player player, Audience console, String variable) {
		GeoResponse geoResponse = null;

		if (player != null && Arrays.asList("country_code", "country_name", "region_name", "isp").contains(variable))
			geoResponse = GeoAPI.getCountry(player.getRemoteAddress());

		if (console != null) {

			// Replace custom expansions
			for (final SimpleExpansion expansion : customExpansions) {
				final String value = expansion.replacePlaceholders(console, variable);

				if (value != null)
					return value;
			}

			// Replace custom variables
			final Function<Audience, String> customReplacer = customVariables.get(variable);

			if (customReplacer != null)
				return customReplacer.apply(console);
		}

		switch (variable) {
			case "date":
				return TimeUtil.getFormattedDate();
			case "date_short":
				return TimeUtil.getFormattedDateShort();
			case "chat_line":
				return Common.chatLine();
			case "chat_line_smooth":
				return Common.chatLineSmooth();
			case "display_name":
			case "player":
			case "player_name": {
				if (console == null)
					return null;

				return player == null ? Common.resolveSenderName(console) : player.getUsername();
			}
			case "ip_address":
			case "pl_address":
				return player == null ? "" : formatIp0(player);
			case "country_code":
				return player == null ? "" : geoResponse.getCountryCode();
			case "country_name":
				return player == null ? "" : geoResponse.getCountryName();
			case "region_name":
				return player == null ? "" : geoResponse.getRegionName();
			case "isp":
				return player == null ? "" : geoResponse.getIsp();
			case "label":
				return SimplePlugin.getInstance().getMainCommand() != null ? SimplePlugin.getInstance().getMainCommand().getLabel() : SimpleLocalization.NONE;
			case "sender_is_player":
				return player != null ? "true" : "false";
			case "sender_is_console":
				return !(console instanceof Audience) ? "true" : "false";
		}

		return null;
	}

	/*
	 * Formats the IP address variable for the player
	 */
	private static String formatIp0(Player player) {
		try {
			return player.getRemoteAddress().toString().split("\\:")[0];

		} catch (final Throwable t) {
			return player.getRemoteAddress() != null ? player.getRemoteAddress().toString() : "";
		}
	}
}
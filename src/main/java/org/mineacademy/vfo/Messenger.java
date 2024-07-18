package org.mineacademy.vfo;

import java.util.HashMap;
import java.util.Map;

import org.mineacademy.vfo.remain.Remain;
import org.mineacademy.vfo.settings.SimpleSettings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

/**
 * Streamlines the process of sending themed messages to Audiences
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Messenger {

	/**
	 * Should we use messenger globally such as in commands & listeners?
	 */
	public static boolean ENABLED = true;

	/**
	 * Cached list of replacements
	 */
	private static final Map<String, String> replacements = new HashMap<>();

	/**
	 * The prefix send while sending info message
	 */
	@Getter
	private static String infoPrefix = "&8[&9i&8]&7";

	/**
	 * The prefix send while sending success message
	 */
	@Getter
	private static String successPrefix = "&8[&2\u2714&8]&7";

	/**
	 * The prefix send while sending warning message
	 */
	@Getter
	private static String warnPrefix = "&8[&6!&8]&6";

	/**
	 * The prefix send while sending error message
	 */
	@Getter
	private static String errorPrefix = "&8[&4\u2715&8]&c";

	/**
	 * The prefix send while sending questions
	 */
	@Getter
	private static String questionPrefix = "&8[&a?&8]&7";

	/**
	 * The prefix send while sending announcements
	 */
	@Getter
	private static String announcePrefix = "&8[&5!&8]&d";

	/**
	 * Set the prefix for info messages
	 *
	 * @param infoPrefix
	 */
	public static void setInfoPrefix(String infoPrefix) {
		Messenger.infoPrefix = infoPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for success messages
	 *
	 * @param successPrefix
	 */
	public static void setSuccessPrefix(String successPrefix) {
		Messenger.successPrefix = successPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for warning messages
	 *
	 * @param warnPrefix
	 */
	public static void setWarnPrefix(String warnPrefix) {
		Messenger.warnPrefix = warnPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for error messages
	 *
	 * @param errorPrefix
	 */
	public static void setErrorPrefix(String errorPrefix) {
		Messenger.errorPrefix = errorPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for question messages
	 *
	 * @param questionPrefix
	 */
	public static void setQuestionPrefix(String questionPrefix) {
		Messenger.questionPrefix = questionPrefix;

		updatePrefixes();
	}

	/**
	 * Set the prefix for announcement messages
	 *
	 * @param announcePrefix
	 */
	public static void setAnnouncePrefix(String announcePrefix) {
		Messenger.announcePrefix = announcePrefix;

		updatePrefixes();
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastInfo(final String message) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastInfo(final Component component) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, infoPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastSuccess(final String message) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastSuccess(final Component component) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, successPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastWarn(final String message) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, warnPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastWarn(final Component component) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, warnPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastError(final String message) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastError(final Component component) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, errorPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param message
	 */
	public static void broadcastQuestion(final String message) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param component
	 */
	public static void broadcastQuestion(final Component component) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, questionPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param message
	 */
	public static void broadcastAnnounce(final String message) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, announcePrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param component
	 */
	public static void broadcastAnnounce(final Component component) {
		for (final Audience online : Remain.getOnlinePlayers())
			tell(online, announcePrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param Audience
	 * @param message
	 */
	public static void info(final Audience Audience, final String message) {
		tell(Audience, infoPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getInfoPrefix()}
	 *
	 * @param Audience
	 * @param component
	 */
	public static void info(final Audience Audience, final Component component) {
		tell(Audience, infoPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param Audience
	 * @param message
	 */
	public static void success(final Audience Audience, final String message) {
		tell(Audience, successPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getSuccessPrefix()}
	 *
	 * @param Audience
	 * @param component
	 */
	public static void success(final Audience Audience, final Component component) {
		tell(Audience, successPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param Audience
	 * @param message
	 */
	public static void warn(final Audience Audience, final String message) {
		tell(Audience, warnPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getWarnPrefix()}
	 *
	 * @param Audience
	 * @param component
	 */
	public static void warn(final Audience Audience, final Component component) {
		tell(Audience, warnPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param Audience
	 * @param message
	 */
	public static void error(final Audience Audience, final String message) {
		tell(Audience, errorPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getErrorPrefix()}
	 *
	 * @param Audience
	 * @param component
	 */
	public static void error(final Audience Audience, final Component component) {
		tell(Audience, errorPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param Audience
	 * @param message
	 */
	public static void question(final Audience Audience, final String message) {
		tell(Audience, questionPrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getQuestionPrefix()}
	 *
	 * @param Audience
	 * @param component
	 */
	public static void question(final Audience Audience, final Component component) {
		tell(Audience, questionPrefix, component);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param Audience
	 * @param message
	 */
	public static void announce(final Audience Audience, final String message) {
		tell(Audience, announcePrefix, message);
	}

	/**
	 * Send a message prepended with the {@link #getAnnouncePrefix()}
	 *
	 * @param Audience
	 * @param component
	 */
	public static void announce(final Audience Audience, final Component component) {
		tell(Audience, announcePrefix, component);
	}

	/*
	 * Internal method to perform the sending
	 */
	private static void tell(final Audience sender, final String prefix, String message) {

		// Support localization being none or empty
		if (message == null || message.isEmpty() || "none".equals(message))
			return;

		final String oldPrefix = Common.getTellPrefix();

		try {
			Common.setTellPrefix(prefix);

			Common.tell(sender, Common.colorize(message));

		} finally {
			Common.setTellPrefix(oldPrefix);
		}
	}

	/*
	 * Internal method to perform the sending
	 */
	private static void tell(final Audience sender, final String prefix, Component component) {

		// Support localization being none or empty
		if (component == null)
			return;

		final String oldPrefix = Common.getTellPrefix();

		try {
			Common.setTellPrefix(prefix);

			Common.tell(sender, component);

		} finally {
			Common.setTellPrefix(oldPrefix);
		}
	}

	/**
	 * Replace {plugin_prefix} and {X_prefix} and {prefix_X} with respective messenger variables
	 * such as {warn_prefix} with {@link #getWarnPrefix()} etc.
	 *
	 * @param message
	 * @return
	 */
	public static String replacePrefixes(String message) {
		final StringBuilder result = new StringBuilder(message);

		replacements.forEach((key, value) -> {
			int start = result.indexOf(key);

			while (start != -1) {
				result.replace(start, start + key.length(), value);

				start = result.indexOf(key, start + value.length());
			}
		});

		return result.toString();
	}

	/*
	 * Refresh prefixes for maximum performance (replacing takes <0.002ms per call in production (!) testing)
	 */
	private static void updatePrefixes() {
		replacements.put("{plugin_prefix}", SimpleSettings.PLUGIN_PREFIX);
		replacements.put("{info_prefix}", infoPrefix);
		replacements.put("{prefix_info}", infoPrefix);
		replacements.put("{success_prefix}", successPrefix);
		replacements.put("{prefix_success}", successPrefix);
		replacements.put("{warn_prefix}", warnPrefix);
		replacements.put("{prefix_warn}", warnPrefix);
		replacements.put("{error_prefix}", errorPrefix);
		replacements.put("{prefix_error}", errorPrefix);
		replacements.put("{question_prefix}", questionPrefix);
		replacements.put("{prefix_question}", questionPrefix);
		replacements.put("{announce_prefix}", announcePrefix);
		replacements.put("{prefix_announce}", announcePrefix);
	}
}

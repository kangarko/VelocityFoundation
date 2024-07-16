package org.mineacademy.vfo.settings;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.collection.StrictList;
import org.mineacademy.vfo.debug.Debugger;
import org.mineacademy.vfo.debug.LagCatcher;
import org.mineacademy.vfo.plugin.SimplePlugin;

/**
 * A simple implementation of a typical main plugin settings
 * where each key can be accessed in a static way from anywhere.
 * <p>
 * Typically we use this class for settings.yml main plugin config.
 */
// Use for settings.yml
@SuppressWarnings("unused")
public class SimpleSettings extends YamlStaticConfig {

	/**
	 * A flag indicating that this class has been loaded
	 * <p>
	 * You can place this class to {@link org.mineacademy.fo.plugin.SimplePlugin#getSettings()} ()} to
	 * make it load automatically
	 */
	private static boolean settingsClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	@Override
	protected final void onLoad() throws Exception {
		this.loadConfiguration(this.getSettingsFileName());
	}

	/**
	 * Get the file name for these settings, by default settings.yml
	 *
	 * @return
	 */
	protected String getSettingsFileName() {
		return "settings.yml";
	}

	/**
	 * Always keep settings.yml file up to date
	 */
	@Override
	protected final boolean alwaysSaveOnLoad() {
		return true;
	}

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------

	/**
	 * The configuration version number, found in the "Version" key in the file.
	 *
	 * Defaults to 1 if not set in the file.
	 */
	public static Integer VERSION = 1;

	/**
	 * Set and update the config version automatically, however the {@link #VERSION} will
	 * contain the older version used in the file on the disk so you can use
	 * it for comparing in the init() methods
	 * <p>
	 * Please call this as a super method when overloading this!
	 */
	@Override
	protected void preLoad() {
		// Load version first so we can use it later
		setPathPrefix(null);

		if (isSetDefault("Version"))
			if ((VERSION = getInteger("Version")) != this.getConfigVersion())
				set("Version", this.getConfigVersion());
	}

	/**
	 * Return the very latest config version
	 * <p>
	 * Any changes here must also be made to the "Version" key in your settings file.
	 *
	 * @return
	 */
	protected int getConfigVersion() {
		return 1;
	}

	// --------------------------------------------------------------------
	// Settings we offer by default for your main config file
	// Specify those you need to modify
	// --------------------------------------------------------------------

	/**
	 * The {timestamp} and {date}, {date_short} and {date_month} formats.
	 */
	public static DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static DateFormat DATE_FORMAT_SHORT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	public static DateFormat DATE_FORMAT_MONTH = new SimpleDateFormat("dd.MM HH:mm");

	/**
	 * The {location} format.
	 */
	public static String LOCATION_FORMAT = "{world} [{x}, {y}, {z}]";

	/**
	 * What debug sections should we enable in {@link Debugger} ? When you call {@link Debugger#debug(String, String...)}
	 * those that are specified in this settings are logged into the console, otherwise no message is shown.
	 * <p>
	 * Typically this is left empty: Debug: []
	 */
	public static StrictList<String> DEBUG_SECTIONS = new StrictList<>();

	/**
	 * The plugin prefix in front of chat/console messages.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Prefix: "&8[&3ChatControl&8]&7 "
	 */
	public static String PLUGIN_PREFIX = "&7" + SimplePlugin.getNamed() + " //";

	/**
	 * The lag threshold used for {@link LagCatcher} in milliseconds. Set to -1 to disable.
	 * <p>
	 * Typically for ChatControl:
	 * <p>
	 * Log_Lag_Over_Milis: 100
	 */
	public static Integer LAG_THRESHOLD_MILLIS = 100;

	/**
	 * When processing regular expressions, limit executing to the specified time.
	 * This prevents server freeze/crash on malformed regex (loops).
	 * <p>
	 * Regex_Timeout_Milis: 100
	 */
	public static Integer REGEX_TIMEOUT = 100;

	/**
	 * What commands should trigger the your main plugin command (separated by a comma ,)? See {@link SimplePlugin#getMainCommand()}
	 * <p>
	 * Typical values for ChatControl:
	 * <p>
	 * Command_Aliases: [chatcontrol, chc, cc]
	 * <p>
	 * // ONLY MANDATORY IF YOU OVERRIDE {@link SimplePlugin#getMainCommand()} //
	 */
	public static StrictList<String> MAIN_COMMAND_ALIASES = new StrictList<>();

	/**
	 * The localization prefix, given you are using {@link SimpleLocalization} class to load and manage your
	 * locale file. Typically the file path is: localization/messages_PREFIX.yml with this prefix below.
	 * <p>
	 * Typically: Locale: en
	 * <p>
	 * // ONLY MANDATORY IF YOU USE SIMPLELOCALIZATION //
	 */
	public static String LOCALE_PREFIX = "en";

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		Valid.checkBoolean(!settingsClassCalled, "Settings class already loaded!");

		setPathPrefix(null);

		if (isSetDefault("Date_Format"))
			try {
				DATE_FORMAT = new SimpleDateFormat(getString("Date_Format"));

			} catch (final IllegalArgumentException ex) {
				Common.throwError(ex, "Wrong 'Date_Format '" + getString("Date_Format") + "', see https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for examples'");
			}

		if (isSetDefault("Date_Format_Short"))
			try {
				DATE_FORMAT_SHORT = new SimpleDateFormat(getString("Date_Format_Short"));

			} catch (final IllegalArgumentException ex) {
				Common.throwError(ex, "Wrong 'Date_Format_Short '" + getString("Date_Format_Short") + "', see https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for examples'");
			}

		if (isSetDefault("Date_Format_Month"))
			try {
				DATE_FORMAT_MONTH = new SimpleDateFormat(getString("Date_Format_Month"));

			} catch (final IllegalArgumentException ex) {
				Common.throwError(ex, "Wrong 'Date_Format_Month '" + getString("Date_Format_Month") + "', see https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for examples'");
			}

		if (isSetDefault("Location_Format"))
			LOCATION_FORMAT = getString("Location_Format");

		if (isSetDefault("Prefix"))
			PLUGIN_PREFIX = getString("Prefix");

		if (isSetDefault("Log_Lag_Over_Milis")) {
			LAG_THRESHOLD_MILLIS = getInteger("Log_Lag_Over_Milis");
			Valid.checkBoolean(LAG_THRESHOLD_MILLIS == -1 || LAG_THRESHOLD_MILLIS >= 0, "Log_Lag_Over_Milis must be either -1 to disable, 0 to log all or greater!");

			if (LAG_THRESHOLD_MILLIS == 0)
				Common.log("&eLog_Lag_Over_Milis is 0, all performance is logged. Set to -1 to disable.");
		}

		if (isSetDefault("Debug"))
			DEBUG_SECTIONS = new StrictList<>(getStringList("Debug"));

		if (isSetDefault("Regex_Timeout_Milis"))
			REGEX_TIMEOUT = getInteger("Regex_Timeout_Milis");

		// -------------------------------------------------------------------
		// Load maybe-mandatory values
		// -------------------------------------------------------------------

		{ // Load localization
			final boolean keySet = isSetDefault("Locale");

			LOCALE_PREFIX = keySet ? getString("Locale") : LOCALE_PREFIX;
		}

		{ // Load main command alias
			final boolean keySet = isSetDefault("Command_Aliases");

			MAIN_COMMAND_ALIASES = keySet ? getCommandList("Command_Aliases") : MAIN_COMMAND_ALIASES;
		}

		settingsClassCalled = true;
	}

	/**
	 * Was this class loaded?
	 *
	 * @return
	 */
	public static final Boolean isSettingsCalled() {
		return settingsClassCalled;
	}

	/**
	 * Reset the flag indicating that the class has been loaded,
	 * used in reloading.
	 */
	public static final void resetSettingsCall() {
		settingsClassCalled = false;
	}
}

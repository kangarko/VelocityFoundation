package org.mineacademy.vfo.settings;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.FileUtil;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.command.DebugCommand;
import org.mineacademy.vfo.command.PermsCommand;
import org.mineacademy.vfo.command.ReloadCommand;
import org.mineacademy.vfo.model.ChatPaginator;
import org.mineacademy.vfo.plugin.SimplePlugin;
import org.mineacademy.vfo.remain.CompChatColor;
import org.mineacademy.vfo.settings.FileConfig.AccusativeHelper;

/**
 * A simple implementation of a basic localization file.
 * We create the localization/messages_LOCALEPREFIX.yml file
 * automatically and fill it with values from your localization/messages_LOCALEPREFIX.yml
 * file placed within in your plugin's jar file.
 */
@SuppressWarnings("unused")
public class SimpleLocalization extends YamlStaticConfig {

	/**
	 * A flag indicating that this class has been loaded
	 * <p>
	 * You can place this class to {@link SimplePlugin#getSettings()} to make
	 * it load automatically
	 */
	private static boolean localizationClassCalled;

	// --------------------------------------------------------------------
	// Loading
	// --------------------------------------------------------------------

	/**
	 * Create and load the localization/messages_LOCALEPREFIX.yml file.
	 * <p>
	 * See {@link SimpleSettings#LOCALE_PREFIX} for the locale prefix.
	 * <p>
	 * The locale file is extracted from your plugins jar to the localization/ folder
	 * if it does not exists, or updated if it is out of date.
	 */
	@Override
	protected final void onLoad() throws Exception {
		final String localePath = "localization/messages_" + SimpleSettings.LOCALE_PREFIX + ".yml";
		final Object content = FileUtil.getInternalFileContent(localePath);

		Valid.checkNotNull(content, SimplePlugin.getNamed() + " does not support the localization: messages_" + SimpleSettings.LOCALE_PREFIX
				+ ".yml (For custom locale, set the Locale to 'en' and edit your English file instead)");

		this.loadConfiguration(localePath);
	}

	// --------------------------------------------------------------------
	// Version
	// --------------------------------------------------------------------

	/**
	 * The configuration version number, found in the "Version" key in the file.,
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
	protected final void preLoad() {
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

	/**
	 * Always keep the lang file up to date.
	 */
	@Override
	protected final boolean alwaysSaveOnLoad() {
		return true;
	}

	// --------------------------------------------------------------------
	// Shared values
	// --------------------------------------------------------------------

	// NB: Those keys are optional - you do not have to write them into your messages_X.yml files
	// but if you do, we will use your values instead of the default ones!

	/**
	 * Locale keys related to your plugin commands
	 */
	public static final class Commands {

		/**
		 * The message at "No_Console" key shown when console is denied executing a command.
		 */
		public static String NO_CONSOLE = "&cYou may only use this command as a player";

		/**
		 * The message shown when console runs a command without specifying target player name
		 */
		public static String CONSOLE_MISSING_PLAYER_NAME = "When running from console, specify player name.";

		/**
		 * The message shown when there is a fatal error running this command
		 */
		public static String COOLDOWN_WAIT = "&cWait {duration} second(s) before using this command again.";

		/**
		 * Keys below indicate an invalid action or input
		 */
		public static String INVALID_ARGUMENT = "&cInvalid argument. Run &6/{label} ? &cfor help.";
		public static String INVALID_SUB_ARGUMENT = "&cInvalid argument. Run '/{label} {0}' for help.";
		public static String INVALID_ARGUMENT_MULTILINE = "&cInvalid argument. Usage:";
		public static String INVALID_TIME = "Expected time such as '3 hours' or '15 minutes'. Got: '{input}'";
		public static String INVALID_NUMBER = "The number must be a whole or a decimal number. Got: '{input}'";
		public static String INVALID_STRING = "Invalid string. Got: '{input}'";
		public static String INVALID_WORLD = "Invalid world '{world}'. Available: {available}";

		/**
		 * The authors label
		 */
		public static String LABEL_AUTHORS = "Made by";

		/**
		 * The description label
		 */
		public static String LABEL_DESCRIPTION = "&c&lDescription:";

		/**
		 * The optional arguments label
		 */
		public static String LABEL_OPTIONAL_ARGS = "optional arguments";

		/**
		 * The required arguments label
		 */
		public static String LABEL_REQUIRED_ARGS = "required arguments";

		/**
		 * The usage label
		 */
		public static String LABEL_USAGE = "&c&lUsage:";

		/**
		 * The help for label
		 */
		public static String LABEL_HELP_FOR = "Help for /{label}";

		/**
		 * The label shown when building subcommands
		 */
		public static String LABEL_SUBCOMMAND_DESCRIPTION = " &f/{label} {sublabel} {usage+}{dash+}{description}";

		/**
		 * The keys below are shown as hover tooltip on /command help menu.
		 */
		public static String HELP_TOOLTIP_DESCRIPTION = "&7Description: &f{description}";
		public static String HELP_TOOLTIP_PERMISSION = "&7Permission: &f{permission}";
		public static String HELP_TOOLTIP_USAGE = "&7Usage: &f";

		/**
		 * The keys below are used in the {@link ReloadCommand}
		 */
		public static String RELOAD_DESCRIPTION = "Reload the configuration.";
		public static String RELOAD_STARTED = "Reloading plugin's data, please wait..";
		public static String RELOAD_SUCCESS = "&6{plugin_name} {plugin_version} has been reloaded.";
		public static String RELOAD_FILE_LOAD_ERROR = "&4Oups, &cthere was a problem loading files from your disk! See the console for more information. {plugin_name} has not been reloaded.";
		public static String RELOAD_FAIL = "&4Oups, &creloading failed! See the console for more information. Error: {error}";

		/**
		 * The message shown when there is a fatal error running this command
		 */
		public static String ERROR = "&4&lOups! &cThe command failed :( Check the console and report the error.";

		/**
		 * The message shown when player has no permissions to view ANY subcommands in group command.
		 */
		public static String HEADER_NO_SUBCOMMANDS = "&cThere are no arguments for this command.";

		/**
		 * The message shown when player has no permissions to view ANY subcommands in group command.
		 */
		public static String HEADER_NO_SUBCOMMANDS_PERMISSION = "&cYou don't have permissions to view any subcommands.";

		/**
		 * The primary color shown in the ----- COMMAND ----- header
		 */
		public static CompChatColor HEADER_COLOR = CompChatColor.GOLD;

		/**
		 * The secondary color shown in the ----- COMMAND ----- header such as in /chc ?
		 */
		public static CompChatColor HEADER_SECONDARY_COLOR = CompChatColor.RED;

		/**
		 * The format of the header
		 */
		public static String HEADER_FORMAT = "&r\n{theme_color}&m<center>&r{theme_color} {title} &m\n&r";

		/**
		 * The center character of the format in case \<center\> is used
		 */
		public static String HEADER_CENTER_LETTER = "-";

		/**
		 * The padding of the header in case \<center\> is used
		 */
		public static Integer HEADER_CENTER_PADDING = 130;

		/**
		 * Key for when plugin is reloading {@link SimplePlugin}
		 */
		public static String RELOADING = "reloading";

		/**
		 * Key for when plugin is disabled {@link SimplePlugin}
		 */
		public static String DISABLED = "disabled";

		/**
		 * The message shown when plugin is reloading or was disabled and player attempts to run command
		 */
		public static String CANNOT_USE_WHILE_NULL = "&cCannot use this command while the plugin is {state}.";

		/**
		 * The message shown in SimpleCommand.findWorld()
		 */
		public static String CANNOT_AUTODETECT_WORLD = "Only living players can use ~ for their world!";

		/**
		 * The keys below are used in the {@link DebugCommand}
		 */
		public static String DEBUG_DESCRIPTION = "ZIP your settings for reporting bugs.";
		public static String DEBUG_PREPARING = "&6Preparing debug log...";
		public static String DEBUG_SUCCESS = "&2Successfuly copied {amount} file(s) to debug.zip. Your sensitive MySQL information has been removed from yml files. Please upload it via ufile.io and send it to us for review.";
		public static String DEBUG_COPY_FAIL = "&cCopying files failed on file {file} and it was stopped. See console for more information.";
		public static String DEBUG_ZIP_FAIL = "&cCreating a ZIP of your files failed, see console for more information. Please ZIP debug/ folder and send it to us via ufile.io manually.";

		/**
		 * The keys below are used in the {@link PermsCommand}
		 */
		public static String PERMS_DESCRIPTION = "List all permissions the plugin has.";
		public static String PERMS_USAGE = "[phrase]";
		public static String PERMS_HEADER = "Listing All {plugin_name} Permissions";
		public static String PERMS_MAIN = "Main";
		public static String PERMS_PERMISSIONS = "Permissions:";
		public static String PERMS_TRUE_BY_DEFAULT = "&7[true by default]";
		public static String PERMS_INFO = "&7Info: &f";
		public static String PERMS_DEFAULT = "&7Default? ";
		public static String PERMS_APPLIED = "&7Do you have it? ";
		public static String PERMS_YES = "&2yes";
		public static String PERMS_NO = "&cno";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Commands");

			if (isSetDefault("No_Console"))
				NO_CONSOLE = getString("No_Console");

			if (isSetDefault("Console_Missing_Player_Name"))
				CONSOLE_MISSING_PLAYER_NAME = getString("Console_Missing_Player_Name");

			if (isSetDefault("Cooldown_Wait"))
				COOLDOWN_WAIT = getString("Cooldown_Wait");

			if (isSetDefault("Invalid_Argument"))
				INVALID_ARGUMENT = getString("Invalid_Argument");

			if (isSetDefault("Invalid_Sub_Argument"))
				INVALID_SUB_ARGUMENT = getString("Invalid_Sub_Argument");

			if (isSetDefault("Invalid_Argument_Multiline"))
				INVALID_ARGUMENT_MULTILINE = getString("Invalid_Argument_Multiline");

			if (isSetDefault("Invalid_Time"))
				INVALID_TIME = getString("Invalid_Time");

			if (isSetDefault("Invalid_Number"))
				INVALID_NUMBER = getString("Invalid_Number");

			if (isSetDefault("Invalid_String"))
				INVALID_STRING = getString("Invalid_String");

			if (isSetDefault("Invalid_World"))
				INVALID_WORLD = getString("Invalid_World");

			if (isSetDefault("Label_Authors"))
				LABEL_AUTHORS = getString("Label_Authors");

			if (isSetDefault("Label_Description"))
				LABEL_DESCRIPTION = getString("Label_Description");

			if (isSetDefault("Label_Optional_Args"))
				LABEL_OPTIONAL_ARGS = getString("Label_Optional_Args");

			if (isSetDefault("Label_Required_Args"))
				LABEL_REQUIRED_ARGS = getString("Label_Required_Args");

			if (isSetDefault("Label_Usage"))
				LABEL_USAGE = getString("Label_Usage");

			if (isSetDefault("Label_Help_For"))
				LABEL_HELP_FOR = getString("Label_Help_For");

			if (isSetDefault("Label_Subcommand_Description"))
				LABEL_SUBCOMMAND_DESCRIPTION = getString("Label_Subcommand_Description");

			if (isSetDefault("Help_Tooltip_Description"))
				HELP_TOOLTIP_DESCRIPTION = getString("Help_Tooltip_Description");

			if (isSetDefault("Help_Tooltip_Permission"))
				HELP_TOOLTIP_PERMISSION = getString("Help_Tooltip_Permission");

			if (isSetDefault("Help_Tooltip_Usage"))
				HELP_TOOLTIP_USAGE = getString("Help_Tooltip_Usage");

			if (isSetDefault("Reload_Description"))
				RELOAD_DESCRIPTION = getString("Reload_Description");

			if (isSetDefault("Reload_Started"))
				RELOAD_STARTED = getString("Reload_Started");

			if (isSetDefault("Reload_Success"))
				RELOAD_SUCCESS = getString("Reload_Success");

			if (isSetDefault("Reload_File_Load_Error"))
				RELOAD_FILE_LOAD_ERROR = getString("Reload_File_Load_Error");

			if (isSetDefault("Reload_Fail"))
				RELOAD_FAIL = getString("Reload_Fail");

			if (isSetDefault("Error"))
				ERROR = getString("Error");

			if (isSetDefault("Header_No_Subcommands"))
				HEADER_NO_SUBCOMMANDS = getString("Header_No_Subcommands");

			if (isSetDefault("Header_No_Subcommands_Permission"))
				HEADER_NO_SUBCOMMANDS_PERMISSION = getString("Header_No_Subcommands_Permission");

			if (isSetDefault("Header_Color"))
				HEADER_COLOR = get("Header_Color", CompChatColor.class);

			if (isSetDefault("Header_Secondary_Color"))
				HEADER_SECONDARY_COLOR = get("Header_Secondary_Color", CompChatColor.class);

			if (isSetDefault("Header_Format"))
				HEADER_FORMAT = getString("Header_Format");

			if (isSetDefault("Header_Center_Letter")) {
				HEADER_CENTER_LETTER = getString("Header_Center_Letter");

				Valid.checkBoolean(HEADER_CENTER_LETTER.length() == 1, "Header_Center_Letter must only have 1 letter, not " + HEADER_CENTER_LETTER.length() + ":" + HEADER_CENTER_LETTER);
			}

			if (isSetDefault("Header_Center_Padding"))
				HEADER_CENTER_PADDING = getInteger("Header_Center_Padding");

			if (isSet("Reloading"))
				RELOADING = getString("Reloading");

			if (isSet("Disabled"))
				DISABLED = getString("Disabled");

			if (isSet("Use_While_Null"))
				CANNOT_USE_WHILE_NULL = getString("Use_While_Null");

			if (isSet("Cannot_Autodetect_World"))
				CANNOT_AUTODETECT_WORLD = getString("Cannot_Autodetect_World");

			if (isSetDefault("Debug_Description"))
				DEBUG_DESCRIPTION = getString("Debug_Description");

			if (isSetDefault("Debug_Preparing"))
				DEBUG_PREPARING = getString("Debug_Preparing");

			if (isSetDefault("Debug_Success"))
				DEBUG_SUCCESS = getString("Debug_Success");

			if (isSetDefault("Debug_Copy_Fail"))
				DEBUG_COPY_FAIL = getString("Debug_Copy_Fail");

			if (isSetDefault("Debug_Zip_Fail"))
				DEBUG_ZIP_FAIL = getString("Debug_Zip_Fail");

			if (isSetDefault("Perms_Description"))
				PERMS_DESCRIPTION = getString("Perms_Description");

			if (isSetDefault("Perms_Usage"))
				PERMS_USAGE = getString("Perms_Usage");

			if (isSetDefault("Perms_Header"))
				PERMS_HEADER = getString("Perms_Header");

			if (isSetDefault("Perms_Main"))
				PERMS_MAIN = getString("Perms_Main");

			if (isSetDefault("Perms_Permissions"))
				PERMS_PERMISSIONS = getString("Perms_Permissions");

			if (isSetDefault("Perms_True_By_Default"))
				PERMS_TRUE_BY_DEFAULT = getString("Perms_True_By_Default");

			if (isSetDefault("Perms_Info"))
				PERMS_INFO = getString("Perms_Info");

			if (isSetDefault("Perms_Default"))
				PERMS_DEFAULT = getString("Perms_Default");

			if (isSetDefault("Perms_Applied"))
				PERMS_APPLIED = getString("Perms_Applied");

			if (isSetDefault("Perms_Yes"))
				PERMS_YES = getString("Perms_Yes");

			if (isSetDefault("Perms_No"))
				PERMS_NO = getString("Perms_No");
		}
	}

	/**
	 * Key related to players
	 */
	public static final class Player {

		/**
		 * Message shown when the player is not online on this server
		 */
		public static String NOT_ONLINE = "&cPlayer {player} &cis not online on this server.";

		/**
		 * Message shown the an offline player is returned null from a given UUID.
		 */
		public static String INVALID_UUID = "&cCould not find a player from UUID {uuid}.";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Player");

			if (isSetDefault("Not_Online"))
				NOT_ONLINE = getString("Not_Online");

			if (isSetDefault("Invalid_UUID"))
				INVALID_UUID = getString("Invalid_UUID");
		}
	}

	/**
	 * Keys related to {@link ChatPaginator}
	 */
	public static final class Pages {

		public static String NO_PAGE_NUMBER = "&cPlease specify the page number for this command.";
		public static String NO_PAGES = "There are no results to list.";
		public static String NO_PAGE = "Pages do not contain the given page number.";
		public static String INVALID_PAGE = "&cYour input '{input}' is not a valid number.";
		public static String GO_TO_PAGE = "&7Go to page {page}";
		public static String GO_TO_FIRST_PAGE = "&7Go to the first page";
		public static String GO_TO_LAST_PAGE = "&7Go to the last page";
		public static String[] TOOLTIP = {
				"&7You can also navigate using the",
				"&7hidden /#flp <page> command."
		};

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Pages");

			if (isSetDefault("No_Page_Number"))
				NO_PAGE_NUMBER = getString("No_Page_Number");

			if (isSetDefault("No_Pages"))
				NO_PAGES = getString("No_Pages");

			if (isSetDefault("No_Page"))
				NO_PAGE = getString("No_Page");

			if (isSetDefault("Invalid_Page"))
				INVALID_PAGE = getString("Invalid_Page");

			if (isSetDefault("Go_To_Page"))
				GO_TO_PAGE = getString("Go_To_Page");

			if (isSetDefault("Go_To_First_Page"))
				GO_TO_FIRST_PAGE = getString("Go_To_First_Page");

			if (isSetDefault("Go_To_Last_Page"))
				GO_TO_LAST_PAGE = getString("Go_To_Last_Page");

			if (isSetDefault("Tooltip"))
				TOOLTIP = Common.toArray(getStringList("Tooltip"));
		}
	}

	/**
	 * Keys related to the GUI system
	 */
	public static final class Menu {

		/**
		 * Message shown when the player is not online on this server
		 */
		public static String ITEM_DELETED = "&2The {item} has been deleted.";

		/**
		 * Message shown when the player tries to open menu, but has an ongoing conversation.
		 */
		public static String CANNOT_OPEN_DURING_CONVERSATION = "&cType 'exit' to quit your conversation before opening menu.";

		/**
		 * Message shown on error
		 */
		public static String ERROR = "&cOups! There was a problem with this menu! Please contact the administrator to review the console for details.";

		/**
		 * Keys related to menu pagination
		 */
		public static String PAGE_PREVIOUS = "&8<< &fPage {page}";
		public static String PAGE_NEXT = "Page {page} &8>>";
		public static String PAGE_FIRST = "&7First Page";
		public static String PAGE_LAST = "&7Last Page";

		/**
		 * Keys related to menu titles and tooltips
		 */
		public static String TITLE_TOOLS = "Tools Menu";
		public static String TOOLTIP_INFO = "&fMenu Information";
		public static String BUTTON_RETURN_TITLE = "&4&lReturn";
		public static String[] BUTTON_RETURN_LORE = { "", "Return back." };

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Menu");

			if (isSetDefault("Item_Deleted"))
				ITEM_DELETED = getString("Item_Deleted");

			if (isSetDefault("Cannot_Open_During_Conversation"))
				CANNOT_OPEN_DURING_CONVERSATION = getString("Cannot_Open_During_Conversation");

			if (isSetDefault("Error"))
				ERROR = getString("Error");

			if (isSetDefault("Page_Previous"))
				PAGE_PREVIOUS = getString("Page_Previous");

			if (isSetDefault("Page_Next"))
				PAGE_NEXT = getString("Page_Next");

			if (isSetDefault("Page_First"))
				PAGE_FIRST = getString("Page_First");

			if (isSetDefault("Page_Last"))
				PAGE_LAST = getString("Page_Last");

			if (isSetDefault("Title_Tools"))
				TITLE_TOOLS = getString("Title_Tools");

			if (isSetDefault("Tooltip_Info"))
				TOOLTIP_INFO = getString("Tooltip_Info");

			if (isSetDefault("Button_Return_Title"))
				BUTTON_RETURN_TITLE = getString("Button_Return_Title");

			if (isSetDefault("Button_Return_Lore"))
				BUTTON_RETURN_LORE = Common.toArray(getStringList("Button_Return_Lore"));
		}
	}

	/**
	 * Keys related to tools
	 */
	public static final class Tool {

		/**
		 * The message shown when a tool errors out.
		 */
		public static String ERROR = "&cOups! There was a problem with this tool! Please contact the administrator to review the console for details.";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix("Tool");

			if (isSetDefault("Error"))
				ERROR = getString("Error");
		}
	}

	/**
	 * Keys related to cases
	 */
	public static class Cases {

		public static AccusativeHelper SECOND = AccusativeHelper.of("second", "seconds");
		public static AccusativeHelper MINUTE = AccusativeHelper.of("minute", "minutes");
		public static AccusativeHelper HOUR = AccusativeHelper.of("hour", "hours");
		public static AccusativeHelper DAY = AccusativeHelper.of("day", "days");
		public static AccusativeHelper WEEK = AccusativeHelper.of("week", "weeks");
		public static AccusativeHelper MONTH = AccusativeHelper.of("month", "months");
		public static AccusativeHelper YEAR = AccusativeHelper.of("year", "years");

		private static void init() {
			setPathPrefix("Cases");

			if (isSetDefault("Second"))
				SECOND = getCasus("Second");

			if (isSetDefault("Minute"))
				MINUTE = getCasus("Minute");

			if (isSetDefault("Hour"))
				HOUR = getCasus("Hour");

			if (isSetDefault("Day"))
				DAY = getCasus("Day");

			if (isSetDefault("Week"))
				WEEK = getCasus("Week");

			if (isSetDefault("Month"))
				MONTH = getCasus("Month");

			if (isSetDefault("Year"))
				YEAR = getCasus("Year");
		}
	}

	/**
	 * Keys related to updating the plugin
	 */
	public static final class Update {

		/**
		 * The message if a new version is found but not downloaded
		 */
		public static String AVAILABLE = "&2A new version of &3{plugin_name}&2 is available.\n"
				+ "&2Current version: &f{current}&2; New version: &f{new}\n"
				+ "&2URL: &7https://spigotmc.org/resources/{resource_id}/.";

		/**
		 * The message if a new version is found and downloaded
		 */
		public static String DOWNLOADED = "&3{plugin_name}&2 has been upgraded from {current} to {new}.\n"
				+ "&2Visit &7https://spigotmc.org/resources/{resource_id} &2for more information.\n"
				+ "&2Please restart the server to load the new version.";

		/**
		 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
		 */
		private static void init() {
			setPathPrefix(null);

			// Upgrade from old path
			if (isSet("Update_Available"))
				move("Update_Available", "Update.Available");

			setPathPrefix("Update");

			if (isSetDefault("Available"))
				AVAILABLE = getString("Available");

			if (isSetDefault("Downloaded"))
				DOWNLOADED = getString("Downloaded");
		}
	}

	/**
	 * Denotes the "none" message
	 */
	public static String NONE = "None";

	/**
	 * The message for player if they lack a permission.
	 */
	public static String NO_PERMISSION = "&cInsufficient permission ({permission}).";

	/**
	 * The console localized name. Example: Console
	 */
	public static String CONSOLE_NAME = "Console";

	/**
	 * The message when a section is missing from data file (the one ending in .db) (typically we use
	 * this file to store serialized values such as arenas from minigame plugins).
	 */
	public static String DATA_MISSING = "&c{name} lacks database information! Please only create {type} in-game! Skipping..";

	/**
	 * Load the values -- this method is called automatically by reflection in the {@link YamlStaticConfig} class!
	 */
	private static void init() {
		setPathPrefix(null);
		Valid.checkBoolean(!localizationClassCalled, "Localization class already loaded!");

		if (isSetDefault("No_Permission"))
			NO_PERMISSION = getString("No_Permission");

		if (isSetDefault("Console_Name"))
			CONSOLE_NAME = getString("Console_Name");

		if (isSetDefault("Data_Missing"))
			DATA_MISSING = getString("Data_Missing");

		if (isSetDefault("None"))
			NONE = getString("None");

		localizationClassCalled = true;
	}

	/**
	 * Was this class loaded?
	 *
	 * @return
	 */
	public static final Boolean isLocalizationCalled() {
		return localizationClassCalled;
	}

	/**
	 * Reset the flag indicating that the class has been loaded,
	 * used in reloading.
	 */
	public static final void resetLocalizationCall() {
		localizationClassCalled = false;
	}
}

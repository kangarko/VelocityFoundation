package org.mineacademy.vfo.constants;

import java.util.UUID;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.TimeUtil;
import org.mineacademy.vfo.plugin.SimplePlugin;

/**
 * Stores constants for this plugin
 */
public final class FoConstants {

	/**
	 * Represents a UUID consisting of 0's only
	 */
	public static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	public static final class File {

		/**
		 * The name of our settings file
		 */
		public static final String SETTINGS = "settings.yml";

		/**
		 * The error.log file created automatically to log errors to
		 */
		public static final String ERRORS = "error.log";

		/**
		 * The debug.log file to log debug messages to
		 */
		public static final String DEBUG = "debug.log";

		/**
		 * The data.db file (uses YAML) for saving various data
		 */
		public static final String DATA = "data.db";

		/**
		 * Files related to the ChatControl plugin
		 */
		public static final class ChatControl {

			/**
			 * The command-spy.log file in logs/ folder
			 */
			public static final String COMMAND_SPY = "logs/command-spy.log";

			/**
			 * The chat log file in logs/ folder
			 */
			public static final String CHAT_LOG = "logs/chat.log";

			/**
			 * The admin log in log/s folder
			 */
			public static final String ADMIN_CHAT = "logs/admin-chat.log";

			/**
			 * The bungee chat log file in logs/ folder
			 */
			public static final String BUNGEE_CHAT = "logs/bungee-chat.log";

			/**
			 * The rules log file in logs/ folder
			 */
			public static final String RULES_LOG = "logs/rules.log";

			/**
			 * The console log file in logs/ folder
			 */
			public static final String CONSOLE_LOG = "logs/console.log";

			/**
			 * The file logging channels joins and leaves in logs/ folder
			 */
			public static final String CHANNEL_JOINS = "logs/channel-joins.log";
		}
	}

	public static final class Header {

		/**
		 * The header for data.db file
		 *
		 * Use YamlConfig/setHeader() to override this.
		 */
		public static final String[] DATA_FILE = {
				"",
				"This file stores various data you create via the plugin.",
				"",
				" ** THE FILE IS MACHINE GENERATED. PLEASE DO NOT EDIT **",
				""
		};

		/**
		 * The header that is put into the file that has been automatically
		 * updated and comments were lost.
		 *
		 * Use Use YamlConfig/setHeader() to override this.
		 */
		public static final String[] UPDATED_FILE = {
				Common.configLine(),
				"",
				" Your file has been automatically updated at " + TimeUtil.getFormattedDate(),
				" to " + SimplePlugin.getNamed() + " " + SimplePlugin.getVersion(),
				"",
				" Unfortunatelly, due to how Bukkit saves all .yml files, it was not possible",
				" preserve the documentation comments in your file. We apologize.",
				"",
				" If you'd like to view the default file, you can either:",
				" a) Open the " + SimplePlugin.getSource().getName() + " with a WinRar or similar",
				" b) or, visit: https://github.com/kangarko/" + SimplePlugin.getNamed() + "/wiki",
				"",
				Common.configLine(),
				""
		};
	}
}

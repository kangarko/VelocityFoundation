package org.mineacademy.vfo.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.vfo.plugin.SimplePlugin;
import org.mineacademy.vfo.settings.SimpleLocalization;
import org.mineacademy.vfo.settings.SimpleLocalization.Commands;
import org.mineacademy.vfo.settings.YamlConfig;

/**
 * A simple predefined sub-command for quickly reloading the plugin
 * using /{label} reload|rl
 */
public final class ReloadCommand extends SimpleCommand {

	/**
	 * Create a new reload sub-command with the given permission.
	 *
	 * @param label
	 * @param permission
	 */
	public ReloadCommand(String label, String permission) {
		super(label, permission);

		this.setDescription(Commands.RELOAD_DESCRIPTION);
	}

	/**
	 * Create a new reload sub-command
	 *
	 * @param label
	 */
	public ReloadCommand(String label) {
		super(label);

		this.setDescription(Commands.RELOAD_DESCRIPTION);
	}

	@Override
	protected void onCommand() {
		try {
			this.tell(Commands.RELOAD_STARTED);

			// Syntax check YML files before loading
			boolean syntaxParsed = true;

			final List<File> yamlFiles = new ArrayList<>();

			this.collectYamlFiles(SimplePlugin.getData(), yamlFiles);

			for (final File file : yamlFiles)
				try {
					YamlConfig.fromFile(file);

				} catch (final Throwable t) {
					t.printStackTrace();

					syntaxParsed = false;
				}

			if (!syntaxParsed) {
				this.tell(SimpleLocalization.Commands.RELOAD_FILE_LOAD_ERROR);

				return;
			}

			SimplePlugin.getInstance().reload();
			this.tell(SimpleLocalization.Commands.RELOAD_SUCCESS);

		} catch (final Throwable t) {
			this.tell(SimpleLocalization.Commands.RELOAD_FAIL.replace("{error}", t.getMessage() != null ? t.getMessage() : "unknown"));

			t.printStackTrace();
		}
	}

	/*
	 * Get a list of all files ending with "yml" in the given directory
	 * and its subdirectories
	 */
	private List<File> collectYamlFiles(File directory, List<File> list) {

		if (directory.exists())
			for (final File file : directory.listFiles()) {
				if (file.getName().endsWith("yml"))
					list.add(file);

				if (file.isDirectory())
					this.collectYamlFiles(file, list);
			}

		return list;
	}

	/**
	 * @see org.mineacademy.vfo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return NO_COMPLETE;
	}
}
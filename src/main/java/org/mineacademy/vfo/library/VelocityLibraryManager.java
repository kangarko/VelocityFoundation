package org.mineacademy.vfo.library;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.plugin.PluginManager;

/**
 * A runtime dependency manager for Velocity plugins.
 */
public class VelocityLibraryManager extends LibraryManager {
	/**
	 * Velocity plugin manager used for adding files to the plugin's classpath
	 */
	@NotNull
	private final PluginManager pluginManager;

	/**
	 * The plugin instance required by the plugin manager to add files to the
	 * plugin's classpath
	 */
	@NotNull
	private final Object plugin;

	/**
	 * Creates a new Velocity library manager.
	 *
	 * @param dataDirectory plugin's data directory
	 * @param pluginManager Velocity plugin manager
	 * @param plugin        the plugin to manage
	 */
	public VelocityLibraryManager(Object plugin, Path dataDirectory, PluginManager pluginManager) {
		super(toRootServerPath(dataDirectory));

		this.pluginManager = pluginManager;
		this.plugin = plugin;
	}

	/*
	 * Finds the root directory from the plugin's data directory
	 */
	private static Path toRootServerPath(Path dataDirectory) {
		final File pluginsFolder = dataDirectory.toFile().getParentFile();

		// For some reasons if we call getParentFile() on pluginsFolder above it will return null, so we have to manually get it from the last index of /
		final File rootFolder = new File(pluginsFolder.getAbsolutePath().substring(0, pluginsFolder.getAbsolutePath().lastIndexOf(File.separatorChar)));

		return new File(rootFolder, "libraries").toPath();
	}

	/**
	 * Adds a file to the Velocity plugin's classpath.
	 *
	 * @param file the file to add
	 */
	@Override
	protected void addToClasspath(@NotNull Path file) {
		pluginManager.addToClasspath(plugin, file);
	}

	@Override
	protected InputStream getResourceAsStream(@NotNull String path) {
		return getClass().getClassLoader().getResourceAsStream(path);
	}
}

package org.mineacademy.vfo.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.FileUtil;
import org.mineacademy.vfo.ReflectionUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import lombok.NonNull;

/**
 * The core settings class. Fully compatible with Minecraft 1.7.10 to the
 * latest one, including comments support (default file required, see saveComments()
 * and automatic config upgrading if we request a value that only exist in the default file.
 */
public class YamlConfig extends FileConfig {

	/**
	 * The Yaml instance
	 */
	private final Yaml yaml;

	/**
	 * Should we save empty sections or null values (requires NO default file)
	 */
	private boolean saveEmptyValues = true;

	/**
	 * Create a new instance (do not load it, use {@link #load(File)} to load)
	 */
	protected YamlConfig() {
		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setIndent(2);
		dumperOptions.setWidth(4096); // Do not wrap long lines

		YamlRepresenter representer;

		try {
			representer = new YamlRepresenter(dumperOptions);

		} catch (final Throwable t) {
			representer = new YamlRepresenter();
		}

		representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		// Load options only if available
		if (ReflectionUtil.isClassAvailable("org.yaml.snakeyaml.LoaderOptions")) {
			final LoaderOptions loaderOptions = new LoaderOptions();

			Yaml yaml;
			YamlConstructor constructor;

			try {
				constructor = new YamlConstructor(loaderOptions);

			} catch (final Throwable t) {
				// 1.12
				constructor = new YamlConstructor();
			}

			try {
				loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
				loaderOptions.setCodePointLimit(Integer.MAX_VALUE);

			} catch (final Throwable t) {
				// Thankfully unsupported
				// https://i.imgur.com/wAgKukK.png
			}

			try {
				yaml = new Yaml(constructor, representer, dumperOptions, loaderOptions);
			} catch (final Throwable t) {
				yaml = new Yaml(constructor, representer, dumperOptions);
			}

			this.yaml = yaml;
		}

		else
			this.yaml = new Yaml(new YamlConstructor(), representer, dumperOptions);
	}

	/**
	 * Return true if you have a default file and want to save comments from it
	 *
	 * Any user-generated comments will be lost, any user-written values will be lost.
	 *
	 * Please see {@link #getUncommentedSections()} to write sections containing maps users
	 * can create to prevent losing them.
	 *
	 * @return
	 */
	protected boolean saveComments() {
		return true;
	}

	/**
	 * See {@link #saveComments()}
	 *
	 * @return
	 */
	protected List<String> getUncommentedSections() {
		return new ArrayList<>();
	}

	/**
	 * (Requires no default file or saveComments() on false)
	 * Set if we should remove empty lists or sections when saving.
	 * Defaults to true, that means that empty sections will be saved.
	 *
	 * @param saveEmptyValues
	 */
	public final void setSaveEmptyValues(boolean saveEmptyValues) {
		this.saveEmptyValues = saveEmptyValues;
	}

	/**
	 * Returns true if this config contains any keys what so ever. Override for
	 * custom logic.
	 *
	 * @return
	 */
	public boolean isValid() {
		return !this.section.map.isEmpty();
	}

	// ------------------------------------------------------------------------------------
	// File manipulation
	// ------------------------------------------------------------------------------------

	/**
	 * Attempts to load configuration from the given internal path in your JAR.
	 * We automatically move the file to your plugin's folder if it does not exist.
	 * Subfolders are supported, example: localization/messages_en.yml
	 *
	 * @param internalPath
	 */
	public final void loadConfiguration(String internalPath) {
		this.loadConfiguration(internalPath, internalPath);
	}

	/**
	 * Load configuration from the optional from path in your JAR file,
	 * extracting it to the given path in your plugin's folder if it does not exist.
	 *
	 * @param from
	 * @param to
	 */
	public final void loadConfiguration(@Nullable String from, String to) {
		File file;

		if (from != null) {

			// Copy if not exists yet
			file = FileUtil.extract(from, to);

			// Initialize file early
			this.file = file;

			// Keep a loaded copy to copy default values from
			final YamlConfig defaultConfig = new YamlConfig();
			final String defaultContent = String.join("\n", FileUtil.getInternalFileContent(from));

			defaultConfig.file = file;
			defaultConfig.loadFromString(defaultContent);

			this.defaults = defaultConfig.section;
			this.defaultsPath = from;
		}

		else {
			file = FileUtil.getFile(to);

			if (!file.exists()) {
				FileUtil.createIfNotExists(to);

				if (this.getHeader() != null)
					this.shouldSave = true;
			}
		}

		this.load(file);
	}

	/**
	 * Loads the configuration from the internal path WITHOUT calling onLoad(),
	 * without setting defaults and without extracting the file.
	 *
	 * @param internalPath
	 */
	public final void loadInternal(String internalPath) {
		final String content = String.join("\n", FileUtil.getInternalFileContent(internalPath));

		this.loadFromString(content);
	}

	/*
	 * Dumps all values in this config into a saveable format
	 */
	@NonNull
	@Override
	public final String saveToString() {

		// Do not use comments
		if (this.defaults == null || !this.saveComments()) {
			String header = "";

			if (this.getHeader() != null) {
				for (final String line : this.getHeader())
					header += "# " + line + "\n";

				header += "\n";
			}

			final Map<String, Object> values = this.section.getValues(false);

			if (!this.saveEmptyValues)
				removeEmptyValues(values);

			String dump = this.yaml.dump(values);

			// Blank config
			if (dump.equals("{}\n"))
				dump = "";

			return header + dump;
		}

		// Special case, write using comments engine
		try {
			YamlComments.writeComments(this.defaultsPath, this.file, this.getUncommentedSections());

		} catch (final IOException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	/*
	 * Attempts to remove empty maps, lists or arrays from the given map
	 */
	private static void removeEmptyValues(Map<String, Object> map) {
		for (final Iterator<Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
			final Entry<String, Object> entry = it.next();
			final Object value = entry.getValue();

			if (value instanceof ConfigSection) {
				final Map<String, Object> childMap = ((ConfigSection) value).map;

				removeEmptyValues(childMap);

				if (childMap.isEmpty())
					it.remove();
			}

			if (value == null
					|| value instanceof Iterable<?> && !((Iterable<?>) value).iterator().hasNext()
					|| value.getClass().isArray() && ((Object[]) value).length == 0
					|| value instanceof Map<?, ?> && ((Map<?, ?>) value).isEmpty()) {

				it.remove();

				continue;
			}
		}
	}

	/*
	 * Loads configuration from the given string contents
	 */
	@Override
	final void loadFromString(@NonNull String contents) {

		Map<?, ?> input;

		try {
			input = (Map<?, ?>) this.yaml.load(contents);

		} catch (final YAMLException ex) {
			throw ex;

		} catch (final ClassCastException e) {
			throw new IllegalArgumentException("Top level is not a Map.");
		}

		final String header = this.parseHeader(contents);

		if (header.trim().length() > 0)
			this.setHeader(header);

		this.section.map.clear();

		if (input != null)
			this.convertMapsToSections(input, this.section);
	}

	/*
	 * Converts the given maps to sections
	 */
	private void convertMapsToSections(@NonNull Map<?, ?> input, @NonNull ConfigSection section) {
		for (final Map.Entry<?, ?> entry : input.entrySet()) {
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();

			if (value instanceof Map)
				this.convertMapsToSections((Map<?, ?>) value, section.createSection(key));
			else
				section.store(key, value);
		}
	}

	/*
	 * Converts the given input to header
	 */
	@NonNull
	private String parseHeader(@NonNull String input) {
		final String commentPrefix = "# ";
		final String[] lines = input.split("\r?\n", -1);
		final StringBuilder result = new StringBuilder();

		boolean readingHeader = true;
		boolean foundHeader = false;

		for (int i = 0; i < lines.length && readingHeader; i++) {
			final String line = lines[i].trim();

			if (line.startsWith(commentPrefix) || line.equals("#")) {
				if (i > 0)
					result.append("\n");

				if (line.length() > commentPrefix.length())
					result.append(line.substring(commentPrefix.length()));

				foundHeader = true;

			} else if (foundHeader && line.length() == 0)
				result.append("\n");

			else if (foundHeader)
				readingHeader = false;
		}

		final String string = result.toString();

		return string.trim().isEmpty() ? "" : string + "\n";
	}

	@Override
	public int hashCode() {
		return this.getFileName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof YamlConfig && ((YamlConfig) obj).getFileName().equals(this.getFileName());
	}

	// -----------------------------------------------------------------------------------------------------
	// Static
	// -----------------------------------------------------------------------------------------------------

	/**
	 * Loads configuration from the internal JAR path, extracting it if needed.
	 *
	 * @param path
	 * @return
	 */
	@NonNull
	public static final YamlConfig fromInternalPath(@NonNull String path) {

		final YamlConfig config = new YamlConfig();

		try {
			config.loadConfiguration(path);

		} catch (final Exception ex) {
			Common.error(ex, "Cannot load " + path);
		}

		return config;
	}

	/**
	 * Loads configuration from the internal JAR path without setting the file,
	 * without extracting it and without defaults.
	 *
	 * @param path
	 * @return
	 */
	@NonNull
	public static final YamlConfig fromInternalPathFast(@NonNull String path) {

		final YamlConfig config = new YamlConfig();

		try {
			config.loadInternal(path);

		} catch (final Exception ex) {
			Common.error(ex, "Cannot load " + path);
		}

		return config;
	}

	/**
	 * Loads configuration from the file in your plugin's folder.
	 *
	 * @param file
	 * @return
	 */
	@NonNull
	public static final YamlConfig fromFile(@NonNull File file) {

		final YamlConfig config = new YamlConfig();

		try {
			config.load(file);
		} catch (final Exception ex) {
			Common.error(ex, "Cannot load " + file);
		}

		return config;
	}

	/**
	 * Loads configuration from the file in your plugin's folder.
	 *
	 * @param file
	 * @return
	 */
	@NonNull
	public static final YamlConfig fromFileFast(@NonNull File file) {
		final YamlConfig config = new YamlConfig();

		try {
			final List<String> content = FileUtil.readLines(file);
			config.loadFromString(String.join("\n", content));

		} catch (final Exception ex) {
			Common.error(ex, "Cannot load " + file);
		}

		return config;
	}

	// -----------------------------------------------------------------------------------------------------
	// Classes
	// -----------------------------------------------------------------------------------------------------

	/**
	 * Helper class, credits to the original Bukkit/Spigot team, enhanced by MineAcademy
	 */
	private final static class YamlConstructor extends SafeConstructor {

		public YamlConstructor(LoaderOptions options) {
			super(options);

			this.yamlConstructors.put(Tag.MAP, new ConstructCustomObject());
		}

		public YamlConstructor() {
			super();

			this.yamlConstructors.put(Tag.MAP, new ConstructCustomObject());
		}

		private class ConstructCustomObject extends ConstructYamlMap {

			@Override
			public Object construct(@NonNull Node node) {
				if (node.isTwoStepsConstruction())
					throw new YAMLException("Unexpected referential mapping structure. Node: " + node);

				return super.construct(node);
			}

			@Override
			public void construct2ndStep(@NonNull Node node, @NonNull Object object) {
				throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
			}
		}
	}

	/**
	 * Helper class, credits to the original Bukkit/Spigot team, enhanced by MineAcademy
	 */
	private final static class YamlRepresenter extends Representer {

		public YamlRepresenter(DumperOptions options) {
			super(options);

			this.multiRepresenters.put(ConfigSection.class, new RepresentConfigurationSection());
			this.multiRepresenters.remove(Enum.class);
		}

		public YamlRepresenter() {
			super();

			this.multiRepresenters.put(ConfigSection.class, new RepresentConfigurationSection());
			this.multiRepresenters.remove(Enum.class);
		}

		private class RepresentConfigurationSection extends RepresentMap {

			@NonNull
			@Override
			public Node representData(@NonNull Object data) {
				return super.representData(((ConfigSection) data).getValues(false));
			}
		}
	}
}

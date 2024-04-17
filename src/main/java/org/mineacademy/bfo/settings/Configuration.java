package org.mineacademy.bfo.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

//Credits to md_5 for original bungeecord config class
@SuppressWarnings("rawtypes")
public final class Configuration {

	private static final char SEPARATOR = '.';
	final Map<String, Object> self;
	private final Configuration defaults;

	public Configuration() {
		this(null);
	}

	public Configuration(Configuration defaults) {
		this(new LinkedHashMap<>(), defaults);
	}

	Configuration(Map<?, ?> map, Configuration defaults) {
		this.self = new LinkedHashMap<>();
		this.defaults = defaults;

		for (final Map.Entry<?, ?> entry : map.entrySet()) {
			final String key = (entry.getKey() == null) ? "null" : entry.getKey().toString();

			if (entry.getValue() instanceof Map) {
				this.self.put(key, new Configuration((Map) entry.getValue(), (defaults == null) ? null : defaults.getSection(key)));
			} else {
				this.self.put(key, entry.getValue());
			}
		}
	}

	private Configuration getSectionFor(String path) {
		final int index = path.indexOf(SEPARATOR);
		if (index == -1) {
			return this;
		}

		final String root = path.substring(0, index);
		Object section = self.get(root);
		if (section == null) {
			section = new Configuration((defaults == null) ? null : defaults.getSection(root));
			self.put(root, section);
		}

		return (Configuration) section;
	}

	private String getChild(String path) {
		final int index = path.indexOf(SEPARATOR);
		return (index == -1) ? path : path.substring(index + 1);
	}

	/*------------------------------------------------------------------------*/

	public <T> T get(String path, T def) {
		final Configuration section = getSectionFor(path);
		Object val;
		if (section == this) {
			val = self.get(path);
		} else {
			val = section.get(getChild(path), def);
		}

		if (val == null && def instanceof Configuration) {
			self.put(path, def);
		}

		return (val != null) ? (T) val : def;
	}

	public boolean contains(String path) {
		return get(path, null) != null;
	}

	public Object get(String path) {
		return get(path, getDefault(path));
	}

	public Object getDefault(String path) {
		return (defaults == null) ? null : defaults.get(path);
	}

	public void set(String path, Object value) {
		if (value instanceof Map) {
			value = new Configuration((Map) value, (defaults == null) ? null : defaults.getSection(path));
		}

		final Configuration section = getSectionFor(path);
		if (section == this) {
			if (value == null) {
				self.remove(path);
			} else {
				self.put(path, value);
			}
		} else {
			section.set(getChild(path), value);
		}
	}

	/*------------------------------------------------------------------------*/
	public Configuration getSection(String path) {
		final Object def = getDefault(path);
		return (Configuration) get(path, (def instanceof Configuration) ? def : new Configuration((defaults == null) ? null : defaults.getSection(path)));
	}

	/**
	 * Gets keys, not deep by default.
	 *
	 * @return top level keys for this section
	 */
	public Collection<String> getKeys() {
		return new LinkedHashSet<>(self.keySet());
	}

	/*------------------------------------------------------------------------*/
	public byte getByte(String path) {
		final Object def = getDefault(path);
		return getByte(path, (def instanceof Number) ? ((Number) def).byteValue() : 0);
	}

	public byte getByte(String path, byte def) {
		final Object val = get(path, def);
		return (val instanceof Number) ? ((Number) val).byteValue() : def;
	}

	public List<Byte> getByteList(String path) {
		final List<?> list = getList(path);
		final List<Byte> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof Number) {
				result.add(((Number) object).byteValue());
			}
		}

		return result;
	}

	public short getShort(String path) {
		final Object def = getDefault(path);
		return getShort(path, (def instanceof Number) ? ((Number) def).shortValue() : 0);
	}

	public short getShort(String path, short def) {
		final Object val = get(path, def);
		return (val instanceof Number) ? ((Number) val).shortValue() : def;
	}

	public List<Short> getShortList(String path) {
		final List<?> list = getList(path);
		final List<Short> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof Number) {
				result.add(((Number) object).shortValue());
			}
		}

		return result;
	}

	public int getInt(String path) {
		final Object def = getDefault(path);
		return getInt(path, (def instanceof Number) ? ((Number) def).intValue() : 0);
	}

	public int getInt(String path, int def) {
		final Object val = get(path, def);
		return (val instanceof Number) ? ((Number) val).intValue() : def;
	}

	public List<Integer> getIntList(String path) {
		final List<?> list = getList(path);
		final List<Integer> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof Number) {
				result.add(((Number) object).intValue());
			}
		}

		return result;
	}

	public long getLong(String path) {
		final Object def = getDefault(path);
		return getLong(path, (def instanceof Number) ? ((Number) def).longValue() : 0);
	}

	public long getLong(String path, long def) {
		final Object val = get(path, def);
		return (val instanceof Number) ? ((Number) val).longValue() : def;
	}

	public List<Long> getLongList(String path) {
		final List<?> list = getList(path);
		final List<Long> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof Number) {
				result.add(((Number) object).longValue());
			}
		}

		return result;
	}

	public float getFloat(String path) {
		final Object def = getDefault(path);
		return getFloat(path, (def instanceof Number) ? ((Number) def).floatValue() : 0);
	}

	public float getFloat(String path, float def) {
		final Object val = get(path, def);
		return (val instanceof Number) ? ((Number) val).floatValue() : def;
	}

	public List<Float> getFloatList(String path) {
		final List<?> list = getList(path);
		final List<Float> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof Number) {
				result.add(((Number) object).floatValue());
			}
		}

		return result;
	}

	public double getDouble(String path) {
		final Object def = getDefault(path);
		return getDouble(path, (def instanceof Number) ? ((Number) def).doubleValue() : 0);
	}

	public double getDouble(String path, double def) {
		final Object val = get(path, def);
		return (val instanceof Number) ? ((Number) val).doubleValue() : def;
	}

	public List<Double> getDoubleList(String path) {
		final List<?> list = getList(path);
		final List<Double> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof Number) {
				result.add(((Number) object).doubleValue());
			}
		}

		return result;
	}

	public boolean getBoolean(String path) {
		final Object def = getDefault(path);
		return getBoolean(path, (def instanceof Boolean) ? (Boolean) def : false);
	}

	public boolean getBoolean(String path, boolean def) {
		final Object val = get(path, def);
		return (val instanceof Boolean) ? (Boolean) val : def;
	}

	public List<Boolean> getBooleanList(String path) {
		final List<?> list = getList(path);
		final List<Boolean> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof Boolean) {
				result.add((Boolean) object);
			}
		}

		return result;
	}

	public char getChar(String path) {
		final Object def = getDefault(path);
		return getChar(path, (def instanceof Character) ? (Character) def : '\u0000');
	}

	public char getChar(String path, char def) {
		final Object val = get(path, def);
		return (val instanceof Character) ? (Character) val : def;
	}

	public List<Character> getCharList(String path) {
		final List<?> list = getList(path);
		final List<Character> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof Character) {
				result.add((Character) object);
			}
		}

		return result;
	}

	public String getString(String path) {
		final Object def = getDefault(path);
		return getString(path, (def instanceof String) ? (String) def : "");
	}

	public String getString(String path, String def) {
		final Object val = get(path, def);
		return (val instanceof String) ? (String) val : def;
	}

	public List<String> getStringList(String path) {
		final List<?> list = getList(path);
		final List<String> result = new ArrayList<>();

		for (final Object object : list) {
			if (object instanceof String) {
				result.add((String) object);
			}
		}

		return result;
	}

	/*------------------------------------------------------------------------*/
	public List<?> getList(String path) {
		final Object def = getDefault(path);
		return getList(path, (def instanceof List<?>) ? (List<?>) def : Collections.EMPTY_LIST);
	}

	public List<?> getList(String path, List<?> def) {
		final Object val = get(path, def);
		return (val instanceof List<?>) ? (List<?>) val : def;
	}
}

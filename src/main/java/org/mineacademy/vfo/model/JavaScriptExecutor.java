package org.mineacademy.vfo.model;

import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.mineacademy.vfo.Common;
import org.mineacademy.vfo.ReflectionUtil;
import org.mineacademy.vfo.Valid;
import org.mineacademy.vfo.exception.EventHandledException;
import org.mineacademy.vfo.plugin.SimplePlugin;

import lombok.NonNull;
import net.kyori.adventure.audience.Audience;

/**
 * An engine that compiles and executes code on the fly.
 * <p>
 * The code is based off JavaScript with new Java methods, see:
 * https://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/
 */
public final class JavaScriptExecutor {

	/**
	 * The engine singleton
	 */
	private static ScriptEngine engine;

	// Load the engine
	static {
		try {
			Thread.currentThread().setContextClassLoader(SimplePlugin.class.getClassLoader());

			ScriptEngineManager engineManager = new ScriptEngineManager();
			ScriptEngine scriptEngine = engineManager.getEngineByName("Nashorn");

			// Workaround for newer Minecraft releases, still unsure what the cause is
			if (scriptEngine == null) {
				engineManager = new ScriptEngineManager(null);

				scriptEngine = engineManager.getEngineByName("Nashorn");
			}

			// If still fails, try to load our own library for Java 15 and up
			if (scriptEngine == null) {
				final String nashorn = "org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory";

				if (ReflectionUtil.isClassAvailable(nashorn)) {
					final ScriptEngineFactory engineFactory = ReflectionUtil.instantiate(ReflectionUtil.lookupClass(nashorn));

					engineManager.registerEngineName("Nashorn", engineFactory);
					scriptEngine = engineManager.getEngineByName("Nashorn");
				}
			}

			engine = scriptEngine;

		} catch (final Throwable t) {
			//Common.error(t, "Error initializing JavaScript engine");
		}
	}

	/**
	 * Compiles and executes the given JavaScript code
	 *
	 * @param javascript
	 * @return
	 */
	public static Object run(final String javascript) {
		return run(javascript, null, null);
	}

	/**
	 * Runs the given JavaScript code for the player,
	 * making the "player" variable in the code usable
	 *
	 * @param javascript
	 * @param sender
	 * @return
	 */
	public static Object run(final String javascript, final Audience sender) {
		return run(javascript, sender, null);
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param sender
	 * @param event
	 * @return
	 */
	public static Object run(@NonNull String javascript, final Audience sender, final Object event) {
		if (engine == null) {
			Common.warning("Ignoring JavaScript due to missing library. To fix this, compile nashorn-core as dependency to your jar. Ignoring code: '" + javascript + "'");

			return null;
		}

		try {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			if (sender != null)
				engine.put("player", sender);

			else
				Valid.checkBoolean(!javascript.contains("player."), "Not running script because it uses 'player' but player was null! Script: " + javascript);

			if (event != null)
				engine.put("event", event);

			return engine.eval(javascript);

		} catch (final Throwable ex) {
			final String message = ex.toString();
			String error = "Script execution failed for";

			if (message.contains("ReferenceError:") && message.contains("is not defined"))
				error = "Found invalid or unparsed variable in";

			// Special support for throwing exceptions in the JS code so that users
			// can send messages to player directly if upstream supports that
			final String cause = ex.getCause().toString();

			if (ex.getCause() != null && cause.contains("event handled")) {
				final String[] errorMessageSplit = cause.contains("event handled: ") ? cause.split("event handled\\: ") : new String[0];

				if (errorMessageSplit.length == 2)
					Common.tellNoPrefix(sender, errorMessageSplit[1]);

				throw new EventHandledException(true);
			}

			throw new RuntimeException(error + " '" + javascript + "', sender: " + (sender == null ? "null" : sender.getClass() + ": " + sender), ex);
		}
	}

	/**
	 * Executes the Javascript code with the given variables - you have to handle the error yourself
	 *
	 * @param javascript
	 * @param replacements
	 *
	 * @return
	 */
	public static Object run(final String javascript, final Map<String, Object> replacements) {

		if (engine == null) {
			Common.warning("Ignoring JavaScript due to missing library. To fix this, compile nashorn-core as dependency to your jar. Ignoring code: '" + javascript + "'");

			return javascript;
		}

		try {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			if (replacements != null)
				for (final Map.Entry<String, Object> replacement : replacements.entrySet())
					engine.put(replacement.getKey(), replacement.getValue());

			return engine.eval(javascript);

		} catch (final ScriptException ex) {
			throw new RuntimeException("Script execution failed for '" + javascript + "'", ex);
		}
	}
}
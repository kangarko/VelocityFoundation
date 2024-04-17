package org.mineacademy.vfo.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Place this annotation over any of the following classes to make Foundation
 * automatically register it when the plugin starts, and properly reload it.
 *
 * Supported classes:
 * - SimpleListener
 * - PacketListener
 * - VelocityListener
 * - DiscordListener
 * - SimpleCommand
 * - SimpleCommandGroup
 * - SimpleExpansion
 * - YamlConfig (we will load your config when the plugin starts and reload it properly)
 * - any class that has Subscribe annotated methods
 *
 * In addition, the following classes will self-register automatically regardless
 * if you place this annotation on them or not:
 * - Tool (and its derivates such as Rocket)
 * - SimpleEnchantment
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AutoRegister {

	/**
	 * When false, we wont print console warnings such as that registration failed
	 * because the server runs outdated MC version (example: SimpleEnchantment) or lacks
	 * necessary plugins to be hooked into (example: DiscordListener, PacketListener)
	 *
	 * @return
	 */
	boolean hideIncompatibilityWarnings() default false;
}

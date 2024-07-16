package org.mineacademy.vfo;

import org.mineacademy.vfo.plugin.SimplePlugin;
import org.mineacademy.vfo.remain.Remain;

import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;

/**
 * Utility class for managing players.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerUtil {

	// ------------------------------------------------------------------------------------------------------------
	// Misc
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Kicks the player on the main thread with a colorized message
	 *
	 * @param player
	 * @param messages
	 *
	 */
	public static void kick(final Player player, final String... messages) {
		player.disconnect(Remain.convertLegacyToAdventure(Common.colorize(String.join("\n", messages))));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Permissions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return if the given sender has a certain permission
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	public static boolean hasPerm(final Audience sender, String permission) {
		Valid.checkNotNull(sender, "cannot call hasPerm for null sender!");

		if (!(sender instanceof PermissionSubject))
			return true;

		final PermissionSubject permissible = (PermissionSubject) sender;

		if (permission == null) {
			Common.log("THIS IS NOT AN ACTUAL ERROR, YOUR PLUGIN WILL WORK FINE");
			Common.log("Internal check got null permission as input, this is no longer allowed.");
			Common.log("We'll return true to prevent errors. Contact developers of " + SimplePlugin.getNamed());
			Common.log("to get it solved and include the fake error below:");

			new Throwable().printStackTrace();

			return true;
		}

		Valid.checkBoolean(!permission.contains("{plugin_name}") && !permission.contains("{plugin_name_lower}"),
				"Found {plugin_name} variable calling hasPerm(" + sender + ", " + permission + ")." + "This is now disallowed, contact plugin authors to put " + SimplePlugin.getNamed().toLowerCase() + " in their permission.");

		return permissible.hasPermission(permission);
	}

}

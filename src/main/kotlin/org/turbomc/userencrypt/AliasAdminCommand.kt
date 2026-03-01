package org.turbomc.userencrypt

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor


/**
 * Admin command to change aliases manually.
 * Usage: /userencrypt setalias <originalName> <newAlias>
 */
class AliasAdminCommand(
    private val databaseManager: DatabaseManager,
    private val proxyServer: ProxyServer
) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source: CommandSource = invocation.source()
        val args: Array<String> = invocation.arguments()

        if (args.size != 3 || !args[0].equals("setalias", ignoreCase = true)) {
            source.sendMessage(Component.text("Usage: /ue setalias <OriginalName> <NewAlias>", NamedTextColor.RED))
            return
        }

        val originalName = args[1]
        val newAlias = args[2]

        // Validate length and characters of the new alias
        if (newAlias.length > 16 || !newAlias.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            source.sendMessage(Component.text("Invalid alias! Must be alphanumeric and max 16 characters.", NamedTextColor.RED))
            return
        }

        // Check if the new alias is already taken by someone else
        val existingOwnerAlias = databaseManager.getAlias(originalName)
        if (databaseManager.aliasExists(newAlias) && existingOwnerAlias != newAlias) {
            source.sendMessage(Component.text("The alias '$newAlias' is already taken by another player!", NamedTextColor.RED))
            return
        }

        // Update Database
        databaseManager.saveAlias(originalName, newAlias)
        source.sendMessage(Component.text("Successfully changed alias of '$originalName' to '$newAlias'.", NamedTextColor.GREEN))

        // If the player is currently online (using their old alias), kick them so the proxy resets their profile.
        if (existingOwnerAlias != null) {
            proxyServer.getPlayer(existingOwnerAlias).ifPresent { player ->
                player.disconnect(
                    Component.text("Your username alias has been updated by an admin.\nPlease reconnect.", NamedTextColor.YELLOW)
                )
            }
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return invocation.source().hasPermission("userencrypt.admin")
    }

    override fun suggest(invocation: SimpleCommand.Invocation): MutableList<String> {
        val args = invocation.arguments()
        return when (args.size) {
            0, 1 -> mutableListOf("setalias")
            else -> mutableListOf()
        }
    }
}
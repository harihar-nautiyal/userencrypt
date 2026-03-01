package org.turbomc.userencrypt

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.util.GameProfile
import org.geysermc.floodgate.api.FloodgateApi
import org.slf4j.Logger
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

@Plugin(
    id = "userencrypt",
    name = "UserEncrypt",
    version = BuildConstants.VERSION,
    description = "UserEncrypt minecraft velocity plugin to prevent username stealing in offline mode servers.",
    url = "https://h01.in/projects/userencrypt",
    authors = ["Harihar Nautiyal"]
)
class UserEncryptPlugin @Inject constructor(
    private val proxyServer: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataFolder: Path
) {
    private lateinit var databaseManager: DatabaseManager
    private lateinit var aliasGenerator: AliasGenerator
    private var floodgateLink: FloodgateLink? = null

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // 1. Initialize Floodgate Link
        if (proxyServer.pluginManager.isLoaded("floodgate")) {
            try {
                this.floodgateLink = FloodgateLink()
                logger.info("Successfully hooked into Floodgate API. Bedrock players will be skipped.")
            } catch (e: Throwable) {
                logger.warn("Floodgate plugin was found, but its API could not be loaded.", e)
            }
        } else {
            logger.info("Floodgate not found. All players will be treated as Java players.")
        }

        // 2. Initialize Database
        try {
            if (dataFolder.notExists()) {
                dataFolder.createDirectories()
            }
            val dbPath = dataFolder.resolve("players.db")
            databaseManager = DatabaseManager(dbPath)
            aliasGenerator = AliasGenerator(databaseManager)
            logger.info("Database initialized successfully.")
        } catch (e: Exception) {
            logger.error("Failed to initialize database. Plugin will not function correctly.", e)
            return
        }

        // 3. Register Commands
        val commandManager = proxyServer.commandManager
        val commandMeta = commandManager.metaBuilder("userencrypt")
            .aliases("ue")
            .plugin(this)
            .build()

        commandManager.register(commandMeta, AliasAdminCommand(databaseManager, proxyServer))

        logger.info("UserEncrypt initialized successfully.")
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        if (::databaseManager.isInitialized) {
            databaseManager.close()
            logger.info("Database connection closed.")
        }
    }

    @Subscribe
    fun onGameProfileRequest(event: GameProfileRequestEvent) {
        val playerUuid = event.gameProfile.id
        val originalName = event.username

        // Skip Bedrock players
        if (floodgateLink?.isFloodgatePlayer(playerUuid) == true) {
            logger.info("Skipping encryption for Floodgate player: '$originalName'")
            return
        }

        // Get existing alias or generate a new one
        val alias = databaseManager.getAlias(originalName) ?: run {
            val newAlias = aliasGenerator.generateUniqueUsername()
            databaseManager.saveAlias(originalName, newAlias)
            logger.info("Registered Java player '$originalName' with new alias '$newAlias'.")
            newAlias
        }

        logger.info("Processing login for '$originalName' as '$alias'")

        // Apply offline UUID based on the alias
        val offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$alias".toByteArray(Charsets.UTF_8))
        event.gameProfile = GameProfile(offlineUuid, alias, emptyList())
    }

    /**
     * Isolates Floodgate API to prevent NoClassDefFoundError
     */
    private class FloodgateLink {
        private val api = FloodgateApi.getInstance()
        fun isFloodgatePlayer(uuid: UUID): Boolean = api.isFloodgatePlayer(uuid)
    }
}



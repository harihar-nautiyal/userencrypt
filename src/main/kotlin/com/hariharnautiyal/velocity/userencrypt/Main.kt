package com.hariharnautiyal.velocity.userencrypt

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.GameProfileRequestEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.util.GameProfile
import io.github.serpro69.kfaker.Faker
import org.slf4j.Logger
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

@Plugin(
    id = "userencrypt",
    name = "userencrypt",
    version = BuildConstants.VERSION,
    description = "UserEncrypt minecraft velocity plugin to prevent from username stealing in offline mode servers.",
    url = "https://harihar.site/projects/userencrypt",
    authors = ["Harihar Nautiyal"]
)
class Main @Inject constructor(
    private val proxyServer: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataFolder: Path
) {
    private lateinit var connection: Connection
    private val faker = Faker()
    private var floodgateLink: FloodgateLink? = null

    /**
     * This private inner class isolates all Floodgate API code.
     * The JVM will only attempt to load this class when we explicitly create an instance of it,
     * preventing a NoClassDefFoundError if Floodgate is not installed.
     */
    private class FloodgateLink {
        private val api = org.geysermc.floodgate.api.FloodgateApi.getInstance()

        fun isFloodgatePlayer(uuid: UUID): Boolean {
            return api.isFloodgatePlayer(uuid)
        }
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        if (proxyServer.pluginManager.isLoaded("floodgate")) {
            try {
                this.floodgateLink = FloodgateLink()
                logger.info("Successfully hooked into Floodgate API. Bedrock players will be skipped.")
            } catch (e: NoClassDefFoundError) {
                logger.warn("Floodgate plugin was found, but its API could not be loaded. Continuing without Floodgate support.")
            }
        } else {
            logger.info("Floodgate not found. All players will be treated as Java players.")
        }

        try {
            if (dataFolder.notExists()) {
                dataFolder.createDirectories()
            }
            val dbPath = dataFolder.resolve("players.db")
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:${dbPath.toAbsolutePath()}")
            createTable()
            logger.info("UserEncrypt ${BuildConstants.VERSION} initialized successfully using SQLite storage.")
        } catch (e: Exception) {
            logger.error("Failed to initialize UserEncrypt. The plugin will be disabled.", e)
        }
    }

    private fun createTable() {
        val sql = "CREATE TABLE IF NOT EXISTS player_aliases (original_name TEXT PRIMARY KEY, alias TEXT NOT NULL UNIQUE);"
        connection.createStatement().use { it.execute(sql) }
    }

    private fun getAlias(originalName: String): String? {
        val sql = "SELECT alias FROM player_aliases WHERE original_name = ?;"
        return connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, originalName)
            val rs = pstmt.executeQuery()
            if (rs.next()) rs.getString("alias") else null
        }
    }

    private fun saveAlias(originalName: String, alias: String) {
        val sql = "INSERT INTO player_aliases(original_name, alias) VALUES(?,?);"
        connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, originalName)
            pstmt.setString(2, alias)
            pstmt.executeUpdate()
        }
    }

    private fun aliasExists(alias: String): Boolean {
        val sql = "SELECT 1 FROM player_aliases WHERE alias = ?;"
        return connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, alias)
            pstmt.executeQuery().next()
        }
    }

    private fun generateUniqueUsername(): String {
        var newUsername: String
        do {
            val rawTitle = faker.game.title().replace(Regex("[^a-zA-Z0-9_]"), "")
            val randomNumber = faker.random.nextInt(1000, 9999)
            val maxBaseLength = 16 - randomNumber.toString().length
            val trimmedBase = rawTitle.take(maxBaseLength)
            newUsername = if (trimmedBase.isEmpty()) "Player$randomNumber" else "$trimmedBase$randomNumber"
        } while (aliasExists(newUsername))
        return newUsername
    }

    @Subscribe
    fun onGameProfileRequest(event: GameProfileRequestEvent) {
        val playerUuid = event.gameProfile.id
        if (floodgateLink?.isFloodgatePlayer(playerUuid) == true) {
            logger.info("Player '${event.username}' is a Floodgate (Bedrock) player. Skipping username encryption.")
            return
        }

        val originalName = event.username

        val alias = getAlias(originalName) ?: run {
            val newAlias = generateUniqueUsername()
            saveAlias(originalName, newAlias)
            logger.info("Registered Java player '$originalName' with new alias '$newAlias'.")
            newAlias
        }

        logger.info("Processing login for '$originalName' as '$alias'")
        val offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$alias".toByteArray(Charsets.UTF_8))
        val newProfile = GameProfile(offlineUuid, alias, emptyList())
        event.gameProfile = newProfile
    }
}
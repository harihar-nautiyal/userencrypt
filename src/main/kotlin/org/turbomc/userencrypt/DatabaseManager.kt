package org.turbomc.userencrypt

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.use

/**
 * Handles all SQLite Database interactions.
 * Synchronized to prevent SQLITE_BUSY errors during concurrent logins.
 */
class DatabaseManager(dbPath: Path) {
    private val connection: Connection

    init {
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbPath.toAbsolutePath()}")
        createTable()
    }

    @Synchronized
    private fun createTable() {
        val sql = "CREATE TABLE IF NOT EXISTS player_aliases (original_name TEXT PRIMARY KEY, alias TEXT NOT NULL UNIQUE);"
        connection.createStatement().use { it.execute(sql) }
    }

    @Synchronized
    fun getAlias(originalName: String): String? {
        val sql = "SELECT alias FROM player_aliases WHERE original_name = ?;"
        return connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, originalName)
            val rs = pstmt.executeQuery()
            if (rs.next()) rs.getString("alias") else null
        }
    }

    @Synchronized
    fun saveAlias(originalName: String, alias: String) {
        // Upsert logic: If the original_name exists, update the alias.
        val sql = """
            INSERT INTO player_aliases(original_name, alias) VALUES(?, ?)
            ON CONFLICT(original_name) DO UPDATE SET alias=excluded.alias;
        """.trimIndent()

        connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, originalName)
            pstmt.setString(2, alias)
            pstmt.executeUpdate()
        }
    }

    @Synchronized
    fun aliasExists(alias: String): Boolean {
        val sql = "SELECT 1 FROM player_aliases WHERE alias = ?;"
        return connection.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, alias)
            pstmt.executeQuery().next()
        }
    }

    @Synchronized
    fun close() {
        if (!connection.isClosed) {
            connection.close()
        }
    }
}
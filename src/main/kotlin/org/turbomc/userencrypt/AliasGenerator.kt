package org.turbomc.userencrypt

import io.github.serpro69.kfaker.Faker

/**
 * Handles generation of random aliases
 */
class AliasGenerator(private val databaseManager: DatabaseManager) {
    private val faker = Faker()

    fun generateUniqueUsername(): String {
        var newUsername: String
        var attempts = 0
        do {
            val rawTitle = faker.game.title().replace(Regex("[^a-zA-Z0-9_]"), "")
            val randomNumber = faker.random.nextInt(1000, 9999)
            val maxBaseLength = 16 - randomNumber.toString().length
            val trimmedBase = rawTitle.take(maxBaseLength)

            newUsername = if (trimmedBase.isEmpty()) "Player$randomNumber" else "$trimmedBase$randomNumber"
            attempts++

            // Fallback to prevent absolute infinite loops if Faker breaks
            if (attempts > 50) {
                newUsername = "User${faker.random.nextInt(100000, 999999)}"
            }
        } while (databaseManager.aliasExists(newUsername))

        return newUsername
    }
}

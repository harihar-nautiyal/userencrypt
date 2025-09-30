# UserEncrypt Minecraft Plugin

## Project Overview
The UserEncrypt project is a Minecraft server plugin for Velocity, designed to enhance the security of "offline-mode" servers. When a player connects to an offline server, there is no authentication to verify their identity, allowing anyone to join using any username. This vulnerability is especially dangerous if a user impersonates an administrator.

UserEncrypt mitigates this risk by assigning players a unique, irreversible username upon their first login. For example, a player with the username "batman" might be permanently assigned "bat7852". In-game, this player will always be identified as "bat7852". If another person attempts to log in as "batman," the plugin will recognize that the original player has already been assigned a unique identifier, thereby securing the original username.

## Features

- **Username Encryption**: The core feature automatically encrypts a player’s original username into a unique and un-guessable alias. This new username is consistently applied every time the player joins, ensuring their identity remains secure.
- **Unique Username Storage Mode**: This alternative method assigns a unique, human-readable username to each player, which is then stored in a database. This provides a more user-friendly approach while maintaining the same level of security.
- **Floodgate Support**: The plugin automatically detects if Floodgate is installed and running, and it will skip username encryption for Bedrock players.

## How It Works
When a player joins for the first time, UserEncrypt generates a unique username for them. This new username is then stored in a local SQLite database, mapped to the player's original username. On subsequent joins, the plugin retrieves the assigned username from the database and applies it to the player. This ensures that the player's identity remains consistent and secure.

## Installation
### Releases
You can download latest version of userencrypt from [releases](https://github.com/harihar-nautiyal/userencrypt/releases)

### Modrinth
1. Download the latest version of the plugin from the [Modrinth page](https://harihar.site/projects/userencrypt).
2. Place the downloaded `.jar` file into the `plugins` folder of your Velocity proxy.
3. Restart your Velocity proxy.

## Building from Source
To build the plugin from source, you will need to have Java 17 and Gradle installed.

1. Clone the repository:
```bash
git clone https://github.com/hariharnautiyal/userencrypt
```
2. Navigate to the project directory:
```bash
cd userencrypt
```
3. Build the plugin using Gradle:
```bash
./gradlew shadowJar
```
The compiled `.jar` file will be located in the `build/libs` directory.

## Technology Stack

- **Core Language**: Kotlin
- **Build Tool**: Gradle
- **Proxy Support**: Velocity
- **Server Support**: Paper, Spigot
- **Distribution**: Published and managed on Modrinth
- **Version Control**: Git & GitHub for open-source collaboration

## Contributing
Contributions are welcome! If you would like to contribute to the project, please feel free to open a pull request on the [GitHub repository](https.github.com/your-username/userencrypt).

## License
This project is licensed under the MIT License. See the `LICENSE` file for more details.

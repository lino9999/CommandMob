# CommandMob

A lightweight Minecraft plugin that executes commands when players kill specific mobs.

## Features

- ✅ Execute commands based on mob kills
- ✅ Weighted probability system for different rewards
- ✅ Customizable messages
- ✅ Support for multiple placeholders
- ✅ Optimized for performance
- ✅ Simple configuration

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart the server
4. Configure the plugin in `plugins/SimpleCommandMob/config.yml`

## Commands

- `/mobcommands reload` - Reload the plugin configuration
- Aliases: `/mobcmd reload`, `/scm reload`

## Configuration

### Basic Example

```yaml
mobs:
  ZOMBIE:
    commands:
      "give %player% iron_ingot 1": 50      # 50% chance
      "give %player% gold_ingot 1": 30      # 30% chance
      "give %player% diamond 1": 20         # 20% chance
```

### Available Placeholders

- `%player%` - Player name
- `%uuid%` - Player UUID
- `%world%` - World name
- `%x%` - Player X coordinate
- `%y%` - Player Y coordinate
- `%z%` - Player Z coordinate

### Supported Mobs

All vanilla Minecraft mobs are supported. Use the official entity type names in uppercase:
- `ZOMBIE`, `SKELETON`, `CREEPER`, `SPIDER`
- `ENDER_DRAGON`, `WITHER`, `GUARDIAN`
- And many more...

## Weight System

The weight system determines the probability of each command being executed:

```yaml
commands:
  "command1": 60    # 60% chance (60/100)
  "command2": 30    # 30% chance (30/100)
  "command3": 10    # 10% chance (10/100)
  # Total: 100
```

## Examples

### Give Random Loot
```yaml
ENDER_DRAGON:
  commands:
    "give %player% elytra 1": 20
    "give %player% dragon_egg 1": 80
```

### Economy Integration
```yaml
CREEPER:
  commands:
    "eco give %player% 100": 70
    "eco give %player% 500": 30
```

### Effects and Broadcasts
```yaml
WITHER:
  commands:
    "effect give %player% strength 300 2": 50
    "broadcast &d%player% defeated the Wither!": 50
```

## Compatibility

- Minecraft 1.21+
- No dependencies required
- Compatible with economy plugins (Essentials, Vault, etc.)

## Author

**Lino9999**

## Version

1.3

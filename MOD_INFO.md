# Mobility Enchantments Mod for Minecraft 1.21.10

This Fabric mod adds five new mobility enchantments for chestplates that provide unique movement abilities.

## Enchantments

All enchantments are **Very Rare**, **Level I only**, and can only be applied to **chestplates**. They are **mutually exclusive** with each other and with all protection variants (Protection, Fire Protection, Blast Protection, Projectile Protection, Feather Falling).

### 1. Swooping
- **Activation**: Jump while falling
- **Effect**: Glides using the player's current velocity vector (non-steerable, unlike elytra)
- **Air Control**: Player can move slightly in XZ plane (same as normal falling)
- **Rocket Boost**: Not compatible
- **Hunger Cost**: 0.1 hunger per second (0.05 drumsticks/second)
- **Termination**: Stops when player runs out of hunger or lands

### 2. Dash
- **Activation**: Jump while falling
- **Effect**: Instantly propels player ~10 blocks in the direction they're looking
- **Hunger Cost**: 2 hunger per use (1 drumstick)
- **Multiple Uses**: Can be used multiple times until hunger depletes
- **Velocity**: Configurable in `MobilityConfig.DASH_VELOCITY` (default: 1.5)

### 3. Double Jump
- **Activation**: Jump while falling
- **Effect**: Standard double jump - allows jumping in mid-air
- **Hunger Cost**: 2 hunger per use (1 drumstick)
- **Limit**: One use per air time (resets on landing)

### 4. Elytra
- **Activation**: Jump while falling
- **Effect**: Elytra-like flight with reduced lift
- **Lift Multiplier**: 0.5x (configurable in `MobilityConfig.ELYTRA_LIFT_MULTIPLIER`)
- **Rocket Boost**: Compatible
- **Hunger Cost**: 1 hunger per 15 seconds of use
- **Termination**: Stops when player runs out of hunger or lands

### 5. Wall Jump
- **Activation**: Jump while very close to a wall (1/16 block from hitbox edge)
- **Effect**:
  - Jumps away from wall at 45° angle upward
  - Enters "wall jumping mode" until landing
  - In wall jumping mode: custom air control replaces normal air movement
- **Corner Handling**: At corners (2 walls), jumps at 45° between wall normals
- **Air Control**: Custom force application (configurable)
- **Speed Limit**: Adjustable in wall jumping mode
- **Hunger Cost**: 0.5 hunger per jump (0.25 drumsticks)

## Configuration

All tunable constants are in `ninja.trek.mobility.config.MobilityConfig`:

### Swooping
- `SWOOPING_HUNGER_PER_SECOND`: 0.1f
- `SWOOPING_AIR_CONTROL`: 0.02f

### Dash
- `DASH_VELOCITY`: 1.5
- `DASH_HUNGER_COST`: 2

### Double Jump
- `DOUBLE_JUMP_HUNGER_COST`: 2
- `DOUBLE_JUMP_VELOCITY`: 0.42

### Elytra
- `ELYTRA_LIFT_MULTIPLIER`: 0.5
- `ELYTRA_HUNGER_PER_15S`: 1
- `ELYTRA_HUNGER_TICK_INTERVAL`: 300

### Wall Jump
- `WALL_JUMP_HUNGER_COST`: 0.5f
- `WALL_JUMP_VELOCITY`: 0.6
- `WALL_DETECTION_DISTANCE`: 0.0625 (1/16 block)
- `WALL_JUMP_AIR_CONTROL`: 0.02f
- `WALL_JUMP_SPEED_LIMIT`: 1.0

### General
- `ABILITY_COOLDOWN_TICKS`: 3

## Obtaining Enchantments

These enchantments are **command/creative only**. To apply them:

```
/give @s diamond_chestplate{Enchantments:[{id:"mobility-enchantments:swooping",lvl:1}]}
/give @s diamond_chestplate{Enchantments:[{id:"mobility-enchantments:dash",lvl:1}]}
/give @s diamond_chestplate{Enchantments:[{id:"mobility-enchantments:double_jump",lvl:1}]}
/give @s diamond_chestplate{Enchantments:[{id:"mobility-enchantments:elytra",lvl:1}]}
/give @s diamond_chestplate{Enchantments:[{id:"mobility-enchantments:wall_jump",lvl:1}]}
```

## Technical Details

### Implementation

The mod uses:
- **Data-driven enchantments** (JSON definitions in `data/mobility-enchantments/enchantment/`)
- **Mixins** to modify player physics and movement
- **Player state tracking** via interface injection
- **Server-side physics** with client synchronization

### Key Files

- `ModEnchantments.java`: Registry keys for enchantments
- `MobilityState.java`: Interface for tracking player ability states
- `LivingEntityMixin.java`: Main mechanics implementation
- `PlayerEntityMixin.java`: Movement input handling
- `ServerPlayerEntityMixin.java`: State storage implementation
- `EnchantmentUtil.java`: Helper methods for enchantment checks and hunger

### Hunger System

- Hunger is consumed using the vanilla `HungerManager.addExhaustion()` method
- 1 hunger = 0.5 drumsticks (half a food icon)
- Creative/spectator mode players bypass hunger costs
- Abilities fail to activate or stop if insufficient hunger

### Wall Detection

Wall detection uses probe points at the top and bottom of the player on all 4 cardinal directions:
- **Distance**: 1/16 block beyond player hitbox
- **Probe Heights**: Player Y + 1.5 (top) and Y + 0.2 (bottom)
- **Corner Detection**: Combines normals of detected walls
- **Max Walls**: Limited to 2 (prevents opposite wall detection)

## Version

- **Minecraft**: 1.21.10
- **Fabric Loader**: 0.17.3+
- **Fabric API**: 0.138.0+1.21.10
- **Java**: 21+

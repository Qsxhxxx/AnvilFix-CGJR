# Anvil XP
A Forge mod for Minecraft 1.20.1 that fixes and extends vanilla anvil mechanics.

## Features
### Anvil XP Overhaul
- **Level Cap**: Configurable maximum XP level for anvil operations (default: 40; set to -1 to remove cap entirely)
- **Removed Cumulative Repair Cost**: Eliminates the vanilla stacking repair penalty that rises with each successive anvil use
- **Custom XP Cost Formula**: Fully customizable expression for calculating XP expenditure. Supports operators `+-*/^`, math functions `sqrt/ln/log/exp`, constants `pi/e`, and variable `x` (total combined enchantment levels)

### Overcapped Enchant Preservation & Diminishing Returns
- **Preserve Overleveled Enchantments**: Anvil combinations no longer clamp enchant levels to vanilla hard caps (e.g. Sharpness X remains Sharpness X instead of being capped at Sharpness V)
- **Hyperbolic Diminishing Scaling**: Overleveled enchant effects are balanced via the decay formula:
  `d(n) = base × (1 + factor) ÷ (factor + n - maxLevel)`
- **Floating-Point Attribute Compensation**: Level decay paired with fractional stat compensation delivers smooth, gradual falloff for overcapped enchantments

### Coverage Scope
Diminishing scaling applies to all weapon and armor enchantments:
- Damage Enchantments: Sharpness, Smite, Bane of Arthropods, Impaling
- Protection Enchantments: Protection, Fire Protection, Feather Falling, Blast Protection, Projectile Protection
- Ranged & Trident Enchantments (bows, crossbows, tridents, skeleton arrows): Power, Punch, Flame, Infinity, Multishot, Quick Charge, Piercing, Riptide, Loyalty, Channeling
- Compatibility with third-party mod enchantments

## Configuration
| Config Key | Default Value | Description |
|------------|---------------|-------------|
| levelLimit | -1 | Maximum allowed XP level for anvils; -1 = unlimited |
| removeIncrementalRepairCost | true | Toggle removal of vanilla stacking repair cost penalty |
| xpFormula | x*3/2 | Custom XP calculation formula; set to -1 to revert to vanilla behavior |
| keepOverLevelEnchantment | true | Retain enchant levels exceeding vanilla hard caps after anvil merges |
| overLevelDiminishFactor | 0.3 | Diminishing return coefficient; set to -1 to disable decay scaling |
| debugLogEnabled | false | Toggle verbose debug logging to game log |

## License

Released under the [GPL-3.0 license](https://github.com/Qsxhxxx/AnvilFix-CGJR/blob/main/LICENSE)

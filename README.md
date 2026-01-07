# AE2 Personal ME System

An AE2 addon that provides a wearable personal terminal and configurable virtual crafting recipes.

## Features

### Personal Wireless Terminal
- Wearable wireless terminal (Curios slot or inventory)
- Press inventory key (E) to open ME terminal instead of vanilla inventory
- Bind directly from Wireless Access Point GUI
- Infinite range when bound

### Communication Relay
- Block that provides virtual crafting recipes to ME networks
- Recipes execute instantly without Crafting CPUs
- Configurable via NBT - perfect for modpack makers
- Supports item tags, NBT matching, and multi-input/output recipes
- Right-click to view configured recipes

## Usage

### Personal Terminal
1. Obtain a Personal Wireless Terminal
2. Equip in a Curios slot (or keep in inventory)
3. Open a Wireless Access Point and click the bind button
4. Press E - ME terminal opens instead of vanilla inventory

### Communication Relay
Place the block and connect to an ME network. Recipes stored in the block's NBT will appear as craftable patterns.

#### KubeJS Example
```js
ServerEvents.commandRegistry(event => {
    event.register(
        event.commands.literal('giverelay')
            .requires(src => src.hasPermission(2))
            .executes(ctx => {
                ctx.source.player.give(Item.of('personalmesystem:communication_relay', {
                    BlockEntityTag: {
                        CustomName: '{"text":"Trading Post"}',
                        Description: ['{"text":"Exchange materials"}'],
                        virtual_recipes: [
                            {
                                inputs: [{ item: 'minecraft:cobblestone', count: 64 }],
                                outputs: [{ item: 'minecraft:diamond', count: 1 }]
                            },
                            {
                                inputs: [{ tag: 'forge:ingots/iron', count: 9 }],
                                outputs: [{ item: 'minecraft:iron_block', count: 1 }]
                            }
                        ]
                    }
                }))
                return 1
            })
    )
})
```

#### Recipe Format
```json
{
  "inputs": [
    { "item": "minecraft:diamond", "count": 1 },
    { "tag": "forge:ingots/gold", "count": 4 }
  ],
  "outputs": [
    { "item": "minecraft:netherite_ingot", "count": 1 }
  ]
}
```

## Configuration

### Server Config (`personalmesystem-server.toml`)
- `powerUsage` - AE power consumed per virtual craft (default: 0)

### Client Config (`personalmesystem-client.toml`)
- `showRecipeTooltips` - Show recipes in item tooltips (default: true)
- `maxTooltipRecipes` - Max recipes shown in tooltips (default: 3)

## Dependencies

- Minecraft 1.20.1
- Forge 47.x
- Applied Energistics 2 (15.0.0+)
- Curios API (5.2.0+)
- FancyMenu (optional) - enables AE2 screen customization

## License

MIT

package com.yardenzamir.personalmesystem.command;

import appeng.api.config.Actionable;
import com.yardenzamir.personalmesystem.PersonalMESystemMod;
import com.yardenzamir.personalmesystem.item.PersonalWirelessTerminalItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

public class PersonalMECommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("personalmesystem")
                .then(Commands.literal("give")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> giveToSelf(ctx))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> giveToPlayer(ctx, EntityArgument.getPlayer(ctx, "player")))))
        );

        // Shorthand alias
        dispatcher.register(Commands.literal("pme")
                .then(Commands.literal("give")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> giveToSelf(ctx))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> giveToPlayer(ctx, EntityArgument.getPlayer(ctx, "player")))))
        );
    }

    private static int giveToSelf(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            return giveToPlayer(ctx, player);
        }
        source.sendFailure(Component.literal("Must be a player to use this command without arguments"));
        return 0;
    }

    private static int giveToPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        var source = ctx.getSource();
        PersonalWirelessTerminalItem terminalItem = PersonalMESystemMod.PERSONAL_TERMINAL.get();
        ItemStack terminal = new ItemStack(terminalItem);

        // Fill with power
        terminalItem.injectAEPower(terminal, terminalItem.getAEMaxPower(terminal), Actionable.MODULATE);

        // Try to equip in Curios slot first
        var curiosOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosOpt.isPresent()) {
            ICuriosItemHandler handler = curiosOpt.get();

            // Try to find an empty curio slot
            var stacksHandler = handler.getStacksHandler("curio");
            if (stacksHandler.isPresent()) {
                var stacks = stacksHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).isEmpty()) {
                        stacks.setStackInSlot(i, terminal);
                        source.sendSuccess(() -> Component.literal("Gave Personal ME Terminal to " + player.getName().getString() + " (equipped in curio slot)"), true);
                        return 1;
                    }
                }
            }
        }

        // Fallback: give to inventory
        if (player.getInventory().add(terminal)) {
            source.sendSuccess(() -> Component.literal("Gave Personal ME Terminal to " + player.getName().getString() + " (added to inventory)"), true);
            return 1;
        }

        // Drop at feet if inventory full
        player.drop(terminal, false);
        source.sendSuccess(() -> Component.literal("Gave Personal ME Terminal to " + player.getName().getString() + " (dropped - inventory full)"), true);
        return 1;
    }
}

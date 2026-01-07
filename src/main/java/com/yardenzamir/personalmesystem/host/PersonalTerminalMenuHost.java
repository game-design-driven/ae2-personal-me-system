package com.yardenzamir.personalmesystem.host;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.helpers.WirelessTerminalMenuHost;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Menu host for Personal Terminal:
 * - Always opens (even when unbound)
 * - Never closes the menu
 * - No energy cost
 * - Infinite range (always connected if bound)
 */
public class PersonalTerminalMenuHost extends WirelessTerminalMenuHost {

    // Empty storage for when not bound - prevents menu from closing
    private static final MEStorage EMPTY_STORAGE = new MEStorage() {
        @Override
        public void getAvailableStacks(KeyCounter out) {
            // Empty
        }

        @Override
        public Component getDescription() {
            return Component.literal("Not Connected");
        }
    };

    // Cache grid reference at construction for thread-safe access during crafting calculations
    @Nullable
    private final IGrid cachedGrid;
    @Nullable
    private final IGridNode cachedGridNode;

    public PersonalTerminalMenuHost(Player player, @Nullable Integer slot, ItemStack itemStack,
                                    BiConsumer<Player, appeng.menu.ISubMenu> returnToMainMenu) {
        super(player, slot, itemStack, returnToMainMenu);
        // Cache grid at construction time (on main thread) for thread-safe access later
        this.cachedGrid = computeTargetGrid(player, itemStack);
        this.cachedGridNode = cachedGrid != null ? cachedGrid.getPivot() : null;
    }

    @Nullable
    private static IGrid computeTargetGrid(Player player, ItemStack itemStack) {
        var item = itemStack.getItem();
        if (item instanceof appeng.items.tools.powered.WirelessTerminalItem terminal) {
            return terminal.getLinkedGrid(itemStack, player.level(), null);
        }
        return null;
    }

    @Override
    public MEStorage getInventory() {
        // Return parent inventory if bound, otherwise empty storage
        // This prevents the menu from being invalidated when unbound
        var parentInventory = super.getInventory();
        return parentInventory != null ? parentInventory : EMPTY_STORAGE;
    }

    @Override
    public boolean onBroadcastChanges(AbstractContainerMenu menu) {
        // Always return true - never close the menu
        return true;
    }

    @Override
    public boolean rangeCheck() {
        // Always in range if we have a grid connection
        return getActionableNode() != null;
    }

    @Override
    public IGridNode getActionableNode() {
        // Return cached grid node for thread-safe access during crafting calculations
        return cachedGridNode;
    }

    @Override
    public boolean drainPower() {
        // No power drain
        return true;
    }

    @Nullable
    private IGrid getTargetGrid() {
        // Use cached grid for thread-safe access
        return cachedGrid;
    }
}

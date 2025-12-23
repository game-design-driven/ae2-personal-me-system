package com.example.personalmesystem.menu;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.locator.MenuLocator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Menu locator for items in Curios slots.
 * Allows AE2 menus to be opened from Curios-equipped items.
 */
public record CuriosMenuLocator(String slotType, int slotIndex) implements MenuLocator {

    @Nullable
    @Override
    public <T> T locate(Player player, Class<T> hostInterface) {
        var curiosOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosOpt.isEmpty()) {
            return null;
        }

        var stacksHandler = curiosOpt.get().getStacksHandler(slotType);
        if (stacksHandler.isEmpty()) {
            return null;
        }

        var stacks = stacksHandler.get().getStacks();
        if (slotIndex < 0 || slotIndex >= stacks.getSlots()) {
            return null;
        }

        ItemStack stack = stacks.getStackInSlot(slotIndex);
        if (stack.isEmpty() || !(stack.getItem() instanceof IMenuItem menuItem)) {
            return null;
        }

        // Use -1 for inventory slot since it's not in player inventory
        ItemMenuHost menuHost = menuItem.getMenuHost(player, -1, stack, null);

        if (hostInterface.isInstance(menuHost)) {
            return hostInterface.cast(menuHost);
        }

        return null;
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeUtf(slotType);
        buf.writeInt(slotIndex);
    }

    public static CuriosMenuLocator readFromPacket(FriendlyByteBuf buf) {
        String slotType = buf.readUtf();
        int slotIndex = buf.readInt();
        return new CuriosMenuLocator(slotType, slotIndex);
    }

    @Override
    public String toString() {
        return "CuriosMenuLocator{slotType='" + slotType + "', slotIndex=" + slotIndex + "}";
    }
}

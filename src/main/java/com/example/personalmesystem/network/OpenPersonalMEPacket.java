package com.example.personalmesystem.network;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.example.personalmesystem.item.PersonalWirelessTerminalItem;
import com.example.personalmesystem.menu.CuriosMenuLocator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

/**
 * Sent from client when player presses inventory key.
 * Server finds and opens the Personal Terminal.
 */
public class OpenPersonalMEPacket {

    public OpenPersonalMEPacket() {
    }

    public static void encode(OpenPersonalMEPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenPersonalMEPacket decode(FriendlyByteBuf buf) {
        return new OpenPersonalMEPacket();
    }

    public static void handle(OpenPersonalMEPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Check Curios first
            var curiosOpt = CuriosApi.getCuriosInventory(player).resolve();
            if (curiosOpt.isPresent()) {
                var curiosInv = curiosOpt.get();

                var stacksHandler = curiosInv.getStacksHandler("curio");
                if (stacksHandler.isPresent()) {
                    var stacks = stacksHandler.get().getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (stack.getItem() instanceof PersonalWirelessTerminalItem terminal) {
                            var locator = new CuriosMenuLocator("curio", i);
                            if (MenuOpener.open(terminal.getMenuType(), player, locator)) {
                                return;
                            }
                        }
                    }
                }
            }

            // Check inventory
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.items.size(); i++) {
                ItemStack stack = inventory.items.get(i);
                if (stack.getItem() instanceof PersonalWirelessTerminalItem terminal) {
                    var locator = MenuLocators.forInventorySlot(i);
                    if (MenuOpener.open(terminal.getMenuType(), player, locator)) {
                        return;
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

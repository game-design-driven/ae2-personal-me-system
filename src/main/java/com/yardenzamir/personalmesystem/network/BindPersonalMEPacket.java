package com.yardenzamir.personalmesystem.network;

import com.yardenzamir.personalmesystem.PersonalMESystemMod;
import com.yardenzamir.personalmesystem.item.PersonalWirelessTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

/**
 * Sent from client when player clicks bind button in WAP GUI.
 * Server links the Personal Terminal to the specified WAP.
 */
public class BindPersonalMEPacket {
    private static final String TAG_ACCESS_POINT = "accessPoint";
    private final BlockPos wapPos;

    public BindPersonalMEPacket(BlockPos wapPos) {
        this.wapPos = wapPos;
    }

    public static void encode(BindPersonalMEPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.wapPos);
    }

    public static BindPersonalMEPacket decode(FriendlyByteBuf buf) {
        return new BindPersonalMEPacket(buf.readBlockPos());
    }

    public static void handle(BindPersonalMEPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Find Personal Terminal in Curios or inventory
            ItemStack terminal = findPersonalTerminal(player);
            if (terminal == null || terminal.isEmpty()) {
                player.displayClientMessage(Component.literal("No Personal Terminal found"), true);
                PersonalMESystemMod.LOGGER.warn("[PersonalME] No terminal found for binding");
                return;
            }

            // Link directly by writing GlobalPos to NBT (same as WirelessTerminalItem)
            GlobalPos globalPos = GlobalPos.of(player.level().dimension(), msg.wapPos);
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, globalPos)
                    .result()
                    .ifPresent(tag -> terminal.getOrCreateTag().put(TAG_ACCESS_POINT, tag));

            player.displayClientMessage(Component.literal("Personal Terminal linked!"), true);
            PersonalMESystemMod.LOGGER.info("[PersonalME] Linked terminal to WAP at {}", msg.wapPos);
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack findPersonalTerminal(ServerPlayer player) {
        // Check Curios first
        var curiosOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosOpt.isPresent()) {
            var stacksHandler = curiosOpt.get().getStacksHandler("curio");
            if (stacksHandler.isPresent()) {
                var stacks = stacksHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof PersonalWirelessTerminalItem) {
                        return stack;
                    }
                }
            }
        }

        // Check inventory
        var inventory = player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (stack.getItem() instanceof PersonalWirelessTerminalItem) {
                return stack;
            }
        }

        return null;
    }
}

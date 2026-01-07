package com.yardenzamir.personalmesystem.client;

import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.client.gui.implementations.WirelessAccessPointScreen;
import appeng.menu.implementations.WirelessAccessPointMenu;
import com.yardenzamir.personalmesystem.PersonalMESystemMod;
import com.yardenzamir.personalmesystem.item.PersonalWirelessTerminalItem;
import com.yardenzamir.personalmesystem.menu.CommunicationRelayMenu;
import com.yardenzamir.personalmesystem.network.BindPersonalMEPacket;
import com.yardenzamir.personalmesystem.network.NetworkHandler;
import com.yardenzamir.personalmesystem.network.OpenPersonalMEPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import top.theillusivec4.curios.api.CuriosApi;

public class ClientSetup {

    public static void init() {
        MinecraftForge.EVENT_BUS.register(ClientSetup.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::onClientSetup);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(CommunicationRelayMenu.TYPE.get(), CommunicationRelayScreen::new);
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!hasPersonalTerminal(mc)) {
            return;
        }

        // Cancel vanilla inventory and open ME terminal instead
        event.setCanceled(true);
        NetworkHandler.sendToServer(new OpenPersonalMEPacket());
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof WirelessAccessPointScreen wapScreen)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!hasPersonalTerminal(mc)) {
            return;
        }

        WirelessAccessPointMenu menu = wapScreen.getMenu();
        var host = menu.getBlockEntity();
        if (!(host instanceof WirelessAccessPointBlockEntity wap)) {
            return;
        }

        var wapPos = wap.getBlockPos();

        int buttonSize = 16;
        int x = wapScreen.getGuiLeft() - 20;
        int y = wapScreen.getGuiTop() + 8 + 20 * 2;

        ItemStack terminalIcon = new ItemStack(PersonalMESystemMod.PERSONAL_TERMINAL.get());
        ItemIconButton bindButton = new ItemIconButton(
                x, y, buttonSize,
                terminalIcon,
                Component.translatable("gui.personalmesystem.bind_to_wap"),
                btn -> NetworkHandler.sendToServer(new BindPersonalMEPacket(wapPos))
        );

        event.addListener(bindButton);
    }

    private static boolean hasPersonalTerminal(Minecraft mc) {
        if (mc.player == null) return false;

        // Check Curios slots
        var curiosOpt = CuriosApi.getCuriosInventory(mc.player).resolve();
        if (curiosOpt.isPresent()) {
            var result = curiosOpt.get().findFirstCurio(
                    stack -> stack.getItem() instanceof PersonalWirelessTerminalItem);
            if (result.isPresent()) {
                return true;
            }
        }

        // Check inventory
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.getItem() instanceof PersonalWirelessTerminalItem) {
                return true;
            }
        }

        return false;
    }
}

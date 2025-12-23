package com.example.personalmesystem.client;

import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.client.gui.implementations.WirelessAccessPointScreen;
import appeng.menu.implementations.WirelessAccessPointMenu;
import com.example.personalmesystem.PersonalMESystemMod;
import com.example.personalmesystem.item.PersonalWirelessTerminalItem;
import com.example.personalmesystem.network.BindPersonalMEPacket;
import com.example.personalmesystem.network.NetworkHandler;
import com.example.personalmesystem.network.OpenPersonalMEPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;

public class ClientSetup {

    public static void init() {
        PersonalMESystemMod.LOGGER.info("[PersonalME] ClientSetup.init()");
        MinecraftForge.EVENT_BUS.register(ClientSetup.class);
    }

    private static Boolean cachedHasTerminal = null;
    private static int cacheTickCounter = 0;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            cachedHasTerminal = null; // Reset cache when screen changes
            return;
        }

        // Check if inventory key is being pressed
        if (!mc.options.keyInventory.isDown()) {
            return;
        }

        // Cache the terminal check for 20 ticks (1 second) to avoid lag
        cacheTickCounter++;
        if (cachedHasTerminal == null || cacheTickCounter >= 20) {
            cachedHasTerminal = hasPersonalTerminal(mc);
            cacheTickCounter = 0;
        }

        // Only intercept if player has Personal Terminal
        if (cachedHasTerminal && mc.options.keyInventory.consumeClick()) {
            NetworkHandler.sendToServer(new OpenPersonalMEPacket());
        }
        // If no terminal, don't consume - vanilla handles it
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof WirelessAccessPointScreen wapScreen)) {
            return;
        }

        // Only show button if player has a Personal Terminal
        Minecraft mc = Minecraft.getInstance();
        if (!hasPersonalTerminal(mc)) {
            return;
        }

        // Get the WAP position from the menu
        WirelessAccessPointMenu menu = wapScreen.getMenu();
        var host = menu.getBlockEntity();
        if (!(host instanceof WirelessAccessPointBlockEntity wap)) {
            return;
        }

        var wapPos = wap.getBlockPos();

        // Position in the left toolbar, below existing buttons
        // From common.json: verticalToolbar at left: -2, top: 6
        // VerticalButtonBar uses MARGIN=2, VERTICAL_SPACING=4
        // Button X = guiLeft + (-2) - 2 - 16 = guiLeft - 20
        // First button Y = guiTop + 6 + 2 = guiTop + 8
        // Each subsequent button: +16 (height) + 4 (spacing) = +20
        // WAP has 2 buttons (help, power unit), so ours is at Y = guiTop + 8 + 20*2
        int buttonSize = 16;
        int x = wapScreen.getGuiLeft() - 20;
        int y = wapScreen.getGuiTop() + 8 + 20 * 2;

        ItemStack terminalIcon = new ItemStack(PersonalMESystemMod.PERSONAL_TERMINAL.get());
        ItemIconButton bindButton = new ItemIconButton(
                x, y, buttonSize,
                terminalIcon,
                Component.literal("Bind to personal ME system"),
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

package com.yardenzamir.personalmesystem;

import appeng.menu.locator.MenuLocators;
import com.yardenzamir.personalmesystem.client.ClientSetup;
import com.yardenzamir.personalmesystem.command.PersonalMECommands;
import com.yardenzamir.personalmesystem.item.PersonalWirelessTerminalItem;
import com.yardenzamir.personalmesystem.menu.CuriosMenuLocator;
import com.yardenzamir.personalmesystem.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

@Mod(PersonalMESystemMod.MOD_ID)
public class PersonalMESystemMod {
    public static final String MOD_ID = "personalmesystem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<PersonalWirelessTerminalItem> PERSONAL_TERMINAL =
            ITEMS.register("personal_terminal", () -> new PersonalWirelessTerminalItem(new Item.Properties()));

    public PersonalMESystemMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::enqueueIMC);

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.init();
            // Register our custom menu locator for Curios slots
            MenuLocators.register(CuriosMenuLocator.class,
                    CuriosMenuLocator::writeToPacket,
                    CuriosMenuLocator::readFromPacket);
            LOGGER.info("[PersonalME] Registered CuriosMenuLocator");
        });
    }

    private void enqueueIMC(InterModEnqueueEvent event) {
        // Register Curios slot
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE,
                () -> SlotTypePreset.CURIO.getMessageBuilder().build());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PersonalMECommands.register(event.getDispatcher());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}

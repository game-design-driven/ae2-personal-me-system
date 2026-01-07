package com.yardenzamir.personalmesystem;

import appeng.menu.locator.MenuLocators;
import com.yardenzamir.personalmesystem.block.CommunicationRelayBlock;
import com.yardenzamir.personalmesystem.block.CommunicationRelayBlockEntity;
import com.yardenzamir.personalmesystem.client.ClientSetup;
import com.yardenzamir.personalmesystem.item.CommunicationRelayItem;
import com.yardenzamir.personalmesystem.item.PersonalWirelessTerminalItem;
import com.yardenzamir.personalmesystem.menu.CommunicationRelayMenu;
import com.yardenzamir.personalmesystem.menu.CuriosMenuLocator;
import com.yardenzamir.personalmesystem.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.yardenzamir.personalmesystem.config.ServerConfig;
import com.yardenzamir.personalmesystem.config.ClientConfig;
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

    // Registries
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    // Items
    public static final RegistryObject<PersonalWirelessTerminalItem> PERSONAL_TERMINAL =
            ITEMS.register("personal_terminal", () -> new PersonalWirelessTerminalItem(new Item.Properties()));

    // Blocks
    public static final RegistryObject<CommunicationRelayBlock> COMMUNICATION_RELAY_BLOCK =
            BLOCKS.register("communication_relay", CommunicationRelayBlock::new);

    // Block Items
    public static final RegistryObject<CommunicationRelayItem> COMMUNICATION_RELAY_ITEM =
            ITEMS.register("communication_relay", () ->
                    new CommunicationRelayItem(COMMUNICATION_RELAY_BLOCK.get(), new Item.Properties()));

    // Block Entities
    public static final RegistryObject<BlockEntityType<CommunicationRelayBlockEntity>> COMMUNICATION_RELAY_BE =
            BLOCK_ENTITIES.register("communication_relay", () ->
                    BlockEntityType.Builder.of(CommunicationRelayBlockEntity::create,
                            COMMUNICATION_RELAY_BLOCK.get()).build(null));

    public PersonalMESystemMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CommunicationRelayMenu.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::enqueueIMC);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.init();
            MenuLocators.register(CuriosMenuLocator.class,
                    CuriosMenuLocator::writeToPacket,
                    CuriosMenuLocator::readFromPacket);

            COMMUNICATION_RELAY_BLOCK.get().setBlockEntity(
                    CommunicationRelayBlockEntity.class,
                    COMMUNICATION_RELAY_BE.get(),
                    null,
                    null
            );
        });
    }

    private void enqueueIMC(InterModEnqueueEvent event) {
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE,
                () -> SlotTypePreset.CURIO.getMessageBuilder().build());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}

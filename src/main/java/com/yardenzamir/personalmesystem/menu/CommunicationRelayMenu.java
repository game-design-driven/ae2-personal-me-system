package com.yardenzamir.personalmesystem.menu;

import appeng.api.stacks.AEItemKey;
import com.yardenzamir.personalmesystem.PersonalMESystemMod;
import com.yardenzamir.personalmesystem.block.CommunicationRelayBlockEntity;
import com.yardenzamir.personalmesystem.virtualcraft.VirtualPatternInput;
import com.yardenzamir.personalmesystem.virtualcraft.VirtualRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu for viewing Communication Relay recipes.
 * This is a view-only menu with no slots.
 */
public class CommunicationRelayMenu extends AbstractContainerMenu {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, PersonalMESystemMod.MOD_ID);

    public static final RegistryObject<MenuType<CommunicationRelayMenu>> TYPE =
            MENUS.register("communication_relay", () ->
                    IForgeMenuType.create(CommunicationRelayMenu::fromNetwork));

    /**
     * Rich recipe display data including ItemStacks for icon rendering.
     */
    public record RichRecipeDisplay(
            List<StackDisplay> inputs,
            List<StackDisplay> outputs
    ) {}

    /**
     * Display data for a single stack (item or tag).
     */
    public record StackDisplay(
            ItemStack icon,        // Representative icon to display
            String displayText,    // Text like "64x Cobblestone" or "9x #forge:ingots/iron"
            long count,
            boolean isTag,
            @Nullable String tagId // Tag ID if this is a tag input
    ) {}

    private final List<RichRecipeDisplay> recipes;
    private final List<Component> description;  // Custom lore/description
    private final Component relayName;          // Custom name if set
    @Nullable
    private final CommunicationRelayBlockEntity blockEntity;

    // Server constructor
    public CommunicationRelayMenu(int containerId, Inventory playerInventory, CommunicationRelayBlockEntity blockEntity) {
        super(TYPE.get(), containerId);
        this.blockEntity = blockEntity;
        this.recipes = new ArrayList<>();
        this.description = new ArrayList<>();
        this.relayName = blockEntity.getCustomName() != null
                ? blockEntity.getCustomName()
                : Component.translatable("block.personalmesystem.communication_relay");

        // Load description from block entity
        this.description.addAll(blockEntity.getDescription());

        // Populate recipes from block entity
        for (VirtualRecipe recipe : blockEntity.getRecipes()) {
            List<StackDisplay> inputs = new ArrayList<>();
            for (VirtualPatternInput.InputSpec spec : recipe.inputs()) {
                inputs.add(createStackDisplay(spec));
            }

            List<StackDisplay> outputs = new ArrayList<>();
            for (var stack : recipe.outputs()) {
                if (stack.what() instanceof AEItemKey itemKey) {
                    ItemStack icon = itemKey.toStack((int) Math.min(stack.amount(), 64));
                    String text = stack.amount() + "x " + itemKey.getDisplayName().getString();
                    outputs.add(new StackDisplay(icon, text, stack.amount(), false, null));
                }
            }

            recipes.add(new RichRecipeDisplay(inputs, outputs));
        }
    }

    private StackDisplay createStackDisplay(VirtualPatternInput.InputSpec spec) {
        if (spec.isTag()) {
            // For tags, get the first item as representative icon
            var possibleItems = spec.getPossibleItems();
            ItemStack icon = possibleItems.isEmpty() ? ItemStack.EMPTY
                    : ((AEItemKey) possibleItems.get(0).what()).toStack(1);
            String tagId = spec.getItemTag().location().toString();
            String text = spec.getCount() + "x #" + tagId;
            return new StackDisplay(icon, text, spec.getCount(), true, tagId);
        } else {
            var item = spec.getSpecificItem();
            if (item != null && item.what() instanceof AEItemKey itemKey) {
                ItemStack icon = itemKey.toStack((int) Math.min(item.amount(), 64));
                String text = item.amount() + "x " + itemKey.getDisplayName().getString();
                return new StackDisplay(icon, text, item.amount(), false, null);
            }
            return new StackDisplay(ItemStack.EMPTY, "???", 0, false, null);
        }
    }

    // Client constructor (from network)
    public static CommunicationRelayMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Component relayName = buf.readComponent();

        // Read description
        int descCount = buf.readVarInt();
        List<Component> description = new ArrayList<>();
        for (int i = 0; i < descCount; i++) {
            description.add(buf.readComponent());
        }

        // Read recipes
        int recipeCount = buf.readVarInt();
        List<RichRecipeDisplay> recipes = new ArrayList<>();

        for (int i = 0; i < recipeCount; i++) {
            // Read inputs
            int inputCount = buf.readVarInt();
            List<StackDisplay> inputs = new ArrayList<>();
            for (int j = 0; j < inputCount; j++) {
                inputs.add(readStackDisplay(buf));
            }

            // Read outputs
            int outputCount = buf.readVarInt();
            List<StackDisplay> outputs = new ArrayList<>();
            for (int j = 0; j < outputCount; j++) {
                outputs.add(readStackDisplay(buf));
            }

            recipes.add(new RichRecipeDisplay(inputs, outputs));
        }

        return new CommunicationRelayMenu(containerId, playerInventory, recipes, description, relayName);
    }

    private static StackDisplay readStackDisplay(FriendlyByteBuf buf) {
        ItemStack icon = buf.readItem();
        String displayText = buf.readUtf();
        long count = buf.readVarLong();
        boolean isTag = buf.readBoolean();
        String tagId = isTag ? buf.readUtf() : null;
        return new StackDisplay(icon, displayText, count, isTag, tagId);
    }

    // Client constructor with data
    private CommunicationRelayMenu(int containerId, Inventory playerInventory,
            List<RichRecipeDisplay> recipes, List<Component> description, Component relayName) {
        super(TYPE.get(), containerId);
        this.blockEntity = null;
        this.recipes = recipes;
        this.description = description;
        this.relayName = relayName;
    }

    public void writeToBuffer(FriendlyByteBuf buf, BlockPos pos) {
        buf.writeBlockPos(pos);
        buf.writeComponent(relayName);

        // Write description
        buf.writeVarInt(description.size());
        for (Component line : description) {
            buf.writeComponent(line);
        }

        // Write recipes
        buf.writeVarInt(recipes.size());
        for (var recipe : recipes) {
            // Write inputs
            buf.writeVarInt(recipe.inputs().size());
            for (var input : recipe.inputs()) {
                writeStackDisplay(buf, input);
            }

            // Write outputs
            buf.writeVarInt(recipe.outputs().size());
            for (var output : recipe.outputs()) {
                writeStackDisplay(buf, output);
            }
        }
    }

    private void writeStackDisplay(FriendlyByteBuf buf, StackDisplay display) {
        buf.writeItem(display.icon());
        buf.writeUtf(display.displayText());
        buf.writeVarLong(display.count());
        buf.writeBoolean(display.isTag());
        if (display.isTag()) {
            buf.writeUtf(display.tagId() != null ? display.tagId() : "");
        }
    }

    public List<RichRecipeDisplay> getRecipes() {
        return recipes;
    }

    public List<Component> getDescription() {
        return description;
    }

    public Component getRelayName() {
        return relayName;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // No slots to move
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return true; // Client side
        Level level = blockEntity.getLevel();
        if (level == null) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}

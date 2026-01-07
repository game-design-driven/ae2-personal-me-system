package com.yardenzamir.personalmesystem.block;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.helpers.BaseActionSource;
import com.yardenzamir.personalmesystem.PersonalMESystemMod;
import com.yardenzamir.personalmesystem.config.ServerConfig;
import com.yardenzamir.personalmesystem.virtualcraft.VirtualPatternDetails;
import com.yardenzamir.personalmesystem.virtualcraft.VirtualPatternInput;
import com.yardenzamir.personalmesystem.virtualcraft.VirtualRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Block entity for Communication Relay.
 * Reads virtual recipes from NBT and provides them to the connected ME network.
 */
public class CommunicationRelayBlockEntity extends AENetworkBlockEntity implements ICraftingProvider, IPowerChannelState, Nameable {

    public static final int POWERED_FLAG = 1;
    public static final int CHANNEL_FLAG = 2;

    private static final String NBT_RECIPES = "virtual_recipes";
    private static final String NBT_CUSTOM_NAME = "CustomName";
    private static final String NBT_DESCRIPTION = "Description";

    private final List<VirtualPatternDetails> patterns = new ArrayList<>();
    private final List<Component> description = new ArrayList<>();
    private final IActionSource actionSource = new BaseActionSource();
    @Nullable
    private Component customName = null;
    private int clientFlags = 0;

    public CommunicationRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(ICraftingProvider.class, this);
    }

    // Factory method for deferred registration
    public static CommunicationRelayBlockEntity create(BlockPos pos, BlockState state) {
        return new CommunicationRelayBlockEntity(PersonalMESystemMod.COMMUNICATION_RELAY_BE.get(), pos, state);
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(orientation.getSide(RelativeSide.BACK));
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            markForUpdate();
        }
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        boolean c = super.readFromStream(data);
        int old = clientFlags;
        clientFlags = data.readByte();
        return old != clientFlags || c;
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        clientFlags = 0;

        getMainNode().ifPresent((grid, node) -> {
            if (grid.getEnergyService().isNetworkPowered()) {
                clientFlags |= POWERED_FLAG;
            }
            if (node.meetsChannelRequirements()) {
                clientFlags |= CHANNEL_FLAG;
            }
        });

        data.writeByte((byte) clientFlags);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        loadRecipesFromTag(tag);
        loadCustomDataFromTag(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveRecipesToTag(tag);
        saveCustomDataToTag(tag);
    }

    private void loadCustomDataFromTag(CompoundTag tag) {
        // Load custom name
        if (tag.contains(NBT_CUSTOM_NAME, Tag.TAG_STRING)) {
            this.customName = Component.Serializer.fromJson(tag.getString(NBT_CUSTOM_NAME));
        } else {
            this.customName = null;
        }

        // Load description
        this.description.clear();
        if (tag.contains(NBT_DESCRIPTION, Tag.TAG_LIST)) {
            ListTag descList = tag.getList(NBT_DESCRIPTION, Tag.TAG_STRING);
            for (int i = 0; i < descList.size(); i++) {
                String json = descList.getString(i);
                Component line = Component.Serializer.fromJson(json);
                if (line != null) {
                    this.description.add(line);
                }
            }
        }
    }

    private void saveCustomDataToTag(CompoundTag tag) {
        // Save custom name
        if (this.customName != null) {
            tag.putString(NBT_CUSTOM_NAME, Component.Serializer.toJson(this.customName));
        }

        // Save description
        if (!this.description.isEmpty()) {
            ListTag descList = new ListTag();
            for (Component line : this.description) {
                descList.add(StringTag.valueOf(Component.Serializer.toJson(line)));
            }
            tag.put(NBT_DESCRIPTION, descList);
        }
    }

    private void loadRecipesFromTag(CompoundTag tag) {
        patterns.clear();

        if (!tag.contains(NBT_RECIPES, Tag.TAG_LIST)) {
            return;
        }

        ListTag recipeList = tag.getList(NBT_RECIPES, Tag.TAG_COMPOUND);
        for (int i = 0; i < recipeList.size(); i++) {
            try {
                CompoundTag recipeTag = recipeList.getCompound(i);
                VirtualRecipe recipe = parseRecipe(recipeTag, i);
                if (recipe != null) {
                    patterns.add(new VirtualPatternDetails(recipe));
                }
            } catch (Exception e) {
                PersonalMESystemMod.LOGGER.warn("Failed to parse virtual recipe at index {}: {}", i, e.getMessage());
            }
        }

        // Notify grid of pattern change
        getMainNode().ifPresent((grid, node) -> {
            grid.getCraftingService().refreshNodeCraftingProvider(node);
        });
    }

    private void saveRecipesToTag(CompoundTag tag) {
        ListTag recipeList = new ListTag();
        for (VirtualPatternDetails pattern : patterns) {
            recipeList.add(serializeRecipe(pattern.getRecipe()));
        }
        tag.put(NBT_RECIPES, recipeList);
    }

    private VirtualRecipe parseRecipe(CompoundTag tag, int index) {
        ListTag inputsTag = tag.getList("inputs", Tag.TAG_COMPOUND);
        ListTag outputsTag = tag.getList("outputs", Tag.TAG_COMPOUND);

        if (inputsTag.isEmpty() || outputsTag.isEmpty()) {
            return null;
        }

        List<VirtualPatternInput.InputSpec> inputs = new ArrayList<>();
        List<GenericStack> outputs = new ArrayList<>();

        for (int i = 0; i < inputsTag.size(); i++) {
            VirtualPatternInput.InputSpec spec = parseInputSpec(inputsTag.getCompound(i));
            if (spec != null) {
                inputs.add(spec);
            }
        }

        for (int i = 0; i < outputsTag.size(); i++) {
            GenericStack stack = parseStack(outputsTag.getCompound(i));
            if (stack != null) {
                outputs.add(stack);
            }
        }

        if (inputs.isEmpty() || outputs.isEmpty()) {
            return null;
        }

        // Generate a unique ID based on block position and index
        ResourceLocation id = new ResourceLocation(PersonalMESystemMod.MOD_ID,
                "relay_" + getBlockPos().toShortString().replace(", ", "_") + "_" + index);

        return new VirtualRecipe(id, inputs, outputs);
    }

    /**
     * Parse an input specification. Supports:
     * - {item:"minecraft:diamond", count:1} - specific item
     * - {item:"minecraft:diamond", count:1, nbt:{...}} - item with NBT
     * - {tag:"forge:ingots/iron", count:1} - any item matching tag
     */
    private VirtualPatternInput.InputSpec parseInputSpec(CompoundTag tag) {
        int count = tag.getInt("count");
        if (count <= 0) count = 1;

        // Check for tag-based input first
        if (tag.contains("tag", Tag.TAG_STRING)) {
            String tagId = tag.getString("tag");
            ResourceLocation tagLoc = new ResourceLocation(tagId);
            TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagLoc);
            return new VirtualPatternInput.InputSpec(itemTag, count);
        }

        // Otherwise, parse as specific item
        GenericStack stack = parseStack(tag);
        if (stack == null) return null;

        return new VirtualPatternInput.InputSpec(stack);
    }

    /**
     * Parse an output stack. Supports:
     * - {item:"minecraft:diamond", count:1} - specific item
     * - {item:"minecraft:diamond", count:1, nbt:{...}} - item with NBT
     */
    private GenericStack parseStack(CompoundTag tag) {
        String itemId = tag.getString("item");
        int count = tag.getInt("count");
        if (count <= 0) count = 1;

        ResourceLocation itemLoc = new ResourceLocation(itemId);
        var item = ForgeRegistries.ITEMS.getValue(itemLoc);
        if (item == null) {
            return null;
        }

        // Check for NBT data
        AEItemKey key;
        if (tag.contains("nbt", Tag.TAG_COMPOUND)) {
            ItemStack stack = new ItemStack(item, 1);
            stack.setTag(tag.getCompound("nbt"));
            key = AEItemKey.of(stack);
        } else {
            key = AEItemKey.of(item);
        }

        return new GenericStack(key, count);
    }

    private CompoundTag serializeRecipe(VirtualRecipe recipe) {
        CompoundTag tag = new CompoundTag();

        ListTag inputsTag = new ListTag();
        for (VirtualPatternInput.InputSpec input : recipe.inputs()) {
            inputsTag.add(serializeInputSpec(input));
        }
        tag.put("inputs", inputsTag);

        ListTag outputsTag = new ListTag();
        for (GenericStack output : recipe.outputs()) {
            outputsTag.add(serializeStack(output));
        }
        tag.put("outputs", outputsTag);

        return tag;
    }

    private CompoundTag serializeInputSpec(VirtualPatternInput.InputSpec spec) {
        CompoundTag tag = new CompoundTag();
        if (spec.isTag()) {
            tag.putString("tag", spec.getItemTag().location().toString());
        } else if (spec.getSpecificItem() != null) {
            return serializeStack(spec.getSpecificItem());
        }
        tag.putInt("count", (int) spec.getCount());
        return tag;
    }

    private CompoundTag serializeStack(GenericStack stack) {
        CompoundTag tag = new CompoundTag();
        if (stack.what() instanceof AEItemKey itemKey) {
            tag.putString("item", ForgeRegistries.ITEMS.getKey(itemKey.getItem()).toString());
            // Preserve NBT data if present
            if (itemKey.hasTag()) {
                tag.put("nbt", itemKey.getTag().copy());
            }
        }
        tag.putInt("count", (int) stack.amount());
        return tag;
    }

    // ICraftingProvider implementation

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return Collections.unmodifiableList(new ArrayList<>(patterns));
    }

    @Override
    public int getPatternPriority() {
        return Integer.MAX_VALUE; // Virtual recipes have highest priority
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!(patternDetails instanceof VirtualPatternDetails vp)) {
            return false;
        }

        var grid = getMainNode().getGrid();
        if (grid == null) {
            return false;
        }

        // Check power usage
        double powerCost = ServerConfig.POWER_USAGE.get();
        if (powerCost > 0) {
            double extracted = grid.getEnergyService().extractAEPower(powerCost, Actionable.MODULATE, appeng.api.config.PowerMultiplier.CONFIG);
            if (extracted < powerCost) {
                return false;
            }
        }

        var storage = grid.getStorageService().getInventory();

        for (GenericStack output : vp.getOutputs()) {
            storage.insert(output.what(), output.amount(), Actionable.MODULATE, actionSource);
        }

        return true;
    }

    @Override
    public boolean isBusy() {
        return false; // Virtual crafting is instant
    }

    @Override
    public Set<AEKey> getEmitableItems() {
        return Set.of();
    }

    // IPowerChannelState implementation

    @Override
    public boolean isActive() {
        if (isClientSide()) {
            return isPowered() && (clientFlags & CHANNEL_FLAG) == CHANNEL_FLAG;
        }
        return getMainNode().isOnline();
    }

    @Override
    public boolean isPowered() {
        return (clientFlags & POWERED_FLAG) == POWERED_FLAG;
    }

    /**
     * Get the list of recipes stored in this relay.
     */
    public List<VirtualRecipe> getRecipes() {
        return patterns.stream()
                .map(VirtualPatternDetails::getRecipe)
                .toList();
    }

    /**
     * Parse recipes from an item's BlockEntityTag for tooltip display.
     * Used by the item tooltip handler.
     */
    public static List<RecipeDisplay> parseRecipesFromItemTag(CompoundTag blockEntityTag) {
        List<RecipeDisplay> recipes = new ArrayList<>();

        if (!blockEntityTag.contains(NBT_RECIPES, Tag.TAG_LIST)) {
            return recipes;
        }

        ListTag recipeList = blockEntityTag.getList(NBT_RECIPES, Tag.TAG_COMPOUND);
        for (int i = 0; i < recipeList.size(); i++) {
            try {
                CompoundTag recipeTag = recipeList.getCompound(i);
                RecipeDisplay display = parseRecipeDisplay(recipeTag);
                if (display != null) {
                    recipes.add(display);
                }
            } catch (Exception ignored) {
            }
        }

        return recipes;
    }

    private static RecipeDisplay parseRecipeDisplay(CompoundTag tag) {
        ListTag inputsTag = tag.getList("inputs", Tag.TAG_COMPOUND);
        ListTag outputsTag = tag.getList("outputs", Tag.TAG_COMPOUND);

        if (inputsTag.isEmpty() || outputsTag.isEmpty()) {
            return null;
        }

        List<String> inputs = new ArrayList<>();
        List<String> outputs = new ArrayList<>();

        for (int i = 0; i < inputsTag.size(); i++) {
            CompoundTag inputTag = inputsTag.getCompound(i);
            inputs.add(parseStackDisplay(inputTag));
        }

        for (int i = 0; i < outputsTag.size(); i++) {
            CompoundTag outputTag = outputsTag.getCompound(i);
            outputs.add(parseStackDisplay(outputTag));
        }

        return new RecipeDisplay(inputs, outputs);
    }

    private static String parseStackDisplay(CompoundTag tag) {
        int count = tag.getInt("count");
        if (count <= 0) count = 1;

        // Check for tag-based input
        if (tag.contains("tag", Tag.TAG_STRING)) {
            return count + "x #" + tag.getString("tag");
        }

        // Specific item
        String itemId = tag.getString("item");
        ResourceLocation itemLoc = new ResourceLocation(itemId);
        var item = ForgeRegistries.ITEMS.getValue(itemLoc);
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            if (tag.contains("nbt", Tag.TAG_COMPOUND)) {
                stack.setTag(tag.getCompound("nbt"));
            }
            return count + "x " + stack.getHoverName().getString();
        }
        return count + "x " + itemId;
    }

    /**
     * Simple record for displaying recipes in tooltips.
     */
    public record RecipeDisplay(List<String> inputs, List<String> outputs) {}

    // Nameable implementation

    @Override
    public Component getName() {
        return customName != null ? customName : Component.translatable("block.personalmesystem.communication_relay");
    }

    @Override
    @Nullable
    public Component getCustomName() {
        return customName;
    }

    public void setCustomName(@Nullable Component name) {
        this.customName = name;
    }

    /**
     * Get the description/lore lines for this relay.
     */
    public List<Component> getDescription() {
        return Collections.unmodifiableList(description);
    }

    /**
     * Set the description/lore lines for this relay.
     */
    public void setDescription(List<Component> lines) {
        this.description.clear();
        if (lines != null) {
            this.description.addAll(lines);
        }
    }
}

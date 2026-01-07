package com.yardenzamir.personalmesystem.virtualcraft;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of IPatternDetails.IInput for virtual recipes.
 * Supports both specific items (with optional NBT) and item tags.
 */
public class VirtualPatternInput implements IPatternDetails.IInput {

    /**
     * Represents an input specification - either a specific item or a tag.
     */
    public static class InputSpec {
        private final GenericStack specificItem; // For specific item inputs
        private final TagKey<Item> itemTag; // For tag-based inputs
        private final long count;

        // Constructor for specific item
        public InputSpec(GenericStack stack) {
            this.specificItem = stack;
            this.itemTag = null;
            this.count = stack.amount();
        }

        // Constructor for tag-based input
        public InputSpec(TagKey<Item> tag, long count) {
            this.specificItem = null;
            this.itemTag = tag;
            this.count = count;
        }

        public boolean isTag() {
            return itemTag != null;
        }

        @Nullable
        public GenericStack getSpecificItem() {
            return specificItem;
        }

        @Nullable
        public TagKey<Item> getItemTag() {
            return itemTag;
        }

        public long getCount() {
            return count;
        }

        /**
         * Get all possible items for this input spec.
         * For specific items, returns just that item.
         * For tags, returns all items in the tag.
         */
        public List<GenericStack> getPossibleItems() {
            List<GenericStack> result = new ArrayList<>();
            if (specificItem != null) {
                result.add(new GenericStack(specificItem.what(), 1));
            } else if (itemTag != null) {
                // Resolve all items in the tag
                var tagItems = ForgeRegistries.ITEMS.tags();
                if (tagItems != null) {
                    var tag = tagItems.getTag(itemTag);
                    for (Item item : tag) {
                        result.add(new GenericStack(AEItemKey.of(item), 1));
                    }
                }
            }
            return result;
        }

        /**
         * Check if an item matches this input spec.
         */
        public boolean matches(AEKey input) {
            if (!(input instanceof AEItemKey itemKey)) {
                return false;
            }

            if (specificItem != null) {
                // For specific items with NBT, do exact match
                return specificItem.what().equals(input);
            } else if (itemTag != null) {
                // For tags, check if item is in tag
                return itemKey.getItem().builtInRegistryHolder().is(itemTag);
            }
            return false;
        }

        /**
         * Get display representation for tooltips.
         */
        public String getDisplayString() {
            if (specificItem != null) {
                if (specificItem.what() instanceof AEItemKey itemKey) {
                    return count + "x " + itemKey.getDisplayName().getString();
                }
                return count + "x " + specificItem.what().toString();
            } else if (itemTag != null) {
                return count + "x #" + itemTag.location();
            }
            return "???";
        }
    }

    private final GenericStack[] possibleInputs;
    private final InputSpec spec;

    public VirtualPatternInput(InputSpec spec) {
        this.spec = spec;
        List<GenericStack> possible = spec.getPossibleItems();
        if (possible.isEmpty()) {
            // Fallback: if tag has no items, create a dummy entry
            this.possibleInputs = new GenericStack[0];
        } else {
            this.possibleInputs = possible.toArray(GenericStack[]::new);
        }
    }

    public InputSpec getSpec() {
        return spec;
    }

    @Override
    public GenericStack[] getPossibleInputs() {
        return possibleInputs;
    }

    @Override
    public long getMultiplier() {
        return spec.getCount();
    }

    @Override
    public boolean isValid(AEKey input, Level level) {
        return spec.matches(input);
    }

    @Override
    @Nullable
    public AEKey getRemainingKey(AEKey template) {
        // Virtual recipes don't have container items
        return null;
    }
}

package com.yardenzamir.personalmesystem.item;

import com.yardenzamir.personalmesystem.block.CommunicationRelayBlockEntity;
import com.yardenzamir.personalmesystem.config.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommunicationRelayItem extends BlockItem {

    public CommunicationRelayItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (!ClientConfig.SHOW_RECIPE_TOOLTIPS.get()) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("BlockEntityTag")) {
            tooltip.add(Component.translatable("tooltip.personalmesystem.no_recipes").withStyle(ChatFormatting.GRAY));
            return;
        }

        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
        List<CommunicationRelayBlockEntity.RecipeDisplay> recipes =
                CommunicationRelayBlockEntity.parseRecipesFromItemTag(blockEntityTag);

        if (recipes.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.personalmesystem.no_recipes").withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("tooltip.personalmesystem.recipes_count", recipes.size()).withStyle(ChatFormatting.GOLD));

        int maxToShow = ClientConfig.MAX_TOOLTIP_RECIPES.get();
        if (maxToShow == 0) maxToShow = recipes.size();

        int shown = 0;
        for (CommunicationRelayBlockEntity.RecipeDisplay recipe : recipes) {
            if (shown >= maxToShow) {
                int remaining = recipes.size() - shown;
                tooltip.add(Component.translatable("tooltip.personalmesystem.and_more", remaining)
                        .withStyle(ChatFormatting.DARK_GRAY));
                break;
            }

            StringBuilder sb = new StringBuilder("  ");
            sb.append(String.join(" + ", recipe.inputs()));
            sb.append(" -> ");
            sb.append(String.join(", ", recipe.outputs()));

            tooltip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
            shown++;
        }
    }
}

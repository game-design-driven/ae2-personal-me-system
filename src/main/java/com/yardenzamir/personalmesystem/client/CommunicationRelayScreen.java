package com.yardenzamir.personalmesystem.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yardenzamir.personalmesystem.menu.CommunicationRelayMenu;
import com.yardenzamir.personalmesystem.menu.CommunicationRelayMenu.RichRecipeDisplay;
import com.yardenzamir.personalmesystem.menu.CommunicationRelayMenu.StackDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class CommunicationRelayScreen extends AbstractContainerScreen<CommunicationRelayMenu> {

    private static final int ROW_HEIGHT = 20;
    private static final int ICON_SIZE = 16;
    private static final int DESC_LINE_HEIGHT = 10;
    private static final int PADDING = 8;
    private static final int MAX_WIDTH = 250;
    private static final int MIN_WIDTH = 120;

    private List<FormattedCharSequence> wrappedDesc;
    private int contentWidth;

    public CommunicationRelayScreen(CommunicationRelayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = PADDING;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        List<Component> desc = menu.getDescription();
        List<RichRecipeDisplay> recipes = menu.getRecipes();

        // Calculate width based on recipes
        int recipeWidth = MIN_WIDTH;
        for (RichRecipeDisplay recipe : recipes) {
            recipeWidth = Math.max(recipeWidth, calculateRowWidth(recipe));
        }
        contentWidth = Math.min(MAX_WIDTH, recipeWidth + PADDING * 2);

        // Wrap description text to fit
        int textWidth = contentWidth - PADDING * 2;
        wrappedDesc = new ArrayList<>();
        for (Component line : desc) {
            wrappedDesc.addAll(font.split(line, textWidth));
        }

        // Calculate height
        int h = 24; // title
        if (!wrappedDesc.isEmpty()) {
            h += wrappedDesc.size() * DESC_LINE_HEIGHT + 6;
        }
        if (!recipes.isEmpty()) {
            h += 14 + 6 + recipes.size() * ROW_HEIGHT;
        } else {
            h += 12;
        }
        h += PADDING;

        this.imageWidth = contentWidth;
        this.imageHeight = h;
        this.inventoryLabelY = this.imageHeight + 100;

        super.init();
    }

    private int calculateRowWidth(RichRecipeDisplay recipe) {
        int w = 0;
        for (int i = 0; i < recipe.inputs().size(); i++) {
            if (i > 0) w += 10;
            w += ICON_SIZE + 2;
        }
        w += 20; // arrow
        for (int i = 0; i < recipe.outputs().size(); i++) {
            if (i > 0) w += 2;
            w += ICON_SIZE + 2;
        }
        return w;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        graphics.fill(x + 4, y + 18, x + imageWidth - 4, y + imageHeight - 4, 0xFF8B8B8B);
        graphics.fill(x + 5, y + 19, x + imageWidth - 5, y + imageHeight - 5, 0xFF373737);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title - truncate if needed
        String title = menu.getRelayName().getString();
        int maxTitleWidth = imageWidth - PADDING * 2;
        if (font.width(title) > maxTitleWidth) {
            title = font.plainSubstrByWidth(title, maxTitleWidth - font.width("...")) + "...";
        }
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);

        List<RichRecipeDisplay> recipes = menu.getRecipes();
        int y = 24;

        // Wrapped description
        for (FormattedCharSequence line : wrappedDesc) {
            graphics.drawString(font, line, PADDING, y, 0xAAAAAA, false);
            y += DESC_LINE_HEIGHT;
        }
        if (!wrappedDesc.isEmpty()) y += 6;

        // Recipes
        if (recipes.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.personalmesystem.no_recipes"), PADDING, y, 0xAAAAAA, false);
            return;
        }

        graphics.drawString(font, Component.translatable("gui.personalmesystem.recipes_count", recipes.size()), PADDING, y, 0xFFD700, false);
        y += 14;
        graphics.fill(PADDING, y, imageWidth - PADDING, y + 1, 0xFF555555);
        y += 6;

        for (RichRecipeDisplay recipe : recipes) {
            renderRecipeRow(graphics, recipe, PADDING, y);
            y += ROW_HEIGHT;
        }
    }

    private void renderRecipeRow(GuiGraphics graphics, RichRecipeDisplay recipe, int x, int y) {
        for (int i = 0; i < recipe.inputs().size(); i++) {
            if (i > 0) {
                graphics.drawString(font, "+", x, y + 4, 0xFFFFFF, false);
                x += 10;
            }
            renderIcon(graphics, recipe.inputs().get(i), x, y);
            x += ICON_SIZE + 2;
        }

        x += 4;
        graphics.drawString(font, "->", x, y + 4, 0x00FF00, false);
        x += 16;

        for (int i = 0; i < recipe.outputs().size(); i++) {
            if (i > 0) x += 2;
            renderIcon(graphics, recipe.outputs().get(i), x, y);
            x += ICON_SIZE + 2;
        }
    }

    private void renderIcon(GuiGraphics graphics, StackDisplay stack, int x, int y) {
        if (!stack.icon().isEmpty()) {
            // renderLabels has pose already translated, so use local coords
            graphics.renderItem(stack.icon(), x, y);
            if (stack.count() > 1) {
                String count = stack.count() > 999 ? (stack.count() / 1000) + "k" : String.valueOf(stack.count());
                graphics.renderItemDecorations(font, stack.icon(), x, y, count);
            }
            if (stack.isTag()) {
                graphics.drawString(font, "#", x, y, 0xFFFF00, true);
            }
        } else {
            graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF444444);
        }
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        List<RichRecipeDisplay> recipes = menu.getRecipes();
        if (recipes.isEmpty()) return;

        int y = 24;
        if (!wrappedDesc.isEmpty()) y += wrappedDesc.size() * DESC_LINE_HEIGHT + 6;
        y += 20;

        int relX = mouseX - leftPos, relY = mouseY - topPos;

        for (RichRecipeDisplay recipe : recipes) {
            int x = PADDING;
            for (int i = 0; i < recipe.inputs().size(); i++) {
                if (i > 0) x += 10;
                if (isOver(relX, relY, x, y)) {
                    showTooltip(graphics, recipe.inputs().get(i), mouseX, mouseY);
                    return;
                }
                x += ICON_SIZE + 2;
            }
            x += 20;
            for (int i = 0; i < recipe.outputs().size(); i++) {
                if (i > 0) x += 2;
                if (isOver(relX, relY, x, y)) {
                    showTooltip(graphics, recipe.outputs().get(i), mouseX, mouseY);
                    return;
                }
                x += ICON_SIZE + 2;
            }
            y += ROW_HEIGHT;
        }
    }

    private boolean isOver(int mx, int my, int x, int y) {
        return mx >= x && mx < x + ICON_SIZE && my >= y && my < y + ICON_SIZE;
    }

    private void showTooltip(GuiGraphics graphics, StackDisplay stack, int mx, int my) {
        List<Component> tip = new ArrayList<>();
        tip.add(stack.icon().isEmpty() ? Component.literal(stack.displayText()) : stack.icon().getHoverName());
        tip.add(Component.translatable("gui.personalmesystem.amount", stack.count()).withStyle(ChatFormatting.GRAY));
        if (stack.isTag() && stack.tagId() != null) {
            tip.add(Component.translatable("gui.personalmesystem.tag", stack.tagId()).withStyle(ChatFormatting.YELLOW));
        }
        graphics.renderTooltip(font, tip, java.util.Optional.empty(), mx, my);
    }
}

package com.yardenzamir.personalmesystem.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A button that displays an item icon, styled like AE2 toolbar buttons.
 */
public class ItemIconButton extends Button {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ae2", "textures/guis/states.png");
    private final ItemStack icon;

    public ItemIconButton(int x, int y, int size, ItemStack icon, Component tooltip, OnPress onPress) {
        super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();

        // Draw AE2-style button background (from states.png, the toolbar button background is at 240,240 16x16)
        guiGraphics.blit(TEXTURE, getX(), getY(), 240, 240, 16, 16);

        // Draw hover highlight
        if (isHovered) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x40FFFFFF);
        }

        // Draw item icon centered
        guiGraphics.renderItem(icon, getX(), getY());

        RenderSystem.enableDepthTest();
    }
}

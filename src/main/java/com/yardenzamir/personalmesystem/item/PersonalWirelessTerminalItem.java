package com.yardenzamir.personalmesystem.item;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.yardenzamir.personalmesystem.host.PersonalTerminalMenuHost;
import com.yardenzamir.personalmesystem.menu.CuriosMenuLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Personal wireless terminal that can be worn as a Curio.
 * Unlike the regular wireless terminal:
 * - Can be opened even when not linked
 * - Never closes when out of range (shows grayed out instead)
 */
public class PersonalWirelessTerminalItem extends WirelessTerminalItem implements ICurioItem {

    public PersonalWirelessTerminalItem(Item.Properties props) {
        super(() -> 1600000.0, props.stacksTo(1));
    }

    @Override
    protected boolean checkPreconditions(ItemStack item, Player player) {
        // Always allow opening - no link or power check
        return !item.isEmpty() && item.getItem() == this;
    }

    @Override
    public boolean hasPower(Player player, double amt, ItemStack is) {
        // Always has power
        return true;
    }

    @Override
    public boolean usePower(Player player, double amount, ItemStack is) {
        // Never actually consume power
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var is = player.getItemInHand(hand);

        // Only open on server side
        if (!level.isClientSide() && checkPreconditions(is, player)) {
            MenuOpener.open(getMenuType(), player, MenuLocators.forHand(player, hand));
        }

        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()), is);
    }

    @Override
    @Nullable
    public IGrid getLinkedGrid(ItemStack item, Level level, @Nullable Player sendMessagesTo) {
        // Never send messages to player - suppress "device not linked" etc.
        return super.getLinkedGrid(item, level, null);
    }

    @Override
    @Nullable
    public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        // Use our custom host that never closes
        // inventorySlot of -1 means item is in Curios, not player inventory
        Integer slot = inventorySlot >= 0 ? inventorySlot : null;
        return new PersonalTerminalMenuHost(player, slot, stack,
                (p, subMenu) -> {
                    if (slot != null) {
                        // Item is in player inventory - reopen from there
                        openFromInventory(p, slot, true);
                    } else {
                        // Item is in Curios - find it and reopen from there
                        openFromCurios(p);
                    }
                });
    }

    /**
     * Try to open the terminal from a Curios slot by searching for it.
     */
    private void openFromCurios(Player player) {
        var curiosOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosOpt.isEmpty()) {
            return;
        }

        var curios = curiosOpt.get();
        // Search all curios slots for our terminal
        for (var handler : curios.getCurios().entrySet()) {
            var slotType = handler.getKey();
            var stacks = handler.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                var stack = stacks.getStackInSlot(i);
                if (stack.getItem() == this) {
                    // Found it - open from this slot
                    var locator = new CuriosMenuLocator(slotType, i);
                    MenuOpener.open(getMenuType(), player, locator, true);
                    return;
                }
            }
        }
    }

    // ICurioItem implementation
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}

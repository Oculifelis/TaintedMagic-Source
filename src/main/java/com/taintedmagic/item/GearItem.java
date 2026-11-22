package com.taintedmagic.item;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import com.taintedmagic.TMDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * Armour, goggles and baubles. Everything they do is read from components on the stack (vis discount, self repair,
 * damage ward, Thaumaturge's warp/runic/revealing), so a datapack or a recipe patch can change any piece.
 */
public class GearItem extends Item implements IVisDiscountGear {
    public GearItem(Properties props) {
        super(props);
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return stack.getOrDefault(TMDataComponents.VIS_DISCOUNT.get(), 0);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        Integer every = stack.get(TMDataComponents.SELF_REPAIR.get());
        if (every != null && stack.isDamaged() && owner.tickCount % every == 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        int discount = getVisDiscount(stack);
        if (discount > 0) {
            out.accept(Component.translatable("tooltip.taintedmagic.vis_discount", discount).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (stack.has(TMDataComponents.SELF_REPAIR.get())) {
            out.accept(Component.translatable("tooltip.taintedmagic.self_repair").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

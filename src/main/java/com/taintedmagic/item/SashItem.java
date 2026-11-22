package com.taintedmagic.item;

import com.taintedmagic.TMDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Voidwalker's Sash: a Curios belt. Sneak-use flips its speed mode, which only matters with Voidwalker boots on. */
public class SashItem extends GearItem {
    public SashItem(Properties props) {
        super(props);
    }

    public static boolean speedMode(ItemStack stack) {
        return stack.getOrDefault(TMDataComponents.SASH_SPEED.get(), true);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            boolean on = !speedMode(stack);
            stack.set(TMDataComponents.SASH_SPEED.get(), on);
            player.sendOverlayMessage(Component.translatable(on ? "tooltip.taintedmagic.sash.on" : "tooltip.taintedmagic.sash.off"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        boolean on = speedMode(stack);
        out.accept(Component.translatable("tooltip.taintedmagic.sash.mode").withStyle(ChatFormatting.GRAY)
                .append(" ").append(Component.translatable(on ? "tooltip.taintedmagic.sash.on" : "tooltip.taintedmagic.sash.off")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)));
        super.appendHoverText(stack, context, display, out, flag);
    }
}

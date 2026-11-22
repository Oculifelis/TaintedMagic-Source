package com.taintedmagic.item;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.taintedmagic.TMDataComponents;
import com.taintedmagic.TMDataComponents.MiningModes;
import com.taintedmagic.data.TMTuning;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The Thaumic Disassembler: one tool for every block. Sneak-use cycles its mining speed through the stack's
 * {@code taintedmagic:mining_modes} list (rewriting the tool component), and it mends itself by drawing perditio vis
 * from a wand on the hotbar.
 */
public class DisassemblerItem extends WeaponItem {
    public DisassemblerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MiningModes modes = stack.get(TMDataComponents.MINING_MODES.get());
        if (!player.isShiftKeyDown() || modes == null || modes.speeds().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            MiningModes next = modes.next();
            stack.set(TMDataComponents.MINING_MODES.get(), next);
            Tool tool = stack.get(DataComponents.TOOL);
            if (tool != null) {
                stack.set(DataComponents.TOOL, new Tool(tool.rules().stream()
                        .map(r -> r.speed().isPresent() ? new Tool.Rule(r.blocks(), java.util.Optional.of(next.speed()), r.correctForDrops()) : r)
                        .toList(), tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
            }
            player.sendOverlayMessage(Component.translatable("tooltip.taintedmagic.disassembler.mode", formatSpeed(next.speed())));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        int every = (int) TMTuning.get("disassembler.repair_interval_ticks");
        if (owner instanceof Player player && stack.isDamaged() && owner.tickCount % every == 0
                && WandVisHelper.consumeVisFromHotbar(player, (float) TMTuning.get("disassembler.repair_vis"), TTAspects.PERDITIO, false)) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        MiningModes modes = stack.get(TMDataComponents.MINING_MODES.get());
        if (modes != null && !modes.speeds().isEmpty()) {
            out.accept(Component.translatable("tooltip.taintedmagic.disassembler.mode", formatSpeed(modes.speed())).withStyle(ChatFormatting.GRAY));
        }
        out.accept(Component.translatable("tooltip.taintedmagic.disassembler").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String formatSpeed(float speed) {
        return speed == (int) speed ? Integer.toString((int) speed) : Float.toString(speed);
    }
}

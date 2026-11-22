package com.taintedmagic.event;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import com.taintedmagic.TMDataComponents;
import com.taintedmagic.TMDataComponents.AspectReward;
import com.taintedmagic.item.GearItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Component-driven behaviour on items we do not own: the Magic Funguar's aspect reward, and Voidsent Blood's
 * self repair on Thaumaturge's cult attire (any stack with {@code taintedmagic:self_repair} that is not one of our
 * {@link GearItem}s, which repair themselves).
 */
public final class ConsumeEvents {
    private ConsumeEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ConsumeEvents::onFinishUsing);
        NeoForge.EVENT_BUS.addListener(ConsumeEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ConsumeEvents::onTooltip);
    }

    private static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        AspectReward reward = event.getItem().get(TMDataComponents.ASPECT_REWARD.get());
        if (reward == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceKey<IAspect> pick = reward.pool().get(player.getRandom().nextInt(reward.pool().size()));
        player.level().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(pick)
                .ifPresent(aspect -> AspectPoolAccess.grant(player, aspect, reward.amount()));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            Integer every = stack.get(TMDataComponents.SELF_REPAIR.get());
            if (every != null && stack.isDamaged() && !(stack.getItem() instanceof GearItem) && player.tickCount % every == 0) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.has(TMDataComponents.SELF_REPAIR.get()) && !(stack.getItem() instanceof GearItem)) {
            event.getToolTip().add(Component.translatable("tooltip.taintedmagic.voidtouched").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}

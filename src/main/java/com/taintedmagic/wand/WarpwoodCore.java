package com.taintedmagic.wand;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.taintedmagic.data.TMTuning;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Warpwood feeds on its holder's corruption: every primal gains a little vis on a timer that shortens the more
 * permanent warp the holder carries. A warp ward (which shields the mind from warp) also starves the wood.
 */
public final class WarpwoodCore implements IWandRodOnUpdate {
    @Override
    public void onUpdate(ItemStack wand, Player player) {
        if (player.level().isClientSide() || player.hasEffect(TTMobEffects.WARP_WARD)) {
            return;
        }
        int permanent = WarpHelper.getWarp(player).get(WarpType.PERMANENT);
        int base = (int) TMTuning.get("warpwood_core.base_interval_ticks");
        int interval = Math.max(base - Math.max(1, permanent), (int) TMTuning.get("warpwood_core.min_interval_ticks"));
        if (player.tickCount % interval != 0) {
            return;
        }
        int amount = (int) TMTuning.get("warpwood_core.vis_per_refill");
        int max = WandVisHelper.capacityOf(wand);
        for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
            if (WandVisHelper.storedIn(wand, primal) < max) {
                WandVisHelper.topUp(wand, primal, amount, true);
            }
        }
    }
}

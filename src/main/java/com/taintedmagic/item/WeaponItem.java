package com.taintedmagic.item;

import com.taintedmagic.TMDataComponents;
import com.taintedmagic.TMDataComponents.HitEffect;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Blades and tools. On top of {@link GearItem}'s self repair and tooltip, every hit applies the stack's
 * {@code taintedmagic:on_hit} effects, so a datapack or a recipe patch decides what a weapon inflicts.
 */
public class WeaponItem extends GearItem {
    public WeaponItem(Properties props) {
        super(props);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        applyOnHit(stack, target, attacker);
    }

    public static void applyOnHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide()) {
            return;
        }
        for (HitEffect e : stack.getOrDefault(TMDataComponents.ON_HIT.get(), List.<HitEffect>of())) {
            target.addEffect(new MobEffectInstance(e.effect(), e.ticks(), e.amplifier()), attacker);
        }
    }
}

package com.taintedmagic.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.taintedmagic.event.ToolEvents;
import net.minecraft.world.entity.item.ItemEntity;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Mage's mace: a heavy physical blow per power level, best with the touch delivery. With the {@code bloodlust}
 * setting on, the caster heals a share of the damage dealt.
 */
public final class MaceEffect extends AbstractEffectBehavior {
    public static final MapCodec<MaceEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 100).optionalFieldOf("damage_per_power", 3F).forGetter(e -> e.damagePerPower),
            Codec.floatRange(0, 10).optionalFieldOf("knockback", 0.6F).forGetter(e -> e.knockback),
            Codec.floatRange(0, 1).optionalFieldOf("bloodlust_heal", 0.25F).forGetter(e -> e.bloodlustHeal)).apply(i, MaceEffect::new));

    private final float damagePerPower;
    private final float knockback;
    private final float bloodlustHeal;

    public MaceEffect(float damagePerPower, float knockback, float bloodlustHeal) {
        this.damagePerPower = damagePerPower;
        this.knockback = knockback;
        this.bloodlustHeal = bloodlustHeal;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TMSpellBehaviors.MACE.get();
    }

    @Override
    protected boolean widens() {
        return true;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        SpellUtil.hostile(ctx, target.entity()).ifPresent(living -> {
            float damage = damagePerPower * power;
            float before = living.getHealth();
            living.hurtServer(ctx.level(), SpellUtil.blow(ctx), damage);
            double dx = ctx.caster().getX() - living.getX();
            double dz = ctx.caster().getZ() - living.getZ();
            living.knockback(knockback, dx, dz);
            if (SpellUtil.setting(ctx, "bloodlust", 0) > 0) {
                ctx.caster().heal(Math.max(0, before - living.getHealth()) * bloodlustHeal);
                if (living.isDeadOrDying()) {
                    ItemEntity blood = ToolEvents.blood(ctx.level(), living);
                    if (blood != null) {
                        ctx.level().addFreshEntity(blood);
                    }
                }
            }
            ctx.fx().impact(ctx.part().fx(), living.position().add(0, living.getBbHeight() / 2, 0), ctx.color());
        });
    }
}

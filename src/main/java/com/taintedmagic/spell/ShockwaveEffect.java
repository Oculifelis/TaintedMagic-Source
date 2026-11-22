package com.taintedmagic.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/**
 * Tainted shockwave: every hostile within {@code base_radius + radius_per_level * radius setting} of the target
 * point takes magic damage per power level and is thrown away from the centre.
 */
public final class ShockwaveEffect extends AbstractEffectBehavior {
    public static final MapCodec<ShockwaveEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 100).optionalFieldOf("damage_per_power", 2F).forGetter(e -> e.damagePerPower),
            Codec.floatRange(0, 64).optionalFieldOf("base_radius", 4F).forGetter(e -> e.baseRadius),
            Codec.floatRange(0, 64).optionalFieldOf("radius_per_level", 2F).forGetter(e -> e.radiusPerLevel),
            Codec.floatRange(0, 10).optionalFieldOf("knockback", 1.2F).forGetter(e -> e.knockback),
            SpellUtil.SOUND.optionalFieldOf("sound").forGetter(e -> e.sound)).apply(i, ShockwaveEffect::new));

    private final float damagePerPower;
    private final float baseRadius;
    private final float radiusPerLevel;
    private final float knockback;
    private final Optional<Holder<SoundEvent>> sound;

    public ShockwaveEffect(float damagePerPower, float baseRadius, float radiusPerLevel, float knockback,
                           Optional<Holder<SoundEvent>> sound) {
        this.sound = sound;
        this.damagePerPower = damagePerPower;
        this.baseRadius = baseRadius;
        this.radiusPerLevel = radiusPerLevel;
        this.knockback = knockback;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TMSpellBehaviors.SHOCKWAVE.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        Vec3 centre = target.position();
        double radius = baseRadius + radiusPerLevel * SpellUtil.setting(ctx, "radius", 0);
        for (LivingEntity living : ctx.level().getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius))) {
            if (living == ctx.caster() || ctx.isAlly(living) || living.distanceToSqr(centre) > radius * radius) {
                continue;
            }
            living.hurtServer(ctx.level(), SpellUtil.magic(ctx), damagePerPower * power);
            Vec3 away = living.position().subtract(centre);
            if (away.lengthSqr() > 1.0E-4) {
                living.knockback(knockback, -away.x, -away.z);
            }
        }
        ctx.fx().burst(ctx.part().fx(), centre, Vec3.ZERO, ctx.color());
        SpellUtil.play(ctx, sound, centre);
    }
}

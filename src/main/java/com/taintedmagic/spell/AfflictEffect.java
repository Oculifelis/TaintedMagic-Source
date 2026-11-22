package com.taintedmagic.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

/**
 * Magic damage per power level, then each listed effect. The part's {@code duration} setting (if it has one)
 * multiplies effect lengths; {@code amplify} (if it has one) raises their level.
 */
public final class AfflictEffect extends AbstractEffectBehavior {
    public record Affliction(Holder<MobEffect> effect, float seconds, int amplifier) {
        static final Codec<Affliction> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(Affliction::effect),
                Codec.floatRange(0, 3600).optionalFieldOf("seconds", 5F).forGetter(Affliction::seconds),
                Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(Affliction::amplifier)).apply(i, Affliction::new));
    }

    public static final MapCodec<AfflictEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0, 100).optionalFieldOf("damage_per_power", 2F).forGetter(e -> e.damagePerPower),
            Affliction.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(e -> e.effects),
            SpellUtil.SOUND.optionalFieldOf("sound").forGetter(e -> e.sound)).apply(i, AfflictEffect::new));

    private final float damagePerPower;
    private final List<Affliction> effects;
    private final Optional<Holder<SoundEvent>> sound;

    public AfflictEffect(float damagePerPower, List<Affliction> effects, Optional<Holder<SoundEvent>> sound) {
        this.damagePerPower = damagePerPower;
        this.effects = effects;
        this.sound = sound;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TMSpellBehaviors.AFFLICT.get();
    }

    @Override
    protected boolean widens() {
        return true;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        SpellUtil.hostile(ctx, target.entity()).ifPresent(living -> {
            if (damagePerPower > 0) {
                living.hurtServer(ctx.level(), SpellUtil.magic(ctx), damagePerPower * power);
            }
            int lengthen = Math.max(1, SpellUtil.setting(ctx, "duration", 1));
            int amplify = SpellUtil.setting(ctx, "amplify", 0);
            for (Affliction a : effects) {
                living.addEffect(new MobEffectInstance(a.effect(), Math.round(a.seconds() * 20 * lengthen), a.amplifier() + amplify),
                        ctx.caster());
            }
            Vec3 at = living.position().add(0, living.getBbHeight() / 2, 0);
            ctx.fx().impact(ctx.part().fx(), at, ctx.color());
            SpellUtil.play(ctx, sound, at);
        });
    }
}

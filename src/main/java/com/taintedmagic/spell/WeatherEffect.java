package com.taintedmagic.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;

/** Meteorology: clears the sky if it is raining, otherwise starts rain for {@code duration_ticks}. Once per cast. */
public final class WeatherEffect extends AbstractEffectBehavior {
    public static final MapCodec<WeatherEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(20, 1_000_000).optionalFieldOf("duration_ticks", 12000).forGetter(e -> e.durationTicks)).apply(i, WeatherEffect::new));

    private final int durationTicks;

    public WeatherEffect(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TMSpellBehaviors.WEATHER.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (index != 0) {
            return;
        }
        ServerLevel level = ctx.level();
        if (level.isRaining()) {
            level.getServer().setWeatherParameters(durationTicks, 0, false, false);
        } else {
            level.getServer().setWeatherParameters(0, durationTicks, true, false);
        }
        ctx.fx().burst(ctx.part().fx(), target.position(), target.direction(), ctx.color());
    }
}

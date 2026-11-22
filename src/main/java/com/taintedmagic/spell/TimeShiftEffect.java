package com.taintedmagic.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Moves the world clock forward by {@code ticks} (half a day by default) once per cast. */
public final class TimeShiftEffect extends AbstractEffectBehavior {
    public static final MapCodec<TimeShiftEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 24000).optionalFieldOf("ticks", 12000).forGetter(e -> e.ticks)).apply(i, TimeShiftEffect::new));

    private final int ticks;

    public TimeShiftEffect(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TMSpellBehaviors.TIME_SHIFT.get();
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
        // 26.1 keeps time on per-dimension world clocks; dimensions without one (the Nether, the End) are unaffected.
        ctx.level().dimensionTypeRegistration().value().defaultClock().ifPresent(clock -> {
            ctx.level().getServer().clockManager().addTicks(clock, ticks);
            ctx.fx().burst(ctx.part().fx(), target.position(), target.direction(), ctx.color());
        });
    }
}

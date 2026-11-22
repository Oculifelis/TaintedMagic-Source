package com.taintedmagic.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPartKind;
import com.mojang.serialization.MapCodec;
import com.taintedmagic.TaintedMagic;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tainted Magic's foci, as Thaumaturge spell effects. A TC4 focus was one fixed spell; here each is an effect part
 * the player combines with any delivery in the focal manipulator. Every number is a field of the behaviour JSON in
 * {@code data/taintedmagic/thaumaturge/spell_part/}, and the per-cast knobs are the part's settings.
 */
public final class TMSpellBehaviors {
    public static final DeferredRegister<SpellBehaviorType<?>> BEHAVIORS = DeferredRegister.create(SpellBehaviorType.REGISTRY_KEY,
            TaintedMagic.MODID);

    /** Damage plus a list of mob effects (taint, eldritch, vis shard). */
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<AfflictEffect>> AFFLICT = effect("afflict", AfflictEffect.CODEC);
    /** Damage and knockback in a radius around the target point (tainted shockwave). */
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ShockwaveEffect>> SHOCKWAVE = effect("shockwave",
            ShockwaveEffect.CODEC);
    /** A physical blow that can heal the caster (mage's mace). */
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<MaceEffect>> MACE = effect("mace", MaceEffect.CODEC);
    /** Pushes the world clock forward (time focus). */
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<TimeShiftEffect>> TIME_SHIFT = effect("time_shift",
            TimeShiftEffect.CODEC);
    /** Starts or stops rain (meteorology focus). */
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<WeatherEffect>> WEATHER = effect("weather", WeatherEffect.CODEC);

    private TMSpellBehaviors() {
    }

    private static <B extends SpellBehavior> DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<B>> effect(String name, MapCodec<B> codec) {
        return BEHAVIORS.register(name, () -> new SpellBehaviorType<>(SpellPartKind.EFFECT, codec));
    }

    public static void register(IEventBus bus) {
        BEHAVIORS.register(bus);
    }
}

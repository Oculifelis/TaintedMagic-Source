package com.taintedmagic;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events (files and subtitles in assets, made by tools/gen_sounds.py). Spell parts name them in their JSON. */
public final class TMSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TaintedMagic.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SHARD = sound("shard");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOCKWAVE = sound("shockwave");

    private TMSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TaintedMagic.id(name)));
    }

    static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}

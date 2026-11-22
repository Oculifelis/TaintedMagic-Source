package com.taintedmagic;

import com.mojang.logging.LogUtils;
import com.taintedmagic.data.TMTuning;
import com.taintedmagic.event.ConsumeEvents;
import com.taintedmagic.event.GearEvents;
import com.taintedmagic.event.ToolEvents;
import com.taintedmagic.spell.TMSpellBehaviors;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;

/** Tainted Magic (Thaumcraft 4 addon by Pelz), reimplemented for Thaumaturge (NeoForge 26.1.2). */
@Mod(TaintedMagic.MODID)
public final class TaintedMagic {
    public static final String MODID = "taintedmagic";
    public static final Logger LOG = LogUtils.getLogger();

    public TaintedMagic(IEventBus modBus) {
        TMDataComponents.register(modBus);
        TMRegistry.register(modBus);
        TMSounds.register(modBus);
        TMSpellBehaviors.register(modBus);
        GearEvents.register();
        ToolEvents.register();
        ConsumeEvents.register();
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> e.addListener(id("tuning"), new TMTuning.Listener()));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}

package com.taintedmagic.event;

import com.taintedmagic.TMRegistry;
import com.taintedmagic.TaintedMagic;
import com.taintedmagic.data.TMTuning;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Bloodlust: a kill made with a weapon in {@code #taintedmagic:bloodlust} sheds crystallised blood. */
public final class ToolEvents {
    public static final TagKey<Item> BLOODLUST = TagKey.create(Registries.ITEM, TaintedMagic.id("bloodlust"));

    private ToolEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ToolEvents::onDrops);
    }

    private static void onDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity killer && killer.getMainHandItem().is(BLOODLUST)
                && event.getEntity().level() instanceof ServerLevel level) {
            ItemEntity drop = blood(level, event.getEntity());
            if (drop != null) {
                event.getDrops().add(drop);
            }
        }
    }

    /** Zero to {@code bloodlust.max_drops} crystal blood at the victim, or null for none. */
    public static ItemEntity blood(ServerLevel level, LivingEntity victim) {
        int count = level.getRandom().nextInt((int) TMTuning.get("bloodlust.max_drops") + 1);
        if (count == 0) {
            return null;
        }
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(TMRegistry.CRYSTAL_BLOOD.get(), count));
        drop.setPickUpDelay(10);
        return drop;
    }
}

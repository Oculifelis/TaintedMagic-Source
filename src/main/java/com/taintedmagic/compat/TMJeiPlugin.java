package com.taintedmagic.compat;

import com.taintedmagic.TaintedMagic;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * JEI: information pages for the items that come from drops, trees or a use mechanic rather than a recipe. Every
 * recipe here is one of Thaumaturge's types (arcane, crucible, infusion), which its own plugin shows. Loaded only
 * with JEI.
 */
@JeiPlugin
public final class TMJeiPlugin implements IModPlugin {
    private static final String[] INFO = {"crystal_blood", "warpwood_sapling", "warpwood_log", "warpwood_leaves",
            "voidwalker_sash", "thaumic_disassembler", "warped_shard", "fluxed_shard"};

    @Override
    public Identifier getPluginUid() {
        return TaintedMagic.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (String name : INFO) {
            BuiltInRegistries.ITEM.getOptional(TaintedMagic.id(name)).ifPresent(item ->
                    registration.addIngredientInfo(item, Component.translatable("jei.taintedmagic.info." + name)));
        }
    }
}

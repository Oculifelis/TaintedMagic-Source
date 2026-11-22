package com.taintedmagic;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Armour materials. Defence follows the original (helm/chest/legs/boots); the original's durability factors were
 * effectively unbreakable at 1.7.10 scale, so they are scaled to stay well above diamond. Repair items are item tags.
 */
public final class TMMaterials {
    public static final ResourceKey<EquipmentAsset> ASSET_SHADOW_FORTRESS = asset("shadow_fortress");
    public static final ResourceKey<EquipmentAsset> ASSET_VOID_FORTRESS = asset("void_fortress");
    public static final ResourceKey<EquipmentAsset> ASSET_WARPED_GOGGLES = asset("warped_goggles");
    public static final ResourceKey<EquipmentAsset> ASSET_VOID_GOGGLES = asset("void_goggles");
    public static final ResourceKey<EquipmentAsset> ASSET_VOIDWALKER = asset("voidwalker");

    public static final ArmorMaterial SHADOW_FORTRESS = new ArmorMaterial(60, defense(4, 10, 6, 0), 30, SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0F, 0.1F, repair("repairs_shadow_fortress"), ASSET_SHADOW_FORTRESS);
    public static final ArmorMaterial VOID_FORTRESS = new ArmorMaterial(40, defense(4, 8, 6, 0), 20, SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.5F, 0.0F, repair("repairs_void_fortress"), ASSET_VOID_FORTRESS);
    public static final ArmorMaterial WARPED_GOGGLES = new ArmorMaterial(45, defense(2, 0, 0, 0), 25, SoundEvents.ARMOR_EQUIP_LEATHER,
            0.0F, 0.0F, repair("repairs_warped_goggles"), ASSET_WARPED_GOGGLES);
    public static final ArmorMaterial VOID_GOGGLES = new ArmorMaterial(23, defense(3, 0, 0, 0), 25, SoundEvents.ARMOR_EQUIP_IRON,
            1.0F, 0.0F, repair("repairs_void_fortress"), ASSET_VOID_GOGGLES);
    public static final ArmorMaterial VOIDWALKER = new ArmorMaterial(40, defense(0, 0, 0, 4), 25, SoundEvents.ARMOR_EQUIP_LEATHER,
            2.0F, 0.0F, repair("repairs_void_fortress"), ASSET_VOIDWALKER);

    // Tool materials. Damage bonuses are the original's; speeds above diamond as in 1.7.10.
    public static final ToolMaterial SHADOW_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2200, 17.0F, 6.0F, 30,
            repair("repairs_shadowmetal"));
    public static final ToolMaterial PRIMAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 512, 16.0F, 14.5F, 28,
            repair("repairs_void_fortress"));
    public static final ToolMaterial CRYSTAL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2000, 16.0F, 2.0F, 30,
            repair("repairs_crystal_dagger"));
    public static final ToolMaterial DISASSEMBLER = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3000, 20.0F, 0.0F, 20,
            repair("repairs_disassembler"));
    /** Everything a pick, axe, shovel or hoe mines; the Disassembler mines all of it. */
    public static final TagKey<Block> MINEABLE_DISASSEMBLER = TagKey.create(Registries.BLOCK, TaintedMagic.id("mineable/disassembler"));

    private TMMaterials() {
    }

    private static Map<ArmorType, Integer> defense(int helmet, int chest, int legs, int boots) {
        return Map.of(ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chest, ArmorType.LEGGINGS, legs, ArmorType.BOOTS, boots,
                ArmorType.BODY, chest);
    }

    private static TagKey<Item> repair(String name) {
        return TagKey.create(Registries.ITEM, TaintedMagic.id(name));
    }

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, TaintedMagic.id(name));
    }
}

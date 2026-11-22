package com.taintedmagic;

import com.leclowndu93150.thaumaturge.api.wands.WandCap;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.content.wands.ItemWandCap;
import com.leclowndu93150.thaumaturge.content.wands.ItemWandRod;
import com.leclowndu93150.thaumaturge.content.equipment.GearWarp;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.taintedmagic.TMDataComponents.HitEffect;
import com.taintedmagic.TMDataComponents.MiningModes;
import com.taintedmagic.item.DisassemblerItem;
import com.taintedmagic.item.KatanaItem;
import com.taintedmagic.item.PrimordialEdgeItem;
import com.taintedmagic.item.WeaponItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import com.taintedmagic.item.GearItem;
import com.taintedmagic.item.SashItem;
import com.taintedmagic.wand.WarpwoodCore;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.equipment.ArmorType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TMRegistry {
    private static final String MODID = TaintedMagic.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Thaumaturge's cap and rod registries are static (not datapack); the assembly recipes for every pairing are
    // created from them, so registering a part here is all a new cap or rod needs.
    public static final DeferredRegister<WandCap> CAPS = DeferredRegister.create(WandCap.REGISTRY_KEY, MODID);
    public static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, MODID);

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    // ---- wand parts -----------------------------------------------------------------------------------------
    // Cost multipliers and craft factors follow the original caps (iron 1.1, gold 1.0, thaumium 0.9, void 0.8 for scale).
    public static final DeferredHolder<WandCap, WandCap> CAP_CLOTH = cap("cloth", 0.95F, 3);
    public static final DeferredHolder<WandCap, WandCap> CAP_SHADOWCLOTH = cap("shadowcloth", 0.90F, 3);
    public static final DeferredHolder<WandCap, WandCap> CAP_CRIMSONCLOTH = cap("crimsoncloth", 0.85F, 5);
    public static final DeferredHolder<WandCap, WandCap> CAP_SHADOWMETAL = cap("shadowmetal", 0.70F, 12);

    // Warpwood: 250 vis as a wand rod and 500 as a staff core (staves hold twice their rod), glowing, warp-fed.
    public static final DeferredHolder<WandRod, WandRod> ROD_WARPWOOD = RODS.register("warpwood", () -> new WandRod(250, 16,
            TaintedMagic.id("textures/models/wand_rod_warpwood.png"), new WarpwoodCore(), true, false, false,
            TaintedMagic.id("rod_warpwood")));
    public static final DeferredHolder<WandRod, WandRod> ROD_WARPWOOD_STAFF = RODS.register("warpwood_staff", () -> new WandRod(500, 20,
            TaintedMagic.id("textures/models/wand_rod_warpwood.png"), new WarpwoodCore(), true, true, true,
            TaintedMagic.id("rod_warpwood_staff")));

    // ---- blocks ---------------------------------------------------------------------------------------------
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARPWOOD_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            TaintedMagic.id("warpwood_tree"));
    public static final TreeGrower WARPWOOD_GROWER = new TreeGrower("taintedmagic:warpwood", Optional.empty(), Optional.of(WARPWOOD_TREE),
            Optional.empty());

    public static final DeferredBlock<Block> SHADOW_ORE = block("shadow_ore", Block::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.0f, 6.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> SHADOWMETAL_BLOCK = block("shadowmetal_block", Block::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(5.0f, 8.0f).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops());
    public static final DeferredBlock<RotatedPillarBlock> WARPWOOD_LOG = block("warpwood_log", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 5.0F).sound(SoundType.WOOD).lightLevel(s -> 3).ignitedByLava());
    public static final DeferredBlock<RotatedPillarBlock> WARPWOOD_WOOD = block("warpwood_wood", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 5.0F).sound(SoundType.WOOD).lightLevel(s -> 3).ignitedByLava());
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_WARPWOOD_LOG = block("stripped_warpwood_log", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 5.0F).sound(SoundType.WOOD).lightLevel(s -> 3).ignitedByLava());
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_WARPWOOD_WOOD = block("stripped_warpwood_wood", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 5.0F).sound(SoundType.WOOD).lightLevel(s -> 3).ignitedByLava());
    public static final DeferredBlock<Block> WARPWOOD_PLANKS = block("warpwood_planks", Block::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<UntintedParticleLeavesBlock> WARPWOOD_LEAVES = block("warpwood_leaves",
            p -> new UntintedParticleLeavesBlock(0.01F, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFF9A4CB0), p),
            p -> p.mapColor(MapColor.COLOR_MAGENTA).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion().ignitedByLava()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<SaplingBlock> WARPWOOD_SAPLING = block("warpwood_sapling", p -> new SaplingBlock(WARPWOOD_GROWER, p),
            p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY));

    // ---- materials ------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> SHADOWMETAL_INGOT = item("shadowmetal_ingot", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SHADOW_CLOTH = item("shadow_cloth", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> CRIMSON_CLOTH = item("crimson_cloth", p -> p.rarity(Rarity.UNCOMMON));
    // Converted to Thaumaturge's Alienis / Vitium vis crystals; kept registered for old worlds, nothing crafts them.
    public static final DeferredItem<Item> WARPED_SHARD = item("warped_shard", p -> obsolete(p, "warped_shard"));
    public static final DeferredItem<Item> FLUXED_SHARD = item("fluxed_shard", p -> obsolete(p, "fluxed_shard"));
    public static final DeferredItem<Item> CRYSTAL_BLOOD = item("crystal_blood", p -> p.rarity(Rarity.UNCOMMON));

    // ---- wand parts as items --------------------------------------------------------------------------------
    public static final DeferredItem<ItemWandCap> WAND_CAP_CLOTH = capItem("cloth", CAP_CLOTH, Rarity.COMMON);
    public static final DeferredItem<ItemWandCap> WAND_CAP_SHADOWCLOTH = capItem("shadowcloth", CAP_SHADOWCLOTH, Rarity.UNCOMMON);
    public static final DeferredItem<ItemWandCap> WAND_CAP_CRIMSONCLOTH = capItem("crimsoncloth", CAP_CRIMSONCLOTH, Rarity.UNCOMMON);
    public static final DeferredItem<ItemWandCap> WAND_CAP_SHADOWMETAL = capItem("shadowmetal", CAP_SHADOWMETAL, Rarity.RARE);
    public static final DeferredItem<ItemWandRod> WAND_ROD_WARPWOOD = tab(ITEMS.registerItem("wand_rod_warpwood",
            p -> new ItemWandRod(p, ROD_WARPWOOD), p -> p.rarity(Rarity.RARE)));
    public static final DeferredItem<ItemWandRod> STAFF_ROD_WARPWOOD = tab(ITEMS.registerItem("staff_rod_warpwood",
            p -> new ItemWandRod(p, ROD_WARPWOOD_STAFF), p -> p.rarity(Rarity.EPIC)));

    // ---- armour (phase 3) -----------------------------------------------------------------------------------
    // Warp, vis discount, self repair, damage wards, revealing and runic charge are all stack components, so each
    // piece is a plain GearItem and a datapack or recipe patch can retune it.
    public static final DeferredItem<GearItem> SHADOW_FORTRESS_HELMET = shadowFortress("helmet", ArmorType.HELMET);
    public static final DeferredItem<GearItem> SHADOW_FORTRESS_CHESTPLATE = shadowFortress("chestplate", ArmorType.CHESTPLATE);
    public static final DeferredItem<GearItem> SHADOW_FORTRESS_LEGGINGS = shadowFortress("leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<GearItem> VOID_FORTRESS_HELMET = voidFortress("helmet", ArmorType.HELMET);
    public static final DeferredItem<GearItem> VOID_FORTRESS_CHESTPLATE = voidFortress("chestplate", ArmorType.CHESTPLATE);
    public static final DeferredItem<GearItem> VOID_FORTRESS_LEGGINGS = voidFortress("leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<GearItem> WARPED_GOGGLES = gear("warped_goggles", p -> GearWarp.with(p, 1)
            .humanoidArmor(TMMaterials.WARPED_GOGGLES, ArmorType.HELMET).rarity(Rarity.UNCOMMON)
            .component(TTDataComponents.GOGGLES_UPGRADE.get(), Unit.INSTANCE));
    public static final DeferredItem<GearItem> VOID_GOGGLES = gear("void_goggles", p -> GearWarp.with(p, 5)
            .humanoidArmor(TMMaterials.VOID_GOGGLES, ArmorType.HELMET).rarity(Rarity.RARE)
            .component(TTDataComponents.GOGGLES_UPGRADE.get(), Unit.INSTANCE)
            .component(TMDataComponents.VIS_DISCOUNT.get(), 10).component(TMDataComponents.SELF_REPAIR.get(), 20));
    // Voidwalker boots: step assist, speed, no fall damage. Plain attributes on top of the armour's own.
    public static final DeferredItem<GearItem> VOIDWALKER_BOOTS = gear("voidwalker_boots", p -> GearWarp.with(p, 5)
            .humanoidArmor(TMMaterials.VOIDWALKER, ArmorType.BOOTS).rarity(Rarity.EPIC)
            .attributes(TMMaterials.VOIDWALKER.createAttributes(ArmorType.BOOTS)
                    .withModifierAdded(Attributes.STEP_HEIGHT, new AttributeModifier(TaintedMagic.id("voidwalker_step"), 0.4,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET)
                    .withModifierAdded(Attributes.MOVEMENT_SPEED, new AttributeModifier(TaintedMagic.id("voidwalker_speed"), 0.4,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.FEET)
                    .withModifierAdded(Attributes.FALL_DAMAGE_MULTIPLIER, new AttributeModifier(TaintedMagic.id("voidwalker_fall"), -1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.FEET))
            .component(TMDataComponents.VIS_DISCOUNT.get(), 5).component(TMDataComponents.SELF_REPAIR.get(), 20)
            .component(TMDataComponents.DAMAGE_WARD.get(), new TMDataComponents.DamageWard(0.02F, 0.02F, 0.02F)));
    public static final DeferredItem<SashItem> VOIDWALKER_SASH = tab(ITEMS.registerItem("voidwalker_sash", SashItem::new,
            p -> GearWarp.with(p, 2).stacksTo(1).rarity(Rarity.EPIC).component(TTDataComponents.RUNIC_CHARGE.get(), 20)
                    .component(TMDataComponents.SASH_SPEED.get(), true)));
    public static final DeferredItem<Item> CRIMSON_PLATING = item("crimson_plating", p -> p.rarity(Rarity.UNCOMMON));

    // ---- blades and tools (phase 5) -------------------------------------------------------------------------
    // Damage baselines are chosen so totals match the original (1.7.10 sword = 4 + material, pick 2 +, axe 3 +, shovel 1 +).
    public static final DeferredItem<WeaponItem> SHADOWMETAL_SWORD = weapon("shadowmetal_sword",
            p -> p.sword(TMMaterials.SHADOW_TOOL, 4.0F, -2.4F).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<WeaponItem> SHADOWMETAL_PICKAXE = weapon("shadowmetal_pickaxe",
            p -> p.pickaxe(TMMaterials.SHADOW_TOOL, 2.0F, -2.8F).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<AxeItem> SHADOWMETAL_AXE = tab(ITEMS.registerItem("shadowmetal_axe",
            p -> new AxeItem(TMMaterials.SHADOW_TOOL, 3.0F, -3.0F, p), p -> p.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ShovelItem> SHADOWMETAL_SHOVEL = tab(ITEMS.registerItem("shadowmetal_shovel",
            p -> new ShovelItem(TMMaterials.SHADOW_TOOL, 1.0F, -3.0F, p), p -> p.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<HoeItem> SHADOWMETAL_HOE = tab(ITEMS.registerItem("shadowmetal_hoe",
            p -> new HoeItem(TMMaterials.SHADOW_TOOL, -TMMaterials.SHADOW_TOOL.attackDamageBonus(), 0.0F, p),
            p -> p.rarity(Rarity.UNCOMMON)));

    // Katanas never wear out (no durability). Damage is the original's attack modifier; on-hit effects are a component.
    public static final DeferredItem<KatanaItem> THAUMIUM_KATANA = katana("thaumium_katana", 14.25, 0, Rarity.UNCOMMON, List::of);
    public static final DeferredItem<KatanaItem> VOIDMETAL_KATANA = katana("voidmetal_katana", 17.55, 3, Rarity.RARE, () -> List.of(
            new HitEffect(MobEffects.WEAKNESS, 60, 0), new HitEffect(MobEffects.HUNGER, 120, 0)));
    public static final DeferredItem<KatanaItem> SHADOWMETAL_KATANA = katana("shadowmetal_katana", 20.75, 7, Rarity.EPIC, () -> List.of(
            new HitEffect(MobEffects.SLOWNESS, 120, 2), new HitEffect(TTMobEffects.BLURRED_VISION, 120, 0)));

    public static final DeferredItem<PrimordialEdgeItem> PRIMORDIAL_EDGE = tab(ITEMS.registerItem("primordial_edge", PrimordialEdgeItem::new,
            p -> GearWarp.with(p, 5).sword(TMMaterials.PRIMAL, 4.0F, -2.4F).rarity(Rarity.EPIC)
                    .component(TMDataComponents.SELF_REPAIR.get(), 20)
                    .component(TMDataComponents.ON_HIT.get(), List.of(new HitEffect(MobEffects.WITHER, 5, 1),
                            new HitEffect(MobEffects.WEAKNESS, 5, 1)))));
    public static final DeferredItem<WeaponItem> CRYSTAL_DAGGER = weapon("crystal_dagger",
            p -> p.sword(TMMaterials.CRYSTAL, 4.0F, -1.8F).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> THAUMIC_ALLOY = item("thaumic_alloy", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<DisassemblerItem> THAUMIC_DISASSEMBLER = tab(ITEMS.registerItem("thaumic_disassembler",
            DisassemblerItem::new, p -> p.tool(TMMaterials.DISASSEMBLER, TMMaterials.MINEABLE_DISASSEMBLER, 19.0F, -2.8F, 0.0F)
                    .rarity(Rarity.EPIC).component(TMDataComponents.MINING_MODES.get(), new MiningModes(List.of(20F, 8F, 128F, 0F), 0))));

    private static DeferredItem<WeaponItem> weapon(String name, UnaryOperator<Item.Properties> props) {
        return tab(ITEMS.registerItem(name, WeaponItem::new, props));
    }

    private static DeferredItem<KatanaItem> katana(String name, double damage, int warp, Rarity rarity, Supplier<List<HitEffect>> hits) {
        return tab(ITEMS.registerItem(name, KatanaItem::new, p -> GearWarp.with(p, warp).stacksTo(1).rarity(rarity).enchantable(15)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.2, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build())
                .component(DataComponents.WEAPON, new Weapon(0))
                .component(TMDataComponents.ON_HIT.get(), hits.get())));
    }

    // ---- creation, voidsent blood, funguar (phase 6) ----------------------------------------------------------
    public static final DeferredItem<Item> CREATION_SHARD = item("creation_shard",
            p -> p.rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<Item> VOIDSENT_BLOOD = item("voidsent_blood", p -> p.rarity(Rarity.UNCOMMON));
    // Always edible; strong regeneration, haste and speed, and a pinch of one primal for the research pool.
    public static final DeferredItem<Item> MAGIC_FUNGUAR = item("magic_funguar", p -> p.rarity(Rarity.UNCOMMON)
            .food(new FoodProperties(5, 10.0F, true), Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.REGENERATION, 200, 2), new MobEffectInstance(MobEffects.HASTE, 100, 2),
                    new MobEffectInstance(MobEffects.SPEED, 100, 2)))).build())
            .component(TMDataComponents.ASPECT_REWARD.get(), new TMDataComponents.AspectReward(TTAspects.PRIMALS, 1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.taintedmagic"))
            .icon(() -> new ItemStack(SHADOWMETAL_INGOT.get()))
            .displayItems((params, out) -> TAB_ITEMS.forEach(i -> out.accept(i.get())))
            .build());

    private TMRegistry() {
    }

    private static DeferredHolder<WandCap, WandCap> cap(String name, float cost, int craftCost) {
        return CAPS.register(name, () -> new WandCap(cost, List.of(), 0.0F, craftCost, TaintedMagic.id("textures/models/wand_cap_" + name + ".png")));
    }

    private static DeferredItem<ItemWandCap> capItem(String name, Supplier<WandCap> cap, Rarity rarity) {
        return tab(ITEMS.registerItem("wand_cap_" + name, p -> new ItemWandCap(p, cap), p -> p.rarity(rarity)));
    }

    private static DeferredItem<GearItem> gear(String name, UnaryOperator<Item.Properties> props) {
        return tab(ITEMS.registerItem(name, GearItem::new, props));
    }

    // Shadow Fortress: warp 5, 5% discount and a strong ward against magic, fire and blasts on every piece.
    private static DeferredItem<GearItem> shadowFortress(String piece, ArmorType type) {
        return gear("shadow_fortress_" + piece, p -> GearWarp.with(p, 5).humanoidArmor(TMMaterials.SHADOW_FORTRESS, type).rarity(Rarity.EPIC)
                .component(TMDataComponents.VIS_DISCOUNT.get(), 5)
                .component(TMDataComponents.DAMAGE_WARD.get(), new TMDataComponents.DamageWard(0.04F, 0.06F, 0.06F)));
    }

    // Void Fortress: warp 3, 5% discount, mends itself like void metal, lighter ward.
    private static DeferredItem<GearItem> voidFortress(String piece, ArmorType type) {
        return gear("void_fortress_" + piece, p -> GearWarp.with(p, 3).humanoidArmor(TMMaterials.VOID_FORTRESS, type).rarity(Rarity.RARE)
                .component(TMDataComponents.VIS_DISCOUNT.get(), 5).component(TMDataComponents.SELF_REPAIR.get(), 20)
                .component(TMDataComponents.DAMAGE_WARD.get(), new TMDataComponents.DamageWard(0.03F, 0.04F, 0.04F)));
    }

    static <T extends Item> DeferredItem<T> tab(DeferredItem<T> item) {
        TAB_ITEMS.add(item);
        return item;
    }

    private static Item.Properties obsolete(Item.Properties p, String name) {
        return p.component(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("tooltip.taintedmagic.obsolete." + name).withStyle(ChatFormatting.GRAY))));
    }

    private static DeferredItem<Item> item(String name, UnaryOperator<Item.Properties> props) {
        return tab(ITEMS.registerItem(name, Item::new, props));
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<Block.Properties, B> factory,
                                                           UnaryOperator<Block.Properties> props) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, props);
        TAB_ITEMS.add(ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    static void register(IEventBus bus) {
        CAPS.register(bus);
        RODS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}

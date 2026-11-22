package com.taintedmagic.event;

import com.leclowndu93150.thaumaturge.compat.curio.ThaumaturgeCuriosCompat;
import com.leclowndu93150.thaumaturge.content.equipment.FortressArmorItem;
import com.taintedmagic.TMDataComponents;
import com.taintedmagic.TMDataComponents.DamageWard;
import com.taintedmagic.TaintedMagic;
import com.taintedmagic.data.TMTuning;
import com.taintedmagic.item.SashItem;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Worn-gear behaviour that is not a plain attribute: damage wards, the fortress set bonus, fortress masks on our helms
 * and the Voidwalker's Sash speed mode. Which items count is decided by item tags; the numbers live in tuning JSON.
 */
public final class GearEvents {
    /** Our fortress pieces (masks and the set bonus only react when at least one of these is worn). */
    public static final TagKey<Item> FORTRESS_ARMOR = TagKey.create(Registries.ITEM, TaintedMagic.id("fortress_armor"));
    /** Every piece that counts toward a fortress set, Thaumaturge's own included. */
    public static final TagKey<Item> FORTRESS_SET = TagKey.create(Registries.ITEM, TaintedMagic.id("fortress_set"));
    public static final TagKey<Item> VOIDWALKER_BOOTS = TagKey.create(Registries.ITEM, TaintedMagic.id("voidwalker_boots"));
    public static final TagKey<Item> SPEED_SASHES = TagKey.create(Registries.ITEM, TaintedMagic.id("speed_sashes"));
    /** Warp afflictions a Grinning Devil mask can turn away. */
    public static final TagKey<MobEffect> WARP_AFFLICTIONS = TagKey.create(Registries.MOB_EFFECT, TaintedMagic.id("warp_afflictions"));

    private static final Identifier SET_ARMOR = TaintedMagic.id("fortress_set_armor");
    private static final Identifier SET_TOUGHNESS = TaintedMagic.id("fortress_set_toughness");
    private static final Identifier SASH_SPEED = TaintedMagic.id("voidwalker_sash_speed");
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int MASK_DEVIL = 0;
    private static final int MASK_GHOST = 1;
    private static final int MASK_FIEND = 2;

    private GearEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GearEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(GearEvents::onDamageDealt);
        NeoForge.EVENT_BUS.addListener(GearEvents::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(GearEvents::onPlayerTick);
    }

    /** The fortress mask on the wearer's helmet, or -1. Only our helms: Thaumaturge handles its own. */
    private static int mask(LivingEntity entity) {
        ItemStack helm = entity.getItemBySlot(EquipmentSlot.HEAD);
        return helm.is(FORTRESS_ARMOR) ? FortressArmorItem.mask(helm) : -1;
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        boolean magic = source.is(DamageTypeTags.WITCH_RESISTANT_TO);
        boolean fire = source.is(DamageTypeTags.IS_FIRE);
        boolean explosion = source.is(DamageTypeTags.IS_EXPLOSION);
        if (magic || fire || explosion) {
            float ward = 0;
            for (EquipmentSlot slot : ARMOR) {
                DamageWard w = target.getItemBySlot(slot).get(TMDataComponents.DAMAGE_WARD.get());
                if (w != null) {
                    ward += Math.max(magic ? w.magic() : 0, Math.max(fire ? w.fire() : 0, explosion ? w.explosion() : 0));
                }
            }
            if (ward > 0) {
                event.setAmount(event.getAmount() * (1 - Math.min(ward, 0.8F)));
            }
        }
        if (target.level().isClientSide() || !(source.getEntity() instanceof LivingEntity attacker) || attacker == target) {
            return;
        }
        if (mask(target) == MASK_GHOST
                && target.getRandom().nextFloat() < event.getAmount() / (float) TMTuning.get("mask.ghost_chance_divisor")) {
            attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) TMTuning.get("mask.ghost_wither_ticks")), target);
        }
    }

    private static void onDamageDealt(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide() && mask(player) == MASK_FIEND
                && player.getRandom().nextFloat() < event.getInflictedDamage() / (float) TMTuning.get("mask.fiend_chance_divisor")) {
            player.heal((float) TMTuning.get("mask.fiend_heal"));
        }
    }

    private static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (mask(entity) == MASK_DEVIL && event.getEffectInstance().getEffect().is(WARP_AFFLICTIONS)
                && entity.getRandom().nextFloat() < TMTuning.get("mask.devil_ward_chance")) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 10 != 0) {
            return;
        }
        int pieces = 0;
        boolean ours = false;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(FORTRESS_SET)) {
                pieces++;
                ours |= stack.is(FORTRESS_ARMOR);
            }
        }
        double bonus = 0;
        if (ours && pieces >= 2) {
            bonus = pieces - 1;
            if (mask(player) >= 0) {
                bonus += TMTuning.get("fortress_set.mask_bonus");
            }
        }
        setModifier(player, Attributes.ARMOR, SET_ARMOR, bonus * TMTuning.get("fortress_set.armor_per_piece"),
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.ARMOR_TOUGHNESS, SET_TOUGHNESS, bonus * TMTuning.get("fortress_set.toughness_per_piece"),
                AttributeModifier.Operation.ADD_VALUE);

        boolean sashSpeed = player.getItemBySlot(EquipmentSlot.FEET).is(VOIDWALKER_BOOTS) && wearsSpeedSash(player);
        setModifier(player, Attributes.MOVEMENT_SPEED, SASH_SPEED, sashSpeed ? TMTuning.get("voidwalker.sash_speed_bonus") : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static boolean wearsSpeedSash(Player player) {
        if (!ModList.get().isLoaded("curios")) {
            return false;
        }
        List<ItemStack> curios = ThaumaturgeCuriosCompat.equippedCurios(player);
        for (ItemStack stack : curios) {
            if (stack.is(SPEED_SASHES) && SashItem.speedMode(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        AttributeModifier current = inst.getModifier(id);
        if (amount == 0) {
            if (current != null) {
                inst.removeModifier(id);
            }
        } else if (current == null || current.amount() != amount) {
            inst.removeModifier(id);
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
        }
    }
}

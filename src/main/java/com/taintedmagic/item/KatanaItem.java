package com.taintedmagic.item;

import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.taintedmagic.TMDataComponents;
import com.taintedmagic.TMDataComponents.Inscription;
import com.taintedmagic.data.TMTuning;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Katanas: hold use to wind up, release to strike whatever is under the crosshair for up to twice the blade's damage.
 * A full wind-up (not sneaking) also unleashes the blade's inscription: fire bursts where the strike lands, taint lashes
 * everything in a cone ahead, undead drinks a share of the damage back as health. Numbers are tuning keys under
 * {@code katana.*}; the inscription is a component set by infusion.
 */
public class KatanaItem extends WeaponItem {
    public KatanaItem(Properties props) {
        super(props);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)) {
            return true;
        }
        int held = getUseDuration(stack, user) - remaining;
        if (held < TMTuning.get("katana.min_charge_ticks")) {
            return false;
        }
        double full = TMTuning.get("katana.full_charge_ticks");
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1 + Math.min(held / full, 1)));
        double reach = TMTuning.get("katana.reach");
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof LivingEntity && e.isPickable() && e != player, reach);
        Vec3 look = player.getLookAngle();
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
        server.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + look.x * 1.5, player.getEyeY() - 0.3 + look.y * 1.5,
                player.getZ() + look.z * 1.5, 1, 0, 0, 0, 0);

        float dealt = 0;
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity target) {
            float before = target.getHealth();
            if (target.hurtServer(server, player.damageSources().playerAttack(player), damage)) {
                target.knockback(0.6, -look.x, -look.z);
                applyOnHit(stack, target, player);
                dealt = Math.max(0, before - target.getHealth());
            }
        }
        stack.hurtAndBreak(1, player, user.getUsedItemHand());

        Inscription inscription = stack.get(TMDataComponents.INSCRIPTION.get());
        if (inscription != null && held >= TMTuning.get("katana.inscription_charge_ticks") && !player.isShiftKeyDown()) {
            switch (inscription) {
                case FIRE -> fireBurst(server, player, damage);
                case TAINT -> taintCone(server, player, damage);
                case UNDEAD -> player.heal(dealt * (float) TMTuning.get("katana.undead_leech"));
            }
        }
        player.getCooldowns().addCooldown(stack, (int) TMTuning.get("katana.cooldown_ticks"));
        return true;
    }

    /** A burst of flame where the look ray first meets a block or a creature. Hurts creatures, never breaks blocks. */
    private static void fireBurst(ServerLevel level, Player player, float damage) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof LivingEntity && e != player,
                TMTuning.get("katana.fire_range"));
        Vec3 at = hit.getLocation();
        float radius = (float) TMTuning.get("katana.fire_radius");
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
                e -> e != player && e.isAlive())) {
            if (e.hurtServer(level, player.damageSources().indirectMagic(player, player), damage * (float) TMTuning.get("katana.fire_fraction"))) {
                e.igniteForSeconds((float) TMTuning.get("katana.fire_seconds"));
            }
        }
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 40, radius / 2, radius / 2, radius / 2, 0.05);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    /** Taint lashes every creature in a cone ahead: a fraction of the strike each, plus flux taint. */
    private static void taintCone(ServerLevel level, Player player, float damage) {
        double range = TMTuning.get("katana.taint_range");
        double cos = Math.cos(Math.toRadians(TMTuning.get("katana.taint_half_angle")));
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> e != player && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > range || to.normalize().dot(look) < cos) {
                continue;
            }
            if (e.hurtServer(level, player.damageSources().indirectMagic(player, player), damage * (float) TMTuning.get("katana.taint_fraction"))) {
                e.addEffect(new MobEffectInstance(TTMobEffects.FLUX_TAINT, (int) TMTuning.get("katana.taint_effect_ticks"), 0), player);
            }
        }
        for (int i = 1; i <= (int) range; i++) {
            Vec3 p = eye.add(look.scale(i));
            level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 4 + i, i * 0.15, i * 0.15, i * 0.15, 0.02);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        Inscription inscription = stack.get(TMDataComponents.INSCRIPTION.get());
        if (inscription != null) {
            out.accept(Component.translatable("tooltip.taintedmagic.inscription." + inscription.getSerializedName()).withStyle(ChatFormatting.GOLD));
        }
        out.accept(Component.translatable("tooltip.taintedmagic.katana").withStyle(ChatFormatting.DARK_GRAY));
    }
}

package com.taintedmagic.item;

import com.taintedmagic.data.TMTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Primordial Edge: hold use and everything nearby (creatures, dropped items, orbs) is dragged toward the wielder,
 * and whatever reaches arm's length is cut a little with magic. Range, pull and damage are tuning keys under
 * {@code primordial_edge.*}.
 */
public class PrimordialEdgeItem extends WeaponItem {
    public PrimordialEdgeItem(Properties props) {
        super(props);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
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
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        double range = TMTuning.get("primordial_edge.range");
        double pull = TMTuning.get("primordial_edge.pull");
        float damage = (float) TMTuning.get("primordial_edge.close_damage");
        Vec3 centre = user.position().add(0, 0.5, 0);
        if (remaining % 10 == 0) {
            server.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.3F, 0.5F);
        }
        for (Entity e : level.getEntities(user, user.getBoundingBox().inflate(range), Entity::isAlive)) {
            Vec3 to = centre.subtract(e.position()).scale(1 / 20.0);
            double d = to.length();
            if (e instanceof LivingEntity living && !e.isInvulnerable() && e.distanceTo(user) < 2.0) {
                living.hurtServer(server, user.damageSources().indirectMagic(user, user), damage);
            }
            double strength = 1.0 - d;
            if (strength > 0 && d > 1.0E-4) {
                strength *= strength;
                e.addDeltaMovement(new Vec3(to.x / d * strength * pull, to.y / d * strength * pull * 1.5, to.z / d * strength * pull));
                e.hurtMarked = true;
            }
        }
    }
}

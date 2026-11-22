package com.taintedmagic.spell;

import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class SpellUtil {
    private SpellUtil() {
    }

    /** A setting's value, or the fallback when this part does not declare that setting. */
    static int setting(CastContext ctx, String key, int fallback) {
        return ctx.part().setting(key).isPresent() ? ctx.setting(key) : fallback;
    }

    /** A living, non-allied target other than the caster. */
    static Optional<LivingEntity> hostile(CastContext ctx, Optional<Entity> entity) {
        return entity.filter(e -> e instanceof LivingEntity && e != ctx.caster() && e.isAlive() && !ctx.isAlly(e))
                .map(e -> (LivingEntity) e);
    }

    static DamageSource magic(CastContext ctx) {
        return ctx.level().damageSources().indirectMagic(ctx.caster(), ctx.caster());
    }

    /** Optional {@code "sound"} field shared by our behaviours: a sound event id played where the effect lands. */
    static final Codec<Holder<SoundEvent>> SOUND = BuiltInRegistries.SOUND_EVENT.holderByNameCodec();

    static void play(CastContext ctx, Optional<Holder<SoundEvent>> sound, Vec3 at) {
        sound.ifPresent(s -> ctx.level().playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, 1.0F,
                0.9F + ctx.level().getRandom().nextFloat() * 0.2F));
    }

    static DamageSource blow(CastContext ctx) {
        return ctx.caster() instanceof Player player ? ctx.level().damageSources().playerAttack(player)
                : ctx.level().damageSources().mobAttack(ctx.caster());
    }
}

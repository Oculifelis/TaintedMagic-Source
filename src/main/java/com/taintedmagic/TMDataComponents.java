package com.taintedmagic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item state for Tainted Magic gear. Warp, runic charge, revealing and fortress masks reuse Thaumaturge's own components
 * ({@code thaumaturge:warp}, {@code runic_charge}, {@code goggles_upgrade}, {@code fortress_mask}); these cover the rest.
 */
public final class TMDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
            TaintedMagic.MODID);

    /** Percent off every vis cost while worn (Thaumaturge turns it into its vis_discount attribute). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VIS_DISCOUNT = COMPONENTS.registerComponentType(
            "vis_discount", b -> b.persistent(Codec.intRange(0, 100)).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Restores one durability every this many ticks while carried or worn. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SELF_REPAIR = COMPONENTS.registerComponentType(
            "self_repair", b -> b.persistent(Codec.intRange(1, 72000)).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Extra protection against magic, fire and explosions, as fractions of the damage, summed over worn pieces. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DamageWard>> DAMAGE_WARD = COMPONENTS.registerComponentType(
            "damage_ward", b -> b.persistent(DamageWard.CODEC).networkSynchronized(DamageWard.STREAM_CODEC));
    /** Voidwalker's Sash speed mode (sneak-use toggles it). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SASH_SPEED = COMPONENTS.registerComponentType(
            "sash_speed", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Effects every hit with this weapon applies to the target. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<HitEffect>>> ON_HIT = COMPONENTS.registerComponentType(
            "on_hit", b -> b.persistent(HitEffect.CODEC.listOf()).networkSynchronized(HitEffect.STREAM_CODEC.apply(ByteBufCodecs.list())));
    /** A katana's inscription (set by infusion). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Inscription>> INSCRIPTION = COMPONENTS.registerComponentType(
            "inscription", b -> b.persistent(Inscription.CODEC).networkSynchronized(Inscription.STREAM_CODEC));
    /** The Thaumic Disassembler's mining speeds and which one is active (sneak-use cycles it). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiningModes>> MINING_MODES = COMPONENTS.registerComponentType(
            "mining_modes", b -> b.persistent(MiningModes.CODEC).networkSynchronized(MiningModes.STREAM_CODEC));

    /** Eating this grants {@code amount} of one aspect, picked at random from {@code pool}, to the eater's research pool. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AspectReward>> ASPECT_REWARD = COMPONENTS.registerComponentType(
            "aspect_reward", b -> b.persistent(AspectReward.CODEC).networkSynchronized(AspectReward.STREAM_CODEC));

    public record AspectReward(List<ResourceKey<IAspect>> pool, int amount) {
        public static final Codec<AspectReward> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(IAspect.REGISTRY_KEY).listOf(1, 64).fieldOf("pool").forGetter(AspectReward::pool),
                Codec.intRange(1, 1000).optionalFieldOf("amount", 1).forGetter(AspectReward::amount)).apply(i, AspectReward::new));
        public static final StreamCodec<ByteBuf, AspectReward> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(IAspect.REGISTRY_KEY).apply(ByteBufCodecs.list()), AspectReward::pool, ByteBufCodecs.VAR_INT,
                AspectReward::amount, AspectReward::new);
    }

    public record HitEffect(Holder<MobEffect> effect, int ticks, int amplifier) {
        public static final Codec<HitEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
                MobEffect.CODEC.fieldOf("effect").forGetter(HitEffect::effect),
                Codec.intRange(1, 1_000_000).fieldOf("ticks").forGetter(HitEffect::ticks),
                Codec.intRange(0, 255).optionalFieldOf("amplifier", 0).forGetter(HitEffect::amplifier)).apply(i, HitEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, HitEffect> STREAM_CODEC = StreamCodec.composite(
                MobEffect.STREAM_CODEC, HitEffect::effect, ByteBufCodecs.VAR_INT, HitEffect::ticks, ByteBufCodecs.VAR_INT, HitEffect::amplifier,
                HitEffect::new);
    }

    public enum Inscription implements StringRepresentable {
        FIRE("fire"), TAINT("taint"), UNDEAD("undead");

        public static final Codec<Inscription> CODEC = StringRepresentable.fromEnum(Inscription::values);
        public static final StreamCodec<ByteBuf, Inscription> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[i], Enum::ordinal);
        private final String name;

        Inscription(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record MiningModes(List<Float> speeds, int index) {
        public static final Codec<MiningModes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0, 10_000).listOf(1, 16).fieldOf("speeds").forGetter(MiningModes::speeds),
                Codec.intRange(0, 15).optionalFieldOf("index", 0).forGetter(MiningModes::index)).apply(i, MiningModes::new));
        public static final StreamCodec<ByteBuf, MiningModes> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), MiningModes::speeds, ByteBufCodecs.VAR_INT, MiningModes::index, MiningModes::new);

        public float speed() {
            return speeds.get(Math.floorMod(index, speeds.size()));
        }

        public MiningModes next() {
            return new MiningModes(speeds, (index + 1) % speeds.size());
        }
    }

    public record DamageWard(float magic, float fire, float explosion) {
        public static final Codec<DamageWard> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0, 1).optionalFieldOf("magic", 0F).forGetter(DamageWard::magic),
                Codec.floatRange(0, 1).optionalFieldOf("fire", 0F).forGetter(DamageWard::fire),
                Codec.floatRange(0, 1).optionalFieldOf("explosion", 0F).forGetter(DamageWard::explosion)).apply(i, DamageWard::new));
        public static final StreamCodec<ByteBuf, DamageWard> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, DamageWard::magic, ByteBufCodecs.FLOAT, DamageWard::fire, ByteBufCodecs.FLOAT, DamageWard::explosion,
                DamageWard::new);
    }

    private TMDataComponents() {
    }

    static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}

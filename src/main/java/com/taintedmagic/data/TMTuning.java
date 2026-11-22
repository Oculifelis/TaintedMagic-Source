package com.taintedmagic.data;

import com.taintedmagic.TaintedMagic;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Every Tainted Magic tunable. Read from {@code data/<ns>/tainted/tuning/*.json}: {@code {"warpwood_core": {"min_interval_ticks": 40}}}.
 * Files merge in load order; an unknown key or a value out of range is skipped with a log line, the rest still applies.
 */
public final class TMTuning {
    public record Spec(double def, double min, double max) {
    }

    public static final Map<String, Spec> DEFAULTS = new LinkedHashMap<>();

    static {
        // Warpwood core: refills one vis of each primal every (base - permanent warp) ticks, never faster than min.
        DEFAULTS.put("warpwood_core.base_interval_ticks", new Spec(200, 1, 12000));
        DEFAULTS.put("warpwood_core.min_interval_ticks", new Spec(50, 1, 12000));
        DEFAULTS.put("warpwood_core.vis_per_refill", new Spec(1, 0, 1000));
        // Fortress sets: each worn fortress piece past the first adds armour and toughness, a mask on the helm adds more.
        DEFAULTS.put("fortress_set.armor_per_piece", new Spec(1, 0, 20));
        DEFAULTS.put("fortress_set.toughness_per_piece", new Spec(1, 0, 20));
        DEFAULTS.put("fortress_set.mask_bonus", new Spec(1, 0, 20));
        // Fortress masks: angry ghost withers attackers (chance = damage / divisor), sipping fiend heals on hit,
        // grinning devil turns away warp afflictions.
        DEFAULTS.put("mask.ghost_chance_divisor", new Spec(10, 1, 1000));
        DEFAULTS.put("mask.ghost_wither_ticks", new Spec(80, 0, 12000));
        DEFAULTS.put("mask.fiend_chance_divisor", new Spec(12, 1, 1000));
        DEFAULTS.put("mask.fiend_heal", new Spec(1, 0, 100));
        DEFAULTS.put("mask.devil_ward_chance", new Spec(0.5, 0, 1));
        // Voidwalker's Sash in speed mode, worn with Voidwalker boots: extra movement speed (fraction of base).
        DEFAULTS.put("voidwalker.sash_speed_bonus", new Spec(0.8, 0, 10));
        // Katanas: hold use, release to strike the crosshair target for up to twice the blade's damage (full at
        // full_charge_ticks); inscriptions fire on a wind-up of at least inscription_charge_ticks.
        DEFAULTS.put("katana.min_charge_ticks", new Spec(5, 0, 1000));
        DEFAULTS.put("katana.full_charge_ticks", new Spec(40, 1, 1000));
        DEFAULTS.put("katana.inscription_charge_ticks", new Spec(30, 0, 1000));
        DEFAULTS.put("katana.reach", new Spec(5, 1, 32));
        DEFAULTS.put("katana.cooldown_ticks", new Spec(10, 0, 1200));
        DEFAULTS.put("katana.fire_range", new Spec(24, 1, 128));
        DEFAULTS.put("katana.fire_radius", new Spec(2.5, 0.5, 16));
        DEFAULTS.put("katana.fire_fraction", new Spec(0.5, 0, 10));
        DEFAULTS.put("katana.fire_seconds", new Spec(4, 0, 60));
        DEFAULTS.put("katana.taint_range", new Spec(8, 1, 32));
        DEFAULTS.put("katana.taint_half_angle", new Spec(30, 1, 90));
        DEFAULTS.put("katana.taint_fraction", new Spec(0.6, 0, 10));
        DEFAULTS.put("katana.taint_effect_ticks", new Spec(100, 0, 12000));
        DEFAULTS.put("katana.undead_leech", new Spec(0.25, 0, 1));
        // Primordial Edge: while held up, pulls everything within range toward the wielder and nicks what arrives.
        DEFAULTS.put("primordial_edge.range", new Spec(15, 1, 64));
        DEFAULTS.put("primordial_edge.pull", new Spec(0.2, 0, 5));
        DEFAULTS.put("primordial_edge.close_damage", new Spec(1, 0, 100));
        // Thaumic Disassembler: mends one point every interval for this much perditio vis from a hotbar wand.
        DEFAULTS.put("disassembler.repair_interval_ticks", new Spec(20, 1, 12000));
        DEFAULTS.put("disassembler.repair_vis", new Spec(5, 0, 1000));
        // Bloodlust: each kill sheds 0..max_drops crystal blood.
        DEFAULTS.put("bloodlust.max_drops", new Spec(4, 0, 64));
    }

    private static volatile Map<String, Double> values = Map.of();

    private TMTuning() {
    }

    public static double get(String key) {
        Double v = values.get(key);
        return v != null ? v : DEFAULTS.get(key).def();
    }

    public static final class Listener extends SimpleJsonResourceReloadListener<JsonElement> {
        public Listener() {
            super(ExtraCodecs.JSON, FileToIdConverter.json("tainted/tuning"));
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
            Map<String, Double> next = new LinkedHashMap<>();
            new java.util.TreeMap<>(files).forEach((file, json) -> {
                if (!json.isJsonObject()) {
                    TaintedMagic.LOG.error("Tainted tuning {}: not an object (skipped)", file);
                    return;
                }
                for (Map.Entry<String, JsonElement> group : json.getAsJsonObject().entrySet()) {
                    if (!group.getValue().isJsonObject()) {
                        TaintedMagic.LOG.error("Tainted tuning {}: '{}' is not an object (skipped)", file, group.getKey());
                        continue;
                    }
                    JsonObject g = group.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : g.entrySet()) {
                        String key = group.getKey() + "." + e.getKey();
                        Spec spec = DEFAULTS.get(key);
                        if (spec == null || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isNumber()) {
                            TaintedMagic.LOG.error("Tainted tuning {}: unknown or non-numeric '{}' (skipped)", file, key);
                            continue;
                        }
                        double v = e.getValue().getAsDouble();
                        if (v < spec.min() || v > spec.max()) {
                            TaintedMagic.LOG.error("Tainted tuning {}: '{}' = {} outside {}..{} (skipped)", file, key, v, spec.min(), spec.max());
                            continue;
                        }
                        next.put(key, v);
                    }
                }
            });
            values = Map.copyOf(next);
            TaintedMagic.LOG.info("Tainted tuning: {} files, {} values", files.size(), next.size());
        }
    }
}

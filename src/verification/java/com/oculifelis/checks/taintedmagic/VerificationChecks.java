package com.oculifelis.checks.taintedmagic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leclowndu93150.thaumaturge.api.spell.CastStyle;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.taintedmagic.TMDataComponents;
import com.taintedmagic.TMRegistry;
import com.taintedmagic.TaintedMagic;
import com.taintedmagic.data.TMTuning;
import com.taintedmagic.event.GearEvents;
import com.taintedmagic.event.ToolEvents;
import com.taintedmagic.item.SashItem;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Development checks loaded from the verification source set, separate from the ordinary mod jar. Each test logs one
 * "CHECKS" line with what it measured, so the run log doubles as the test record.
 */
@Mod(VerificationChecks.ID)
public final class VerificationChecks {
    public static final String ID = "taintedmagic_checks";
    private static final String TARGET = TaintedMagic.MODID;
    private static final Logger LOG = LogUtils.getLogger();
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("registered_items_have_names_and_models", VerificationChecks::items);
        TESTS.put("recipe_resources_load", VerificationChecks::recipes);
        TESTS.put("spell_parts_load", VerificationChecks::spellParts);
        TESTS.put("taint_focus_hurts_and_afflicts", VerificationChecks::taintFocus);
        TESTS.put("shockwave_focus_hits_everything_near", VerificationChecks::shockwaveFocus);
        TESTS.put("mage_mace_focus_hurts", VerificationChecks::maceFocus);
        TESTS.put("time_focus_moves_the_clock", VerificationChecks::timeFocus);
        TESTS.put("meteorology_focus_toggles_rain", VerificationChecks::weatherFocus);
        TESTS.put("katana_charged_strike", VerificationChecks::katana);
        TESTS.put("disassembler_cycles_modes", VerificationChecks::disassembler);
        TESTS.put("sash_toggles_speed", VerificationChecks::sash);
        TESTS.put("fortress_set_bonus", VerificationChecks::fortressSet);
        TESTS.put("fortress_magic_ward", VerificationChecks::magicWard);
        TESTS.put("bloodlust_blood_drops", VerificationChecks::bloodlust);
        TESTS.put("tuning_defaults_and_balance", VerificationChecks::balance);
        TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
    }

    public VerificationChecks(IEventBus bus) {
        FUNCTIONS.register(bus);
        bus.addListener(VerificationChecks::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
            event.registerTest(id(name), new FunctionGameTestInstance(function, new TestData<>(environment, id("arena"), 100, 0, true)));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    private static JsonObject asset(String path) {
        try (InputStream stream = TaintedMagic.class.getResourceAsStream("/assets/" + TARGET + "/" + path)) {
            if (stream == null) throw new IllegalStateException("Missing asset " + TARGET + ":" + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(path, ex);
        }
    }

    // ---- registries and data ----

    private static void items(GameTestHelper h) {
        JsonObject lang = asset("lang/en_us.json");
        int count = 0;
        for (var item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            if (!TARGET.equals(key.getNamespace())) continue;
            String description = item.getDescriptionId();
            h.assertTrue(lang.has(description) && !lang.get(description).getAsString().isBlank(), "English name for " + key + " (" + description + ")");
            JsonObject model = asset("items/" + key.getPath() + ".json");
            h.assertTrue(model.has("model"), "Item model definition for " + key);
            count++;
        }
        h.assertTrue(count > 0, "The mod registers items");
        LOG.info("CHECKS {} item registry, English names and model definitions: {} checked", TARGET, count);
        h.succeed();
    }

    private static void recipes(GameTestHelper h) {
        var manager = h.getLevel().getServer().getRecipeManager();
        int required = 0, conditionalLoaded = 0, conditionalAbsent = 0;
        var resources = h.getLevel().getServer().getResourceManager().listResources("recipe", key -> TARGET.equals(key.getNamespace()) && key.getPath().endsWith(".json"));
        for (var entry : resources.entrySet()) {
            Identifier file = entry.getKey();
            JsonObject json;
            try (var reader = entry.getValue().openAsReader()) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(file.toString(), ex);
            }
            String path = file.getPath().substring("recipe/".length(), file.getPath().length() - ".json".length());
            var key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(TARGET, path));
            boolean loaded = manager.byKey(key).isPresent();
            if (json.has("neoforge:conditions")) {
                if (loaded) conditionalLoaded++; else conditionalAbsent++;
            } else {
                h.assertTrue(loaded, "Recipe decoded and loaded: " + file);
                required++;
            }
        }
        h.assertTrue(required > 0, "The mod has unconditional recipes to verify");
        LOG.info("CHECKS {} recipes: {} required loaded, {} conditional loaded, {} conditional absent", TARGET, required, conditionalLoaded, conditionalAbsent);
        h.succeed();
    }

    private static final List<String> PARTS = List.of("eldritch", "mage_mace", "meteorology", "taint", "tainted_shockwave", "time", "vis_shard");

    private static ResourceKey<SpellPart> part(String name) {
        return ResourceKey.create(SpellPart.REGISTRY_KEY, Identifier.fromNamespaceAndPath(TARGET, name));
    }

    private static void spellParts(GameTestHelper h) {
        var registry = h.getLevel().registryAccess().lookupOrThrow(SpellPart.REGISTRY_KEY);
        StringBuilder seen = new StringBuilder();
        for (String name : PARTS) {
            var holder = registry.get(part(name));
            h.assertTrue(holder.isPresent(), "Spell part loaded: " + name);
            SpellPart sp = holder.get().value();
            seen.append(name).append('=').append(sp.behavior().getClass().getSimpleName()).append(' ');
        }
        LOG.info("CHECKS {} spell parts: {}", TARGET, seen.toString().trim());
        h.succeed();
    }

    // ---- foci ----

    private static ServerPlayer caster(GameTestHelper h) {
        // A fake player has no client, so mods that sync on join (Curios) send it nothing.
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "tm_check"));
        Vec3 at = h.absoluteVec(new Vec3(0.5, 2, 0.5));
        player.snapTo(at.x, at.y, at.z, 0, 0);
        player.setGameMode(GameType.SURVIVAL);
        player.connection.markClientLoaded(); // a player whose client never loaded takes no damage
        return player;
    }

    private static void cast(ServerPlayer caster, String partName, List<SpellTarget> targets) {
        Spell spell = new Spell(CastStyle.INSTANT, SpellNode.of(Spell.ORIGIN).then(SpellNode.of(part(partName))));
        Spells.cast(caster, ItemStack.EMPTY, spell, targets, 1.0F);
    }

    private static Pig pig(GameTestHelper h, int x, int z) {
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, x, 2, z);
        pig.setHealth(pig.getMaxHealth());
        return pig;
    }

    private static void taintFocus(GameTestHelper h) {
        ServerPlayer player = caster(h);
        Pig pig = pig(h, 1, 1);
        float before = pig.getHealth();
        cast(player, "taint", List.of(SpellTarget.entity(pig, new Vec3(0, 0, 1))));
        h.runAfterDelay(2, () -> {
            h.assertTrue(pig.getHealth() < before, "Taint focus hurt the pig (" + before + " -> " + pig.getHealth() + ")");
            h.assertTrue(pig.hasEffect(TTMobEffects.FLUX_TAINT), "Taint focus applied flux taint");
            h.assertTrue(pig.hasEffect(MobEffects.POISON), "Taint focus applied poison");
            LOG.info("CHECKS {} taint focus: pig {} -> {}, flux taint {}s, poison {}s", TARGET, before, pig.getHealth(),
                    pig.getEffect(TTMobEffects.FLUX_TAINT).getDuration() / 20, pig.getEffect(MobEffects.POISON).getDuration() / 20);
            h.succeed();
        });
    }

    private static void shockwaveFocus(GameTestHelper h) {
        ServerPlayer player = caster(h);
        Pig a = pig(h, 1, 1);
        Pig b = pig(h, 3, 3);
        float before = a.getHealth();
        cast(player, "tainted_shockwave", List.of(SpellTarget.entity(a, new Vec3(0, 0, 1))));
        h.runAfterDelay(2, () -> {
            h.assertTrue(a.getHealth() < before, "Shockwave hurt the pig at the centre");
            h.assertTrue(b.getHealth() < before, "Shockwave hurt the pig nearby");
            LOG.info("CHECKS {} tainted shockwave: centre pig {} -> {}, near pig {} -> {}", TARGET, before, a.getHealth(), before, b.getHealth());
            h.succeed();
        });
    }

    private static void maceFocus(GameTestHelper h) {
        ServerPlayer player = caster(h);
        Pig pig = pig(h, 1, 1);
        float before = pig.getHealth();
        cast(player, "mage_mace", List.of(SpellTarget.entity(pig, new Vec3(0, 0, 1))));
        h.runAfterDelay(2, () -> {
            h.assertTrue(pig.getHealth() < before, "Mage's mace focus hurt the pig (" + before + " -> " + pig.getHealth() + ")");
            LOG.info("CHECKS {} mage's mace: pig {} -> {}", TARGET, before, pig.getHealth());
            h.succeed();
        });
    }

    private static void timeFocus(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = caster(h);
        var clock = level.dimensionTypeRegistration().value().defaultClock();
        h.assertTrue(clock.isPresent(), "The test dimension has a world clock");
        long before = level.getServer().clockManager().getTotalTicks(clock.get());
        cast(player, "time", List.of(SpellTarget.origin(player)));
        long after = level.getServer().clockManager().getTotalTicks(clock.get());
        h.assertTrue(after - before >= 11000, "Time focus moved the clock (" + before + " -> " + after + ")");
        LOG.info("CHECKS {} time focus: clock moved {} ticks", TARGET, after - before);
        h.succeed();
    }

    private static void weatherFocus(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer player = caster(h);
        boolean before = level.getWeatherData().isRaining();
        cast(player, "meteorology", List.of(SpellTarget.origin(player)));
        boolean after = level.getWeatherData().isRaining();
        h.assertTrue(before != after, "Meteorology focus toggled rain (" + before + " -> " + after + ")");
        // The focus reads the rain level the sky shows, which takes about a second to ramp, as in the original.
        h.runAfterDelay(60, () -> {
            cast(player, "meteorology", List.of(SpellTarget.origin(player)));
            boolean back = level.getWeatherData().isRaining();
            h.assertTrue(back == before, "A second cast toggled it back");
            LOG.info("CHECKS {} meteorology focus: rain {} -> {} -> {}", TARGET, before, after, back);
            h.succeed();
        });
    }

    // ---- tools and gear ----

    private static void katana(GameTestHelper h) {
        ServerPlayer player = caster(h);
        Vec3 at = h.absoluteVec(new Vec3(1.5, 0, 0.5));
        player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        var pig = h.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 0, 2);
        ItemStack katana = new ItemStack(TMRegistry.SHADOWMETAL_KATANA.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, katana);
        player.getAttributes().addTransientAttributeModifiers(attackModifiers(katana));
        float before = pig.getHealth();
        int duration = katana.getUseDuration(player);
        katana.releaseUsing(h.getLevel(), player, duration - 40);
        h.assertTrue(pig.getHealth() < before, "A full charge strike hurt the pig (" + before + " -> " + pig.getHealth() + ")");
        h.assertTrue(pig.hasEffect(MobEffects.SLOWNESS), "Shadowmetal katana applied slowness on hit");
        h.assertTrue(player.getCooldowns().isOnCooldown(katana), "The katana went on cooldown");
        LOG.info("CHECKS {} shadowmetal katana: attack {} , pig {} -> {}, slowness {}", TARGET,
                player.getAttributeValue(Attributes.ATTACK_DAMAGE), before, pig.getHealth(), pig.hasEffect(MobEffects.SLOWNESS));
        h.succeed();
    }

    private static com.google.common.collect.Multimap<Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> attackModifiers(ItemStack stack) {
        com.google.common.collect.Multimap<Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> map =
                com.google.common.collect.HashMultimap.create();
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .forEach(EquipmentSlot.MAINHAND, map::put);
        return map;
    }

    private static void disassembler(GameTestHelper h) {
        ServerPlayer player = caster(h);
        player.setShiftKeyDown(true);
        ItemStack tool = new ItemStack(TMRegistry.THAUMIC_DISASSEMBLER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var start = tool.get(TMDataComponents.MINING_MODES.get());
        h.assertTrue(start != null && start.speeds().size() >= 2, "The disassembler has mining modes");
        StringBuilder order = new StringBuilder().append(start.speed());
        for (int i = 0; i < start.speeds().size(); i++) {
            tool.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            order.append(" > ").append(tool.get(TMDataComponents.MINING_MODES.get()).speed());
        }
        h.assertTrue(tool.get(TMDataComponents.MINING_MODES.get()).speed() == start.speed(), "Modes wrap around to the first one");
        LOG.info("CHECKS {} disassembler mining modes: {}", TARGET, order);
        h.succeed();
    }

    private static void sash(GameTestHelper h) {
        ServerPlayer player = caster(h);
        player.setShiftKeyDown(true);
        ItemStack sash = new ItemStack(TMRegistry.VOIDWALKER_SASH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, sash);
        boolean before = SashItem.speedMode(sash);
        sash.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(SashItem.speedMode(sash) != before, "Sneak-use toggled the sash speed mode");
        LOG.info("CHECKS {} voidwalker sash: speed mode {} -> {}", TARGET, before, SashItem.speedMode(sash));
        h.succeed();
    }

    private static void fortressSet(GameTestHelper h) {
        ServerPlayer player = caster(h);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TMRegistry.SHADOW_FORTRESS_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TMRegistry.SHADOW_FORTRESS_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(TMRegistry.SHADOW_FORTRESS_LEGGINGS.get()));
        for (int i = 1; i <= 20; i++) {
            player.tickCount = i;
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        }
        var armor = player.getAttribute(Attributes.ARMOR).getModifier(TaintedMagic.id("fortress_set_armor"));
        var tough = player.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(TaintedMagic.id("fortress_set_toughness"));
        h.assertTrue(armor != null && armor.amount() >= 2, "Three fortress pieces give the set armour bonus (" + (armor == null ? "none" : armor.amount()) + ")");
        h.assertTrue(tough != null && tough.amount() >= 2, "Three fortress pieces give the set toughness bonus");
        LOG.info("CHECKS {} fortress set: armour +{}, toughness +{}", TARGET, armor.amount(), tough.amount());
        h.succeed();
    }

    private static void magicWard(GameTestHelper h) {
        var bare = h.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
        var armored = h.spawnWithNoFreeWill(EntityType.ZOMBIE, 3, 2, 3);
        armored.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TMRegistry.SHADOW_FORTRESS_HELMET.get()));
        armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TMRegistry.SHADOW_FORTRESS_CHESTPLATE.get()));
        armored.setItemSlot(EquipmentSlot.LEGS, new ItemStack(TMRegistry.SHADOW_FORTRESS_LEGGINGS.get()));
        ServerLevel level = h.getLevel();
        bare.hurtServer(level, level.damageSources().magic(), 10.0F);
        armored.hurtServer(level, level.damageSources().magic(), 10.0F);
        float lostBare = bare.getMaxHealth() - bare.getHealth();
        float lostArmored = armored.getMaxHealth() - armored.getHealth();
        h.assertTrue(lostBare > 0, "Magic damage reached the unarmoured zombie");
        h.assertTrue(lostArmored < lostBare, "Shadow fortress armour cut magic damage (" + lostBare + " vs " + lostArmored + ")");
        LOG.info("CHECKS {} fortress magic ward: bare lost {}, armoured lost {}", TARGET, lostBare, lostArmored);
        h.succeed();
    }

    private static void bloodlust(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Pig pig = pig(h, 1, 1);
        int none = 0, max = 0, total = 0;
        for (int i = 0; i < 400; i++) {
            ItemEntity drop = ToolEvents.blood(level, pig);
            if (drop == null) { none++; continue; }
            h.assertTrue(drop.getItem().is(TMRegistry.CRYSTAL_BLOOD.get()), "Bloodlust drops crystal blood");
            max = Math.max(max, drop.getItem().getCount());
            total += drop.getItem().getCount();
        }
        int cap = (int) TMTuning.get("bloodlust.max_drops");
        h.assertTrue(max <= cap && max >= 1, "Blood drops stay within 1.." + cap + " (max seen " + max + ")");
        h.assertTrue(none > 0 && none < 400, "Some kills drop nothing, most drop blood (" + none + " of 400 empty)");
        h.assertTrue(new ItemStack(TMRegistry.CRYSTAL_DAGGER.get()).is(ToolEvents.BLOODLUST), "The crystal dagger is a bloodlust weapon");
        LOG.info("CHECKS {} bloodlust: 400 kills, {} empty, max {}, mean {}", TARGET, none, max, String.format("%.2f", total / 400.0));
        h.succeed();
    }

    private static void balance(GameTestHelper h) {
        for (var e : TMTuning.DEFAULTS.entrySet()) {
            double v = TMTuning.get(e.getKey());
            h.assertTrue(v >= e.getValue().min() && v <= e.getValue().max(), "Tuning " + e.getKey() + " in range (" + v + ")");
        }
        h.assertTrue(TMTuning.get("katana.full_charge_ticks") == 40, "Katana full charge is 40 ticks");
        h.assertTrue(TMTuning.get("warpwood_core.base_interval_ticks") == 200, "Warpwood refill base interval is 200 ticks");
        h.assertTrue(Math.abs(TMRegistry.CAP_CLOTH.get().baseCostModifier() - 0.95F) < 1e-4, "Cloth cap cost 0.95");
        h.assertTrue(Math.abs(TMRegistry.CAP_SHADOWCLOTH.get().baseCostModifier() - 0.90F) < 1e-4, "Shadowcloth cap cost 0.90");
        h.assertTrue(Math.abs(TMRegistry.CAP_CRIMSONCLOTH.get().baseCostModifier() - 0.85F) < 1e-4, "Crimson cloth cap cost 0.85");
        h.assertTrue(Math.abs(TMRegistry.CAP_SHADOWMETAL.get().baseCostModifier() - 0.70F) < 1e-4, "Shadowmetal cap cost 0.70");
        h.assertTrue(TMRegistry.ROD_WARPWOOD.get().capacity() == 250, "Warpwood rod holds 250");
        h.assertTrue(TMRegistry.ROD_WARPWOOD_STAFF.get().capacity() == 500, "Warpwood staff holds 500");
        double[] katanas = {damage(TMRegistry.THAUMIUM_KATANA.get().getDefaultInstance()), damage(TMRegistry.VOIDMETAL_KATANA.get().getDefaultInstance()),
                damage(TMRegistry.SHADOWMETAL_KATANA.get().getDefaultInstance())};
        h.assertTrue(katanas[0] == 14.25 && katanas[1] == 17.55 && katanas[2] == 20.75, "Katana damage 14.25 / 17.55 / 20.75");
        var food = TMRegistry.MAGIC_FUNGUAR.get().getDefaultInstance().get(DataComponents.FOOD);
        h.assertTrue(food != null && food.nutrition() == 5, "Magic funguar restores 5 hunger");
        h.assertTrue(TMRegistry.MAGIC_FUNGUAR.get().getDefaultInstance().has(TMDataComponents.ASPECT_REWARD.get()), "Magic funguar grants aspects");
        h.assertTrue(new ItemStack(TMRegistry.SHADOW_FORTRESS_HELMET.get()).is(GearEvents.FORTRESS_ARMOR), "Shadow fortress helm is fortress armour");
        h.assertTrue(new ItemStack(TMRegistry.VOIDWALKER_BOOTS.get()).is(GearEvents.VOIDWALKER_BOOTS), "Voidwalker boots are tagged");
        h.assertTrue(new ItemStack(TMRegistry.VOIDWALKER_SASH.get()).is(GearEvents.SPEED_SASHES), "Voidwalker sash is a speed sash");
        LOG.info("CHECKS {} balance: {} tuning keys in range, caps 0.95/0.90/0.85/0.70, rods 250/500, katanas {}/{}/{}, funguar food 5",
                TARGET, TMTuning.DEFAULTS.size(), katanas[0], katanas[1], katanas[2]);
        h.succeed();
    }

    private static double damage(ItemStack stack) {
        double[] out = {0};
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.is(Attributes.ATTACK_DAMAGE)) out[0] += mod.amount();
        });
        return out[0];
    }
}

package com.taintedmagic.client;

import com.leclowndu93150.thaumaturge.client.entity.TTModelLayers;
import com.leclowndu93150.thaumaturge.client.model.gear.FortressArmorModel;
import com.taintedmagic.TMRegistry;
import com.taintedmagic.TaintedMagic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client side: the fortress sets wear Thaumaturge's fortress armour model, as the original wore Thaumcraft's. */
@Mod(value = TaintedMagic.MODID, dist = Dist.CLIENT)
public final class TaintedMagicClient {
    public TaintedMagicClient(IEventBus modBus) {
        modBus.addListener(TaintedMagicClient::onClientExtensions);
    }

    private static void onClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new FortressModel(), TMRegistry.SHADOW_FORTRESS_HELMET.get(), TMRegistry.SHADOW_FORTRESS_CHESTPLATE.get(),
                TMRegistry.SHADOW_FORTRESS_LEGGINGS.get(), TMRegistry.VOID_FORTRESS_HELMET.get(),
                TMRegistry.VOID_FORTRESS_CHESTPLATE.get(), TMRegistry.VOID_FORTRESS_LEGGINGS.get());
    }

    /** Picks the head, chest or legs part of the fortress model by the layer and the piece's equip slot. */
    private static final class FortressModel implements IClientItemExtensions {
        private HumanoidModel<HumanoidRenderState> head;
        private HumanoidModel<HumanoidRenderState> chest;
        private HumanoidModel<HumanoidRenderState> legs;

        @Override
        public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layer, Model original) {
            if (layer == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS) {
                if (legs == null) legs = bake(TTModelLayers.FORTRESS_ARMOR_LEGS);
                return legs;
            }
            Equippable eq = stack.get(DataComponents.EQUIPPABLE);
            if (eq != null && eq.slot() == EquipmentSlot.HEAD) {
                if (head == null) head = bake(TTModelLayers.FORTRESS_ARMOR_HEAD);
                return head;
            }
            if (chest == null) chest = bake(TTModelLayers.FORTRESS_ARMOR_CHEST);
            return chest;
        }

        private static HumanoidModel<HumanoidRenderState> bake(ModelLayerLocation layer) {
            return new FortressArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
        }
    }
}

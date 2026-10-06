package com.godofthings.item;

import com.godofthings.Godofthings;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

/**
 * 神之护甲材质：数值沿用下界合金，但<b>穿戴图层指向本模组自己的贴图</b>。
 *
 * <p><b>为什么不再直接用 {@link ArmorMaterials#NETHERITE}</b>：那是原版材质 Holder，
 * 穿在身上的盔甲模型会画原版下界合金的「黑紫」图层，与物品图标（金/紫/青的模组视觉语言）
 * 不一致。自定义 {@code assetName = "godofthings"} 后，游戏会加载
 * {@code assets/godofthings/textures/models/armor/godofthings_layer_1.png}（外层）与
 * {@code _layer_2.png}（内层，腿甲用），这两张图由原版下界合金图层按本模组色板重着色而来
 * —— <b>形状/明暗结构与原版逐像素一致</b>，只换色，因此穿戴贴合度与原版完全相同。</p>
 *
 * <p>数值（防御/韧性/击退抗性/附魔附着力/修复材料）与下界合金保持一致 ——
 * 本模组护甲的强度来自套装效果，不靠面板数值，避免平衡偏移。</p>
 */
public final class GodArmorMaterials
{
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Godofthings.MODID);

    /** 神之护甲材质（图层 → godofthings:textures/models/armor/godofthings_layer_N.png） */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOD =
            ARMOR_MATERIALS.register("god", () -> new ArmorMaterial(
                    // 防御值与下界合金一致（3/8/6/3 + 韧性 3 + 击退抗性 0.1）
                    Map.of(
                            ArmorItem.Type.HELMET, 3,
                            ArmorItem.Type.CHESTPLATE, 8,
                            ArmorItem.Type.LEGGINGS, 6,
                            ArmorItem.Type.BOOTS, 3
                    ),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    // 修复材料：下界合金锭（与 setNoRepair 并存；setNoRepair 优先，铁砧无法修复）
                    () -> Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT),
                    List.of(new ArmorMaterial.Layer(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "godofthings"))),
                    3.0F,
                    0.1F
            ));

    private GodArmorMaterials() {}

    /** 注册到模组总线（Godofthings 构造函数调用）。 */
    public static void register(IEventBus modEventBus)
    {
        ARMOR_MATERIALS.register(modEventBus);
    }
}

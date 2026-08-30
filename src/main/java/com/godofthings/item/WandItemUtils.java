package com.godofthings.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 造化垂青之杖的工具方法（对应 useless_mod 的 UselessItemUtils 中与本物品相关的部分）。
 */
public final class WandItemUtils
{
    private static final int POTION_DURATION = 20000;

    private WandItemUtils() {}

    /** 玩家背包中存在造化垂青之杖时，持续施加配置的药水效果。 */
    public static void applyWandEffects(Player player)
    {
        if (player == null)
        {
            return;
        }
        if (WandConfig.shouldEnablePotionEffects())
        {
            for (String effectConfig : WandConfig.getCustomPotionEffects())
            {
                applyPotionEffectFromConfig(player, effectConfig);
            }
        }
    }

    /** 解析 "modid:effect,amplifier" 并施加药水效果。 */
    private static void applyPotionEffectFromConfig(Player player, String effectConfig)
    {
        try
        {
            String[] parts = effectConfig.split(",");
            if (parts.length != 2)
            {
                return;
            }
            String effectId = parts[0];
            int amplifier = Integer.parseInt(parts[1]) - 1;

            ResourceLocation location = ResourceLocation.tryParse(effectId);
            if (location == null)
            {
                return;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(location);
            if (effect == null)
            {
                return;
            }
            MobEffectInstance current = player.getEffect(effect);
            if (current == null || current.getDuration() < 200)
            {
                player.addEffect(new MobEffectInstance(effect, POTION_DURATION, Math.max(0, amplifier), true, false, true));
            }
        }
        catch (Exception ignored)
        {
            // 静默处理配置解析错误
        }
    }

    /** 强制击杀后，若开启了捕捉模式，掉落对应生物的刷怪蛋。 */
    public static void tryCaptureSpawnEgg(LivingEntity killedEntity, ItemStack stack, Player player)
    {
        if (killedEntity.level().isClientSide()
                || !(stack.getItem() instanceof GodFavorWandItem)
                || !WandModes.isBeefCaptureEnabled(stack))
        {
            return;
        }
        SpawnEggItem spawnEgg = SpawnEggItem.byId(killedEntity.getType());
        if (spawnEgg == null)
        {
            return;
        }
        ItemStack spawnEggStack = new ItemStack(spawnEgg);
        if (spawnEggStack.isEmpty())
        {
            return;
        }
        if (!player.getInventory().add(spawnEggStack))
        {
            player.drop(spawnEggStack, false);
        }
    }

    /** 掉落处理：AE2 优先 -> 玩家背包。返回是否全部处理（true=无需再掉落）。 */
    public static boolean addToInventoryOrAE(Player player, ItemStack drop, ItemStack tool)
    {
        List<ItemStack> list = new ArrayList<>();
        list.add(drop);
        GodFavorWandItem.handleDrops(list, player, tool);
        return list.isEmpty();
    }

    /** 在精准采集 / 时运之间切换附魔（并切换 CustomModelData 纹理）。 */
    public static void switchEnchant(ItemStack stack, boolean fortune)
    {
        Map<Enchantment, Integer> map = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (fortune)
        {
            map.remove(Enchantments.SILK_TOUCH);
            map.put(Enchantments.BLOCK_FORTUNE, WandConfig.getFortuneLevel());
            map.put(Enchantments.MOB_LOOTING, WandConfig.getLootingLevel());
        }
        else
        {
            map.remove(Enchantments.BLOCK_FORTUNE);
            map.put(Enchantments.SILK_TOUCH, 1);
            map.put(Enchantments.MOB_LOOTING, WandConfig.getLootingLevel());
        }
        EnchantmentHelper.setEnchantments(map, stack);
        // 0 = 默认精准采集纹理，1 = 时运纹理
        stack.getOrCreateTag().putInt("CustomModelData", fortune ? 1 : 0);
    }
}

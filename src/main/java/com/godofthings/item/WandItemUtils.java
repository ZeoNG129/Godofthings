package com.godofthings.item;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * 造化垂青之杖的工具方法（对应 useless_mod 的 UselessItemUtils 中与本物品相关的部分）。
 * 1.21.1：附魔走 ItemEnchantments 组件，纹理切换走 CUSTOM_MODEL_DATA 组件。
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
            // 1.21.1：MOB_EFFECT 注册表查询返回 Holder<MobEffect>
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(location).orElse(null);
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

    /** 在精准采集 / 时运之间切换附魔（并切换 CUSTOM_MODEL_DATA 纹理）。 */
    public static void switchEnchant(ItemStack stack, boolean fortune, RegistryAccess access)
    {
        ItemEnchantments current = stack.getEnchantments();

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        // 保留无关附魔，silk/fortune/looting 由下方逻辑重设
        for (Holder<Enchantment> holder : current.keySet())
        {
            if (holder.is(Enchantments.SILK_TOUCH)
                    || holder.is(Enchantments.FORTUNE)
                    || holder.is(Enchantments.LOOTING))
            {
                continue;
            }
            mutable.set(holder, current.getLevel(holder));
        }

        if (fortune)
        {
            mutable.set(enchantHolder(Enchantments.FORTUNE, access), WandConfig.getFortuneLevel());
            mutable.set(enchantHolder(Enchantments.LOOTING, access), WandConfig.getLootingLevel());
        }
        else
        {
            mutable.set(enchantHolder(Enchantments.SILK_TOUCH, access), 1);
            mutable.set(enchantHolder(Enchantments.LOOTING, access), WandConfig.getLootingLevel());
        }
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        // 0 = 默认精准采集纹理，1 = 时运纹理
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(fortune ? 1 : 0));
    }

    /** Enchantments.X 常量（ResourceKey）转 Holder（附魔为数据驱动注册表，需 RegistryAccess）。 */
    public static Holder<Enchantment> enchantHolder(ResourceKey<Enchantment> key, RegistryAccess access)
    {
        return access.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }
}

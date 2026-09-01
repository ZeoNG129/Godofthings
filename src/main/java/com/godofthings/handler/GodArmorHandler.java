package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.item.GodArmorItem;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之护甲效果：穿全套时提供创造飞行、无敌（含 kill）、永不饥饿、火焰免疫、水下呼吸、夜视。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class GodArmorHandler
{
    /** 是否穿齐神之头/甲/腿/鞋全套 */
    public static boolean isFullSetWorn(Player player)
    {
        return player.getInventory().getArmor(3).getItem() instanceof GodArmorItem
                && player.getInventory().getArmor(2).getItem() instanceof GodArmorItem
                && player.getInventory().getArmor(1).getItem() instanceof GodArmorItem
                && player.getInventory().getArmor(0).getItem() instanceof GodArmorItem;
    }

    // 免疫所有伤害（含 kill 指令、火焰、熔岩、虚空、窒息等）
    // NeoForge 1.21.1：旧 LivingHurtEvent / LivingDamageEvent（可取消）由 LivingIncomingDamageEvent
    // 取代——在伤害处理链最前触发，取消即整个伤害流程终止（与旧版双事件取消等价）。
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event)
    {
        if (event.getEntity() instanceof Player player && isFullSetWorn(player))
        {
            event.setCanceled(true);
        }
    }

    // 兜底：即使伤害漏过也绝不会死
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event)
    {
        if (event.getEntity() instanceof Player player && isFullSetWorn(player))
        {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            player.setRemainingFireTicks(0);
        }
    }

    // 免疫所有负面状态：阻止负面药水效果被施加
    // NeoForge 1.21.1：MobEffectEvent.Applicable 不再用 Event.Result.DENY，改为 setResult(Result.DO_NOT_APPLY)
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event)
    {
        if (event.getEntity() instanceof Player player && isFullSetWorn(player)
                && event.getEffectInstance() != null
                && !event.getEffectInstance().getEffect().value().isBeneficial())
        {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
        if (player == null)
        {
            return;
        }
        // 取消飞行惯性（无飞行漂移）：飞行移动由客户端 LocalPlayer 主导，
        // 只在服务端清零无效，必须双端都在移动处理完毕后（旧 END 阶段 → Post 事件）把水平速度归零
        if (player.getAbilities().flying
                && isFullSetWorn(player)
                && player.xxa == 0.0F && player.zza == 0.0F)
        {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(0.0D, motion.y, 0.0D);
        }
        // NeoForge 1.21.1：PlayerTickEvent 无 phase/side 字段，用 level().isClientSide 过滤逻辑服务端
        if (player.level().isClientSide)
        {
            return;
        }
        if (isFullSetWorn(player))
        {
            grantFlight(player);
            player.getFoodData().setFoodLevel(20);          // 永不饥饿
            player.getFoodData().setSaturation(20.0F);      // 无限饱和度
            player.setRemainingFireTicks(0);                // 火焰/熔岩免疫（去除着火反馈）
            if (player.isUnderWater())
            {
                player.setAirSupply(player.getMaxAirSupply()); // 水下呼吸
            }
            removeDebuffs(player); // 免疫所有负面状态
            // 夜视：客户端以改伽马值方式实现（无闪烁），见 GodArmorClientHandler
        }
        else
        {
            revokeFlight(player);
        }
    }

    /** 移除已有的所有负面状态（中毒、凋零、缓慢等） */
    // 1.21.1：MobEffect 以 Holder<MobEffect> 标识（MobEffectInstance.getEffect() / removeEffect 均为 Holder）
    private static void removeDebuffs(Player player)
    {
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects())
        {
            if (!effect.getEffect().value().isBeneficial())
            {
                toRemove.add(effect.getEffect());
            }
        }
        for (Holder<MobEffect> effect : toRemove)
        {
            player.removeEffect(effect);
        }
    }

    /** 授予创造飞行（取消飞行惯性、速度同创造） */
    private static void grantFlight(Player player)
    {
        var abilities = player.getAbilities();
        boolean changed = false;
        if (!abilities.mayfly)
        {
            abilities.mayfly = true;
            changed = true;
        }
        if (abilities.getFlyingSpeed() != 0.05F)
        {
            abilities.setFlyingSpeed(0.05F);
            changed = true;
        }
        if (changed)
        {
            player.onUpdateAbilities();
        }
    }

    /** 脱下时收回飞行能力（保留真实创造/旁观者） */
    private static void revokeFlight(Player player)
    {
        var abilities = player.getAbilities();
        boolean changed = false;
        if (!player.isCreative() && !player.isSpectator())
        {
            if (abilities.mayfly)
            {
                abilities.mayfly = false;
                changed = true;
            }
            if (abilities.flying)
            {
                abilities.flying = false;
                changed = true;
            }
        }
        if (abilities.getFlyingSpeed() != 0.05F)
        {
            abilities.setFlyingSpeed(0.05F);
            changed = true;
        }
        if (changed)
        {
            player.onUpdateAbilities();
        }
    }
}

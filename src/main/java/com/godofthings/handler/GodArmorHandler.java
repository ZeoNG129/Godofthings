package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.item.GodArmorItem;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之护甲效果：穿全套时提供创造飞行、无敌（含 kill）、永不饥饿、火焰免疫、水下呼吸、夜视。
 */
@Mod.EventBusSubscriber(modid = Godofthings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event)
    {
        if (event.getEntity() instanceof Player player && isFullSetWorn(player))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event)
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
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event)
    {
        if (event.getEntity() instanceof Player player && isFullSetWorn(player)
                && event.getEffectInstance() != null
                && !event.getEffectInstance().getEffect().isBeneficial())
        {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        Player player = event.player;
        if (player == null)
        {
            return;
        }
        // 取消飞行惯性（无飞行漂移）：飞行移动由客户端 LocalPlayer 主导，
        // 只在服务端清零无效，必须双端都在移动处理完毕后（END 阶段）把水平速度归零
        if (event.phase == TickEvent.Phase.END
                && player.getAbilities().flying
                && isFullSetWorn(player)
                && player.xxa == 0.0F && player.zza == 0.0F)
        {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(0.0D, motion.y, 0.0D);
        }
        if (event.side != LogicalSide.SERVER)
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
    private static void removeDebuffs(Player player)
    {
        List<MobEffect> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects())
        {
            if (!effect.getEffect().isBeneficial())
            {
                toRemove.add(effect.getEffect());
            }
        }
        for (MobEffect effect : toRemove)
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

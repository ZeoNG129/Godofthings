package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.armor.GodArmorFeatures;
import com.godofthings.armor.GodArmorState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 由「特殊被动」移入套装功能的 6 项（<b>无等级、只有开关</b>）。
 * <p>万民敬仰 / 发光 / 暴食 / 无限交易 / 村民大师 在这里实现；
 * 碧波清眸是客户端雾效，在 {@code FogRendererMixin} 里读同一个开关位。
 * <p>所有效果都用 {@link GodArmorState#active(Player, int)} 判定 —— <b>穿齐全套且该开关打开</b>才生效。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class ArmorExtraFeatures
{
    private ArmorExtraFeatures() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        // 万民敬仰：每 5 秒补一次村庄英雄（时长 15 秒，避免闪烁）
        if (player.tickCount % 100 == 0 && GodArmorState.active(player, GodArmorFeatures.VILLAGE_HERO))
        {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 300, 0, false, false, false));
        }
        // 发光：每 2 秒给 35 格内的生物挂发光
        if (player.tickCount % 40 == 0 && GodArmorState.active(player, GodArmorFeatures.GLOW))
        {
            AABB box = player.getBoundingBox().inflate(35.0);
            for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box))
            {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false, false));
            }
        }
    }

    /** 暴食：进食瞬间完成 */
    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !GodArmorState.active(player, GodArmorFeatures.GLUTTONY))
        {
            return;
        }
        if (event.getItem().has(net.minecraft.core.component.DataComponents.FOOD))
        {
            event.setDuration(1);
        }
    }

    /** 无限交易：每次交易后重置次数 */
    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player
                && GodArmorState.active(player, GodArmorFeatures.UNLIMITED_TRADES)
                && event.getMerchantOffer() != null)
        {
            event.getMerchantOffer().resetUses();
        }
    }

    /** 村民大师：右键村民直接拉满等级 */
    @SubscribeEvent
    public static void onInteractVillager(PlayerInteractEvent.EntityInteract event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !GodArmorState.active(player, GodArmorFeatures.VILLAGER_MASTER))
        {
            return;
        }
        if (event.getTarget() instanceof Villager villager)
        {
            villager.setVillagerData(villager.getVillagerData().setLevel(5));
            villager.setVillagerXp(0);
        }
    }
}
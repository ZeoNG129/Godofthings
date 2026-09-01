package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.item.GodSwordItem;
import com.godofthings.item.SwordModes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 神之剑「吸星 / 吸魂」的服务端逐 tick 处理。
 * <ul>
 *   <li>吸星：手持神之剑时，把附近 16 格内的掉落物直接吸入背包（背包满则拉到脚下），并吸收经验。</li>
 *   <li>吸魂：手持神之剑时，把附近 16 格内的非玩家生物吸到玩家面前一格。</li>
 * </ul>
 * 两者独立开关（见 {@link SwordModes}），由神之剑功能面板（J 键）经网络在服务端切换。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class SwordEffectHandler
{
    /** 吸收半径（格）。 */
    private static final double ABSORB_RANGE = 16.0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            ItemStack sword = findSword(player);
            if (sword == null)
            {
                continue;
            }
            if (SwordModes.isStarAbsorbEnabled(sword))
            {
                absorbDropsAndXp(player);
            }
            if (SwordModes.isSoulAbsorbEnabled(sword))
            {
                absorbEntities(player);
            }
        }
    }

    /** 吸星：掉落物直接进背包 + 吸收经验。 */
    private static void absorbDropsAndXp(ServerPlayer player)
    {
        AABB aabb = player.getBoundingBox().inflate(ABSORB_RANGE);

        for (ItemEntity itemEntity : player.level().getEntitiesOfClass(ItemEntity.class, aabb))
        {
            if (itemEntity.isRemoved())
            {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (player.getInventory().add(stack))
            {
                // 全部进背包
                itemEntity.discard();
            }
            else
            {
                // 背包放不下：剩余部分拉到玩家脚下，避免卡在原处
                itemEntity.setItem(stack);
                itemEntity.teleportTo(player.getX(), player.getY() + 0.5, player.getZ());
            }
        }

        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, aabb))
        {
            if (orb.isRemoved())
            {
                continue;
            }
            player.giveExperiencePoints(orb.getValue());
            orb.discard();
        }
    }

    /** 吸魂：把附近非玩家生物吸到玩家面前一格。 */
    private static void absorbEntities(ServerPlayer player)
    {
        AABB aabb = player.getBoundingBox().inflate(ABSORB_RANGE);

        // 玩家视线水平方向前方 1 格
        double yaw = Math.toRadians(player.getYRot());
        double targetX = player.getX() - Math.sin(yaw);
        double targetY = player.getY();
        double targetZ = player.getZ() + Math.cos(yaw);

        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, aabb))
        {
            if (mob instanceof Player)
            {
                continue; // 不吸玩家（含自己）
            }
            if (mob.isRemoved() || mob.isDeadOrDying())
            {
                continue;
            }
            mob.teleportTo(targetX, targetY, targetZ);
        }
    }

    private static ItemStack findSword(ServerPlayer player)
    {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GodSwordItem)
        {
            return stack;
        }
        stack = player.getOffhandItem();
        if (stack.getItem() instanceof GodSwordItem)
        {
            return stack;
        }
        return null;
    }
}

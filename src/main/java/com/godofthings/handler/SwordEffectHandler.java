package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.item.BlackBoxData;
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
 *   <li>吸星：手持神之剑时，把附近（可调半径）掉落物吸收；若玩家同时携带已开启的神之黑盒，
 *       掉落物按黑盒白名单/黑名单判定（命中入黑盒、未命中销毁），否则直接进背包（满则拉到脚下）；并吸收经验。</li>
 *   <li>吸魂：手持神之剑时，把附近（可调半径）非玩家生物吸到玩家面前一格。</li>
 * </ul>
 * 两者独立开关、独立半径（3~300），见 {@link SwordModes}，由神之剑功能面板（J 键）经网络在服务端切换。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class SwordEffectHandler
{
    /** 扫描间隔（tick）：大范围全量扫描三类实体较贵，每 5 tick 扫一次（4 次/秒）即可，避免每 tick 掉 TPS。 */
    private static final int SCAN_INTERVAL = 5;
    /** 吸魂：目标已在玩家面前 1.5 格内时不再传送，避免每 tick 重复 teleportTo 造成生物抖动与海量移动事件。 */
    private static final double SOUL_TELEPORT_SQ = 1.5 * 1.5;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        tickCounter++;
        if (tickCounter < SCAN_INTERVAL)
        {
            return;
        }
        tickCounter = 0;

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
                absorbDropsAndXp(player, sword);
            }
            if (SwordModes.isSoulAbsorbEnabled(sword))
            {
                absorbEntities(player, sword);
            }
        }
    }

    /** 吸星：掉落物吸收（有黑盒走黑盒判定）+ 吸收经验。 */
    private static void absorbDropsAndXp(ServerPlayer player, ItemStack sword)
    {
        double range = SwordModes.getStarRange(sword);
        AABB aabb = player.getBoundingBox().inflate(range);
        ItemStack box = BlackBoxData.findEnabledBox(player);

        for (ItemEntity itemEntity : player.level().getEntitiesOfClass(ItemEntity.class, aabb))
        {
            if (itemEntity.isRemoved())
            {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty())
            {
                continue;
            }

            if (box.isEmpty())
            {
                // 无黑盒：直接进背包，放不下则拉到脚下
                if (player.getInventory().add(stack))
                {
                    itemEntity.discard();
                }
                else
                {
                    itemEntity.setItem(stack);
                    itemEntity.teleportTo(player.getX(), player.getY() + 0.5, player.getZ());
                }
            }
            else
            {
                // 有开启的黑盒：按白名单/黑名单判定，命中入黑盒（无堆叠上限）、未命中销毁
                if (BlackBoxData.shouldKeep(box, stack, player.level().registryAccess()))
                {
                    BlackBoxData.addToBox(box, stack, player.level().registryAccess());
                }
                stack.setCount(0);
                itemEntity.discard();
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
    private static void absorbEntities(ServerPlayer player, ItemStack sword)
    {
        double range = SwordModes.getSoulRange(sword);
        AABB aabb = player.getBoundingBox().inflate(range);

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
            // 已在玩家面前的目标不再重复传送，避免每 tick teleportTo 造成的抖动与性能开销
            if (mob.distanceToSqr(targetX, targetY, targetZ) <= SOUL_TELEPORT_SQ)
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

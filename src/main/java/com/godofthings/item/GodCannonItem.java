package com.godofthings.item;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 神之炮：电磁炮，一击必杀。
 * <ul>
 *   <li>左键：长按持续发射贯穿激光——沿视线方向击杀路径上所有生物。</li>
 *   <li>右键：三层蓄力（约 1s / 2s 进入二、三层），松开发射范围性电磁炮，
 *       半径随层数增大（4 / 7 / 12 格）。</li>
 * </ul>
 */
public class GodCannonItem extends Item
{
    private static final double RANGE = 128.0;
    /** 蓄力层数 → AoE 半径（索引 0 未用） */
    private static final double[] AOE_RADIUS = { 0.0, 4.0, 7.0, 12.0 };
    /** 光束音效节流：记录上次播放雷声的游戏时间（tick），避免长按持续发射时每秒多次响雷 */
    private static long lastBeamSoundTick = -10L;

    public GodCannonItem(Properties properties)
    {
        super(properties);
    }

    // ==================== 左键：长按持续发射激光 ====================
    // 由客户端 CannonClientHandler 每 tick 检测攻击键（左键）是否按住，
    // 按住时经 CannonMessages 网络包驱动本方法，实现长按持续发射贯穿光束。

    public static void fireBeam(Player player)
    {
        Level level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(RANGE));

        AABB beamBox = new AABB(start, end).inflate(1.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, beamBox,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets)
        {
            if (target.getBoundingBox().inflate(0.5).clip(start, end).isPresent())
            {
                target.kill();
            }
        }

        if (level instanceof ServerLevel serverLevel)
        {
            spawnBeamParticles(serverLevel, start, end);
            // 节流 + 压低音量：约每 0.5s 播一次雷声，音量 0.2F，避免持续发射太吵
            long now = level.getGameTime();
            if (now - lastBeamSoundTick >= 10)
            {
                lastBeamSoundTick = now;
                level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.PLAYERS, 0.2F, 0.6F);
            }
        }
    }

    private static void spawnBeamParticles(ServerLevel level, Vec3 start, Vec3 end)
    {
        Vec3 dir = end.subtract(start).normalize();
        double dist = start.distanceTo(end);
        // 从玩家眼前 2 格后开始生成，避免火花糊脸遮挡视野
        double begin = Math.min(2.0, dist);
        // 步长 2.0：粒子密度降到约 1/4（128 格约 63 个），轨迹仍清晰可见但不遮挡视野
        for (double d = begin; d <= dist; d += 2.0)
        {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
        }
    }

    // ==================== 右键：三层蓄力 → 范围电磁炮 ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity)
    {
        return 72000; // 蓄力时长由释放时机决定（手动释放）
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks)
    {
        int chargeTicks = getUseDuration(stack, entity) - remainingUseTicks;
        int tier = chargeTier(chargeTicks);

        if (level.isClientSide)
        {
            spawnChargeParticles(level, entity, tier);
        }
        else if (entity instanceof Player player && (chargeTicks == 20 || chargeTicks == 40))
        {
            player.displayClientMessage(Component.translatable("message.godofthings.cannon.charge_" + tier), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT,
                    SoundSource.PLAYERS, 0.8F, 0.5F + tier * 0.25F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks)
    {
        int chargeTicks = getUseDuration(stack, entity) - remainingUseTicks;
        int tier = chargeTier(chargeTicks);
        if (!level.isClientSide)
        {
            fireAoE(level, entity, tier);
        }
    }

    private static int chargeTier(int chargeTicks)
    {
        if (chargeTicks >= 40) return 3;
        if (chargeTicks >= 20) return 2;
        return 1;
    }

    private void fireAoE(Level level, LivingEntity shooter, int tier)
    {
        double radius = AOE_RADIUS[tier];
        Vec3 start = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle();
        HitResult hit = shooter.pick(RANGE, 1.0F, false);
        Vec3 center = hit.getType() == HitResult.Type.MISS
                ? start.add(look.scale(RANGE))
                : hit.getLocation();

        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != shooter && e.isAlive());
        for (LivingEntity target : targets)
        {
            if (target.distanceToSqr(center) <= radius * radius)
            {
                target.kill();
            }
        }

        if (level instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                    60, radius * 0.5, radius * 0.5, radius * 0.5, 0.6);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.PLAYERS, 0.7F, 0.8F);
        }
    }

    private void spawnChargeParticles(Level level, LivingEntity entity, int tier)
    {
        Vec3 pos = entity.getEyePosition().add(entity.getLookAngle().scale(1.0));
        for (int i = 0; i < tier; i++)
        {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.x + (level.random.nextDouble() - 0.5) * 0.8,
                    pos.y + (level.random.nextDouble() - 0.5) * 0.8,
                    pos.z + (level.random.nextDouble() - 0.5) * 0.8,
                    0, 0, 0);
        }
    }
}

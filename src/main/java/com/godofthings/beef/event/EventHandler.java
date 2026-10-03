package com.godofthings.beef.event;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.compat.ae.AeDeviceLinker;
import com.godofthings.beef.compat.ae.AeLinkChannelBypass;
import com.godofthings.beef.core.component.UComponents;
import com.godofthings.beef.network.BeefInvulnerabilityStatePacket;
import com.godofthings.beef.network.BeefInvulnerabilitySyncPacket;
import com.godofthings.beef.utils.UselessItemUtils;
import com.godofthings.beef.world.dimension.UselessDimensionConfigManager;
import com.godofthings.beef.world.dimension.UselessDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 照抄自 useless_mod 的服务端事件接线。
 *
 * <p><b>本次死代码清理</b>：牛排工具框架（{@code EndlessBeafItem} 及其挖掘 / 磁力 /
 * 强制击杀 / 时运 / 连锁 / 飞行 / 连点等模式）已整套删除 —— 那些事件处理器全部以
 * 「手持牛排工具」为前提，而本模组早已没有任何该类物品，永远不可能触发。
 * 本类现只保留三块仍然活着的接线：</p>
 * <ol>
 *   <li><b>玩家保护（无敌 / 高级隐身）</b>：其判定入口
 *       {@link UselessItemUtils#hasInvulnerabilityEnabledTargetToolInInventory} 仍支持
 *       外部 mod（omnitools）的物品携带本模组的数据组件，故整层保留；</li>
 *   <li><b>AE 连接</b>：无线访问点绑定与 {@link AeDeviceLinker} 的续命闹钟；</li>
 *   <li><b>无用维度</b>：维度载入时灌入地形配置。</li>
 * </ol>
 */
@EventBusSubscriber(modid = UselessMod.MODID)
public class EventHandler {
    private static final Set<UUID> BEEF_PROTECTED_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> BEEF_ADVANCED_STEALTH_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Integer> CLIENT_BEEF_ADVANCED_STEALTH_ENTITY_IDS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> RESTORING_BEEF_PROTECTED_PLAYERS = ConcurrentHashMap.newKeySet();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && shouldApplyBeefInvulnerability(player)) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.fallDistance = 0.0F;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && hasBeefAdvancedStealthItem(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player
                && hasBeefInvulnerabilityItem(player)
                && isNonBeneficialEffect(event.getEffectInstance())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && shouldApplyBeefInvulnerability(player)) {
            event.setNewDamage(0.0F);
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.fallDistance = 0.0F;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && shouldApplyBeefInvulnerability(player)) {
            event.setCanceled(true);
            restoreBeefProtectedPlayer(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getTarget() instanceof Player player && hasBeefAdvancedStealthItem(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        updateBeefInvulnerability(player);
    }

    public static void updateBeefInvulnerability(Player player) {
        updateBeefInvulnerability(player, false);
    }

    public static void updateBeefInvulnerability(Player player, boolean forceSync) {
        if (player.level().isClientSide()) {
            return;
        }

        migrateLegacyBeefInvulnerability(player);

        boolean hasItemInInventory = UselessItemUtils.hasInvulnerabilityEnabledTargetToolInInventory(player);
        boolean hasAdvancedStealth = UselessItemUtils.hasAdvancedStealthEnabledTargetToolInInventory(player);
        if (hasAdvancedStealth && !hasItemInInventory) {
            UselessItemUtils.enableInvulnerabilityForAdvancedStealth(player);
            hasItemInInventory = UselessItemUtils.hasInvulnerabilityEnabledTargetToolInInventory(player);
        }
        UUID uuid = player.getUUID();

        if (hasItemInInventory) {
            boolean newlyTracked = BEEF_PROTECTED_PLAYERS.add(uuid);
            claimBeefInvulnerability(player);
            if (newlyTracked) {
                clearBeefNegativeEffects(player);
            }
        } else {
            BEEF_PROTECTED_PLAYERS.remove(uuid);
            releaseBeefInvulnerability(player);
        }

        boolean newlyStealthTracked = hasAdvancedStealth && BEEF_ADVANCED_STEALTH_PLAYERS.add(uuid);
        boolean stealthReleased = !hasAdvancedStealth && BEEF_ADVANCED_STEALTH_PLAYERS.remove(uuid);
        if (newlyStealthTracked) {
            clearBeefAdvancedStealthState(player);
        }
        if (player instanceof ServerPlayer serverPlayer
                && (forceSync || newlyStealthTracked || stealthReleased || player.tickCount % 20 == 0)) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    serverPlayer,
                    new BeefInvulnerabilityStatePacket(serverPlayer.getId(), hasAdvancedStealth)
            );
        }
    }

    private static void clearBeefNegativeEffects(Player player) {
        player.getActiveEffects().stream()
                .filter(EventHandler::isNonBeneficialEffect)
                .map(MobEffectInstance::getEffect)
                .toList()
                .forEach(player::removeEffect);
    }

    private static void clearBeefAdvancedStealthState(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof Warden warden
                        && (warden.getTarget() == player || warden.getEntityAngryAt().orElse(null) == player)) {
                    warden.setAttackTarget(null);
                    warden.clearAnger(player);
                } else if (entity instanceof Mob mob && mob.getTarget() == player) {
                    mob.setTarget(null);
                }
            }
        }
    }

    static boolean isNonBeneficialEffect(MobEffectInstance effect) {
        return !effect.getEffect().value().isBeneficial();
    }

    private static void migrateLegacyBeefInvulnerability(Player player) {
        CompoundTag ownershipData = BeefInvulnerabilityOwnership.get(player);
        if (BeefInvulnerabilityOwnership.isMigrationComplete(ownershipData)) {
            return;
        }

        // Preserve an active ownership cycle while upgrading its metadata.
        if (BeefInvulnerabilityOwnership.isOwned(ownershipData)) {
            BeefInvulnerabilityOwnership.markMigrationComplete(ownershipData);
            return;
        }

        // Defer migration until an affected survival/adventure player carries the tool.
        if (player.isCreative()
                || player.isSpectator()
                || !UselessItemUtils.hasTargetToolInInventory(player)) {
            return;
        }

        // Old builds, including v1 ownership tracking, could leave this flag behind
        // after protection had already been released.
        if (player.isInvulnerable()) {
            player.setInvulnerable(false);
        }
        BeefInvulnerabilityOwnership.markMigrationComplete(BeefInvulnerabilityOwnership.getOrCreate(player));
    }

    private static void claimBeefInvulnerability(Player player) {
        CompoundTag ownershipData = BeefInvulnerabilityOwnership.getOrCreate(player);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            BeefInvulnerabilityOwnership.rememberMaxHealthBase(ownershipData, maxHealth.getBaseValue());
        }
        BeefInvulnerabilityOwnership.claim(ownershipData, player.isInvulnerable());
        if (!player.isInvulnerable()) {
            player.setInvulnerable(true);
        }
    }

    private static boolean releaseBeefInvulnerability(Player player) {
        BeefInvulnerabilityOwnership.ReleaseResult result =
                BeefInvulnerabilityOwnership.release(BeefInvulnerabilityOwnership.get(player));
        if (!result.owned()) {
            return false;
        }

        player.setInvulnerable(result.previousInvulnerable());
        return true;
    }

    public static boolean shouldApplyBeefInvulnerability(Player player) {
        return !player.level().isClientSide() && hasBeefInvulnerabilityItem(player);
    }

    public static boolean isRestoringBeefProtectedPlayer(Player player) {
        return RESTORING_BEEF_PROTECTED_PLAYERS.contains(player.getUUID());
    }

    public static boolean hasBeefInvulnerabilityItem(Player player) {
        if (player.level().isClientSide()) {
            return UselessItemUtils.hasInvulnerabilityEnabledTargetToolInInventory(player);
        }
        return BEEF_PROTECTED_PLAYERS.contains(player.getUUID())
                || UselessItemUtils.hasInvulnerabilityEnabledTargetToolInInventory(player);
    }

    public static boolean hasBeefAdvancedStealthItem(Player player) {
        if (player.level().isClientSide()) {
            return CLIENT_BEEF_ADVANCED_STEALTH_ENTITY_IDS.contains(player.getId())
                    || UselessItemUtils.hasAdvancedStealthEnabledTargetToolInInventory(player);
        }
        return BEEF_ADVANCED_STEALTH_PLAYERS.contains(player.getUUID())
                || UselessItemUtils.hasAdvancedStealthEnabledTargetToolInInventory(player);
    }

    public static boolean shouldApplyBeefAdvancedStealth(Player player) {
        return !player.level().isClientSide() && hasBeefAdvancedStealthItem(player);
    }

    public static boolean hasAnyBeefAdvancedStealthPlayers() {
        return !BEEF_ADVANCED_STEALTH_PLAYERS.isEmpty()
                || !CLIENT_BEEF_ADVANCED_STEALTH_ENTITY_IDS.isEmpty();
    }

    public static void setClientBeefAdvancedStealthState(int entityId, boolean stealthEnabled) {
        if (stealthEnabled) {
            CLIENT_BEEF_ADVANCED_STEALTH_ENTITY_IDS.add(entityId);
            return;
        }
        CLIENT_BEEF_ADVANCED_STEALTH_ENTITY_IDS.remove(entityId);
    }

    public static void clearClientBeefAdvancedStealthStates() {
        CLIENT_BEEF_ADVANCED_STEALTH_ENTITY_IDS.clear();
    }

    public static void restoreBeefProtectedPlayer(Player player) {
        if (!RESTORING_BEEF_PROTECTED_PLAYERS.add(player.getUUID())) {
            return;
        }

        try {
            if (!player.level().isClientSide()) {
                BEEF_PROTECTED_PLAYERS.add(player.getUUID());
                migrateLegacyBeefInvulnerability(player);
                claimBeefInvulnerability(player);
                restoreCorruptedMaxHealth(player);
            }

            float maxHealth = player.getMaxHealth();
            if (!Float.isFinite(maxHealth) || maxHealth <= 0.0F) {
                restoreCorruptedMaxHealth(player);
                maxHealth = player.getMaxHealth();
            }
            if (!Float.isFinite(maxHealth) || maxHealth <= 0.0F) {
                return;
            }

            player.dead = false;
            player.deathTime = 0;
            player.hurtTime = 0;
            player.hurtDuration = 0;
            player.setHealth(maxHealth);
            player.setPose(Pose.STANDING);
            player.clearFire();
            player.fallDistance = 0.0F;
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                        serverPlayer,
                        new BeefInvulnerabilityStatePacket(
                                serverPlayer.getId(), BEEF_ADVANCED_STEALTH_PLAYERS.contains(serverPlayer.getUUID()))
                );
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new BeefInvulnerabilitySyncPacket(serverPlayer.getId(), maxHealth));
            }
        } finally {
            RESTORING_BEEF_PROTECTED_PLAYERS.remove(player.getUUID());
        }
    }

    private static void restoreCorruptedMaxHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        if (Double.isFinite(maxHealth.getBaseValue()) && maxHealth.getBaseValue() > 0.0D
                && Float.isFinite(player.getMaxHealth()) && player.getMaxHealth() > 0.0F) {
            return;
        }

        maxHealth.setBaseValue(BeefInvulnerabilityOwnership.previousMaxHealthBase(
                BeefInvulnerabilityOwnership.get(player)));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        updateBeefInvulnerability(event.getEntity(), true);
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAdvancedStealthPlayersTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        updateBeefInvulnerability(event.getEntity(), true);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        updateBeefInvulnerability(event.getEntity(), true);
    }

    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer trackingPlayer)
                || !(event.getTarget() instanceof ServerPlayer target)
                || !shouldApplyBeefAdvancedStealth(target)) {
            return;
        }

        sendAdvancedStealthState(trackingPlayer, target);
    }

    private static void syncAdvancedStealthPlayersTo(ServerPlayer viewer) {
        for (ServerPlayer target : viewer.serverLevel().players()) {
            if (target != viewer && shouldApplyBeefAdvancedStealth(target)) {
                sendAdvancedStealthState(viewer, target);
            }
        }
    }

    private static void sendAdvancedStealthState(ServerPlayer viewer, ServerPlayer target) {
        PacketDistributor.sendToPlayer(
                viewer,
                new BeefInvulnerabilityStatePacket(target.getId(), true)
        );
    }

    /**
     * 无用维度载入时把玩家/数据包配置好的地形设置灌进区块生成器。
     * <p>照抄自上游 EventHandler 的同名方法（此前裁掉，随无用维度子系统一同补回）。</p>
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && UselessDimensions.isUselessDimension(level.dimension())) {
            UselessDimensionConfigManager.apply(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BEEF_PROTECTED_PLAYERS.remove(event.getEntity().getUUID());
        BEEF_ADVANCED_STEALTH_PLAYERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.isCanceled()) return;

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        Level world = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return;

        String className = be.getClass().getName();
        if (!className.contains("WirelessAccessPoint")) return;

        // 「Shift + 右键无线访问点 = 给 AE 连接模式定一个绑定目标」只在 AE 连接模式开启时成立。
        //
        // 这里必须判模式，否则它会无条件抢走这次交互：无线访问点本身也是合法的 AE 端点，
        // 不判模式的话「Shift 右键访问点」会在任何模式下都被吃掉并设成 AE 连接目标。
        if (!stack.getOrDefault(UComponents.AeNetworkConnectComponent.get(), false)) return;

        if (!world.isClientSide) {
            GlobalPos globalPos = GlobalPos.of(world.dimension(), pos);
            stack.set(UComponents.WIRELESS_LINK_TARGET.get(), globalPos);
            player.displayClientMessage(Component.translatable("gui.godofthings.wireless_access_point_bound", pos.toShortString()), true);
        }
        // 取消事件，阻止方块本身的逻辑（如 AE2 的拆卸或旋转）
        event.setCanceled(true);
        // 设置结果，告知系统处理已成功，停止后续传播
        event.setCancellationResult(InteractionResult.sidedSuccess(world.isClientSide));
    }

    /**
     * AE 连接模式：右键「能连入 AE 网络」的机器，把它的 AE 节点并入链接目标所在的那张网。
     *
     * <p>必须拦在 {@link PlayerInteractEvent.RightClickBlock} 这一层，而不是 {@code Item#useOn}：
     * 右键 ME 设备默认会开方块 GUI，只有取消本次交互才拦得住。
     * 潜行时直接让位给既有的「Shift + 右键绑定无线访问点」。</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAeConnectInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getEntity().isShiftKeyDown()) return;

        ItemStack stack = event.getItemStack();
        if (!stack.getOrDefault(UComponents.AeNetworkConnectComponent.get(), false)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        // 目标不是 AE 节点宿主就不接管，交给原有的右键链路。
        if (!AeDeviceLinker.isLinkTarget(level, pos)) return;

        if (level instanceof ServerLevel serverLevel && event.getEntity() instanceof ServerPlayer serverPlayer) {
            AeDeviceLinker.toggle(serverLevel, serverPlayer, stack, pos);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }

    /**
     * AE 连接的续命闹钟：AE2 不保存非空间网格连接，区块 / 存档重载后要把登记过的连接补回来。
     * 每 20 tick 跑一次，链接表为空时几乎零开销。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        AeDeviceLinker.ensureLinks(server);
    }

    /**
     * 服务器启动时构建配方索引
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // 无用维度：开服时把 3 个维度已保存的地形配置全部应用一遍
        UselessDimensionConfigManager.applyAll(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BEEF_PROTECTED_PLAYERS.clear();
        BEEF_ADVANCED_STEALTH_PLAYERS.clear();
        // 通道豁免索引里存的是网格节点引用，别把它们留到下一局。
        AeLinkChannelBypass.clear();
    }
}

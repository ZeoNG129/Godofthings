package com.godofthings.item;

import com.godofthings.Godofthings;
import com.godofthings.handler.GodFavorWandAe2Helper;
import com.godofthings.modes.ModeManager;
import com.godofthings.modes.ToolMode;
import com.godofthings.utils.mining.MiningDispatcher;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 造化垂青之杖：移植自 useless_mod 的 endless_beaf_item。
 * - 下界合金级、无法破坏的万能工具（剑/镐/锹/斧/锄），对所有方块神速挖掘并收集掉落
 * - 强制击杀（一击必杀任意生物，含末影龙/凋灵）
 * - 考古刷子 + 战利品自动回收
 * - 避雷针收集器
 * - 剪羊毛
 * - 精准采集 / 时运附魔切换
 *
 * 1.21.1 变化：
 * - DiggerItem 构造器移除伤害/速度参数；耐久由 TieredItem 自动 .durability(tier.getUses())，
 *   用 DataComponents.UNBREAKABLE 组件达到"永不消耗耐久"。
 * - ToolAction/ToolActions → ItemAbility/ItemAbilities。
 * - IForgeShearable → IShearable（isShearable/onSheared 多 Player 参数，onSheared 无 fortune 参数）。
 * - getUseDuration 增加 LivingEntity 参数；appendHoverText 第二参 Level → Item.TooltipContext。
 * - doesSneakBypassUse / Item#isDamageable / Item#setDamage / getEnchantmentValue(ItemStack) 已移除。
 * - 附魔走 ItemEnchantments 组件（WandItemUtils.switchEnchant），模式标记走 CUSTOM_DATA（WandModes）。
 */
public class GodFavorWandItem extends DiggerItem
{
    private static final String AE2LT_NATURAL_LIGHTNING_TAG = "ae2lt.natural_weather_lightning";
    private static final String TAG_ACCESS_POINT_POS = "accessPoint";

    /** 模式管理器实例（精准/时运/连锁/扳手等）。 */
    private final ModeManager modeManager = new ModeManager();

    public GodFavorWandItem(Item.Properties properties)
    {
        super(Tiers.NETHERITE, BlockTags.MINEABLE_WITH_PICKAXE,
                properties
                        .stacksTo(1)
                        .rarity(Rarity.EPIC)
                        .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(@NotNull ItemStack stack)
    {
        // 返回物品本身，使其在合成后保留在工作台中
        return stack.copy();
    }

    @Override
    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack)
    {
        return true;
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility action)
    {
        // 基础工具能力（所有工具都有）
        if (ItemAbilities.DEFAULT_AXE_ACTIONS.contains(action) ||
                ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(action) ||
                ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(action) ||
                ItemAbilities.DEFAULT_HOE_ACTIONS.contains(action) ||
                ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(action) ||
                action == ItemAbilities.SWORD_SWEEP)
        {
            return true;
        }
        return super.canPerformAction(stack, action);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker)
    {
        if (attacker instanceof Player player && forceKillLivingEntity(stack, target, player))
        {
            return true;
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private static boolean forceKillLivingEntity(ItemStack stack, LivingEntity target, Player player)
    {
        if (target.level().isClientSide || target instanceof Player || !target.isAlive()
                || isForceKillBlacklisted(target)
                || !WandModes.isForceKillEnabled(stack))
        {
            return false;
        }

        ServerLevel level = (ServerLevel) target.level();
        DamageSource damageSource = WandDamageTypes.beefTool(level, player);
        executeForceKill(level, target, damageSource);

        if (target.isRemoved() || !target.isAlive())
        {
            WandItemUtils.tryCaptureSpawnEgg(target, stack, player);
        }
        return true;
    }

    private static void executeForceKill(ServerLevel level, LivingEntity target, DamageSource damageSource)
    {
        float damage = getForceKillDamage(target);

        if (target instanceof EnderDragon dragon)
        {
            dragon.hurt(dragon.head, damageSource, damage);
            if (!target.isAlive())
            {
                return;
            }
        }
        else if (target instanceof WitherBoss wither)
        {
            wither.setInvulnerableTicks(0);
        }

        target.invulnerableTime = 0;
        target.hurt(damageSource, damage);
        if (!target.isAlive())
        {
            return;
        }

        // 兜底：对抵抗伤害的生物直接触发死亡流程（含掉落）
        target.die(damageSource);
    }

    private static float getForceKillDamage(LivingEntity target)
    {
        float damage = target.getHealth() + target.getAbsorptionAmount() + target.getMaxHealth() + 1.0F;
        if (!Float.isFinite(damage))
        {
            return 1024.0F;
        }
        return Math.max(damage, 1024.0F);
    }

    private static boolean isForceKillBlacklisted(Entity entity)
    {
        return WandConfig.getBeefToolForceKillBlacklist().contains(getEntityId(entity));
    }

    private static String getEntityId(Entity entity)
    {
        return entity.getType().builtInRegistryHolder().key().location().toString();
    }

    @Override
    public void onUseTick(@NotNull Level level,
                          @NotNull LivingEntity livingEntity,
                          @NotNull ItemStack stack,
                          int remainingUseDuration)
    {
        if (!(livingEntity instanceof Player player) || remainingUseDuration < 0)
        {
            livingEntity.releaseUsingItem();
            return;
        }

        HitResult hitResult = ProjectileUtil.getHitResultOnViewVector(
                player,
                e -> !e.isSpectator() && e.isPickable(),
                getReach(player)
        );

        if (!(hitResult instanceof BlockHitResult blockHit)
                || hitResult.getType() != HitResult.Type.BLOCK)
        {
            livingEntity.releaseUsingItem();
            return;
        }

        int i = this.getUseDuration(stack, livingEntity) - remainingUseDuration + 1;
        boolean doBrushTick = i % 10 == 5;

        if (!doBrushTick)
        {
            return;
        }

        BlockPos blockPos = blockHit.getBlockPos();
        BlockState blockState = level.getBlockState(blockPos);

        /* ---------- 客户端：粒子 & 音效 ---------- */
        HumanoidArm arm = livingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();

        this.spawnBrushParticles(
                level,
                blockHit,
                blockState,
                livingEntity.getViewVector(0.0F),
                arm
        );

        SoundEvent sound = blockState.getBlock() instanceof BrushableBlock brushable
                ? brushable.getBrushSound()
                : SoundEvents.BRUSH_GENERIC;

        level.playSound(player, blockPos, sound, SoundSource.BLOCKS);

        /* ---------- 服务端：正常刷取 + 战利品直收 ---------- */
        if (!level.isClientSide)
        {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof BrushableBlockEntity brushable)
            {
                // 刷取前记录已有掉落
                AABB area = new AABB(blockPos).inflate(3.0);
                Set<UUID> before = level.getEntitiesOfClass(ItemEntity.class, area)
                                        .stream()
                                        .map(Entity::getUUID)
                                        .collect(Collectors.toSet());

                boolean finished = brushable.brush(
                        level.getGameTime(),
                        player,
                        blockHit.getDirection()
                );

                // 只有刷完那一刻才回收掉落
                if (finished)
                {
                    level.getEntitiesOfClass(ItemEntity.class, area).stream()
                         .filter(e -> !before.contains(e.getUUID()))
                         .forEach(entity ->
                         {
                             ItemStack drop = entity.getItem().copy();
                             if (!drop.isEmpty())
                             {
                                 if (!player.getInventory().add(drop))
                                 {
                                     player.drop(drop, false);
                                 }
                             }
                             entity.discard();
                         });
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx)
    {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();

        if (player == null)
        {
            return InteractionResult.PASS;
        }

        InteractionResult lightningCollectorResult = trySummonLightningForCollector(ctx.getLevel(), ctx.getClickedPos(), ctx.getPlayer());
        if (lightningCollectorResult != InteractionResult.PASS)
        {
            return lightningCollectorResult;
        }

        // ============================================================
        // 1. 刷子功能 (对 BrushableBlock 生效)
        // ============================================================
        HitResult hitresult = ProjectileUtil.getHitResultOnViewVector(
                player, (p) -> !p.isSpectator() && p.isPickable(), getReach(player));

        if (hitresult instanceof BlockHitResult blockHit && hitresult.getType() == HitResult.Type.BLOCK)
        {
            if (world.getBlockState(blockHit.getBlockPos()).getBlock() instanceof BrushableBlock)
            {
                player.startUsingItem(ctx.getHand());
                return InteractionResult.CONSUME;
            }
        }

        // ============================================================
        // 2. 统一工具行为链 (铲子 -> 锄头 -> 斧头)
        // ============================================================
        InteractionResult res = this.tryToolAction(ctx, ItemAbilities.SHOVEL_FLATTEN, SoundEvents.SHOVEL_FLATTEN);
        if (res != InteractionResult.PASS)
        {
            return res;
        }

        res = this.tryToolAction(ctx, ItemAbilities.HOE_TILL, SoundEvents.HOE_TILL);
        if (res != InteractionResult.PASS)
        {
            return res;
        }

        res = this.tryToolAction(ctx, ItemAbilities.AXE_STRIP, SoundEvents.AXE_STRIP);
        if (res != InteractionResult.PASS)
        {
            return res;
        }

        res = this.tryScrapeOrWaxOff(ctx, ItemAbilities.AXE_SCRAPE, SoundEvents.AXE_SCRAPE, 3005);
        if (res != InteractionResult.PASS)
        {
            return res;
        }

        return this.tryScrapeOrWaxOff(ctx, ItemAbilities.AXE_WAX_OFF, SoundEvents.AXE_WAX_OFF, 3004);
    }

    public static InteractionResult trySummonLightningForCollector(Level level, BlockPos clickedPos, @Nullable Player player)
    {
        if (!level.getBlockState(clickedPos).is(net.minecraft.world.level.block.Blocks.LIGHTNING_ROD))
        {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel)
        {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt == null)
            {
                return InteractionResult.FAIL;
            }

            Vec3 target = Vec3.atBottomCenterOf(clickedPos.above());
            bolt.moveTo(target.x, target.y, target.z);
            if (player instanceof ServerPlayer serverPlayer)
            {
                bolt.setCause(serverPlayer);
            }
            // ae2lt 存在时会读取此 NBT 键以识别自然天气闪电；不存在时该键被忽略，不影响功能
            bolt.getPersistentData().putBoolean(AE2LT_NATURAL_LIGHTNING_TAG, true);
            serverLevel.addFreshEntity(bolt);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state)
    {
        float configSpeed = (float) WandConfig.getBeefToolMiningSpeed();
        float baseSpeed = configSpeed > 0 ? configSpeed : super.getDestroySpeed(stack, state);

        float hardness = state.getDestroySpeed(null, null);
        if (hardness < 0)
        {
            return 0.0F;
        }

        float speed = baseSpeed * hardness;

        // 防止 NaN / 极端情况
        if (speed <= 0 || Float.isNaN(speed) || Float.isInfinite(speed))
        {
            return baseSpeed;
        }

        return speed;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state)
    {
        return true; // 所有方块掉落全收集
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack,
                                                           @NotNull Player player,
                                                           @NotNull LivingEntity entity,
                                                           @NotNull InteractionHand hand)
    {
        if (entity instanceof IShearable target)
        {
            BlockPos pos = entity.blockPosition();
            boolean isClient = entity.level().isClientSide();
            if (target.isShearable(player, stack, entity.level(), pos))
            {
                List<ItemStack> drops = target.onSheared(player, stack, entity.level(), pos);
                if (!isClient)
                {
                    for (ItemStack drop : drops)
                    {
                        if (!drop.isEmpty())
                        {
                            if (!player.getInventory().add(drop))
                            {
                                player.drop(drop, false);
                            }
                        }
                    }
                }
                entity.gameEvent(GameEvent.SHEAR, player);
                return InteractionResult.sidedSuccess(isClient);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack,
                              @NotNull Level level,
                              @NotNull Entity entity,
                              int slotId,
                              boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (entity instanceof Player player)
        {
            boolean hasItemInInventory = player.getInventory().items.stream().anyMatch(item -> item.getItem() == this);
            if (hasItemInInventory)
            {
                WandItemUtils.applyWandEffects(player);
            }
        }
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player)
    {
        super.onCraftedBy(stack, level, player);
        // 首次创建时设置为时运模式
        CompoundTag tag = WandModes.getData(stack);
        if (!tag.contains("SilkTouchMode"))
        {
            switchEnchantmentMode(stack, false, level.registryAccess());
        }
        else
        {
            updateEnchantments(stack, level.registryAccess());
        }
    }

    // ==================== 模式管理 ====================

    public boolean isSilkTouchMode(ItemStack stack)
    {
        modeManager.loadFromStack(stack);
        return modeManager.isModeActive(ToolMode.SILK_TOUCH);
    }

    public boolean isFortuneMode(ItemStack stack)
    {
        modeManager.loadFromStack(stack);
        return modeManager.isModeActive(ToolMode.FORTUNE);
    }

    public boolean isEnhancedChainMiningMode(ItemStack stack)
    {
        modeManager.loadFromStack(stack);
        return modeManager.isModeActive(ToolMode.ENHANCED_CHAIN_MINING);
    }

    public boolean isForceMiningMode(ItemStack stack)
    {
        modeManager.loadFromStack(stack);
        return modeManager.isModeActive(ToolMode.FORCE_MINING);
    }

    public boolean isAEStoragePriorityMode(ItemStack stack)
    {
        modeManager.loadFromStack(stack);
        return modeManager.isModeActive(ToolMode.AE_STORAGE_PRIORITY);
    }

    public boolean toggleEnhancedChainMiningMode(ItemStack stack)
    {
        modeManager.loadFromStack(stack);
        modeManager.toggleMode(ToolMode.ENHANCED_CHAIN_MINING);
        modeManager.saveToStack(stack);
        return modeManager.isModeActive(ToolMode.ENHANCED_CHAIN_MINING);
    }

    public boolean toggleForceMiningMode(ItemStack stack)
    {
        modeManager.loadFromStack(stack);
        modeManager.toggleMode(ToolMode.FORCE_MINING);
        modeManager.saveToStack(stack);
        return modeManager.isModeActive(ToolMode.FORCE_MINING);
    }

    public boolean toggleAEStoragePriorityMode(ItemStack stack)
    {
        modeManager.loadFromStack(stack);
        modeManager.toggleMode(ToolMode.AE_STORAGE_PRIORITY);
        modeManager.saveToStack(stack);
        return modeManager.isModeActive(ToolMode.AE_STORAGE_PRIORITY);
    }

    /** 切换精准采集/时运模式（保持其他模式状态）。 */
    public void switchEnchantmentMode(ItemStack stack, boolean silkTouchMode, RegistryAccess access)
    {
        modeManager.loadFromStack(stack);
        modeManager.setModeActive(ToolMode.SILK_TOUCH, silkTouchMode);
        modeManager.setModeActive(ToolMode.FORTUNE, !silkTouchMode);
        modeManager.saveToStack(stack);
        updateEnchantments(stack, access);
    }

    /** 更新实际附魔组件并同步关键模式标记（供挖掘逻辑读取）。 */
    public void updateEnchantments(ItemStack stack, RegistryAccess access)
    {
        ModeManager mm = new ModeManager();
        mm.loadFromStack(stack);
        boolean silkTouchMode = mm.isModeActive(ToolMode.SILK_TOUCH);

        // 附魔 + 纹理（CUSTOM_MODEL_DATA）由 WandItemUtils.switchEnchant 统一处理
        WandItemUtils.switchEnchant(stack, !silkTouchMode, access);

        // 同步顶层模式标记（MiningUtils 读取）
        WandModes.setBoolean(stack, WandModes.SILK_TOUCH_MODE, silkTouchMode);
        WandModes.setBoolean(stack, WandModes.ENHANCED_CHAIN_MINING, mm.isModeActive(ToolMode.ENHANCED_CHAIN_MINING));
    }

    // ==================== GT 扳手模式：切换子类物品 ====================

    private boolean isGTCEUInstalled()
    {
        return ModList.get().isLoaded("gtceu");
    }

    public ItemStack switchToolModeItem(ItemStack oldStack, ModeManager mm, RegistryAccess access)
    {
        ItemStack newStack = ItemStack.EMPTY;
        boolean gtceInstalled = isGTCEUInstalled();
        ResourceLocation omnitoolId = ResourceLocation.tryParse("omnitools:omni_wrench");
        boolean omnitoolsInstalled = omnitoolId != null && BuiltInRegistries.ITEM.containsKey(omnitoolId);

        if (mm.isModeActive(ToolMode.WRENCH_MODE))
        {
            newStack = new ItemStack(Godofthings.GOD_FAVOR_WAND_WRENCH.get());
        }
        else if (gtceInstalled && mm.isModeActive(ToolMode.SCREWDRIVER_MODE))
        {
            newStack = new ItemStack(Godofthings.GOD_FAVOR_WAND_SCREWDRIVER.get());
        }
        else if (gtceInstalled && mm.isModeActive(ToolMode.MALLET_MODE))
        {
            newStack = new ItemStack(Godofthings.GOD_FAVOR_WAND_MALLET.get());
        }
        else if (gtceInstalled && mm.isModeActive(ToolMode.CROWBAR_MODE))
        {
            newStack = new ItemStack(Godofthings.GOD_FAVOR_WAND_CROWBAR.get());
        }
        else if (gtceInstalled && mm.isModeActive(ToolMode.HAMMER_MODE))
        {
            newStack = new ItemStack(Godofthings.GOD_FAVOR_WAND_HAMMER.get());
        }
        else if (omnitoolsInstalled && mm.isModeActive(ToolMode.OMNITOOL_MODE))
        {
            newStack = new ItemStack(BuiltInRegistries.ITEM.get(omnitoolId));
        }
        else
        {
            newStack = new ItemStack(Godofthings.GOD_FAVOR_WAND.get());
        }

        if (!newStack.isEmpty())
        {
            CompoundTag oldData = WandModes.getData(oldStack);
            if (!oldData.isEmpty())
            {
                newStack.set(DataComponents.CUSTOM_DATA, CustomData.of(oldData));
            }
            ItemEnchantments oldEnch = oldStack.get(DataComponents.ENCHANTMENTS);
            if (oldEnch != null)
            {
                newStack.set(DataComponents.ENCHANTMENTS, oldEnch);
            }
        }
        updateEnchantments(newStack, access);
        return newStack;
    }

    // ==================== AE2 存储优先 ====================

    @Nullable
    public static GlobalPos getLinkedPosition(ItemStack stack)
    {
        CompoundTag tag = WandModes.getData(stack);
        if (tag.contains(TAG_ACCESS_POINT_POS, Tag.TAG_COMPOUND))
        {
            return GlobalPos.CODEC.decode(NbtOps.INSTANCE, tag.get(TAG_ACCESS_POINT_POS))
                    .result()
                    .map(Pair::getFirst)
                    .orElse(null);
        }
        return null;
    }

    /** 掉落处理：AE2 优先 -> 玩家背包。返回是否全部处理（true=无需再掉落）。 */
    public static void handleDrops(List<ItemStack> drops, Player player, ItemStack toolStack)
    {
        if (drops == null || drops.isEmpty())
        {
            return;
        }

        java.util.Iterator<ItemStack> iterator = drops.iterator();
        while (iterator.hasNext())
        {
            ItemStack dropStack = iterator.next();
            if (dropStack.isEmpty())
            {
                iterator.remove();
                continue;
            }

            // 仅当 AE2 已加载时才触碰 helper（软兼容：未装 AE2 时此分支永不加载 AE2 类）
            if (ModList.get().isLoaded("ae2")
                    && GodFavorWandAe2Helper.storeItemInAENetwork(dropStack, player, toolStack))
            {
                iterator.remove();
                continue;
            }

            if (player.getInventory().add(dropStack))
            {
                iterator.remove();
            }
        }
    }

    // ==================== 连锁/强制挖掘事件 ====================

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.GAME)
    public static class MiningEventHandler
    {
        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event)
        {
            Player player = event.getPlayer();
            if (player == null) return;

            ItemStack mainHandItem = player.getMainHandItem();
            if (mainHandItem.getItem() instanceof GodFavorWandItem)
            {
                MiningDispatcher.dispatchBreak(event, mainHandItem, player);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack)
    {
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;

        if (player != null && level != null && player.isUsingItem())
        {
            HitResult hitResult = ProjectileUtil.getHitResultOnViewVector(
                    player,
                    e -> !e.isSpectator() && e.isPickable(),
                    getReach(player)
            );

            if (hitResult instanceof BlockHitResult blockHit
                    && hitResult.getType() == HitResult.Type.BLOCK)
            {
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.getBlock() instanceof BrushableBlock)
                {
                    return UseAnim.BRUSH;
                }
            }
        }
        return super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity)
    {
        return 20;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull Item.TooltipContext context,
                                @NotNull List<Component> tooltipComponents,
                                @NotNull TooltipFlag tooltipFlag)
    {
        // 连锁挖掘开关
        boolean chainMiningEnabled = WandModes.isChainMiningEnabled(stack);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.chain_mining_mode")
                .append(": ")
                .append(Component.translatable(
                                chainMiningEnabled ? "tooltip.godofthings.enable" : "tooltip.godofthings.disable")
                        .withStyle(chainMiningEnabled ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.GREEN));

        // 增强连锁挖掘
        boolean enhancedChain = isEnhancedChainMiningMode(stack);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.enhanced_chain_mining_mode")
                .append(": ")
                .append(Component.translatable(
                                enhancedChain ? "tooltip.godofthings.enable" : "tooltip.godofthings.disable")
                        .withStyle(enhancedChain ? ChatFormatting.BLUE : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.BLUE));

        // 强制挖掘状态
        boolean forceMiningEnabled = isForceMiningMode(stack);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.force_mining_mode")
                .append(": ")
                .append(Component.translatable(
                                forceMiningEnabled ? "tooltip.godofthings.enable" : "tooltip.godofthings.disable")
                        .withStyle(forceMiningEnabled ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.RED));

        // AE存储优先
        boolean aePriority = isAEStoragePriorityMode(stack);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.ae_storage_priority_mode")
                .append(": ")
                .append(Component.translatable(
                                aePriority ? "tooltip.godofthings.enable" : "tooltip.godofthings.disable")
                        .withStyle(aePriority ? ChatFormatting.AQUA : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.AQUA));

        boolean forceKillEnabled = WandModes.isForceKillEnabled(stack);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.force_kill_enabled_mode")
                .append(": ")
                .append(Component.translatable(
                                forceKillEnabled ? "tooltip.godofthings.enable" : "tooltip.godofthings.disable")
                        .withStyle(forceKillEnabled ? ChatFormatting.GOLD : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_RED));

        boolean beefInvulnerabilityEnabled = WandModes.isBeefInvulnerabilityEnabled(stack);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.beef_invulnerability_mode")
                .append(": ")
                .append(Component.translatable(
                                beefInvulnerabilityEnabled ? "tooltip.godofthings.enable" : "tooltip.godofthings.disable")
                        .withStyle(beefInvulnerabilityEnabled ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_PURPLE));

        boolean beefCaptureEnabled = WandModes.isBeefCaptureEnabled(stack);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.beef_capture_mode")
                .append(": ")
                .append(Component.translatable(
                                beefCaptureEnabled ? "tooltip.godofthings.enable" : "tooltip.godofthings.disable")
                        .withStyle(beefCaptureEnabled ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                .withStyle(ChatFormatting.DARK_GREEN));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @NotNull Component getName(@NotNull ItemStack stack)
    {
        if (isSilkTouchMode(stack))
        {
            return Component.translatable("item.godofthings.god_favor_wand.silk_touch");
        }
        return Component.translatable("item.godofthings.god_favor_wand.fortune");
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack)
    {
        return true; // 始终显示附魔光效
    }

    private static double getReach(Player player)
    {
        return player.blockInteractionRange();
    }

    /**
     * 通用工具动作逻辑
     */
    private InteractionResult tryToolAction(UseOnContext ctx, ItemAbility ability, SoundEvent sound)
    {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState modified = world.getBlockState(pos).getToolModifiedState(ctx, ability, false);
        if (modified != null)
        {
            world.playSound(ctx.getPlayer(), pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!world.isClientSide)
            {
                world.setBlock(pos, modified, 11);
                if (ctx.getPlayer() instanceof ServerPlayer sp)
                {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(sp, pos, ctx.getItemInHand());
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * 针对铜块刮擦和去蜡的特殊逻辑 (带 LevelEvent 粒子效果)
     */
    private InteractionResult tryScrapeOrWaxOff(UseOnContext ctx, ItemAbility ability, SoundEvent sound,
                                                int levelEvent)
    {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState modified = world.getBlockState(pos).getToolModifiedState(ctx, ability, false);
        if (modified != null)
        {
            world.playSound(ctx.getPlayer(), pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            world.levelEvent(ctx.getPlayer(), levelEvent, pos, 0);
            if (!world.isClientSide)
            {
                world.setBlock(pos, modified, 11);
                if (ctx.getPlayer() instanceof ServerPlayer sp)
                {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(sp, pos, ctx.getItemInHand());
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private void spawnBrushParticles(Level level,
                                     BlockHitResult hitResult,
                                     BlockState state,
                                     Vec3 pos,
                                     HumanoidArm arm)
    {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        int j = level.getRandom().nextInt(7, 12);
        BlockParticleOption blockparticleoption = new BlockParticleOption(ParticleTypes.BLOCK, state);
        Direction direction = hitResult.getDirection();
        Vec3 vec3 = hitResult.getLocation();

        for (int k = 0; k < j; ++k)
        {
            level.addParticle(blockparticleoption,
                    vec3.x - (double) (direction == Direction.WEST ? 1.0E-6F : 0.0F),
                    vec3.y,
                    vec3.z - (double) (direction == Direction.NORTH ? 1.0E-6F : 0.0F),
                    (direction.getAxis() == Direction.Axis.X ? 0.0 :
                            direction.getStepX()) * i * 3.0 * level.getRandom().nextDouble(),
                    0.0,
                    (direction.getAxis() == Direction.Axis.Z ? 0.0 :
                            direction.getStepZ()) * i * 3.0 * level.getRandom().nextDouble()
            );
        }
    }
}

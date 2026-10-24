package com.godofthings.superfusion;

import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * 超级融合卡（移植自 Mekanism-Card 的 {@code super_fusion_card}）。
 * <p>
 * 一张卡把上游四个物品的能力合在一起，用「融合模式」切换：
 * <ul>
 *   <li>{@link FusionMode#TIER_INSTALL}：等级安装 —— 把机器一路升到卡里记录的等级（消耗安装器 + 1000 FE）；</li>
 *   <li>{@link FusionMode#MODULE_UPGRADE}：安装升级 —— 按卡里的档案把升级数量同步到机器；</li>
 *   <li>{@link FusionMode#MEMORY_COPY}：内存复制/粘贴 —— 复制机器配置，粘贴到同类机器；</li>
 *   <li>{@link FusionMode#FULL_PASTE}：全量模式 —— 先升等级，再粘贴升级与配置。</li>
 * </ul>
 * 快捷操作：
 * <ul>
 *   <li>右键机器：执行当前模式；</li>
 *   <li>潜行 + 右键机器：复制该机器配置到卡里；</li>
 *   <li>潜行 + 右键空气：清空卡里的机器数据；</li>
 *   <li>潜行 + 左键空气：轮换融合模式；</li>
 *   <li>左键空气（不潜行）：打开设置界面；</li>
 *   <li>中键机器：把能装的升级一次装满；</li>
 *   <li>按住「批量选择」键（默认不绑定，见「众神之物」分类）+ 左键两个角点：框选批量执行。</li>
 * </ul>
 */
public class SuperFusionItem extends Item
{
    /** 融合模式 */
    public enum FusionMode
    {
        TIER_INSTALL("tooltip.godofthings.super_fusion.mode.tier", ChatFormatting.GREEN),
        MODULE_UPGRADE("tooltip.godofthings.super_fusion.mode.module", ChatFormatting.GOLD),
        MEMORY_COPY("tooltip.godofthings.super_fusion.mode.memory", ChatFormatting.AQUA),
        FULL_PASTE("tooltip.godofthings.super_fusion.mode.full", ChatFormatting.LIGHT_PURPLE);

        private final String translationKey;
        private final ChatFormatting color;

        FusionMode(String translationKey, ChatFormatting color)
        {
            this.translationKey = translationKey;
            this.color = color;
        }

        public Component getDisplayName()
        {
            return Component.translatable(translationKey);
        }

        public ChatFormatting getColor()
        {
            return color;
        }

        private FusionMode next()
        {
            FusionMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static FusionMode byId(int id)
        {
            FusionMode[] values = values();
            return id < 0 || id >= values.length ? TIER_INSTALL : values[id];
        }
    }

    public SuperFusionItem()
    {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    // ===================== 模式读写 =====================

    public static FusionMode getMode(ItemStack stack)
    {
        return FusionMode.byId(stack.getOrDefault(SuperFusionComponents.FUSION_MODE.get(),
                FusionMode.TIER_INSTALL.ordinal()));
    }

    public static void cycleMode(ItemStack stack, Player player)
    {
        setMode(stack, player, getMode(stack).next());
    }

    public static void setMode(ItemStack stack, Player player, FusionMode mode)
    {
        stack.set(SuperFusionComponents.FUSION_MODE.get(), mode.ordinal());
        if (mode == FusionMode.MEMORY_COPY)
        {
            setFuzzy(stack, false);
        }
        player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.mode_switched",
                mode.getDisplayName()).withStyle(mode.getColor()), true);
    }

    /** 模糊目标模式：true = 不限机器种类，false = 精确匹配 */
    public static boolean isFuzzy(ItemStack stack)
    {
        return getMode(stack) != FusionMode.MEMORY_COPY
                && stack.getOrDefault(SuperFusionComponents.FUZZY_TARGET_MODE.get(), false);
    }

    public static void setFuzzy(ItemStack stack, boolean fuzzy)
    {
        stack.set(SuperFusionComponents.FUZZY_TARGET_MODE.get(), fuzzy);
    }

    // ===================== 右键机器 =====================

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null)
        {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();

        if (level.isClientSide())
        {
            return InteractionResult.SUCCESS;
        }

        // 潜行右键：复制配置（不管点的是机器还是空气，上游都按复制处理）
        if (player.isShiftKeyDown())
        {
            MemoryCardLogic.handleCopy(level, pos, player, stack);
            return InteractionResult.CONSUME;
        }

        switch (getMode(stack))
        {
            case TIER_INSTALL -> handleTierOperation(level, pos, face, player, stack);
            case MODULE_UPGRADE -> handleModuleOperation(level, pos, face, player, stack);
            case MEMORY_COPY -> MemoryCardLogic.handlePaste(level, pos, player, stack);
            case FULL_PASTE -> MemoryCardLogic.handleFullPaste(level, pos, player, stack);
        }
        return InteractionResult.CONSUME;
    }

    /** 等级安装模式：单个或连锁批量 */
    public static void handleTierOperation(Level level, BlockPos pos, Direction face, Player player, ItemStack stack)
    {
        Collection<BlockPos> positions = resolvePositions(player, pos, face);
        Block sourceBlock = MemoryCardLogic.getStoredSourceBlock(stack);
        BaseTier desiredTier = sourceBlock == null ? null
                : TierInstallLogic.getEffectiveBaseTier(sourceBlock.builtInRegistryHolder());
        String desiredAdvancedTier = sourceBlock == null ? null
                : ExtraTierIntegration.getAdvancedTierName(sourceBlock.builtInRegistryHolder());

        // 没有复制过任何机器时，退化成「升到原版终极」
        if (sourceBlock == null || desiredTier == null)
        {
            TierInstallLogic.upgradeSingleToUltimate(level, pos, player, stack);
            return;
        }
        TierInstallLogic.upgradeBatchToTier(level, positions, player, stack, desiredTier, desiredAdvancedTier,
                isFuzzy(stack), sourceBlock);
    }

    /** 安装升级模式：单个或连锁批量 */
    public static void handleModuleOperation(Level level, BlockPos pos, Direction face, Player player, ItemStack stack)
    {
        Collection<BlockPos> positions = resolvePositions(player, pos, face);
        if (!isFuzzy(stack))
        {
            Block sourceBlock = MemoryCardLogic.getStoredSourceBlock(stack);
            if (sourceBlock == null)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_data")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            positions = positions.stream()
                    .filter(targetPos -> level.getBlockState(targetPos).getBlock() == sourceBlock)
                    .toList();
            if (positions.isEmpty())
            {
                player.displayClientMessage(Component.translatable(
                                "message.godofthings.super_fusion.type_mismatch")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        ModuleConfigLogic.handlePositions(level, positions, player, stack);
    }

    /** 中键：装满升级 */
    public static void handleMiddleClick(Level level, BlockPos pos, Player player, ItemStack stack)
    {
        ModuleConfigLogic.handleMiddleClick(level, pos, player, stack);
    }

    /** 取「当前目标位置集合」：装了 FTB Ultimine 且按下连锁键时取连锁范围，否则只有单点 */
    private static Collection<BlockPos> resolvePositions(Player player, BlockPos pos, Direction face)
    {
        if (player instanceof ServerPlayer serverPlayer && UltimineBridge.isPressed(serverPlayer))
        {
            Collection<BlockPos> positions = UltimineBridge.getCachedPositions(serverPlayer, pos, face);
            if (positions.size() > 1)
            {
                return positions;
            }
        }
        return List.of(pos);
    }

    // ===================== 工具提示 =====================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        FusionMode mode = getMode(stack);
        tooltip.add(Component.translatable("tooltip.godofthings.super_fusion.current_mode", mode.getDisplayName())
                .withStyle(mode.getColor()));
        tooltip.add(Component.translatable("tooltip.godofthings.super_fusion.module_mode",
                        ModuleConfigLogic.getCurrentMode(stack).getDisplayName())
                .withStyle(ChatFormatting.GOLD));
        if (MemoryCardLogic.hasData(stack))
        {
            Block source = MemoryCardLogic.getStoredSourceBlock(stack);
            if (source != null)
            {
                tooltip.add(Component.translatable("tooltip.godofthings.super_fusion.stored",
                                source.getName().getString())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.godofthings.super_fusion.no_stored")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.godofthings.super_fusion.energy_hint")
                .withStyle(ChatFormatting.DARK_AQUA));
        if (ExtraTierIntegration.isAnyLoaded())
        {
            tooltip.add(Component.translatable("tooltip.godofthings.super_fusion.extra_tier_support")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** 显示能量条（只在电量不满时显示） */
    @Override
    public boolean isBarVisible(ItemStack stack)
    {
        return stack.getOrDefault(SuperFusionComponents.ENERGY.get(), 0) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack)
    {
        int energy = stack.getOrDefault(SuperFusionComponents.ENERGY.get(), 0);
        return Math.round(13.0F * Math.min(1.0F, energy / (float) SuperFusionCapabilities.maxEnergy()));
    }

    @Override
    public int getBarColor(ItemStack stack)
    {
        return 0x00C8FF;
    }

    /** 是否是可批量选择的工具（供客户端框选渲染判断） */
    public static boolean isBatchSelectionTool(ItemStack stack)
    {
        return stack.getItem() instanceof SuperFusionItem;
    }

    /** 供外部读取「卡里记录的源方块」 */
    @Nullable
    public static Block storedSourceBlock(ItemStack stack)
    {
        return MemoryCardLogic.getStoredSourceBlock(stack);
    }

    /** 供外部判断方块能否升级 */
    public static boolean isUpgradeTarget(Level level, BlockPos pos)
    {
        return TierInstallLogic.isUpgradeTarget(level, pos);
    }

    /** 让外部（客户端界面）能拿到属性检查 */
    public static boolean hasUpgradeableAttribute(Block block)
    {
        return Attribute.get(block.builtInRegistryHolder(),
                mekanism.common.block.attribute.AttributeUpgradeable.class) != null;
    }
}

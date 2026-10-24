package com.godofthings.superfusion.network;

import com.godofthings.superfusion.MemoryCardLogic;
import com.godofthings.superfusion.ModuleConfigLogic;
import com.godofthings.superfusion.SuperFusionItem;
import com.godofthings.superfusion.TierInstallLogic;
import com.godofthings.superfusion.util.BatchSelectionHelper;
import mekanism.api.tier.BaseTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * 框选批量执行包：玩家按住「批量选择」键左键点两个角点后，对长方体范围内的机器批量执行当前模式。
 */
public record BatchSelectionPayload(BlockPos firstPos, BlockPos secondPos) implements CustomPacketPayload
{
    public static final Type<BatchSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(com.godofthings.Godofthings.MODID, "super_fusion_batch"));

    public static final StreamCodec<FriendlyByteBuf, BatchSelectionPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BatchSelectionPayload::firstPos,
            BlockPos.STREAM_CODEC, BatchSelectionPayload::secondPos,
            BatchSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(BatchSelectionPayload payload, IPayloadContext context)
    {
        Player player = context.player();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SuperFusionItem))
        {
            return;
        }
        List<BlockPos> positions = BatchSelectionHelper.collectPositions(
                player.level(), payload.firstPos(), payload.secondPos(), player);
        if (positions.isEmpty())
        {
            return;
        }

        SuperFusionItem.FusionMode mode = SuperFusionItem.getMode(stack);
        boolean fuzzy = SuperFusionItem.isFuzzy(stack);
        if (!fuzzy && (mode == SuperFusionItem.FusionMode.TIER_INSTALL
                || mode == SuperFusionItem.FusionMode.MODULE_UPGRADE))
        {
            Block referenceBlock = MemoryCardLogic.getStoredSourceBlock(stack);
            if (referenceBlock == null)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.no_data")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            positions = positions.stream()
                    .filter(pos -> player.level().getBlockState(pos).getBlock() == referenceBlock)
                    .toList();
        }

        switch (mode)
        {
            case TIER_INSTALL ->
            {
                Block sourceBlock = MemoryCardLogic.getStoredSourceBlock(stack);
                BaseTier desiredTier = sourceBlock == null ? null
                        : TierInstallLogic.getEffectiveBaseTier(sourceBlock.builtInRegistryHolder());
                String desiredAdvancedTier = sourceBlock == null ? null
                        : com.godofthings.superfusion.ExtraTierIntegration.getAdvancedTierName(
                                sourceBlock.builtInRegistryHolder());
                if (sourceBlock == null || desiredTier == null)
                {
                    player.displayClientMessage(Component.translatable(
                                    "message.godofthings.super_fusion.no_tier")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
                TierInstallLogic.upgradeBatchToTier(player.level(), positions, player, stack, desiredTier,
                        desiredAdvancedTier, fuzzy, sourceBlock);
            }
            case MODULE_UPGRADE -> ModuleConfigLogic.handlePositions(player.level(), positions, player, stack);
            case MEMORY_COPY -> MemoryCardLogic.handleBatchPaste(player.level(), positions, player, stack);
            case FULL_PASTE -> MemoryCardLogic.handleBatchFullPaste(player.level(), positions, player, stack);
        }
    }
}

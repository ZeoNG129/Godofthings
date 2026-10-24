package com.godofthings.item;

import com.godofthings.block.entity.GodTransmitterBlockEntity;
import com.godofthings.infinityprovider.InfEnhancedBlockEntity;
import com.godofthings.infinityprovider.InfEnhancedHost;
import com.godofthings.infinityprovider.InfProviderSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * 神之绑定器（同心契）：一物三用的绑定工具。
 * <ul>
 *   <li>右键 FE 机器 → 绑定 / 取消绑定到最近的神之传输充能（原有功能）；</li>
 *   <li>右键无限样板供应器 → 选中它为无线宿主（要求已开无线模式）；再点选中 → 取消选中；</li>
 *   <li>选中供应器后右键机器 → 把机器绑定到该供应器（潜行点已绑机器解除）。</li>
 * </ul>
 * <p>
 * 使用 {@code onItemUseFirst} 在方块交互（打开机器 UI）之前拦截。
 */
public class GodBinderItem extends Item
{
    /** 供应器选中状态存在物品 CustomData 里（FE 绑定状态在传输器 BE 上，互不干扰）。 */
    private static final String TAG_PROVIDER_DIM = "InfProviderDim";
    private static final String TAG_PROVIDER_POS = "InfProviderPos";

    public GodBinderItem(Properties properties)
    {
        super(properties);
    }

    // ---- 供应器选中状态 ----

    private static void selectProvider(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag() {
            {
                putString(TAG_PROVIDER_DIM, dimension.location().toString());
                putLong(TAG_PROVIDER_POS, pos.asLong());
            }
        }));
    }

    private record ProviderSelection(ResourceKey<Level> dimension, BlockPos pos) {
    }

    @Nullable
    private static ProviderSelection getProviderSelection(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        var tag = data.copyTag();
        if (!tag.contains(TAG_PROVIDER_POS)) {
            return null;
        }
        var dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(tag.getString(TAG_PROVIDER_DIM)));
        return new ProviderSelection(dim, BlockPos.of(tag.getLong(TAG_PROVIDER_POS)));
    }

    private static void clearProviderSelection(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx)
    {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null || level.isClientSide)
        {
            return InteractionResult.PASS;
        }
        // ── 分支一：无限样板供应器 → 选中 / 取消选中无线宿主 ──
        if (level.getBlockEntity(pos) instanceof InfEnhancedBlockEntity provider)
        {
            return handleProviderClick(stack, player, level, pos, provider);
        }
        // ── 分支二：已选中供应器时点机器 → 无线连接 / 断开 ──
        var selection = getProviderSelection(stack);
        if (selection != null && level instanceof ServerLevel serverLevel)
        {
            var host = resolveProvider(serverLevel, selection);
            if (host != null) {
                return handleMachineClick(stack, player, serverLevel, pos, ctx.getClickedFace(), host);
            }
            // 选中的供应器已消失：清掉选中状态，继续走原有 FE 逻辑
            clearProviderSelection(stack);
        }
        // ── 分支三（原有）：FE 机器 → 绑定 / 取消绑定到最近的神之传输 ──
        if (GodTransmitterBlockEntity.hasEnergyStorage(level, pos))
        {
            GodTransmitterBlockEntity nearest = GodTransmitterBlockEntity.findNearest(level, pos);
            if (nearest == null)
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.no_transmitter"), true);
                return InteractionResult.SUCCESS;
            }
            ResourceKey<Level> dim = level.dimension();
            if (nearest.isBound(dim, pos))
            {
                nearest.unbindMachine(dim, pos);
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.machine_unbound"), true);
            }
            else
            {
                nearest.bindMachine(level, pos);
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.machine_bound"), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ---- 供应器无线连接 ----

    private InteractionResult handleProviderClick(ItemStack stack, Player player, Level level,
            BlockPos pos, InfEnhancedBlockEntity provider) {
        var selection = getProviderSelection(stack);
        if (selection != null && selection.pos().equals(pos)) {
            clearProviderSelection(stack);
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.provider_deselected")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }
        if (provider.infSettings().getProviderMode() != InfProviderSettings.ProviderMode.WIRELESS) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.need_wireless")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }
        selectProvider(stack, level.dimension(), pos);
        player.displayClientMessage(
                Component.translatable("message.godofthings.binder.provider_selected",
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleMachineClick(ItemStack stack, Player player, ServerLevel level,
            BlockPos pos, Direction face, InfEnhancedHost host) {
        if (level.getBlockEntity(pos) instanceof InfEnhancedBlockEntity) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.cannot_bind_provider")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }
        BlockEntity targetBe = level.getBlockEntity(pos);
        if (targetBe == null) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.not_machine")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (host.infRemoveConnection(level.dimension(), pos)) {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.machine_disconnected",
                                pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.not_connected")
                                .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (host.infConnections().size() >= InfEnhancedHost.INF_MAX_CONNECTIONS) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.provider_full",
                            InfEnhancedHost.INF_MAX_CONNECTIONS).withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }
        if (host.infAddOrUpdateConnection(level.dimension(), pos, face)) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.machine_connected",
                            pos.getX(), pos.getY(), pos.getZ(), face.getName())
                            .withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static InfEnhancedBlockEntity resolveProvider(ServerLevel level, ProviderSelection selection) {
        if (level.dimension() != selection.dimension() || !level.isLoaded(selection.pos())) {
            return null;
        }
        return level.getBlockEntity(selection.pos()) instanceof InfEnhancedBlockEntity host ? host : null;
    }
}

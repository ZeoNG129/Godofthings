package com.godofthings.item;

import com.godofthings.block.entity.GodTransmitterBlockEntity;
import com.godofthings.infinityprovider.BinderUsePayload;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

/**
 * 神之绑定器（同心契）：一物三用的绑定工具。
 * <ul>
 *   <li>右键 FE 机器 → 绑定 / 取消绑定到最近的神之传输充能（原有功能）；</li>
 *   <li>右键无限样板供应器 → 选中它为无线宿主（要求已开无线模式）；再点选中 → 取消选中；</li>
 *   <li>选中供应器后右键机器 → 绑定到该供应器；**按住 Ctrl 一键批量绑定连通的同种机器**，
 *       潜行右键断开（潜行 + Ctrl 批量断开）——与 AE2LT 过载无线连接工具一致。</li>
 * </ul>
 * <p>
 * 供应器 / 机器的无线绑定走 {@link BinderUsePayload}：客户端把 Ctrl 状态
 * （服务端读不到按键）随包发给服务端处理。FE 绑定仍在本类服务端直处理。
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
        if (player == null)
        {
            return InteractionResult.PASS;
        }

        boolean clickingBlockEntity = level.getBlockEntity(pos) != null;
        boolean hasSelection = getProviderSelection(stack) != null
                || getOverloadedInterfaceSelection(stack) != null;
        boolean clickingOwnHost = level.getBlockEntity(pos) instanceof InfEnhancedBlockEntity
                || level.getBlockEntity(pos)
                        instanceof com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity;

        // 潜行右键「非机器方块」= 取消选中的供应器；客户端也要拦下来发包，
        // 否则这里会 PASS 给原版、服务端永远收不到取消请求
        if (player.isShiftKeyDown() && !clickingBlockEntity && hasSelection)
        {
            if (level.isClientSide())
            {
                PacketDistributor.sendToServer(new BinderUsePayload(
                        ctx.getHand(), pos, ctx.getClickedFace(),
                        net.minecraft.client.gui.screens.Screen.hasControlDown()));
            }
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        // 目标是自家供应器：选中 / 取消选中（客户端发包，服务端处理，选中状态在物品上需要服务端写）
        if (clickingOwnHost)
        {
            if (level.isClientSide())
            {
                PacketDistributor.sendToServer(new BinderUsePayload(
                        ctx.getHand(), pos, ctx.getClickedFace(),
                        net.minecraft.client.gui.screens.Screen.hasControlDown()));
            }
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        // 已选中供应器时点机器：无线连接 / 断开（Ctrl 批量状态随包走）
        if (hasSelection && clickingBlockEntity)
        {
            if (level.isClientSide())
            {
                PacketDistributor.sendToServer(new BinderUsePayload(
                        ctx.getHand(), pos, ctx.getClickedFace(),
                        net.minecraft.client.gui.screens.Screen.hasControlDown()));
            }
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        // ── 原有：FE 机器 → 绑定 / 取消绑定到最近的神之传输（服务端直处理）──
        if (level.isClientSide || !GodTransmitterBlockEntity.hasEnergyStorage(level, pos))
        {
            return InteractionResult.PASS;
        }
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

    // ---- 服务端统一入口（BinderUsePayload 调用）----

    public static void handleBinderUseOnServer(ServerPlayer player, InteractionHand hand,
            BlockPos pos, Direction face, boolean contiguous) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof GodBinderItem) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0D)) {
            return;
        }

        // 潜行右键「空处 / 非机器方块」= 取消选中的供应器（照抄上游过载无线
        // 连接工具：它的 use() 在任意地方右键就清掉选中状态）。点机器时潜行
        // 仍是「断开连接」，点供应器时仍由 handleProviderClick 处理，都不受影响。
        boolean clickingMachine = level.getBlockEntity(pos) != null;
        if (player.isShiftKeyDown() && !clickingMachine && getProviderSelection(stack) != null) {
            clearProviderSelection(stack);
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.provider_deselected")
                            .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (level.getBlockEntity(pos) instanceof InfEnhancedBlockEntity provider) {
            handleProviderClick(stack, player, level, pos, provider);
            return;
        }
        // 分支一之二：点到过载接口 → 选中 / 取消选中（它有自己的无线连接列表）
        if (level.getBlockEntity(pos)
                instanceof com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity iface) {
            handleOverloadedInterfaceClick(stack, player, level, pos, iface);
            return;
        }

        // 分支二：点到「已选中的过载接口」时连到它
        var ifaceSelection = getOverloadedInterfaceSelection(stack);
        if (ifaceSelection != null) {
            var ifaceBe = resolveOverloadedInterface(level, ifaceSelection);
            if (ifaceBe == null) {
                clearOverloadedInterfaceSelection(stack);
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.host_lost")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }
            handleOverloadedInterfaceConnection(stack, player, level, pos, face, contiguous, ifaceBe);
            return;
        }

        // 分支二：点到机器 → 连接 / 断开（选中供应器已消失则清掉选中状态）
        var selection = getProviderSelection(stack);
        var host = selection == null ? null : resolveProvider(level, selection);
        if (host == null) {
            if (selection != null) {
                clearProviderSelection(stack);
            }
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.host_lost")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (level.getBlockEntity(pos) instanceof InfEnhancedBlockEntity) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.cannot_bind_provider")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (level.getBlockEntity(pos) == null) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.not_machine")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 收集目标：Ctrl 时 BFS 扩展到连通的同种机器（方块一致），否则只点中的这台
        var targets = collectTargets(level, pos, contiguous);
        if (targets.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.not_machine")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 潜行 = 断开
        if (player.isShiftKeyDown()) {
            int disconnected = 0;
            for (var targetPos : targets) {
                if (host.infRemoveConnection(level.dimension(), targetPos)) {
                    disconnected++;
                }
            }
            if (disconnected > 0) {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.machines_disconnected_many",
                                disconnected).withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.not_connected")
                                .withStyle(ChatFormatting.YELLOW), true);
            }
            return;
        }

        int connected = 0;
        int skippedFull = 0;
        for (var targetPos : targets) {
            if (host.infConnections().size() >= InfEnhancedHost.INF_MAX_CONNECTIONS) {
                skippedFull++;
                continue;
            }
            if (host.infAddOrUpdateConnection(level.dimension(), targetPos, face)) {
                connected++;
            }
        }
        if (connected > 1) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.machines_connected_many",
                            connected, face.getName()).withStyle(ChatFormatting.GREEN), true);
        } else if (connected == 1) {
            var p = targets.iterator().next();
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.machine_connected",
                            p.getX(), p.getY(), p.getZ(), face.getName())
                            .withStyle(ChatFormatting.GREEN), true);
        }
        if (skippedFull > 0) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.provider_full_skipped",
                            skippedFull, InfEnhancedHost.INF_MAX_CONNECTIONS)
                            .withStyle(ChatFormatting.RED), true);
        }
    }

    // ---- 供应器选中 ----

    private static void handleProviderClick(ItemStack stack, Player player, Level level,
            BlockPos pos, InfEnhancedBlockEntity provider) {
        var selection = getProviderSelection(stack);
        // 潜行点供应器 = 无条件取消选中（不管点的是不是当前选中的那台）
        if (player.isShiftKeyDown() && selection != null) {
            clearProviderSelection(stack);
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.provider_deselected")
                            .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (selection != null && selection.pos().equals(pos)) {
            clearProviderSelection(stack);
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.provider_deselected")
                            .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (provider.infSettings().getProviderMode() != InfProviderSettings.ProviderMode.WIRELESS) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.need_wireless")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }
        selectProvider(stack, level.dimension(), pos);
        player.displayClientMessage(
                Component.translatable("message.godofthings.binder.provider_selected",
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
    }

    // ---- 过载接口的选中与连接（复用同一份 CustomData 键，二选一存储）----

    private static final String TAG_IFACE_DIM = "InfIfaceDim";
    private static final String TAG_IFACE_POS = "InfIfacePos";

    /** 过载接口选中记录（渲染器也要读，故为 public）。 */
    public record IfaceSelection(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static void selectOverloadedInterface(ItemStack stack, ResourceKey<Level> dim, BlockPos pos) {
        // 清掉供应器选中，避免两个宿主同时被选中
        clearProviderSelection(stack);
        var tag = new CompoundTag();
        tag.putString(TAG_IFACE_DIM, dim.location().toString());
        tag.putLong(TAG_IFACE_POS, pos.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Nullable
    public static IfaceSelection getOverloadedInterfaceSelection(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        var tag = data.copyTag();
        if (!tag.contains(TAG_IFACE_POS)) {
            return null;
        }
        return new IfaceSelection(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(tag.getString(TAG_IFACE_DIM))),
                BlockPos.of(tag.getLong(TAG_IFACE_POS)));
    }

    private static void clearOverloadedInterfaceSelection(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        var tag = data.copyTag();
        tag.remove(TAG_IFACE_DIM);
        tag.remove(TAG_IFACE_POS);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Nullable
    private static com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity
            resolveOverloadedInterface(Level level, IfaceSelection selection) {
        if (!level.dimension().equals(selection.dimension()) || !level.isLoaded(selection.pos())) {
            return null;
        }
        return level.getBlockEntity(selection.pos())
                instanceof com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity iface
                        ? iface
                        : null;
    }

    /** 点过载接口本身：选中 / 取消（要求已切无线模式，与供应器一致）。 */
    private static void handleOverloadedInterfaceClick(ItemStack stack, Player player, Level level,
            BlockPos pos, com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity iface) {
        var selection = getOverloadedInterfaceSelection(stack);
        if (player.isShiftKeyDown() && selection != null) {
            clearOverloadedInterfaceSelection(stack);
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.provider_deselected")
                            .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (selection != null && selection.pos().equals(pos)) {
            clearOverloadedInterfaceSelection(stack);
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.provider_deselected")
                            .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (iface.getInterfaceMode()
                != com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity.InterfaceMode.WIRELESS) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.need_wireless")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }
        selectOverloadedInterface(stack, level.dimension(), pos);
        player.displayClientMessage(
                Component.translatable("message.godofthings.binder.provider_selected",
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
    }

    /** 已选中过载接口后点机器：连接 / 潜行断开（Ctrl 批量）。 */
    private static void handleOverloadedInterfaceConnection(ItemStack stack, Player player,
            ServerLevel level, BlockPos pos, Direction face, boolean contiguous,
            com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity iface) {
        if (level.getBlockEntity(pos) == null) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.not_machine")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }
        var targets = collectTargets(level, pos, contiguous);
        if (targets.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.not_machine")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (player.isShiftKeyDown()) {
            int disconnected = 0;
            for (var targetPos : targets) {
                if (iface.removeConnection(level.dimension(), targetPos)) {
                    disconnected++;
                }
            }
            player.displayClientMessage(disconnected > 0
                    ? Component.translatable("message.godofthings.binder.machines_disconnected_many",
                            disconnected).withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.godofthings.binder.not_connected")
                            .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        int connected = 0;
        for (var targetPos : targets) {
            if (iface.addOrUpdateConnection(
                    new com.godofthings.overloadedinterface.OverloadedInterfaceBlockEntity
                            .WirelessConnection(level.dimension(), targetPos, face))) {
                connected++;
            }
        }
        if (connected > 1) {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.machines_connected_many",
                            connected, face.getName()).withStyle(ChatFormatting.GREEN), true);
        } else if (connected == 1) {
            var p = targets.iterator().next();
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.machine_connected",
                            p.getX(), p.getY(), p.getZ(), face.getName())
                            .withStyle(ChatFormatting.GREEN), true);
        }
    }
    /**
     * 收集连接目标（移植上游 WirelessConnectorTargetHelper.collectTargets）：
     * 非 Ctrl 只返回原点；Ctrl 时从原点 BFS 扩展到六向连通、
     * 方块类型一致且都有方块实体的机器群（同种贴片的机器阵列一键全选）。
     */
    private static java.util.Set<BlockPos> collectTargets(ServerLevel level, BlockPos origin, boolean contiguous) {
        if (!level.isLoaded(origin) || level.getBlockEntity(origin) == null) {
            return java.util.Set.of();
        }
        if (!contiguous) {
            return java.util.Set.of(origin.immutable());
        }
        var originState = level.getBlockState(origin);
        var visited = new java.util.LinkedHashSet<BlockPos>();
        var queue = new java.util.ArrayDeque<BlockPos>();
        queue.add(origin.immutable());
        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            for (var direction : Direction.values()) {
                var next = current.relative(direction);
                if (visited.contains(next) || !level.isLoaded(next)) {
                    continue;
                }
                if (level.getBlockEntity(next) == null) {
                    continue;
                }
                if (!level.getBlockState(next).is(originState.getBlock())) {
                    continue;
                }
                queue.addLast(next.immutable());
            }
        }
        return visited;
    }

    @Nullable
    private static InfEnhancedBlockEntity resolveProvider(ServerLevel level, ProviderSelection selection) {
        if (level.dimension() != selection.dimension() || !level.isLoaded(selection.pos())) {
            return null;
        }
        return level.getBlockEntity(selection.pos()) instanceof InfEnhancedBlockEntity host ? host : null;
    }
}

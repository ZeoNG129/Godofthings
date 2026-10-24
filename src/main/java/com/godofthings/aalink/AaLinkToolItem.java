package com.godofthings.aalink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;

/**
 * 无线连接工具（移植自 AALink 的 WirelessLinkTool）：
 * 把任意 AE 设备手动接入已绑定无线连接器所在的 ME 网络。
 *
 * <ul>
 *   <li>蹲下右键无线连接器 → 绑定（记录连接器位置与维度到物品数据组件）</li>
 *   <li>蹲下对着空气右键 → 取消绑定</li>
 *   <li>右键任意能连接 AE 网络的方块（线缆/总线/机器等）→ 把该设备接入已绑定连接器所在网络</li>
 * </ul>
 *
 * <p>与自动桥接（{@link AaLinkBridge}）同一套逻辑：{@link GridHelper#createConnection}
 * 建立不依赖物理位置的逻辑连接，无视距离与维度；连接结果写入 {@link AaLinkSavedData}，
 * 重进存档/区块重载后保持。</p>
 */
public class AaLinkToolItem extends Item
{
    private static final String TAG_BINDING = "aalink.binding";
    private static final String TAG_DIM = "dim";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";

    public AaLinkToolItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        // 交互链最前端（NeoForge 的 IItemExtension 钩子）：返回非 PASS 时方块 use（打开 GUI）不会执行。
        // 这里消费右键，保证手持工具右键方块时是「连接」而不是打开方块 UI。
        return handleUse(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        return handleUse(context);
    }

    /**
     * 工具交互核心：
     * <ul>
     *   <li>右键无线连接器：蹲下=绑定，非蹲下=提示用蹲下绑定</li>
     *   <li>蹲下右键其它方块：不消费，保持原语义</li>
     *   <li>非蹲下右键其它方块：把该方块接入已绑定连接器所在网络，并阻止方块 UI 打开</li>
     * </ul>
     */
    private InteractionResult handleUse(UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null)
        {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        // 右键无线连接器：蹲下=绑定，否则提示
        if (be instanceof AaLinkConnectorBlockEntity)
        {
            if (!level.isClientSide)
            {
                if (player.isShiftKeyDown())
                {
                    bind(stack, level, pos);
                    player.displayClientMessage(
                            Component.translatable("message.godofthings.aalink.tool_bound",
                                    level.dimension().location().toString(), pos.toShortString()),
                            true);
                }
                else
                {
                    player.displayClientMessage(
                            Component.translatable("message.godofthings.aalink.tool_need_sneak"), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 蹲下右键其它方块：不消费，交给方块/其它逻辑处理
        if (player.isShiftKeyDown())
        {
            return InteractionResult.PASS;
        }

        // 非蹲下右键其它方块：尝试接入网络，并消费交互阻止方块 UI 打开
        if (!level.isClientSide)
        {
            connectDevice((ServerLevel) level, player, stack, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        // 蹲下对着空气右键 → 取消绑定
        if (player.isShiftKeyDown() && isBound(stack))
        {
            if (!level.isClientSide)
            {
                clearBinding(stack);
                player.displayClientMessage(
                        Component.translatable("message.godofthings.aalink.tool_unbound"), true);
            }
            return level.isClientSide
                    ? InteractionResultHolder.success(stack)
                    : InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static void bind(ItemStack stack, Level level, BlockPos pos)
    {
        CompoundTag binding = new CompoundTag();
        binding.putString(TAG_DIM, level.dimension().location().toString());
        binding.putInt(TAG_X, pos.getX());
        binding.putInt(TAG_Y, pos.getY());
        binding.putInt(TAG_Z, pos.getZ());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(TAG_BINDING, binding));
    }

    private static boolean isBound(ItemStack stack)
    {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TAG_BINDING);
    }

    private static void clearBinding(ItemStack stack)
    {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null)
        {
            // CustomData 是不可变值：改副本再写回；删空后整个组件清掉，保持物品干净
            CompoundTag tag = data.copyTag();
            tag.remove(TAG_BINDING);
            if (tag.isEmpty())
            {
                stack.remove(DataComponents.CUSTOM_DATA);
            }
            else
            {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    private static void connectDevice(ServerLevel level, Player player, ItemStack stack, BlockPos devicePos)
    {
        if (!isBound(stack))
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.aalink.tool_not_bound"), true);
            return;
        }
        CompoundTag binding = stack.get(DataComponents.CUSTOM_DATA).copyTag().getCompound(TAG_BINDING);
        ResourceLocation dim = ResourceLocation.tryParse(binding.getString(TAG_DIM));
        BlockPos hubPos = new BlockPos(binding.getInt(TAG_X), binding.getInt(TAG_Y), binding.getInt(TAG_Z));
        ServerLevel hubLevel = dim == null
                ? null
                : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (hubLevel == null)
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.aalink.tool_hub_dim_missing"), true);
            return;
        }
        if (!(hubLevel.getBlockEntity(hubPos) instanceof AaLinkConnectorBlockEntity hub))
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.aalink.tool_hub_missing"), true);
            return;
        }
        IGridNode hubNode = hub.getHubNode();
        if (hubNode == null || hubNode.getGrid() == null)
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.aalink.tool_hub_not_ready"), true);
            return;
        }
        IGridNode deviceNode = AaLinkBridge.resolveDeviceNode(level, devicePos);
        if (deviceNode == null)
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.aalink.tool_not_ae_device"), true);
            return;
        }
        if (deviceNode == hubNode || deviceNode.getGrid() == hubNode.getGrid())
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.aalink.tool_already_linked"), true);
            return;
        }
        try
        {
            GridHelper.createConnection(deviceNode, hubNode);
        }
        catch (Exception e)
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.aalink.tool_failed", e.getMessage()), true);
            return;
        }
        AaLinkSavedData.get(level).link(devicePos); // 持久化：重进存档后保持连接
        player.displayClientMessage(
                Component.translatable("message.godofthings.aalink.tool_linked"), true);
    }
}

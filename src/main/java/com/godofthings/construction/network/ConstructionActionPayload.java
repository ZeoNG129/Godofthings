package com.godofthings.construction.network;

import com.godofthings.Godofthings;
import com.godofthings.construction.ConstructionUndo;
import com.godofthings.construction.GodConstructionItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 神之建筑：设置界面（{@code GodConstructionScreen}）用的客户端 → 服务端动作包。
 *
 * <p>四种动作（{@code action} 字段；{@code value} 只有「设模式」用得到）：</p>
 * <ul>
 *   <li>{@link #ACTION_SET_MODE}：把物品的模式改成 {@code value}（0 建造 / 1 销毁 / 2 替换）；</li>
 *   <li>{@link #ACTION_TOGGLE_PREVIEW}：翻转预览线框开关（数据组件 {@code godofthings:god_construction_preview}）；</li>
 *   <li>{@link #ACTION_TOGGLE_SIMILAR}：翻转相似方块开关（数据组件 {@code godofthings:god_construction_similar}）；</li>
 *   <li>{@link #ACTION_UNDO}：撤回最近一次建造 / 销毁 / 替换（见 {@link ConstructionUndo}）。</li>
 * </ul>
 *
 * <p>服务端只认<b>玩家手上拿着的本族物品</b>（神之建筑 / 天神建筑，主手优先，其次副手）：
 * 界面是自己开的，但网络数据不可信，不能信客户端报来的槽位号。手上没有这东西就整个丢掉。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public record ConstructionActionPayload(int action, int value) implements CustomPacketPayload
{
    /** 设置模式（value = 0 建造 / 1 销毁 / 2 替换） */
    public static final int ACTION_SET_MODE = 0;
    /** 翻转预览线框开关 */
    public static final int ACTION_TOGGLE_PREVIEW = 1;
    /** 翻转相似方块开关 */
    public static final int ACTION_TOGGLE_SIMILAR = 2;
    /** 撤销最近一次批量操作 */
    public static final int ACTION_UNDO = 3;

    public static final Type<ConstructionActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "god_construction_action"));

    public static final StreamCodec<FriendlyByteBuf, ConstructionActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) ->
            {
                buf.writeVarInt(payload.action());
                buf.writeVarInt(payload.value());
            },
            buf -> new ConstructionActionPayload(buf.readVarInt(), buf.readVarInt()));

    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ConstructionActionPayload.TYPE, ConstructionActionPayload.STREAM_CODEC,
                ConstructionActionPayload::handle);
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    /** 客户端发动作：设模式 */
    public static void sendSetMode(int mode)
    {
        PacketDistributor.sendToServer(new ConstructionActionPayload(ACTION_SET_MODE, mode));
    }

    /** 客户端发动作：翻转预览开关 */
    public static void sendTogglePreview()
    {
        PacketDistributor.sendToServer(new ConstructionActionPayload(ACTION_TOGGLE_PREVIEW, 0));
    }

    /** 客户端发动作：翻转相似方块开关 */
    public static void sendToggleSimilar()
    {
        PacketDistributor.sendToServer(new ConstructionActionPayload(ACTION_TOGGLE_SIMILAR, 0));
    }

    /** 客户端发动作：撤销 */
    public static void sendUndo()
    {
        PacketDistributor.sendToServer(new ConstructionActionPayload(ACTION_UNDO, 0));
    }

    public static void handle(ConstructionActionPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
            {
                return;
            }
            // 只认手上拿着的（网络数据不可信，不收客户端报的槽位）
            ItemStack wand = GodConstructionItem.findWand(player);
            if (wand.isEmpty())
            {
                return;
            }

            switch (payload.action())
            {
                case ACTION_SET_MODE ->
                {
                    if (payload.value() >= 0 && payload.value() < GodConstructionItem.MODE_COUNT)
                    {
                        wand.set(Godofthings.GOD_CONSTRUCTION_MODE.get(), payload.value());
                    }
                }
                case ACTION_TOGGLE_PREVIEW -> wand.set(Godofthings.GOD_CONSTRUCTION_PREVIEW.get(),
                        !GodConstructionItem.isPreviewEnabled(wand));
                case ACTION_TOGGLE_SIMILAR -> wand.set(Godofthings.GOD_CONSTRUCTION_SIMILAR.get(),
                        !GodConstructionItem.isSimilarEnabled(wand));
                case ACTION_UNDO -> undo(player);
                default -> { }
            }
        });
    }

    /** 撤销 + 一条行动栏反馈（0 格 = 没有历史，单独提示一句） */
    private static void undo(ServerPlayer player)
    {
        int restored = ConstructionUndo.undo(player);
        player.displayClientMessage(restored > 0
                ? Component.translatable("message.godofthings.god_construction.undone", restored)
                : Component.translatable("message.godofthings.god_construction.undo_empty"), true);
    }

}

package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.backpack.GodBackpackMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 神之背包：客户端 → 服务端的动作包。
 *
 * <p>四种动作（{@code action} 字段，取下面的常量；{@code value} / {@code text} 按动作解释）：</p>
 * <ul>
 *   <li>{@link #ACTION_TOGGLE_MEMORY}：切换某背包槽位的记忆（value = 背包槽位下标 0..119）</li>
 *   <li>{@link #ACTION_TOGGLE_NO_SORT}：切换某背包槽位是否「整理时原地不动」（value = 背包槽位下标）</li>
 *   <li>{@link #ACTION_SET_SEARCH}：设置搜索词（text = 搜索词）</li>
 *   <li>{@link #ACTION_SET_SCROLL}：设置滚动行数（value = 行数，服务端 clamp 到 0..8）</li>
 * </ul>
 *
 * <p>不带 containerId：服务端用 {@code player.containerMenu} 找到当前开着的
 * {@link GodBackpackMenu}，不是背包界面就整个丢掉（客户端伪造的包也不会打到别的界面）。</p>
 */
public record GodBackpackActionPayload(int action, int value, String text) implements CustomPacketPayload
{
    public static final int ACTION_TOGGLE_MEMORY = 0;
    public static final int ACTION_TOGGLE_NO_SORT = 1;
    public static final int ACTION_SET_SEARCH = 2;
    public static final int ACTION_SET_SCROLL = 3;

    /** 文本字段的长度上限（搜索词本身还会被 GodBackpackSettings 再截一次） */
    private static final int MAX_TEXT_LENGTH = 64;

    public static final Type<GodBackpackActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "god_backpack_action"));

    public static final StreamCodec<FriendlyByteBuf, GodBackpackActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) ->
            {
                buf.writeVarInt(payload.action());
                buf.writeVarInt(payload.value());
                buf.writeUtf(payload.text());
            },
            buf -> new GodBackpackActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readUtf()));

    public GodBackpackActionPayload
    {
        // 网络数据不可信：搜索词先截断（服务端还会再截一次），null 当空串
        text = text == null ? "" : text;
        if (text.length() > MAX_TEXT_LENGTH)
        {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(GodBackpackActionPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
            {
                return;
            }
            if (!(player.containerMenu instanceof GodBackpackMenu menu))
            {
                return; // 没开着背包界面（或已经关掉了）→ 丢弃
            }
            switch (payload.action())
            {
                case ACTION_TOGGLE_MEMORY -> menu.toggleMemory(payload.value());
                case ACTION_TOGGLE_NO_SORT -> menu.toggleNoSort(payload.value());
                case ACTION_SET_SEARCH -> menu.setSearchPhrase(payload.text());
                case ACTION_SET_SCROLL -> menu.setScroll(payload.value());
                default -> { }
            }
        });
    }
}

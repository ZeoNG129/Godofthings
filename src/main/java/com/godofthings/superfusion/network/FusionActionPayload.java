package com.godofthings.superfusion.network;

import com.godofthings.superfusion.MemoryCardLogic;
import com.godofthings.superfusion.SuperFusionItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 融合卡客户端动作包：轮换融合模式 / 清空卡内机器数据。
 * <p>
 * 这两件事都由客户端按键触发（潜行+左键空气、潜行+右键空气），必须回服务端执行，
 * 否则数据组件改了但服务端物品栈不同步。
 */
public record FusionActionPayload(int actionId) implements CustomPacketPayload
{
    public static final Type<FusionActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(com.godofthings.Godofthings.MODID, "super_fusion_action"));

    public static final StreamCodec<FriendlyByteBuf, FusionActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FusionActionPayload::actionId,
            FusionActionPayload::new);

    /** 动作编号 */
    private enum Action
    {
        CYCLE_MODE,
        CLEAR_MEMORY;

        private static Action byId(int id)
        {
            Action[] values = values();
            return id < 0 || id >= values.length ? CYCLE_MODE : values[id];
        }
    }

    public static FusionActionPayload cycleMode()
    {
        return new FusionActionPayload(Action.CYCLE_MODE.ordinal());
    }

    public static FusionActionPayload clearMemory()
    {
        return new FusionActionPayload(Action.CLEAR_MEMORY.ordinal());
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(FusionActionPayload payload, IPayloadContext context)
    {
        Player player = context.player();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SuperFusionItem))
        {
            return;
        }
        Action action = Action.byId(payload.actionId());
        if (action == Action.CYCLE_MODE)
        {
            SuperFusionItem.cycleMode(stack, player);
        }
        else if (action == Action.CLEAR_MEMORY)
        {
            MemoryCardLogic.handleClearMachineData(player, stack);
        }
    }
}

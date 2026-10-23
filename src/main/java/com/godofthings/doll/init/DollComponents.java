package com.godofthings.doll.init;

import com.godofthings.Godofthings;
import com.godofthings.doll.entity.TwoIDollMemory;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * 玩偶物品用到的数据组件。
 *
 * <p>从《无用担架》移植时，原实现挂在 {@code useless_stretcher} 命名空间下的
 * {@code StretcherComponents} 里，和手杖、胶囊、范围加速等一堆无关组件混在一起。
 * 这里只保留玩偶真正需要的 8 个，独立成类，避免和主模组的组件表互相干扰。</p>
 *
 * <p><b>注册必须在 {@link Godofthings} 的静态初始化里发生</b> —— 这是本仓库踩过的坑：
 * {@code DeferredRegister} 内部记着「RegisterEvent 是否已触发」，触发之后再调
 * {@code register(...)} 会抛 {@code IllegalStateException}，导致整个服务端启动崩溃。
 * 所以本类只做<b>别名引用</b>，真正的 {@code DATA_COMPONENTS.register(...)} 写在
 * {@code Godofthings} 里。</p>
 */
public final class DollComponents
{
    /** 玩偶朝向（0-3，对应四个水平方向） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TWO_I_ROTATION =
            integerComponent("two_i_rotation");

    /** 礼物累计分（决定好感度成长） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TWO_I_GIFT_SCORE =
            integerComponent("two_i_gift_score");

    /** 好感度（0-100） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TWO_I_BOND =
            integerComponent("two_i_bond");

    /** 玩偶主人 UUID */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> TWO_I_OWNER =
            register("two_i_owner", builder -> builder.persistent(UUIDUtil.CODEC)
                    .networkSynchronized(StreamCodec.<FriendlyByteBuf, UUID>of((buf, value) -> buf.writeUUID(value), buf -> buf.readUUID())));

    /** 玩偶实例 UUID（同一只玩偶跨物品/实体保持同一个身份） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> TWO_I_DOLL_ID =
            register("two_i_doll_id", builder -> builder.persistent(UUIDUtil.CODEC)
                    .networkSynchronized(StreamCodec.<FriendlyByteBuf, UUID>of((buf, value) -> buf.writeUUID(value), buf -> buf.readUUID())));

    /** 玩偶记忆（冷却、最近互动等，仅存档持久化，不走网络） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TwoIDollMemory>> TWO_I_MEMORY =
            register("two_i_memory", builder -> builder.persistent(TwoIDollMemory.CODEC));

    /** 星光庭院邀请是否已用过（星光庭院不移植，但组件保留以免旧存档读不进来） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TWO_I_INVITATION_USED =
            register("two_i_invitation_used", builder -> builder.persistent(Codec.BOOL)
                    .networkSynchronized(StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean)));

    private DollComponents()
    {
    }

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> operator)
    {
        return Godofthings.DATA_COMPONENTS.register(name,
                () -> operator.apply(DataComponentType.builder()).build());
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> integerComponent(String name)
    {
        return register(name, builder -> builder.persistent(Codec.INT)
                .networkSynchronized(StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt)));
    }
}

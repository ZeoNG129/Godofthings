package com.godofthings.doll.init;

import com.godofthings.Godofthings;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.UUID;

/**
 * 玩偶物品用到的数据组件 —— <b>全部是 {@link Godofthings} 里同名注册的别名引用</b>。
 *
 * <p><b>为什么这个类里没有任何 register 调用</b>（仓库硬规则，v7.0.0 的闪退就是这个）：
 * {@code DeferredRegister} 内部记着「RegisterEvent 是否已触发」，触发之后再调
 * {@code register(...)} 抛 {@code IllegalStateException}。组件的真实注册写在
 * {@code Godofthings} 的<b>静态初始化</b>里 —— 静态块在 mod 构造阶段就执行，一定早于
 * RegisterEvent。而本类如果不小心在运行期才被首次加载（例如物品 tick 引用到），
 * 只要它自己不调 register，就只会拿到已注册好的 {@link DeferredHolder}，安全。</p>
 *
 * <p>原版对应 {@code StretcherComponents} 的 TWO_I_* 段；id 保持 {@code two_i_*}
 * 不变，与源码一致。</p>
 */
public final class DollComponents
{
    /** 玩偶朝向（0-3，对应四个水平方向） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TWO_I_ROTATION =
            Godofthings.DOLL_TWO_I_ROTATION;

    /** 礼物累计分（决定好感度成长） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TWO_I_GIFT_SCORE =
            Godofthings.DOLL_TWO_I_GIFT_SCORE;

    /** 好感度（-100..100） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TWO_I_BOND =
            Godofthings.DOLL_TWO_I_BOND;

    /** 玩偶主人 UUID */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> TWO_I_OWNER =
            Godofthings.DOLL_TWO_I_OWNER;

    /** 玩偶实例 UUID（同一只玩偶跨物品/实体保持同一个身份） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> TWO_I_DOLL_ID =
            Godofthings.DOLL_TWO_I_DOLL_ID;

    /** 玩偶记忆（冷却、最近互动等，仅存档持久化，不走网络） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.godofthings.doll.entity.TwoIDollMemory>> TWO_I_MEMORY =
            Godofthings.DOLL_TWO_I_MEMORY;

    /** 星光庭院邀请是否已用过 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TWO_I_INVITATION_USED =
            Godofthings.DOLL_TWO_I_INVITATION_USED;

    private DollComponents()
    {
    }
}

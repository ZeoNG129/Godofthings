package com.godofthings.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 「神之配置」存在物品数据组件里的那份配置（{@code godofthings:god_config}）。
 *
 * <p>字段：方块注册名（{@code blockId}，粘贴时校验机器类型）、六面 I/O 模式
 * （{@code faceModes}，长度恒为 6，取值 0..3 = 无 / 输入 / 输出 / 输入和输出）、
 * AE 并网开关（{@code aeEnabled}）、加速槽里的神之加速数量（{@code accelCount}，0..64）。
 * <b>只记数量不记物品</b> —— 粘贴时是从玩家物品栏里扣真正神之加速补进去。</p>
 */
public record GodConfigData(String blockId, int[] faceModes, boolean aeEnabled, int accelCount)
{
    /** 面数（与 {@link ConfigurableMachine#FACE_COUNT} 一致） */
    public static final int FACE_COUNT = ConfigurableMachine.FACE_COUNT;

    /** 加速数量上限（一组 = 64） */
    public static final int MAX_ACCELERATORS = ConfigurableMachine.MAX_ACCELERATORS;

    /** 存档编解码：{@code {block:"godofthings:furnace", faces:[...], ae:true, accel:12}} */
    public static final Codec<GodConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("block").forGetter(GodConfigData::blockId),
            Codec.INT.listOf().fieldOf("faces").forGetter(GodConfigData::faceList),
            Codec.BOOL.optionalFieldOf("ae", true).forGetter(GodConfigData::aeEnabled),
            Codec.INT.optionalFieldOf("accel", 0).forGetter(GodConfigData::accelCount)
    ).apply(instance, GodConfigData::fromParts));

    /**
     * 网络同步编解码：方块名 + 6 个 varint 面模式 + 开关 + 加速数量。
     * 用 {@link FriendlyByteBuf}（{@code RegistryFriendlyByteBuf} 的父类型）即可满足
     * {@code DataComponentType#networkSynchronized} 的入参要求。
     */
    public static final StreamCodec<FriendlyByteBuf, GodConfigData> STREAM_CODEC = StreamCodec.of(
            (buf, value) ->
            {
                buf.writeUtf(value.blockId());
                for (int i = 0; i < FACE_COUNT; i++)
                {
                    buf.writeVarInt(value.faceModes()[i]);
                }
                buf.writeBoolean(value.aeEnabled());
                buf.writeVarInt(value.accelCount());
            },
            buf ->
            {
                String block = buf.readUtf();
                int[] modes = new int[FACE_COUNT];
                for (int i = 0; i < FACE_COUNT; i++)
                {
                    modes[i] = buf.readVarInt();
                }
                return new GodConfigData(block, modes, buf.readBoolean(), buf.readVarInt());
            });

    /** 统一兜底：面模式定长 6 且折回 0..3，加速数量夹在 0..64，方块名 null → 空串 */
    public GodConfigData
    {
        blockId = blockId == null ? "" : blockId;
        int[] clean = new int[FACE_COUNT];
        if (faceModes != null)
        {
            for (int i = 0; i < FACE_COUNT && i < faceModes.length; i++)
            {
                clean[i] = ((faceModes[i] % 4) + 4) % 4;
            }
        }
        faceModes = clean;
        accelCount = Math.max(0, Math.min(MAX_ACCELERATORS, accelCount));
    }

    /** 存档编解码的组装回调：int 列表 → 定长数组 */
    private static GodConfigData fromParts(String blockId, List<Integer> faces, boolean aeEnabled, int accelCount)
    {
        int[] modes = new int[FACE_COUNT];
        for (int i = 0; i < FACE_COUNT && i < faces.size(); i++)
        {
            Integer value = faces.get(i);
            modes[i] = value == null ? 0 : value;
        }
        return new GodConfigData(blockId, modes, aeEnabled, accelCount);
    }

    /** 存档编解码用：面模式 → int 列表 */
    private List<Integer> faceList()
    {
        List<Integer> list = new ArrayList<>(FACE_COUNT);
        for (int mode : faceModes)
        {
            list.add(mode);
        }
        return list;
    }

    /** 是否真的存了东西（没存过的卡 blockId 是空串） */
    public boolean hasData()
    {
        return !blockId.isEmpty();
    }

    /** 面模式副本（外部随便改也动不到记录里那份） */
    public int[] copyFaceModes()
    {
        return faceModes.clone();
    }

    // record 默认按引用比较 int[]（永远不等），这里按内容比 ——
    // 数据组件靠 equals 判断「要不要同步给客户端」，按引用比会白推包。
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof GodConfigData data))
        {
            return false;
        }
        return aeEnabled == data.aeEnabled
                && accelCount == data.accelCount
                && blockId.equals(data.blockId)
                && Arrays.equals(faceModes, data.faceModes);
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * blockId.hashCode() + Arrays.hashCode(faceModes)) + accelCount * 2 + (aeEnabled ? 1 : 0);
    }

    @Override
    public String toString()
    {
        return "GodConfigData[block=" + blockId + ", faces=" + Arrays.toString(faceModes)
                + ", ae=" + aeEnabled + ", accel=" + accelCount + "]";
    }
}

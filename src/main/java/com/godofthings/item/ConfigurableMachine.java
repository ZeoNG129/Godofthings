package com.godofthings.item;

/**
 * 「神之配置」能读写的机器契约（见 {@link GodConfigItem}）。
 *
 * <p>保存时从机器读出六面 I/O 模式 / AE 并网开关 / 加速槽里的神之加速数量，
 * 粘贴时再写回去。实现方一律直接委托自己已有的字段与方法（{@code faceModes} /
 * {@code aeEnabled} / {@code accelSlot}），本接口不引入任何新的状态，
 * 所以存档格式与机器逻辑都不受影响。</p>
 *
 * <p>目前已实现：神之熔炉 / 神之矿机 / 神之矿物 / 神之作物 / 神之复制 / 神之去皮 /
 * 神之掉落 / 神之怪蛋 / 神之砍杀 / 神之吸收 / 神之合成（最后三台没有加速槽，
 * 加速数量恒为 0，只同步六面模式与 AE 开关；以后要再加新机器，让它的方块实体
 * {@code implements ConfigurableMachine} 即可，物品侧不用改）。</p>
 */
public interface ConfigurableMachine
{
    /** 面数（索引 = {@code Direction.get3DDataValue()}） */
    int FACE_COUNT = 6;

    /** 加速槽容量：一组神之加速（64 个） */
    int MAX_ACCELERATORS = 64;

    /** 六面模式快照（长度恒为 {@link #FACE_COUNT}，取值 0..3，见 {@code FaceMode}） */
    int[] getFaceModesSnapshot();

    /** 覆盖六面模式（越界 / 负值自动折回 0..3；长度不足的面保持原值） */
    void applyFaceModes(int[] modes);

    /** AE 并网开关当前状态 */
    boolean isAeEnabled();

    /** 直接设置 AE 并网开关（不是切换） */
    void setAeEnabled(boolean enabled);

    /** 加速槽里现有的神之加速数量 */
    int acceleratorCount();

    /** 往加速槽里补最多 {@code want} 个神之加速，返回实际补进去的数量 */
    int addAccelerators(int want);
}

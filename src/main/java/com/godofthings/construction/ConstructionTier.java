package com.godofthings.construction;

/**
 * 建筑棒的品阶：四个上限按品阶取值。
 *
 * <p>{@link #GOD} 是神之建筑（{@code godofthings:construction_min}），数值与改造前完全一致；
 * {@link #HEAVENLY} 是天神建筑（{@code godofthings:construction_max}），四个上限全部取
 * {@code Integer.MAX_VALUE}。</p>
 *
 * <p><b>为什么 int 上限就等于「实际无限」：</b>本模组自己的 BFS 从来没有「先圈一片再执行」的中间集合，
 * 队列里最多只放已访问过的连通格，而 1.21.1 的坐标范围（±3×10⁷）和实际可加载区块数加起来也到不了
 * {@code 2^31-1} 个方块；真正先触发的一定是别的边界，不是这个数字。安全兜底因此<b>不放在这个上限上</b>，
 * 而是：</p>
 * <ul>
 *   <li>每 tick 的格子预算 {@link ConstructionJob#PER_TICK_BUDGET}（超大作业跨 tick 分摊，不卡服务端）；</li>
 *   <li>天使放置的步进上限（{@link ConstructionActions#ANGEL_STEP_LIMIT} 格，绝不 {@code eye + look * MAX_VALUE}）；</li>
 *   <li>作业的取消条件（玩家下线 / 换维度 / 手上没棒子）。</li>
 * </ul>
 */
public enum ConstructionTier
{
    /** 神之建筑：建造 1024 / 销毁 81 / 替换 1024 / 天使 2（与改造前逐项一致） */
    GOD(GodConstructionItem.MAX_BUILD_BLOCKS,
            GodConstructionItem.MAX_DESTROY_BLOCKS,
            GodConstructionItem.MAX_REPLACE_BLOCKS,
            GodConstructionItem.ANGEL_DISTANCE),

    /** 天神建筑：四项全是 int 上限 = 实际无限 */
    HEAVENLY(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** 建造模式一次最多铺多少格 */
    private final int maxBuild;
    /** 销毁模式一次最多拆多少格 */
    private final int maxDestroy;
    /** 替换模式一次最多换多少格 */
    private final int maxReplace;
    /** 天使放置最远沿视线走多少格（int 上限 = 实际无限，实际步进另有上限） */
    private final int angelDistance;

    ConstructionTier(int maxBuild, int maxDestroy, int maxReplace, int angelDistance)
    {
        this.maxBuild = maxBuild;
        this.maxDestroy = maxDestroy;
        this.maxReplace = maxReplace;
        this.angelDistance = angelDistance;
    }

    public int getMaxBuild()
    {
        return this.maxBuild;
    }

    public int getMaxDestroy()
    {
        return this.maxDestroy;
    }

    public int getMaxReplace()
    {
        return this.maxReplace;
    }

    public int getAngelDistance()
    {
        return this.angelDistance;
    }
}

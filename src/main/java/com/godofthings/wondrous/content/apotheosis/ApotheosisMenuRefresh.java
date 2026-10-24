package com.godofthings.wondrous.content.apotheosis;

/**
 * 内部桥接接口：Apothic Enchanting 菜单被杖改表后刷新统计（照抄上游 ApotheosisMenuRefresh）。
 * <p>
 * 上游通过 Apotheosis 的 mixin 让其菜单实现此接口；本模组未移植那几个
 * {@code @Pseudo} mixin，这里的实例只会在未安装 Apotheosis 时缺席，
 * {@link ApotheosisTableData#refreshOpenMenus} 的反射分支兜底。
 */
public interface ApotheosisMenuRefresh
{
    void uselessStretcher$refreshStats();
}

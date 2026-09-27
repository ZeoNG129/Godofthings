package com.godofthings.beef.stretcher;

/**
 * 照抄自 UselessStretcher（万象担架）扩展模组的兼容垫片（shim）。
 *
 * <p>「荒辰移晷之杖」（{@link com.godofthings.beef.stretcher.content.item.WondrousStaffItem}）
 * 这一整套代码是<b>逐字照抄</b>该扩展模组的，唯一改动的只有包名前缀
 * {@code com.sorrowmist.useless.stretcher} → {@code com.godofthings.beef.stretcher}
 * 与命名空间 {@code useless_stretcher} → {@code godofthings}。
 * 上游代码里大量出现 {@code UselessStretcherMod.MODID}，为了不改动任何一行照抄代码，
 * 这里保留同名类型。</p>
 *
 * <p>上游这个类是带 {@code @Mod} 的主类；照抄进本模组后<b>去掉注解与构造器</b>，
 * 只留 modid 常量，注册与事件接线一律由 {@link com.godofthings.Godofthings} 与
 * {@link com.godofthings.beef.stretcher.init.StretcherRegistration} 负责
 * （同一个 modid 不允许有两个 {@code @Mod} 类）。</p>
 */
public final class UselessStretcherMod {
    /** 上游为 "useless_stretcher"；照抄进本模组后统一走本模组的命名空间。 */
    public static final String MODID = "godofthings";

    private UselessStretcherMod() {
    }
}

package com.godofthings.ae2;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AE2 软依赖入口。
 * <p>
 * <b>本类不得出现任何 {@code appeng.*} 类型</b>——AE2 是 compileOnly 依赖，未安装时整个
 * appeng 包都不在运行时 classpath 上，本类必须能在那种情况下正常加载（否则模组会在
 * 注册阶段抛 {@code NoClassDefFoundError} 直接崩掉）。
 * <p>
 * 硬引用 AE2 的代码全部隔离在 {@code com.godofthings.ae2} 下的 AE 专属类里
 * （{@link AeRegistration} 与 7 个 {@code *AeBlockEntity}），这些类只在
 * {@link #isLoaded()} 为真时经反射加载；JVM 会随父接口一起加载其直接父接口，
 * 所以「用 implements 声明 AE 接口」的类绝不能在未装 AE2 时被加载。
 * <p>
 * 选用了哪条路径会各打一条 INFO 日志（每种方块实体只打一次），便于出问题时一眼定位。
 */
public final class AeSoftDepend
{
    public static final String AE2_MODID = "ae2";
    private static final String AE_PACKAGE = "com.godofthings.ae2.";
    private static final Logger LOGGER = LogUtils.getLogger();
    /** 已打过日志的方块实体名，避免刷屏 */
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private AeSoftDepend() {}

    /** AE2 是否已安装（唯一允许的判定入口，勿在别处直接硬引用 appeng 类型做判断）。 */
    public static boolean isLoaded()
    {
        return ModList.get().isLoaded(AE2_MODID);
    }

    /**
     * 方块实体供应器包装：装了 AE2 就反射构造 AE 版子类，否则（或构造失败时）退回基础版。
     * <p>
     * 用反射而不是直接写 {@code GodFurnaceAeBlockEntity::new}，是因为类字面量/方法引用会在
     * 注册时立即加载该类，从而连带加载 appeng 接口 —— 未装 AE2 时就是崩溃点。
     */
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityType.BlockEntitySupplier<T> blockEntity(
            BlockEntityType.BlockEntitySupplier<T> fallback, String aeSimpleName)
    {
        return (pos, state) ->
        {
            if (!isLoaded())
            {
                if (LOGGED.add("base:" + aeSimpleName))
                {
                    LOGGER.info("[Godofthings] {} 使用基础版方块实体（未检测到 AE2，AE 功能不可用）", aeSimpleName);
                }
                return fallback.create(pos, state);
            }
            try
            {
                Class<?> cls = Class.forName(AE_PACKAGE + aeSimpleName);
                T created = (T) cls.getConstructor(BlockPos.class, BlockState.class).newInstance(pos, state);
                if (LOGGED.add("ae:" + aeSimpleName))
                {
                    LOGGER.info("[Godofthings] {} 使用 AE 版方块实体（AE2 已安装，AE 并网可用）", aeSimpleName);
                }
                return created;
            }
            catch (ReflectiveOperationException | LinkageError e)
            {
                LOGGER.error("[Godofthings] AE 版方块实体 {} 加载失败，回退基础版（AE 功能本次不可用）", aeSimpleName, e);
                return fallback.create(pos, state);
            }
        };
    }

    /**
     * 反射调用 AE 专属类里的静态方法。
     * <p>
     * 调用方必须先用 {@link #isLoaded()} 守卫；签名用 {@code Class[]} 显式给出，
     * 避免在调用方方法体里出现 appeng 类型常量池项（虽然方法体引用只在执行时解析，
     * 但显式签名能让隔离边界一目了然）。
     */
    public static void invokeStatic(String aeSimpleName, String method, Class<?>[] signature, Object... args)
    {
        try
        {
            Class<?> cls = Class.forName(AE_PACKAGE + aeSimpleName);
            cls.getMethod(method, signature).invoke(null, args);
            if (LOGGED.add("reg:" + aeSimpleName))
            {
                LOGGER.info("[Godofthings] AE 能力注册入口 {} 调用成功", aeSimpleName);
            }
        }
        catch (ReflectiveOperationException | LinkageError e)
        {
            LOGGER.error("[Godofthings] AE 注册入口 {}.{} 调用失败", aeSimpleName, method, e);
        }
    }
}

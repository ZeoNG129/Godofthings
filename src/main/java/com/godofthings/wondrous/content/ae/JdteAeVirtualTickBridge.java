package com.godofthings.wondrous.content.ae;

import java.lang.reflect.Method;

/**
 * jdte-ae 逻辑 AE 时钟的可选桥接（照抄上游 JdteAeVirtualTickBridge）。
 * 只借用目标已选定时的公开单次调用时钟作用域，绝不调用 JDTE 的全网管理器。
 * jdte-ae 不在时全部静默降级为空作用域。
 */
public final class JdteAeVirtualTickBridge
{
    private static final Method ENTER = findEnter();

    private JdteAeVirtualTickBridge()
    {
    }

    public static Scope enter(long virtualTick)
    {
        if (ENTER == null) return Scope.NONE;
        try
        {
            Object scope = ENTER.invoke(null, virtualTick, 1);
            if (scope instanceof AutoCloseable closeable) return new Scope(closeable);
        }
        catch (ReflectiveOperationException | RuntimeException | LinkageError ignored)
        {
            // 可选兼容项：jdte-ae 缺失或变更不能影响 ticking。
        }
        return Scope.NONE;
    }

    private static Method findEnter()
    {
        try
        {
            Class<?> context = Class.forName("com.jdte.ae.AEVirtualTickContext", false,
                    JdteAeVirtualTickBridge.class.getClassLoader());
            return context.getMethod("enter", long.class, int.class);
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            return null;
        }
    }

    public static final class Scope implements AutoCloseable
    {
        private static final Scope NONE = new Scope(null);
        private final AutoCloseable delegate;

        private Scope(AutoCloseable delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void close()
        {
            if (delegate == null) return;
            try
            {
                delegate.close();
            }
            catch (Exception ignored)
            {
                // 可选作用域失败不能打断目标 tick。
            }
        }
    }
}

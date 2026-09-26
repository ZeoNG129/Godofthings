package com.godofthings.ae2;

import com.godofthings.Godofthings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * AE2 能力注册的「软依赖调度器」。
 * <p>
 * <b>本类不得出现任何 {@code appeng.*} 类型</b>：它挂在 MOD 总线上、每次启动都会加载，
 * 一旦里面有 appeng 类型就会在未装 AE2 时崩在注册阶段。
 * 真正的注册逻辑放在 {@link AeRegistration}（硬引用 AE2），只在装了 AE2 时反射加载。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class AeCapabilityDispatch
{
    private AeCapabilityDispatch() {}

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
    {
        if (!AeSoftDepend.isLoaded())
        {
            return; // 未装 AE2：什么都不注册，7 台机器照常工作（只是没有 AE 并网功能）
        }
        AeSoftDepend.invokeStatic("AeRegistration", "register",
                new Class<?>[] { RegisterCapabilitiesEvent.class }, event);
    }
}

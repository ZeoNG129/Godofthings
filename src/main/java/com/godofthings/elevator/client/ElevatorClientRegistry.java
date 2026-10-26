package com.godofthings.elevator.client;

import com.godofthings.Godofthings;
import com.godofthings.elevator.ElevatorBlock;
import com.godofthings.elevator.ElevatorRegistry;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 电梯的客户端注册：设置界面 + 伪装染色 + 箭头独立模型 + baked model 包装。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.neoforge.client.ClientRegistry}（MIT），四个事件一一对应。</p>
 *
 * <p><b>[本地改动]</b>：上游的菜单界面构造器要额外传一个 {@code PacketDistributor::sendToServer}
 * 作为发包函数（多加载器抽象）；本项目直接用自身网络类，{@code ElevatorScreen} 走无参构造。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ElevatorClientRegistry
{
    private ElevatorClientRegistry() {}

    /** 把电梯菜单接上设置界面（漏了这一步，右键会「打不开界面」）。 */
    @SubscribeEvent
    public static void onMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(ElevatorRegistry.ELEVATOR_CONTAINER.get(), ElevatorScreen::new);
    }

    /**
     * 给 16 色电梯方块挂上染色转发器。
     *
     * <p>漏了这一步，伪装成草方块 / 树叶之类需要染色的方块会显示成灰色。</p>
     */
    @SubscribeEvent
    public static void onBlockColorHandlers(RegisterColorHandlersEvent.Block event)
    {
        event.register(new ColorCamoElevator(),
                ElevatorRegistry.ELEVATOR_BLOCKS.values().stream()
                        .map(DeferredHolder::get)
                        .toArray(ElevatorBlock[]::new));
    }

    /**
     * 把箭头模型注册成「独立模型」。
     *
     * <p>漏了这一步，{@code getModel(standalone("godofthings:arrow"))} 只会拿到缺失模型，
     * 箭头永远画不出来。</p>
     */
    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event)
    {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "arrow")));
    }

    /**
     * 烘焙完成后把 16 色电梯的 baked model 包一层 {@link ElevatorBakedModel}，
     * 由它在静态区块几何里画出伪装方块。
     *
     * <p>[本地改动] 过滤条件与上游一致用 {@code contains("elevator_")}（上游 fabric 侧才是 startsWith），
     * 只是把命名空间换成 {@code godofthings}。</p>
     */
    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event)
    {
        event.getModels().entrySet().stream()
                .filter(entry -> Godofthings.MODID.equals(entry.getKey().id().getNamespace())
                        && entry.getKey().id().getPath().contains("elevator_"))
                .forEach(entry -> event.getModels().put(entry.getKey(), new ElevatorBakedModel(entry.getValue())));
    }
}

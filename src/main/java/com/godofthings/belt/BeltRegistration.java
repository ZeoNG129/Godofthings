package com.godofthings.belt;

import com.godofthings.Godofthings;
import com.godofthings.belt.belt.ToolBeltItem;
import com.godofthings.belt.network.BeltContentsChange;
import com.godofthings.belt.network.ContainerSlotsHack;
import com.godofthings.belt.network.OpenBeltSlotInventory;
import com.godofthings.belt.network.SwapItems;
import com.godofthings.belt.network.SyncBeltSlotContents;
import com.godofthings.belt.slot.BeltAttachment;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 神之皮带的注册接线（移植自 ToolBelt 2.2.10 的 {@code dev.gigaherz.toolbelt.ToolBelt}，BSD-3-Clause）。
 *
 * <p>[本地改动] 上游这里是一个独立的 {@code @Mod} 入口；移植后物品 / 菜单 / 附件类型的
 * {@code DeferredRegister} 全部并入 {@link Godofthings}（同一个 {@code godofthings} 命名空间），
 * 本类只负责「非注册表」的那部分接线：网络包、物品能力、玩家皮带槽事件、配置刷新。</p>
 */
public final class BeltRegistration
{
    public static final String MODID = Godofthings.MODID;

    private BeltRegistration() {}

    public static ResourceLocation location(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    /** 在 {@link Godofthings} 的构造里调用（MOD 总线监听器 + 玩家槽事件）。 */
    public static void init(IEventBus modEventBus)
    {
        modEventBus.addListener(BeltRegistration::registerPackets);
        // 皮带物品的两个物品能力（物品栏内容 / 皮带槽物品标记）
        modEventBus.addListener(ToolBeltItem::register);
        // 配置加载与重载（服务器白黑名单 / 客户端轮盘显示选项）
        modEventBus.addListener(ConfigData::onConfigLoad);
        modEventBus.addListener(ConfigData::onConfigReload);
        modEventBus.addListener(BeltRegistration::commonSetup);
        // 玩家皮带槽的存档 / 掉落 / 死亡复制 / 每 tick 处理
        BeltAttachment.registerEvents();
    }

    private static void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            // 查找皮带时的优先级：后注册的先查 —— 皮带槽优先，物品栏保底
            BeltFinderCurios.initCurios();
            BeltFinderInventory.initInventory();
            BeltFinderBeltSlot.initBaubles();
        });
    }

    private static void registerPackets(RegisterPayloadHandlersEvent event)
    {
        final PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0");
        registrar.playToServer(SwapItems.TYPE, SwapItems.STREAM_CODEC, SwapItems::handle);
        registrar.playToServer(OpenBeltSlotInventory.TYPE, OpenBeltSlotInventory.STREAM_CODEC, OpenBeltSlotInventory::handle);
        registrar.playToServer(ContainerSlotsHack.TYPE, ContainerSlotsHack.STREAM_CODEC, ContainerSlotsHack::handle);
        registrar.playToClient(BeltContentsChange.TYPE, BeltContentsChange.STREAM_CODEC, BeltContentsChange::handle);
        registrar.playToClient(SyncBeltSlotContents.TYPE, SyncBeltSlotContents.STREAM_CODEC, SyncBeltSlotContents::handle);
    }
}

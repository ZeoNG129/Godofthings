package com.godofthings.toolbelt;

import com.godofthings.Godofthings;
import com.godofthings.toolbelt.belt.ToolBeltItem;
import com.godofthings.toolbelt.common.BeltContainer;
import com.godofthings.toolbelt.common.BeltSlotContainer;
import com.godofthings.toolbelt.customslots.ExtensionSlotItemCapability;
import com.godofthings.toolbelt.network.BeltContentsChange;
import com.godofthings.toolbelt.network.ContainerSlotsHack;
import com.godofthings.toolbelt.network.OpenBeltSlotInventory;
import com.godofthings.toolbelt.network.SwapItems;
import com.godofthings.toolbelt.network.SyncBeltSlotContents;
import com.godofthings.toolbelt.slot.BeltExtensionSlot;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * 工具皮带（Tool Belt）子模块：已收编进 God of Things，不再独立作为 mod 加载。
 * 原项目：Tool Belt（gigaherz，BSD 3-Clause）。提供可穿戴的 9 格工具皮带，
 * 配合径向菜单（R 键）快速交换工具，以及自定义皮带栏（V 键打开）。
 *
 * 用户改动：删除皮带包（无需升级），工具皮带默认即 9 格。
 */
public final class ToolBelt
{
    public static final String MODID = Godofthings.MODID;
    public static final Logger logger = LogUtils.getLogger();

    /** 皮带栏槽位类型 id（自定义皮带栏与皮带物品共用）。 */
    public static final ResourceLocation BELT_SLOT_TYPE = ResourceLocation.tryBuild(MODID, "belt");

    /** 工具皮带物品。 */
    public static RegistryObject<ToolBeltItem> BELT;

    /** 皮带物品栏（右键皮带打开）。 */
    public static RegistryObject<MenuType<BeltContainer>> BELT_MENU;

    /** 自定义皮带栏物品栏（V 键打开，含合成格与装备栏）。 */
    public static RegistryObject<MenuType<BeltSlotContainer>> BELT_SLOT_MENU;

    private static final String PROTOCOL_VERSION = "1.0";
    public static SimpleChannel channel = NetworkRegistry.ChannelBuilder
            .named(location("general"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    private ToolBelt() {}

    public static ResourceLocation location(String path)
    {
        return ResourceLocation.tryBuild(MODID, path);
    }

    /** 由 Godofthings 主类构造时调用：注册物品、菜单、能力与创造标签。 */
    public static void register(IEventBus modEventBus)
    {
        BELT = Godofthings.ITEMS.register("belt", () -> new ToolBeltItem(new Item.Properties().stacksTo(1)));
        BELT_MENU = Godofthings.MENUS.register("belt_container", () -> IForgeMenuType.create(BeltContainer::new));
        BELT_SLOT_MENU = Godofthings.MENUS.register("belt_slot_container", () -> new MenuType<>(BeltSlotContainer::new, FeatureFlags.DEFAULT_FLAGS));

        modEventBus.addListener(ToolBelt::registerCapabilities);
        modEventBus.addListener(ToolBelt::addItemsToTabs);
    }

    private static void addItemsToTabs(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
        {
            event.accept(BELT);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        ExtensionSlotItemCapability.register(event);
        event.register(BeltExtensionSlot.class);
    }

    /** 由 Godofthings 主类 commonSetup 调用：注册网络通道与皮带栏能力。 */
    public static void commonSetup()
    {
        int messageNumber = 0;
        channel.messageBuilder(SwapItems.class, messageNumber++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SwapItems::encode).decoder(SwapItems::decode).consumerNetworkThread(SwapItems::handle).add();
        channel.messageBuilder(BeltContentsChange.class, messageNumber++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BeltContentsChange::encode).decoder(BeltContentsChange::decode).consumerNetworkThread(BeltContentsChange::handle).add();
        channel.messageBuilder(OpenBeltSlotInventory.class, messageNumber++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenBeltSlotInventory::encode).decoder(OpenBeltSlotInventory::decode).consumerNetworkThread(OpenBeltSlotInventory::handle).add();
        channel.messageBuilder(ContainerSlotsHack.class, messageNumber++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ContainerSlotsHack::encode).decoder(ContainerSlotsHack::decode).consumerNetworkThread(ContainerSlotsHack::handle).add();
        channel.messageBuilder(SyncBeltSlotContents.class, messageNumber++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBeltSlotContents::encode).decoder(SyncBeltSlotContents::decode).consumerNetworkThread(SyncBeltSlotContents::handle).add();

        BeltExtensionSlot.register();
        BeltFinderBeltSlot.init();
    }
}

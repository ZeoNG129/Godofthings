package com.godofthings.wand;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.godofthings.wand.basics.ConfigClient;
import com.godofthings.wand.basics.ConfigServer;
import com.godofthings.wand.basics.ModStats;
import com.godofthings.wand.containers.ContainerManager;
import com.godofthings.wand.containers.ContainerRegistrar;
import com.godofthings.wand.items.ModItems;
import com.godofthings.wand.network.PacketPreviewResult;
import com.godofthings.wand.network.PacketQueryUndo;
import com.godofthings.wand.network.PacketRequestPreview;
import com.godofthings.wand.network.PacketUndoBlocks;
import com.godofthings.wand.network.PacketWandOption;
import com.godofthings.wand.wand.undo.UndoHistory;

/**
 * 无尽手杖（Construction Wand）子模块：已收编进 God of Things，不再独立作为 mod 加载。
 * 原项目：ConstructionWand-KOTS（作者 ThetaDev / Polaris_Light / adoleiiiiii，MIT）。
 * 这里保留单例与网络通道，由 Godofthings 主类在构造时初始化。
 */
public class ConstructionWand
{
    public static final String MODID = "godofthings";
    public static final String MODNAME = "ConstructionWand";

    public static ConstructionWand instance;
    public static final Logger LOGGER = LogManager.getLogger();
    private static final String PROTOCOL_VERSION = "1";
    public SimpleChannel HANDLER;

    public ContainerManager containerManager;
    public UndoHistory undoHistory;

    public ConstructionWand() {
        instance = this;
        containerManager = new ContainerManager();
        undoHistory = new UndoHistory();
    }

    /** 由 Godofthings 主类调用，注册物品/配置/网络通道。 */
    public void register(FMLJavaModLoadingContext context) {
        ModItems.ITEMS.register(context.getModEventBus());
        context.getModEventBus().addListener(this::commonSetup);
        context.registerConfig(ModConfig.Type.SERVER, ConfigServer.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ConfigClient.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("ConstructionWand ported into godofthings");

        // Register packets
        HANDLER = NetworkRegistry.newSimpleChannel(ResourceLocation.tryBuild(MODID, "constructionwand"),
                () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
        int packetIndex = 0;
        HANDLER.registerMessage(packetIndex++, PacketUndoBlocks.class, PacketUndoBlocks::encode, PacketUndoBlocks::decode, PacketUndoBlocks.Handler::handle);
        HANDLER.registerMessage(packetIndex++, PacketQueryUndo.class, PacketQueryUndo::encode, PacketQueryUndo::decode, PacketQueryUndo.Handler::handle);
        HANDLER.registerMessage(packetIndex++, PacketWandOption.class, PacketWandOption::encode, PacketWandOption::decode, PacketWandOption.Handler::handle);
        HANDLER.registerMessage(packetIndex++, PacketRequestPreview.class, PacketRequestPreview::encode, PacketRequestPreview::decode, PacketRequestPreview.Handler::handle);
        HANDLER.registerMessage(packetIndex, PacketPreviewResult.class, PacketPreviewResult::encode, PacketPreviewResult::decode, PacketPreviewResult.Handler::handle);

        // Container registry (跨模组联动)
        ContainerRegistrar.register();

        // Stats
        ModStats.register();
    }

    public static ResourceLocation loc(String name) {
        return ResourceLocation.tryBuild(MODID, name);
    }
}

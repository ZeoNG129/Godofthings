package com.godofthings.advancedmemorycard.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod 配置注册中心
 * <p>
 * 客户端配置仅在物理客户端加载；服务端配置在服务端（物理服务端+单机逻辑服务端）加载。
 * </p>
 */
public class ModConfigs {
    private static ClientConfig clientConfig;
    private static ServerConfig serverConfig;

    private static ModConfigSpec clientSpec;
    private static ModConfigSpec serverSpec;

    /**
     * 构建所有配置规格，并向 NeoForge 注册。
     * 必须在 Mod 构造器中调用。
     * 使用 ModContainer 确保配置正确关联到当前模组的容器。
     */
    public static void register(ModContainer container) {
        // 构建客户端配置
        var clientPair = new ModConfigSpec.Builder()
                .configure(ClientConfig::new);
        clientConfig = clientPair.getKey();
        clientSpec = clientPair.getValue();

        // 构建服务端配置
        var serverPair = new ModConfigSpec.Builder()
                .configure(ServerConfig::new);
        serverConfig = serverPair.getKey();
        serverSpec = serverPair.getValue();

        // 向 NeoForge 注册客户端配置（仅在物理客户端注册）。
        // 默认文件名会是 godofthings-client.toml，与主模组已注册的同名配置冲突
        // （ConfigTracker 检测到重复直接抛异常让加载失败），必须用独立文件名
        if (FMLEnvironment.dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, clientSpec,
                    "godofthings-amc-client.toml");
        }

        // 向 NeoForge 注册服务端配置（同理用独立文件名）
        container.registerConfig(ModConfig.Type.SERVER, serverSpec,
                "godofthings-amc-server.toml");
    }

    public static ClientConfig getClientConfig() {
        return clientConfig;
    }

    public static ServerConfig getServerConfig() {
        return serverConfig;
    }
}

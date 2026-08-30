package com.godofthings.torcherino.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.godofthings.torcherino.Torcherino;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * 加速火把配置：JSON 配置文件，包含等级、黑名单与在线模式。
 * 移植自 Torcherino（MIT License）。
 */
public final class TorcherinoConfig
{
    public static TorcherinoConfig INSTANCE;

    // 随机刻加速倍率（1 到 4096）
    public final int random_tick_rate = 4;

    // 记录火把放置（服务器日志）
    public final boolean log_placement = FMLLoader.getDist() == Dist.DEDICATED_SERVER;

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private final ResourceLocation[] blacklisted_blocks = new ResourceLocation[]{};

    @SuppressWarnings({"MismatchedReadAndWriteOfArray", "SpellCheckingInspection"})
    private final ResourceLocation[] blacklisted_blockentities = new ResourceLocation[]{};

    private final Tier[] tiers = new Tier[]{new Tier("normal", 4, 4, 1), new Tier("compressed", 36, 4, 1),
            new Tier("double_compressed", 324, 4, 1)};

    public String online_mode = "";

    private TorcherinoConfig() {}

    public static void initialize()
    {
        Gson gson = new GsonBuilder().disableInnerClassSerialization()
                .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
                .setPrettyPrinting()
                .create();
        java.nio.file.Path configPath = FMLPaths.CONFIGDIR.get().resolve("godofthings/torcherino.json");
        Logger logger = LogManager.getLogger("godofthings-torcherino-config");
        var marker = new MarkerManager.Log4jMarker("torcherino");
        TorcherinoConfig config = null;
        try
        {
            Files.createDirectories(configPath.getParent());
        }
        catch (IOException e)
        {
            logger.warn(marker, "Failed to create directory required for torcherino config, using default config.");
            config = new TorcherinoConfig();
        }
        if (config == null)
        {
            if (Files.exists(configPath))
            {
                try (var reader = Files.newBufferedReader(configPath))
                {
                    config = gson.fromJson(reader, TorcherinoConfig.class);
                }
                catch (IOException e)
                {
                    logger.warn(marker, "Failed to read torcherino config file, using default config.");
                    config = new TorcherinoConfig();
                }
            }
            else
            {
                config = new TorcherinoConfig();
                try (var writer = Files.newBufferedWriter(configPath, StandardOpenOption.CREATE_NEW))
                {
                    gson.toJson(config, writer);
                }
                catch (IOException e)
                {
                    logger.warn(marker, "Failed to save default torcherino config file.");
                }
            }
        }
        INSTANCE = config;
        INSTANCE.onConfigLoaded();
    }

    private void onConfigLoaded()
    {
        online_mode = online_mode == null ? "" : online_mode.toUpperCase();
        if (!(online_mode.equals("ONLINE") || online_mode.equals("RESTART")))
        {
            online_mode = "";
        }
        for (Tier tier : tiers)
        {
            Torcherino.registerTier(ResourceLocation.tryBuild(Torcherino.MOD_ID, tier.name), tier.max_speed, tier.xz_range, tier.y_range);
        }
        for (ResourceLocation id : blacklisted_blocks)
        {
            Torcherino.blacklistBlock(id);
        }
        for (ResourceLocation id : blacklisted_blockentities)
        {
            Torcherino.blacklistBlockEntity(id);
        }
    }

    static class Tier
    {
        final String name;
        final int max_speed;
        final int xz_range;
        final int y_range;

        Tier(String name, int max_speed, int xz_range, int y_range)
        {
            this.name = name;
            this.max_speed = max_speed;
            this.xz_range = xz_range;
            this.y_range = y_range;
        }
    }
}

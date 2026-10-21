package com.godofthings.compass.explorers.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;

import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * 结构 → 自定义模型数据（CustomModelData）的映射表（移植自 Explorer's Compass 1.20.x）。
 *
 * <p>配置项 {@code customResource} 打开时，搜到结构后会给物品打一个
 * {@code minecraft:custom_model_data} 组件，这样就能用资源包给每个结构配一张专属贴图。
 * 映射关系存在 {@code config/godofthings/config.json}（一个 {@code [{"key":"minecraft:village_plains","value":1}]}
 * 的数组，键是结构 id、值是模型数据号）。</p>
 *
 * <p>上游写的是 {@code config/explorerscompass/config.json}；这里换成 {@code config/godofthings/config.json}，
 * 免得在 config 根目录下多出一个别人家的文件夹。</p>
 */
public final class BetterUI
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static List<Entry<String, Integer>> entries = new ArrayList<>();

    private BetterUI()
    {
    }

    /** 读（不存在就先建一份默认的）配置目录下的 config.json */
    public static void loadConfig(String modId)
    {
        Gson gson = new Gson();
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(modId);
        Path configPath = configDir.resolve("config.json");

        try
        {
            if (!Files.exists(configDir))
            {
                Files.createDirectories(configDir);
            }
            if (!Files.exists(configPath))
            {
                LOGGER.info("Config file does not exist, creating a default one.");
                createDefaultConfig(configPath);
            }

            String content = Files.readString(configPath);
            List<Map<String, Object>> jsonEntries = gson.fromJson(content,
                    new TypeToken<List<Map<String, Object>>>() {}.getType());
            if (jsonEntries == null)
            {
                entries = new ArrayList<>();
                return;
            }
            entries = jsonEntries.stream()
                    .filter(entry -> entry.get("key") instanceof String && entry.get("value") instanceof Number)
                    .map(entry -> Map.entry((String) entry.get("key"), ((Number) entry.get("value")).intValue()))
                    .collect(Collectors.toList());
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to load explorer's compass custom model data config", e);
            entries = new ArrayList<>();
        }
    }

    private static void createDefaultConfig(Path configPath) throws IOException
    {
        // 示例内容：平原村庄 → 模型数据 1
        String defaultContent = "[{\"key\":\"minecraft:village_plains\",\"value\":1}]";
        Files.writeString(configPath, defaultContent);
    }

    /** 结构 id → 模型数据号的映射（没调用过 {@link #loadConfig} 时是空表） */
    public static List<Entry<String, Integer>> getEntries()
    {
        return entries == null ? new ArrayList<>() : entries;
    }
}

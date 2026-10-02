package com.godofthings.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 玩家数据的导出 / 导入落盘：纯文本 SNBT，放在 {@code <存档>/godofthings/exports/} 下。
 *
 * <p>用 SNBT（{@code CompoundTag.toString()} / {@link net.minecraft.nbt.TagParser#parseTag}）
 * 而不是自己写 JSON：存档结构本来就有 save/load，直接复用不会两边跑偏，而且文件人能读、能手改。</p>
 */
public final class SaveFileIO
{
    private static final String FOLDER = "godofthings/exports";

    private SaveFileIO() {}

    /** 导出目录（不存在就建好），异常时返回 null */
    public static Path dir(MinecraftServer server)
    {
        try
        {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve(FOLDER);
            Files.createDirectories(dir);
            return dir;
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /** 文件名清洗：只留字母数字与 _ . -，防止目录穿越；结果为空就用兜底名 */
    public static String sanitize(String raw, String fallback)
    {
        if (raw == null)
        {
            return fallback;
        }
        String s = raw.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        if (s.isEmpty() || s.equals(".") || s.equals(".."))
        {
            return fallback;
        }
        return s.length() > 48 ? s.substring(0, 48) : s;
    }

    /** 写文件（{@code <kind>-<name>.snbt}），返回绝对路径；失败抛 IOException */
    public static Path write(MinecraftServer server, String kind, String name, String content) throws IOException
    {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve(FOLDER);
        Files.createDirectories(dir);
        Path file = dir.resolve(kind + "-" + name + ".snbt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    /** 读文件；不存在返回 null */
    public static String read(MinecraftServer server, String kind, String name) throws IOException
    {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve(FOLDER).resolve(kind + "-" + name + ".snbt");
        return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
    }

    /** 备份用的时间戳（yyyyMMdd-HHmmss），保证同一秒内多次覆盖也不会互相盖掉 */
    public static String timestamp()
    {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
}

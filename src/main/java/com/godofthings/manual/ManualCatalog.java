package com.godofthings.manual;

import com.godofthings.Godofthings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 手册目录：**直接从注册表生成物品 / 方块条目**，再加固定的一批系统条目。
 *
 * <p>这样设计的直接好处：以后再加物品 / 方块，只要补一条语言键
 * {@code manual.godofthings.<注册名>} 就自动出现在手册里；漏了的话
 * {@code check-lang.ps1} 会报错（它专门校验「每个注册物品 / 方块都有手册条目」）
 * —— 不会再出现「这物品干嘛的没人知道」。</p>
 *
 * <p>标题不用额外写语言键：物品 / 方块条目直接用该物品自己的显示名（跟着语言走）。</p>
 */
public final class ManualCatalog
{
    /** 系统条目（不是某个具体物品，讲的是整套机制） */
    public static final List<String> SYSTEM_IDS = List.of(
            "getting_started", "keys_and_commands", "armor_and_skills",
            "note", "waypoints", "dimensions", "ae2", "compat", "faq");

    private static List<ManualEntry> cache;

    private ManualCatalog() {}

    /** 全部条目（首次调用时构建，之后缓存） */
    public static List<ManualEntry> all()
    {
        if (cache == null)
        {
            cache = build();
        }
        return cache;
    }

    /** 按分类 + 关键词筛选（关键词空串 = 不筛） */
    public static List<ManualEntry> filtered(ManualCategory category, String query)
    {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<ManualEntry> out = new ArrayList<>();
        for (ManualEntry entry : all())
        {
            if (entry.category() == category && entry.matches(q))
            {
                out.add(entry);
            }
        }
        return out;
    }

    private static List<ManualEntry> build()
    {
        List<ManualEntry> out = new ArrayList<>();

        // ① 系统条目
        for (String id : SYSTEM_IDS)
        {
            String key = "system." + id;
            out.add(make(key, ManualCategory.SYSTEM, systemIcon(id)));
        }

        // ② 注册表里的每个物品 / 方块（方块物品归到「方块」页，其余归「物品」页）
        //    用 entrySet 而不是 forEach：1.21.1 的 Registry 迭代形态在各版本间变过，entrySet 最稳。
        for (Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> e : BuiltInRegistries.ITEM.entrySet())
        {
            ResourceLocation id = e.getKey().location();
            if (!Godofthings.MODID.equals(id.getNamespace()))
            {
                continue;
            }
            Item item = e.getValue();
            ManualCategory category = item instanceof BlockItem ? ManualCategory.BLOCK : ManualCategory.ITEM;
            out.add(make(id.getPath(), category, item));
        }
        return out;
    }

    /**
     * 组装一条：标题取物品显示名（系统条目取 {@code manual.godofthings.system.<id>.title}），
     * 正文取 {@code manual.godofthings.<key>}。
     */
    private static ManualEntry make(String key, ManualCategory category, ItemLike icon)
    {
        String id = key;
        ItemStack stack = new ItemStack(icon);
        Component title = category == ManualCategory.SYSTEM
                ? Component.translatable("manual.godofthings." + key + ".title")
                : stack.getHoverName();
        Component body = Component.translatable("manual.godofthings." + key);
        String keywords = (title.getString() + " " + body.getString() + " " + id).toLowerCase(Locale.ROOT);
        return new ManualEntry(id, category, stack, title, body, keywords);
    }

    /** 系统条目列表左侧的小图标：挑一个能代表该主题的物品 */
    private static ItemLike systemIcon(String id)
    {
        return switch (id)
        {
            case "getting_started" -> Items.CRAFTING_TABLE;
            case "keys_and_commands" -> Items.COMMAND_BLOCK;
            case "staff" -> Items.STICK;
            case "armor_and_skills" -> Items.NETHERITE_CHESTPLATE;
            case "note" -> Items.PAPER;
            case "waypoints" -> Items.COMPASS;
            case "dimensions" -> Items.END_PORTAL_FRAME;
            case "ae2" -> Items.ENDER_CHEST;
            case "compat" -> Items.REDSTONE;
            default -> Items.KNOWLEDGE_BOOK;
        };
    }
}

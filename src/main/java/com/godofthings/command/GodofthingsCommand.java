package com.godofthings.command;

import com.godofthings.Godofthings;
import com.godofthings.note.GodNoteData;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteDate;
import com.godofthings.util.SaveFileIO;
import com.godofthings.waypoint.Waypoint;
import com.godofthings.waypoint.WaypointData;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code /godofthings} —— 维护类指令总入口。
 *
 * <ul>
 *   <li>{@code /godofthings doctor} —— 一条指令把「这局里到底有什么在生效」打出来：
 *       装了哪些可选 mod、魔法增幅属性解析成功几项、有没有上游 mod 覆盖本模组的 Mixin、
 *       以及本玩家便签 / 全服传送点的数据统计</li>
 *   <li>{@code /godofthings export note [名称]} / {@code export points [名称]} ——
 *       把便签 / 传送点导成 SNBT 文本，落在 {@code <存档>/godofthings/exports/} 下（换存档、分享、备份用）</li>
 *   <li>{@code /godofthings import note <名称>} / {@code import points <名称>} —— 从上述文件恢复</li>
 * </ul>
 * 都是本机数据操作，不限制权限等级；控制台也能跑（玩家相关那几行会跳过）。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class GodofthingsCommand
{
    /** 有兼容代码的可选 mod（装了才生效的那些） */
    private static final String[] OPTIONAL_MODS = {
            "ae2", "ae2wtlib", "extendedae", "extendedae_plus", "appliedflux", "appmek",
            "jei", "emi", "jade", "modonomicon", "ad_astra",
            "occultism", "malum", "mysticalagriculture", "goety", "irons_spellbooks", "ars_nouveau",
            "enderio", "mekanism", "modern_industrialization", "productivebees", "constructionwand",
            "gtceu", "lootr", "omnitools", "ftbteams"
    };

    /** 会覆盖本模组 Mixin 的上游 mod：{modId, 被覆盖的 Mixin 类名} */
    private static final String[][] MIXIN_OVERRIDERS = {
            { "useless_mod", "EntityGetterMixin" }
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("godofthings")
                .then(Commands.literal("doctor").executes(GodofthingsCommand::doctor))
                .then(Commands.literal("export")
                        .then(Commands.literal("note")
                                .executes(ctx -> exportNote(ctx.getSource(), null))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> exportNote(ctx.getSource(), arg(ctx, "name")))))
                        .then(Commands.literal("points")
                                .executes(ctx -> exportPoints(ctx.getSource(), null))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> exportPoints(ctx.getSource(), arg(ctx, "name"))))))
                .then(Commands.literal("import")
                        .then(Commands.literal("note")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> importNote(ctx.getSource(), arg(ctx, "name")))))
                        .then(Commands.literal("points")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> importPoints(ctx.getSource(), arg(ctx, "name"))))))
                .then(Commands.literal("manual")
                        .executes(GodofthingsCommand::openManual)));
    }

    private static String arg(CommandContext<CommandSourceStack> ctx, String key)
    {
        return StringArgumentType.getString(ctx, key);
    }

    // ------------------------------------------------------------------ doctor

    /** 打开神之手册：内容全在客户端，这里只要踢客户端一下（与 P 键 / 手册物品等价） */
    private static int openManual(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        com.godofthings.network.ManualMessages.sendOpen(player);
        return 1;
    }

    private static int doctor(CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.header")
                .withStyle(ChatFormatting.GOLD), false);

        String modVersion = ModList.get().getModContainerById(Godofthings.MODID)
                .map(container -> container.getModInfo().getVersion().toString()).orElse("?");
        String neoVersion = ModList.get().getModContainerById("neoforge")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("?");
        String mcVersion = SharedConstants.getCurrentVersion().getName();
        source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.version",
                modVersion, mcVersion, neoVersion), false);

        // ① 可选 mod
        List<String> present = new ArrayList<>();
        List<String> absent = new ArrayList<>();
        for (String id : OPTIONAL_MODS)
        {
            (ModList.get().isLoaded(id) ? present : absent).add(id);
        }
        source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.mods_on",
                present.size(), OPTIONAL_MODS.length,
                present.isEmpty() ? "-" : String.join(" ", present)), false);
        if (!absent.isEmpty())
        {
            source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.mods_off",
                    String.join(" ", absent)).withStyle(ChatFormatting.DARK_GRAY), false);
        }

        // ② Mixin 共存
        boolean anyOverride = false;
        for (String[] pair : MIXIN_OVERRIDERS)
        {
            if (ModList.get().isLoaded(pair[0]))
            {
                anyOverride = true;
                String mod = pair[0];
                String mixin = pair[1];
                source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.coexist",
                        mod, mixin).withStyle(ChatFormatting.YELLOW), false);
            }
        }
        if (!anyOverride)
        {
            source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.coexist_none")
                    .withStyle(ChatFormatting.GREEN), false);
        }

        // ③ 数据统计
        ServerPlayer player = source.getPlayer();
        if (player != null)
        {
            com.godofthings.note.NoteShelf shelf = GodNoteData.get(server).shelf(player.getUUID());
            NoteBook book = shelf.current();
            int tasks = book.tasks().size();
            int done = book.doneCount();
            Component name = book.name().isEmpty()
                    ? Component.translatable("gui.godofthings.note.title")
                    : Component.literal(book.name());
            Component hudState = Component.translatable(shelf.hud().enabled
                    ? "gui.godofthings.note.on"
                    : "gui.godofthings.note.off");
            source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.note",
                    tasks, done, name, hudState, shelf.selected() + 1, shelf.size()), false);
        }
        int waypoints = WaypointData.get(server).names().size();
        source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.waypoints", waypoints), false);

        // ④ 存档数据体积：排查「存档怎么越来越大」
        try
        {
            int noteBytes = GodNoteData.get(server).save(new CompoundTag(), server.registryAccess()).sizeInBytes();
            int pointBytes = WaypointData.get(server).save(new CompoundTag(), server.registryAccess()).sizeInBytes();
            source.sendSuccess(() -> Component.translatable("command.godofthings.doctor.data_size",
                    noteBytes, pointBytes), false);
        }
        catch (Throwable ignored)
        {
            // 体积统计失败不影响诊断本身
        }
        return 1;
    }

    // ------------------------------------------------------------------ 便签导出 / 导入

    private static int exportNote(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        com.godofthings.note.NoteShelf shelf = GodNoteData.get(source.getServer()).shelf(player.getUUID());
        boolean empty = true;
        for (NoteBook book : shelf.books())
        {
            if (!book.isEmpty() || !book.name().isEmpty())
            {
                empty = false;
                break;
            }
        }
        if (empty)
        {
            source.sendFailure(Component.translatable("message.godofthings.export.empty"));
            return 0;
        }
        String file = SaveFileIO.sanitize(name, player.getGameProfile().getName() + "-" + NoteDate.today());
        try
        {
            // 导的是整册（多本 + 悬浮窗设置都在里面）
            CompoundTag tag = new CompoundTag();
            shelf.save(tag);
            Path path = SaveFileIO.write(source.getServer(), "note", file, tag.toString());
            source.sendSuccess(() -> Component.translatable("message.godofthings.export.done",
                    path.toString()).withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        catch (Exception e)
        {
            source.sendFailure(Component.translatable("message.godofthings.export.failed",
                    String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static int importNote(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        String file = SaveFileIO.sanitize(name, "");
        if (file.isEmpty())
        {
            source.sendFailure(Component.translatable("message.godofthings.import.invalid"));
            return 0;
        }
        try
        {
            String text = SaveFileIO.read(source.getServer(), "note", file);
            if (text == null)
            {
                Path dir = SaveFileIO.dir(source.getServer());
                source.sendFailure(Component.translatable("message.godofthings.import.missing",
                        file, dir == null ? "?" : dir.toString()));
                return 0;
            }
            CompoundTag tag;
            try
            {
                tag = TagParser.parseTag(text);
            }
            catch (CommandSyntaxException parseError)
            {
                source.sendFailure(Component.translatable("message.godofthings.import.invalid"));
                return 0;
            }
            com.godofthings.note.NoteShelf imported = new com.godofthings.note.NoteShelf();
            imported.load(tag);
            // 覆盖前先把当前内容备份一份（备份失败不影响导入，只提示）
            String backupName = file + "-backup-" + SaveFileIO.timestamp();
            try
            {
                CompoundTag current = new CompoundTag();
                GodNoteData.get(source.getServer()).shelf(player.getUUID()).save(current);
                SaveFileIO.write(source.getServer(), "note", backupName, current.toString());
                source.sendSuccess(() -> Component.translatable("message.godofthings.import.backup",
                        backupName + ".snbt").withStyle(ChatFormatting.GRAY), false);
            }
            catch (Exception backupError)
            {
                source.sendSuccess(() -> Component.translatable("message.godofthings.import.backup_failed",
                        String.valueOf(backupError.getMessage())).withStyle(ChatFormatting.RED), false);
            }
            GodNoteData.get(source.getServer()).put(player.getUUID(), imported);
            com.godofthings.network.GodNoteMessages.sendSync(player);
            source.sendSuccess(() -> Component.translatable("message.godofthings.import.note_done",
                            file, imported.size(),
                            imported.books().stream().mapToInt(b -> b.tasks().size()).sum())
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        catch (Exception e)
        {
            source.sendFailure(Component.translatable("message.godofthings.export.failed",
                    String.valueOf(e.getMessage())));
            return 0;
        }
    }

    // ------------------------------------------------------------------ 传送点导出 / 导入

    private static int exportPoints(CommandSourceStack source, String name)
    {
        WaypointData data = WaypointData.get(source.getServer());
        List<String> names = data.names();
        if (names.isEmpty())
        {
            source.sendFailure(Component.translatable("message.godofthings.export.empty"));
            return 0;
        }
        String file = SaveFileIO.sanitize(name, "points-" + NoteDate.today());
        try
        {
            ListTag list = new ListTag();
            for (String pointName : names)
            {
                Waypoint wp = data.get(pointName);
                if (wp != null)
                {
                    list.add(wp.save(new CompoundTag()));
                }
            }
            CompoundTag root = new CompoundTag();
            root.put("Waypoints", list);
            Path path = SaveFileIO.write(source.getServer(), "points", file, root.toString());
            source.sendSuccess(() -> Component.translatable("message.godofthings.export.done",
                    path.toString()).withStyle(ChatFormatting.GREEN), false);
            return list.size();
        }
        catch (Exception e)
        {
            source.sendFailure(Component.translatable("message.godofthings.export.failed",
                    String.valueOf(e.getMessage())));
            return 0;
        }
    }

    /** 导入传送点：按名字合并（同名覆盖，新名新增），不删除现有条目 */
    private static int importPoints(CommandSourceStack source, String name)
    {
        String file = SaveFileIO.sanitize(name, "");
        if (file.isEmpty())
        {
            source.sendFailure(Component.translatable("message.godofthings.import.invalid"));
            return 0;
        }
        try
        {
            String text = SaveFileIO.read(source.getServer(), "points", file);
            if (text == null)
            {
                Path dir = SaveFileIO.dir(source.getServer());
                source.sendFailure(Component.translatable("message.godofthings.import.missing",
                        file, dir == null ? "?" : dir.toString()));
                return 0;
            }
            CompoundTag root;
            try
            {
                root = TagParser.parseTag(text);
            }
            catch (CommandSyntaxException parseError)
            {
                source.sendFailure(Component.translatable("message.godofthings.import.invalid"));
                return 0;
            }
            WaypointData data = WaypointData.get(source.getServer());
            ListTag list = root.getList("Waypoints", Tag.TAG_COMPOUND);
            int added = 0;
            int updated = 0;
            for (int i = 0; i < list.size(); i++)
            {
                Waypoint wp = Waypoint.load(list.getCompound(i));
                if (wp.name == null || wp.name.isEmpty())
                {
                    continue;
                }
                if (data.get(wp.name) != null)
                {
                    updated++;
                }
                else
                {
                    added++;
                }
                data.set(wp);
            }
            int totalAdded = added;
            int totalUpdated = updated;
            source.sendSuccess(() -> Component.translatable("message.godofthings.import.points_done",
                    file, totalAdded, totalUpdated).withStyle(ChatFormatting.GREEN), false);
            return added + updated;
        }
        catch (Exception e)
        {
            source.sendFailure(Component.translatable("message.godofthings.export.failed",
                    String.valueOf(e.getMessage())));
            return 0;
        }
    }
}

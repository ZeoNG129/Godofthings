package com.godofthings.command;

import com.godofthings.Godofthings;
import com.godofthings.network.GodNoteMessages;
import com.godofthings.note.GodNoteData;
import com.godofthings.note.NoteAdvancements;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteTask;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 神之便签指令（第三条入口）：
 * <ul>
 *   <li>{@code /godnote} —— 打开记事本界面</li>
 *   <li>{@code /godnote add <文字>} —— 直接加一条任务（不必开界面）</li>
 *   <li>{@code /godnote list} —— 把当前任务打到聊天栏</li>
 *   <li>{@code /godnote clear} —— 清空所有任务</li>
 *   <li>{@code /godnote name <文字>} —— 给悬浮窗标题改名（会顺手关掉自动更名）</li>
 *   <li>{@code /godnote auto on|off} —— 自动更名：开着时名字固定为当日日期（MM.dd）</li>
 *   <li>{@code /godnote hud on|off} —— 开关屏幕上的悬浮窗</li>
 * </ul>
 * 便签是玩家个人资料，所以不限制权限等级：谁都能管自己那一本。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class GodNoteCommands
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("godnote")
                .executes(ctx -> open(ctx.getSource()))
                .then(Commands.literal("add")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> add(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "text")))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("clear")
                        .executes(ctx -> clear(ctx.getSource())))
                .then(Commands.literal("name")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> rename(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "text")))))
                .then(Commands.literal("auto")
                        .then(Commands.literal("on").executes(ctx -> auto(ctx.getSource(), true)))
                        .then(Commands.literal("off").executes(ctx -> auto(ctx.getSource(), false))))
                .then(Commands.literal("hud")
                        .then(Commands.literal("on").executes(ctx -> hud(ctx.getSource(), true)))
                        .then(Commands.literal("off").executes(ctx -> hud(ctx.getSource(), false)))));
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        GodNoteMessages.sendSync(player);
        GodNoteMessages.sendOpenScreen(player);
        return 1;
    }

    private static int add(CommandSourceStack source, String text) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        NoteBook book = GodNoteData.get(source.getServer()).shelf(player.getUUID()).current();
        int index = book.add(text);
        if (index < 0)
        {
            source.sendFailure(Component.translatable("message.godofthings.note.add_failed",
                    NoteBook.MAX_TASKS));
            return 0;
        }
        GodNoteData.get(source.getServer()).setDirty();
        GodNoteMessages.sendSync(player);
        source.sendSuccess(() -> Component.translatable("message.godofthings.note.added",
                book.tasks().get(index).text).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        NoteBook book = GodNoteData.get(source.getServer()).shelf(player.getUUID()).current();
        if (book.isEmpty())
        {
            source.sendSuccess(() -> Component.translatable("message.godofthings.note.list_empty"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("message.godofthings.note.list_header",
                book.doneCount(), book.tasks().size()), false);
        for (NoteTask task : book.tasks())
        {
            String line = (task.done ? "[x] " : "[ ] ") + task.text;
            source.sendSuccess(() -> Component.literal(line)
                    .withStyle(task.done ? ChatFormatting.GRAY : ChatFormatting.WHITE), false);
        }
        return book.tasks().size();
    }

    private static int clear(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        NoteBook book = GodNoteData.get(source.getServer()).shelf(player.getUUID()).current();
        int count = book.tasks().size();
        book.clear();
        GodNoteData.get(source.getServer()).setDirty();
        GodNoteMessages.sendSync(player);
        source.sendSuccess(() -> Component.translatable("message.godofthings.note.cleared", count)
                .withStyle(ChatFormatting.GREEN), false);
        return count;
    }

    private static int hud(CommandSourceStack source, boolean enabled) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        com.godofthings.note.NoteShelf shelf = GodNoteData.get(source.getServer()).shelf(player.getUUID());
        shelf.hud().enabled = enabled;
        GodNoteData.get(source.getServer()).setDirty();
        GodNoteMessages.sendSync(player);
        source.sendSuccess(() -> Component.translatable("gui.godofthings.note.hud",
                Component.translatable(enabled
                        ? "gui.godofthings.note.on"
                        : "gui.godofthings.note.off")).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /** 改名：顺手关掉自动更名 —— 手动起名后要是还开着自动，下一 tick 就被日期覆盖了 */
    private static int rename(CommandSourceStack source, String text) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        NoteBook book = GodNoteData.get(source.getServer()).shelf(player.getUUID()).current();
        book.setAutoName(false);
        book.setName(text);
        GodNoteData.get(source.getServer()).setDirty();
        GodNoteMessages.sendSync(player);
        source.sendSuccess(() -> Component.translatable(book.name().isEmpty()
                        ? "message.godofthings.note.renamed_default"
                        : "message.godofthings.note.renamed",
                book.name()).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /** 自动更名开关；打开时立刻刷成当日日期（服务端日期） */
    private static int auto(CommandSourceStack source, boolean enabled) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        NoteBook book = GodNoteData.get(source.getServer()).shelf(player.getUUID()).current();
        book.setAutoName(enabled);
        if (enabled)
        {
            book.applyAutoName();
            NoteAdvancements.award(player, NoteAdvancements.AUTO); // 「开启自动更名」成就
        }
        GodNoteData.get(source.getServer()).setDirty();
        GodNoteMessages.sendSync(player);
        source.sendSuccess(() -> Component.translatable("message.godofthings.note.auto_set",
                Component.translatable(enabled
                        ? "gui.godofthings.note.on"
                        : "gui.godofthings.note.off"),
                book.name()).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}

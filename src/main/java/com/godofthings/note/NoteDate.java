package com.godofthings.note;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 自动更名用的日期文本。
 *
 * <p>格式固定为 {@code MM.dd}（例：10.02 / 10.03），取的是<b>服务端本机日期</b> ——
 * 单人 / 自己开服时就是玩家自己的日期；远程服务器上则以服务器日期为准。</p>
 */
public final class NoteDate
{
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM.dd");

    private NoteDate() {}

    public static String today()
    {
        return LocalDate.now().format(FORMAT);
    }
}

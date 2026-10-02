package com.godofthings.note;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * 神之便签里的一条任务：一行文字 + 一个勾选状态。
 *
 * <p>文字统一做长度与换行裁剪（见 {@link #clampText}），保证客户端界面、HUD 与
 * 服务端存档三边看到的都是同一份规范化数据。</p>
 */
public final class NoteTask
{
    /** 单条任务文字上限（字符数） */
    public static final int MAX_TEXT = 64;

    public String text;
    public boolean done;

    public NoteTask(String text, boolean done)
    {
        this.text = clampText(text);
        this.done = done;
    }

    /** 去掉换行 / 首尾空白并截断到 {@link #MAX_TEXT}；null 视为空串 */
    public static String clampText(String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String s = raw.replace('\n', ' ').replace('\r', ' ').trim();
        return s.length() > MAX_TEXT ? s.substring(0, MAX_TEXT) : s;
    }

    public NoteTask copy()
    {
        return new NoteTask(text, done);
    }

    public void save(CompoundTag tag)
    {
        tag.putString("Text", text);
        tag.putBoolean("Done", done);
    }

    public static NoteTask load(CompoundTag tag)
    {
        return new NoteTask(tag.getString("Text"), tag.getBoolean("Done"));
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeUtf(text, MAX_TEXT);
        buf.writeBoolean(done);
    }

    public static NoteTask read(RegistryFriendlyByteBuf buf)
    {
        return new NoteTask(buf.readUtf(MAX_TEXT), buf.readBoolean());
    }
}

package com.godofthings.note;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之便签里的一条任务：一行文字 + 一个勾选状态 +（可选）若干子任务。
 *
 * <p>结构上是一棵树，但<b>只开放两级</b>（{@link #MAX_DEPTH}）：主任务 + 子任务，
 * 像论文的大标题 / 小标题。序列化写的是递归结构、读的时候按深度夹取 ——
 * 所以就算有人手改存档塞了三级，也只会被安静地截掉，不会崩也不会显示成乱七八糟的缩进。</p>
 *
 * <p>文字统一做长度与换行裁剪（见 {@link #clampText}），保证客户端界面、HUD 与
 * 服务端存档三边看到的都是同一份规范化数据。</p>
 */
public final class NoteTask
{
    /** 单条任务文字上限（字符数） */
    public static final int MAX_TEXT = 64;
    /** 一条主任务最多挂几个子任务 */
    public static final int MAX_CHILDREN = 16;
    /** 支持的最大层级：1 = 只有主任务，2 = 主任务 + 子任务（本模组开放到 2） */
    public static final int MAX_DEPTH = 2;

    public String text;
    public boolean done;
    private final List<NoteTask> children = new ArrayList<>();

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

    // ------------------------------------------------------------------ 子任务

    public List<NoteTask> children()
    {
        return children;
    }

    public boolean hasChildren()
    {
        return !children.isEmpty();
    }

    public int childCount()
    {
        return children.size();
    }

    public int doneChildren()
    {
        int n = 0;
        for (NoteTask c : children)
        {
            if (c.done)
            {
                n++;
            }
        }
        return n;
    }

    /** 挂一个子任务；空文字或到上限返回 null */
    public NoteTask addChild(String childText)
    {
        String clean = clampText(childText);
        if (clean.isEmpty() || children.size() >= MAX_CHILDREN)
        {
            return null;
        }
        NoteTask child = new NoteTask(clean, false);
        children.add(child);
        return child;
    }

    public void appendChild(NoteTask child)
    {
        if (child != null && children.size() < MAX_CHILDREN)
        {
            children.add(child);
        }
    }

    public void removeChild(int index)
    {
        if (index >= 0 && index < children.size())
        {
            children.remove(index);
        }
    }

    public void toggleChild(int index)
    {
        if (index >= 0 && index < children.size())
        {
            children.get(index).done = !children.get(index).done;
        }
    }

    public void setChildText(int index, String childText)
    {
        if (index < 0 || index >= children.size())
        {
            return;
        }
        String clean = clampText(childText);
        if (clean.isEmpty())
        {
            children.remove(index);
        }
        else
        {
            children.get(index).text = clean;
        }
    }

    /** 本条（含子树）的任务总数：自己 + 所有子任务 */
    public int totalCount()
    {
        return 1 + children.size();
    }

    /** 本条（含子树）里已完成的数量 */
    public int totalDone()
    {
        int n = done ? 1 : 0;
        for (NoteTask c : children)
        {
            if (c.done)
            {
                n++;
            }
        }
        return n;
    }

    /**
     * 规范化：文字裁剪、空的丢掉、子任务数量与层级都夹到上限。
     *
     * @param depth 当前层级（1 = 主任务）；{@code depth >= MAX_DEPTH} 时子任务会被整体丢掉
     */
    public void clamp(int depth)
    {
        text = clampText(text);
        if (depth >= MAX_DEPTH)
        {
            children.clear();
            return;
        }
        children.removeIf(c -> clampText(c.text).isEmpty());
        while (children.size() > MAX_CHILDREN)
        {
            children.remove(children.size() - 1);
        }
        for (NoteTask c : children)
        {
            c.text = clampText(c.text);
            c.clamp(depth + 1); // 深度到顶时会把 c 自己的子任务清掉
        }
    }

    public NoteTask copy()
    {
        NoteTask t = new NoteTask(text, done);
        for (NoteTask c : children)
        {
            t.children.add(c.copy());
        }
        return t;
    }

    // ------------------------------------------------------------------ 序列化

    public void save(CompoundTag tag)
    {
        tag.putString("Text", text);
        tag.putBoolean("Done", done);
        if (!children.isEmpty())
        {
            ListTag list = new ListTag();
            for (NoteTask c : children)
            {
                CompoundTag childTag = new CompoundTag();
                c.save(childTag);
                list.add(childTag);
            }
            tag.put("Children", list);
        }
    }

    /** 读一条（顶层入口，depth 从 1 起算） */
    public static NoteTask load(CompoundTag tag)
    {
        return load(tag, 1);
    }

    private static NoteTask load(CompoundTag tag, int depth)
    {
        NoteTask t = new NoteTask(tag.getString("Text"), tag.getBoolean("Done"));
        if (depth < MAX_DEPTH && tag.contains("Children", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("Children", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && t.children.size() < MAX_CHILDREN; i++)
            {
                NoteTask child = load(list.getCompound(i), depth + 1);
                if (!child.text.isEmpty())
                {
                    t.children.add(child);
                }
            }
        }
        return t;
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeUtf(text, MAX_TEXT);
        buf.writeBoolean(done);
        buf.writeVarInt(children.size());
        for (NoteTask c : children)
        {
            c.write(buf);
        }
    }

    public static NoteTask read(RegistryFriendlyByteBuf buf)
    {
        return read(buf, 1);
    }

    private static NoteTask read(RegistryFriendlyByteBuf buf, int depth)
    {
        NoteTask t = new NoteTask(buf.readUtf(MAX_TEXT), buf.readBoolean());
        int n = buf.readVarInt();
        // 无论深度够不够都要把字节读完（否则后续字段错位），深度到顶时只丢弃、不写入
        for (int i = 0; i < n; i++)
        {
            NoteTask child = read(buf, depth + 1);
            if (depth < MAX_DEPTH && t.children.size() < MAX_CHILDREN && !child.text.isEmpty())
            {
                t.children.add(child);
            }
        }
        return t;
    }
}

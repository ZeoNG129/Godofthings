package com.godofthings.note;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个玩家的整本便签：若干条任务（有序）+ 悬浮窗设置。
 *
 * <p>这是客户端界面 / HUD / 服务端存档三边唯一的数据形态；服务端收到客户端上传的副本后
 * 先 {@link #clamp()} 再落库，客户端收到下发的副本后直接替换本地镜像。</p>
 */
public final class NoteBook
{
    /** 单本便签的任务条数上限 */
    public static final int MAX_TASKS = 64;

    private final List<NoteTask> tasks = new ArrayList<>();
    private final NoteHud hud = new NoteHud();

    public List<NoteTask> tasks()
    {
        return tasks;
    }

    public NoteHud hud()
    {
        return hud;
    }

    public boolean isEmpty()
    {
        return tasks.isEmpty();
    }

    public int doneCount()
    {
        int n = 0;
        for (NoteTask t : tasks)
        {
            if (t.done)
            {
                n++;
            }
        }
        return n;
    }

    /** 未完成条目数（HUD 标题上用） */
    public int undoneCount()
    {
        return tasks.size() - doneCount();
    }

    /** 追加一条任务（文字为空或超出条数上限时不做事）；返回新任务的下标，失败返回 -1 */
    public int add(String text)
    {
        String clean = NoteTask.clampText(text);
        if (clean.isEmpty() || tasks.size() >= MAX_TASKS)
        {
            return -1;
        }
        tasks.add(new NoteTask(clean, false));
        return tasks.size() - 1;
    }

    public void toggle(int index)
    {
        if (index >= 0 && index < tasks.size())
        {
            tasks.get(index).done = !tasks.get(index).done;
        }
    }

    public void setText(int index, String text)
    {
        if (index < 0 || index >= tasks.size())
        {
            return;
        }
        String clean = NoteTask.clampText(text);
        if (clean.isEmpty())
        {
            tasks.remove(index);
        }
        else
        {
            tasks.get(index).text = clean;
        }
    }

    public void remove(int index)
    {
        if (index >= 0 && index < tasks.size())
        {
            tasks.remove(index);
        }
    }

    /** 上移 / 下移（排序用） */
    public void move(int index, int delta)
    {
        int target = index + delta;
        if (index < 0 || index >= tasks.size() || target < 0 || target >= tasks.size())
        {
            return;
        }
        NoteTask t = tasks.remove(index);
        tasks.add(target, t);
    }

    /** 删掉所有已勾选的条目，返回删掉的条数 */
    public int clearDone()
    {
        int before = tasks.size();
        tasks.removeIf(t -> t.done);
        return before - tasks.size();
    }

    public void clear()
    {
        tasks.clear();
    }

    /** 服务端落库前的兜底：条数、文字长度、HUD 字段全部夹到合法范围 */
    public void clamp()
    {
        while (tasks.size() > MAX_TASKS)
        {
            tasks.remove(tasks.size() - 1);
        }
        tasks.removeIf(t -> NoteTask.clampText(t.text).isEmpty());
        for (NoteTask t : tasks)
        {
            t.text = NoteTask.clampText(t.text);
        }
        hud.clamp();
    }

    public NoteBook copy()
    {
        NoteBook b = new NoteBook();
        for (NoteTask t : tasks)
        {
            b.tasks.add(t.copy());
        }
        NoteHud h = hud.copy();
        b.hud.enabled = h.enabled;
        b.hud.x = h.x;
        b.hud.y = h.y;
        b.hud.scale = h.scale;
        b.hud.background = h.background;
        b.hud.color = h.color;
        b.hud.opacity = h.opacity;
        b.hud.showDone = h.showDone;
        return b;
    }

    public void save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        for (NoteTask t : tasks)
        {
            CompoundTag c = new CompoundTag();
            t.save(c);
            list.add(c);
        }
        tag.put("Tasks", list);
        CompoundTag hudTag = new CompoundTag();
        hud.save(hudTag);
        tag.put("Hud", hudTag);
    }

    public void load(CompoundTag tag)
    {
        tasks.clear();
        ListTag list = tag.getList("Tasks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            NoteTask t = NoteTask.load(list.getCompound(i));
            if (!t.text.isEmpty())
            {
                tasks.add(t);
            }
        }
        if (tag.contains("Hud"))
        {
            hud.load(tag.getCompound("Hud"));
        }
        clamp();
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeVarInt(tasks.size());
        for (NoteTask t : tasks)
        {
            t.write(buf);
        }
        hud.write(buf);
    }

    public static NoteBook read(RegistryFriendlyByteBuf buf)
    {
        NoteBook b = new NoteBook();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++)
        {
            NoteTask t = NoteTask.read(buf);
            if (!t.text.isEmpty() && b.tasks.size() < MAX_TASKS)
            {
                b.tasks.add(t);
            }
        }
        NoteHud h = NoteHud.read(buf);
        b.hud.enabled = h.enabled;
        b.hud.x = h.x;
        b.hud.y = h.y;
        b.hud.scale = h.scale;
        b.hud.background = h.background;
        b.hud.color = h.color;
        b.hud.opacity = h.opacity;
        b.hud.showDone = h.showDone;
        b.clamp();
        return b;
    }
}

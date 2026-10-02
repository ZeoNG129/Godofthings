package com.godofthings.note;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 一本便签：若干条任务（有序，每条可带子任务）+ 名字 + 自动更名开关。
 *
 * <p>悬浮窗的显示设置（位置/大小/背景）不在这里 —— 它在 {@link NoteShelf} 上，
 * 因为屏幕上只有一个便签窗口，那些设置属于「怎么显示」而不是「哪一本内容」。</p>
 *
 * <p>层级只到「主任务 + 子任务」两级（见 {@link NoteTask#MAX_DEPTH}）。这里提供的是
 * 结构性操作：加 / 删 / 同级重排 / 升降级 —— 界面那层的「扁平行列表 + 拖拽 + Tab」
 * 最终都落到这几个方法上，逻辑集中在这一处，方便被 GameTest 直接测。</p>
 */
public final class NoteBook
{
    /** 单本便签的<b>主任务</b>条数上限 */
    public static final int MAX_TASKS = 64;
    /** 单本便签的<b>总任务数</b>上限（主任务 + 全部子任务），用来兜住同步包大小 */
    public static final int MAX_TOTAL = 256;
    /** 便签名称（悬浮窗标题）的长度上限 */
    public static final int MAX_NAME = 24;

    private final List<NoteTask> tasks = new ArrayList<>();
    /** 名字；空串 = 用默认的「神之便签」 */
    private String name = "";
    /** 自动更名：开启后名字固定为当日日期（MM.dd） */
    private boolean autoName;

    public List<NoteTask> tasks()
    {
        return tasks;
    }

    public String name()
    {
        return name;
    }

    public void setName(String value)
    {
        name = clampName(value);
    }

    public boolean autoName()
    {
        return autoName;
    }

    public void setAutoName(boolean value)
    {
        autoName = value;
    }

    /** 名称去掉换行 / 首尾空白并截断到 {@link #MAX_NAME}；null 视为空串 */
    public static String clampName(String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String s = raw.replace('\n', ' ').replace('\r', ' ').trim();
        return s.length() > MAX_NAME ? s.substring(0, MAX_NAME) : s;
    }

    /**
     * 自动更名：开着的时候把名字刷成当日日期（MM.dd）。
     *
     * <p>名字本身就是日期串，所以不需要另存一份「上次自动命名的日期」—— 直接比字符串就行，
     * 重复调用无副作用；跨天（含游戏一直开着跨零点）时由服务端定时器再刷一次。</p>
     *
     * @return 名字是否真的变了（调用方据此决定要不要落库 + 下发）
     */
    public boolean applyAutoName()
    {
        if (!autoName)
        {
            return false;
        }
        String today = NoteDate.today();
        if (today.equals(name))
        {
            return false;
        }
        name = today;
        return true;
    }

    public boolean isEmpty()
    {
        return tasks.isEmpty();
    }

    // ------------------------------------------------------------------ 计数

    /** 主任务里已完成的数量 */
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

    /** 未完成主任务数 */
    public int undoneCount()
    {
        return tasks.size() - doneCount();
    }

    /** 全部子任务数量 */
    public int childCount()
    {
        int n = 0;
        for (NoteTask t : tasks)
        {
            n += t.childCount();
        }
        return n;
    }

    /** 已完成的子任务数量 */
    public int doneChildren()
    {
        int n = 0;
        for (NoteTask t : tasks)
        {
            n += t.doneChildren();
        }
        return n;
    }

    /** 主任务 + 子任务的总条数 */
    public int totalCount()
    {
        int n = 0;
        for (NoteTask t : tasks)
        {
            n += t.totalCount();
        }
        return n;
    }

    // ------------------------------------------------------------------ 主任务

    /** 追加一条主任务（文字为空 / 超上限时不做事）；返回下标，失败返回 -1 */
    public int add(String text)
    {
        String clean = NoteTask.clampText(text);
        if (clean.isEmpty() || tasks.size() >= MAX_TASKS || totalCount() >= MAX_TOTAL)
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

    /** 删掉一条主任务（连同它的子任务） */
    public void remove(int index)
    {
        if (index >= 0 && index < tasks.size())
        {
            tasks.remove(index);
        }
    }

    /** 主任务同级重排（列表拖拽用）；越界自动夹回 */
    public void moveTask(int from, int to)
    {
        if (from < 0 || from >= tasks.size())
        {
            return;
        }
        int target = Math.max(0, Math.min(to, tasks.size() - 1));
        if (target == from)
        {
            return;
        }
        NoteTask task = tasks.remove(from);
        tasks.add(target, task);
    }

    // ------------------------------------------------------------------ 子任务

    /** 给第 parentIndex 条主任务挂一个子任务；返回子任务下标，失败（越界/超上限/空文字）返回 -1 */
    public int addChild(int parentIndex, String text)
    {
        if (parentIndex < 0 || parentIndex >= tasks.size() || totalCount() >= MAX_TOTAL)
        {
            return -1;
        }
        NoteTask parent = tasks.get(parentIndex);
        if (parent.addChild(text) == null)
        {
            return -1;
        }
        return parent.childCount() - 1;
    }

    public void toggleChild(int parentIndex, int childIndex)
    {
        if (parentIndex >= 0 && parentIndex < tasks.size())
        {
            tasks.get(parentIndex).toggleChild(childIndex);
        }
    }

    public void setChildText(int parentIndex, int childIndex, String text)
    {
        if (parentIndex >= 0 && parentIndex < tasks.size())
        {
            tasks.get(parentIndex).setChildText(childIndex, text);
        }
    }

    /** 删掉一条子任务 */
    public void removeChild(int parentIndex, int childIndex)
    {
        if (parentIndex >= 0 && parentIndex < tasks.size())
        {
            tasks.get(parentIndex).removeChild(childIndex);
        }
    }

    /** 折叠 / 展开某条主任务的子任务（视图状态，但随任务保存 —— 这样改完别的内容不会丢） */
    public void toggleCollapsed(int parentIndex)
    {
        if (parentIndex >= 0 && parentIndex < tasks.size())
        {
            NoteTask t = tasks.get(parentIndex);
            if (t.hasChildren())
            {
                t.collapsed = !t.collapsed;
            }
        }
    }

    /** 同一个父任务下的子任务重排 */
    public void moveChild(int parentIndex, int from, int to)
    {
        if (parentIndex < 0 || parentIndex >= tasks.size())
        {
            return;
        }
        List<NoteTask> children = tasks.get(parentIndex).children();
        if (from < 0 || from >= children.size())
        {
            return;
        }
        int target = Math.max(0, Math.min(to, children.size() - 1));
        if (target == from)
        {
            return;
        }
        NoteTask child = children.remove(from);
        children.add(target, child);
    }

    /**
     * 升级：把第 parentIndex 条主任务下的第 childIndex 个子任务变成主任务，插在父任务后面。
     * （界面里的 Shift+Tab）
     */
    public boolean promoteChild(int parentIndex, int childIndex)
    {
        if (parentIndex < 0 || parentIndex >= tasks.size() || tasks.size() >= MAX_TASKS)
        {
            return false;
        }
        List<NoteTask> children = tasks.get(parentIndex).children();
        if (childIndex < 0 || childIndex >= children.size())
        {
            return false;
        }
        NoteTask moved = children.remove(childIndex);
        moved.children().clear(); // 升级后本来也不该带子任务（只有两级）
        tasks.add(parentIndex + 1, moved);
        return true;
    }

    /**
     * 降级：把第 index 条主任务挂到它前一条主任务下面，成为最后一个子任务。
     * （界面里的 Tab；第一条主任务没有前一条，返回 false）
     */
    public boolean demoteTop(int index)
    {
        if (index <= 0 || index >= tasks.size())
        {
            return false;
        }
        NoteTask parent = tasks.get(index - 1);
        if (parent.childCount() >= NoteTask.MAX_CHILDREN)
        {
            return false;
        }
        NoteTask moved = tasks.remove(index);
        parent.appendChild(moved);
        return true;
    }

    // ------------------------------------------------------------------ 清理

    /** 删掉所有已完成的主任务（连同子任务）与已完成的子任务，返回删掉的条数 */
    public int clearDone()
    {
        int before = totalCount();
        tasks.removeIf(t -> t.done);
        for (NoteTask t : tasks)
        {
            t.children().removeIf(c -> c.done);
        }
        return before - totalCount();
    }

    public void clear()
    {
        tasks.clear();
    }

    /** 服务端落库前的兜底：条数、文字长度、名称、层级全部夹到合法范围 */
    public void clamp()
    {
        while (tasks.size() > MAX_TASKS)
        {
            tasks.remove(tasks.size() - 1);
        }
        tasks.removeIf(t -> NoteTask.clampText(t.text).isEmpty());
        for (NoteTask t : tasks)
        {
            t.clamp(1);
        }
        // 总条数兜底：超了就从尾部砍主任务（连带它的子任务）
        while (totalCount() > MAX_TOTAL && tasks.size() > 1)
        {
            tasks.remove(tasks.size() - 1);
        }
        name = clampName(name);
    }

    public NoteBook copy()
    {
        NoteBook b = new NoteBook();
        for (NoteTask t : tasks)
        {
            b.tasks.add(t.copy());
        }
        b.name = name;
        b.autoName = autoName;
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
        tag.putString("Name", name);
        tag.putBoolean("AutoName", autoName);
    }

    /** 读取；v5.3.0 及以前存在的 "Hud" 标签由 {@link NoteShelf} 在册一级处理，这里忽略 */
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
        name = clampName(tag.getString("Name"));
        autoName = tag.getBoolean("AutoName");
        clamp();
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeVarInt(tasks.size());
        for (NoteTask t : tasks)
        {
            t.write(buf);
        }
        buf.writeUtf(name, MAX_NAME);
        buf.writeBoolean(autoName);
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
        b.name = clampName(buf.readUtf(MAX_NAME));
        b.autoName = buf.readBoolean();
        b.clamp();
        return b;
    }
}

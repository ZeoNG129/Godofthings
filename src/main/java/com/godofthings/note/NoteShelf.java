package com.godofthings.note;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个玩家的整册便签：若干「本」+ 一份共享的悬浮窗设置 + 当前选中的是哪一本。
 *
 * <p>为什么悬浮窗设置放在册上而不是每本一份：屏幕上只有一个便签窗口，
 * 它的位置/大小/背景属于「这台设备怎么显示」，不属于某一本内容；窗口里显示的是<b>当前选中那本</b>。</p>
 *
 * <p><b>旧存档兼容</b>：v5.3.0 及以前每个玩家只有一本，NBT 顶层直接是 {@code Tasks/Name/Hud}。
 * 读取时若没有 {@code Books} 标签，就把整份当成「一本」装进来，界面与悬浮窗表现不变。</p>
 */
public final class NoteShelf
{
    /** 一个人最多几本便签 */
    public static final int MAX_BOOKS = 8;

    private final List<NoteBook> books = new ArrayList<>();
    private final NoteHud hud = new NoteHud();
    private int selected;

    public NoteShelf()
    {
        books.add(new NoteBook());
    }

    public List<NoteBook> books()
    {
        return books;
    }

    public NoteHud hud()
    {
        return hud;
    }

    public int selected()
    {
        return selected;
    }

    public int size()
    {
        return books.size();
    }

    /** 切换当前便签（越界会被夹回来） */
    public void select(int index)
    {
        selected = Math.max(0, Math.min(index, books.size() - 1));
    }

    /** 当前这一本（永不为 null） */
    public NoteBook current()
    {
        selected = Math.max(0, Math.min(selected, books.size() - 1));
        return books.get(selected);
    }

    /** 新建一本（到上限返回 -1），并切到新那本 */
    public int addBook(String name)
    {
        if (books.size() >= MAX_BOOKS)
        {
            return -1;
        }
        NoteBook book = new NoteBook();
        book.setName(name);
        books.add(book);
        selected = books.size() - 1;
        return selected;
    }

    /** 删掉一本；至少保留一本（返回是否真的删了） */
    public boolean removeBook(int index)
    {
        if (books.size() <= 1 || index < 0 || index >= books.size())
        {
            return false;
        }
        books.remove(index);
        selected = Math.max(0, Math.min(selected, books.size() - 1));
        return true;
    }

    /** 把任务从一处挪到另一处（列表拖拽排序用），越界自动夹回 */
    public void moveTask(int from, int to)
    {
        List<NoteTask> tasks = current().tasks();
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

    /** 服务端落库前兜底 */
    public void clamp()
    {
        while (books.size() > MAX_BOOKS)
        {
            books.remove(books.size() - 1);
        }
        if (books.isEmpty())
        {
            books.add(new NoteBook());
        }
        for (NoteBook book : books)
        {
            book.clamp();
        }
        select(selected);
        hud.clamp();
    }

    /** 自动更名：对每一本「开着自动更名」的便签生效，返回是否有任何一本改了名 */
    public boolean applyAutoName()
    {
        boolean changed = false;
        for (NoteBook book : books)
        {
            if (book.applyAutoName())
            {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 整册内容签名：客户端推送去重用它判断「这次改动和上次发出去的是否一样」。
     *
     * <p><b>必须覆盖所有会变的内容</b>—— 曾经漏掉子任务与折叠状态，结果加子任务的整册推送
     * 被去重逻辑当成「没变化」吞掉，用户实测表现为「加的子任务重进界面就没了」「折叠按钮无效」。
     * 改任务结构时记得同步改这里（有 GameTest 回归：noteSignatureCoversSubTasks）。</p>
     */
    public String signature()
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append(selected).append('|')
                .append(hud.enabled ? 1 : 0).append(hud.showDone ? 1 : 0)
                .append(hud.background).append('|').append(hud.color).append('|')
                .append(Math.round(hud.x * 1000)).append(',').append(Math.round(hud.y * 1000))
                .append(',').append(Math.round(hud.scale * 1000)).append(',')
                .append(Math.round(hud.opacity * 1000));
        for (NoteBook book : books)
        {
            sb.append('|').append(book.name()).append(book.autoName() ? '1' : '0');
            for (NoteTask task : book.tasks())
            {
                sb.append('|').append(task.done ? 1 : 0)
                        .append(task.collapsed ? 'c' : 'o').append(task.text);
                for (NoteTask child : task.children())
                {
                    sb.append('>').append(child.done ? 1 : 0).append(child.text);
                }
            }
        }
        return sb.toString();
    }

    public NoteShelf copy()
    {
        NoteShelf shelf = new NoteShelf();
        shelf.books.clear();
        for (NoteBook book : books)
        {
            shelf.books.add(book.copy());
        }
        shelf.selected = selected;
        NoteHud h = hud.copy();
        shelf.hud.enabled = h.enabled;
        shelf.hud.x = h.x;
        shelf.hud.y = h.y;
        shelf.hud.scale = h.scale;
        shelf.hud.background = h.background;
        shelf.hud.color = h.color;
        shelf.hud.opacity = h.opacity;
        shelf.hud.showDone = h.showDone;
        return shelf;
    }

    public void save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        for (NoteBook book : books)
        {
            CompoundTag entry = new CompoundTag();
            book.save(entry);
            list.add(entry);
        }
        tag.put("Books", list);
        tag.putInt("Selected", selected);
        CompoundTag hudTag = new CompoundTag();
        hud.save(hudTag);
        tag.put("Hud", hudTag);
    }

    public void load(CompoundTag tag)
    {
        books.clear();
        if (tag.contains("Books", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("Books", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && books.size() < MAX_BOOKS; i++)
            {
                NoteBook book = new NoteBook();
                book.load(list.getCompound(i));
                books.add(book);
            }
            selected = tag.getInt("Selected");
        }
        else
        {
            // v5.3.0 及以前的单本格式：整份就是一本
            NoteBook book = new NoteBook();
            book.load(tag);
            books.add(book);
            selected = 0;
        }
        if (tag.contains("Hud"))
        {
            hud.load(tag.getCompound("Hud"));
        }
        clamp();
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeVarInt(books.size());
        for (NoteBook book : books)
        {
            book.write(buf);
        }
        buf.writeVarInt(selected);
        hud.write(buf);
    }

    public static NoteShelf read(RegistryFriendlyByteBuf buf)
    {
        NoteShelf shelf = new NoteShelf();
        shelf.books.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n && shelf.books.size() < MAX_BOOKS; i++)
        {
            shelf.books.add(NoteBook.read(buf));
        }
        shelf.selected = buf.readVarInt();
        NoteHud h = NoteHud.read(buf);
        shelf.hud.enabled = h.enabled;
        shelf.hud.x = h.x;
        shelf.hud.y = h.y;
        shelf.hud.scale = h.scale;
        shelf.hud.background = h.background;
        shelf.hud.color = h.color;
        shelf.hud.opacity = h.opacity;
        shelf.hud.showDone = h.showDone;
        shelf.clamp();
        return shelf;
    }
}

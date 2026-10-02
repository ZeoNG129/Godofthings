package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteDate;
import com.godofthings.note.NoteHud;
import com.godofthings.note.NoteShelf;
import com.godofthings.note.NoteTask;
import com.godofthings.waypoint.Waypoint;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 数据层回归测试（{@code gradlew runGameTestServer} 跑）。
 *
 * <p>覆盖的是最怕出「静默 bug」、又完全不需要看画面的那部分：存档 NBT 往返、网络封包往返、
 * 多本便签与旧存档迁移、拖拽排序、自动更名的日期逻辑、以及服务端兜底 clamp。</p>
 *
 * <p>模板用 {@code data/godofthings/structure/note_data.nbt}（1×1×1 空气，只提供一块跑测试的地），
 * 所以 {@code @PrefixGameTestTemplate(false)} 让所有方法共用它、而不是各要一个结构文件。</p>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class NoteDataGameTest
{
    private static final String TEMPLATE = "note_data";

    /** 造一册有代表性的便签：两本、多条任务（含子任务）、勾选、名字、自动更名开关、悬浮窗设置 */
    private static NoteShelf sample()
    {
        NoteShelf shelf = new NoteShelf();
        NoteBook first = shelf.current();
        first.add("买牛奶");
        first.add("写周报");
        first.add("测试勾选");
        first.toggle(2);
        // 「做贤者之石」+ 子任务（用户给的例子）
        int parent = first.add("做贤者之石");
        first.addChild(parent, "热核");
        first.addChild(parent, "工业先锋");
        first.addChild(parent, "还差一步");
        first.toggleChild(parent, 1); // 工业先锋 已完成
        first.setName("我的清单");
        first.setAutoName(false);

        shelf.addBook("第二本");
        shelf.current().add("限时活动");
        shelf.addBook("第三本");

        shelf.select(0);
        shelf.hud().enabled = true;
        shelf.hud().x = 0.15F;
        shelf.hud().y = 0.25F;
        shelf.hud().scale = 1.5F;
        shelf.hud().background = NoteHud.BG_SPACE;
        shelf.hud().opacity = 0.7F;
        shelf.hud().showDone = false;
        return shelf;
    }

    private static boolean sameHud(NoteHud a, NoteHud b)
    {
        return a.enabled == b.enabled
                && a.showDone == b.showDone
                && a.background == b.background
                && Math.abs(a.x - b.x) < 1.0E-5F
                && Math.abs(a.y - b.y) < 1.0E-5F
                && Math.abs(a.scale - b.scale) < 1.0E-5F
                && Math.abs(a.opacity - b.opacity) < 1.0E-5F;
    }

    private static boolean sameShelf(NoteShelf a, NoteShelf b)
    {
        if (a.size() != b.size() || a.selected() != b.selected() || !sameHud(a.hud(), b.hud()))
        {
            return false;
        }
        for (int i = 0; i < a.size(); i++)
        {
            NoteBook left = a.books().get(i);
            NoteBook right = b.books().get(i);
            if (!left.name().equals(right.name())
                    || left.autoName() != right.autoName()
                    || left.tasks().size() != right.tasks().size())
            {
                return false;
            }
            for (int t = 0; t < left.tasks().size(); t++)
            {
                NoteTask lt = left.tasks().get(t);
                NoteTask rt = right.tasks().get(t);
                if (!lt.text.equals(rt.text) || lt.done != rt.done
                        || lt.childCount() != rt.childCount())
                {
                    return false;
                }
                for (int c = 0; c < lt.childCount(); c++)
                {
                    NoteTask lc = lt.children().get(c);
                    NoteTask rc = rt.children().get(c);
                    if (!lc.text.equals(rc.text) || lc.done != rc.done)
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** 存档路径：整册（多本 + 悬浮窗设置 + 当前选中）→ CompoundTag → 整册 */
    @GameTest(template = TEMPLATE)
    public static void noteShelfNbtRoundTrip(GameTestHelper helper)
    {
        NoteShelf original = sample();
        CompoundTag tag = new CompoundTag();
        original.save(tag);

        NoteShelf loaded = new NoteShelf();
        loaded.load(tag);
        helper.assertTrue(sameShelf(original, loaded), "NBT 往返后整册便签内容不一致");
        helper.succeed();
    }

    /** 网络路径：整册 → RegistryFriendlyByteBuf → 整册，且缓冲区必须正好读完 */
    @GameTest(template = TEMPLATE)
    public static void notePacketRoundTrip(GameTestHelper helper)
    {
        NoteShelf original = sample();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), helper.getLevel().registryAccess());
        original.write(buf);
        NoteShelf decoded = NoteShelf.read(buf);

        helper.assertTrue(sameShelf(original, decoded), "封包往返后整册便签内容不一致");
        helper.assertTrue(buf.readableBytes() == 0, "封包读完后还剩 " + buf.readableBytes() + " 字节（读写不对称）");
        helper.succeed();
    }

    /** 旧存档迁移：v5.3.0 的单本格式（顶层 Tasks/Name/Hud）要能读成「一册里的一本」，且悬浮窗设置不丢 */
    @GameTest(template = TEMPLATE)
    public static void noteLegacyMigration(GameTestHelper helper)
    {
        NoteBook legacyBook = new NoteBook();
        legacyBook.add("老任务一");
        legacyBook.add("老任务二");
        legacyBook.toggle(1);
        legacyBook.setName("旧名字");
        CompoundTag legacy = new CompoundTag();
        legacyBook.save(legacy);

        NoteHud legacyHud = new NoteHud();
        legacyHud.enabled = true;
        legacyHud.x = 0.35F;
        legacyHud.showDone = false;
        CompoundTag hudTag = new CompoundTag();
        legacyHud.save(hudTag);
        legacy.put("Hud", hudTag);

        NoteShelf migrated = new NoteShelf();
        migrated.load(legacy);

        helper.assertTrue(migrated.size() == 1, "旧存档应该被读成 1 本，实际 " + migrated.size());
        helper.assertTrue("旧名字".equals(migrated.current().name()), "旧名字没保住");
        helper.assertTrue(migrated.current().tasks().size() == 2, "旧任务没保住");
        helper.assertTrue(migrated.current().tasks().get(1).done, "旧勾选状态没保住");
        helper.assertTrue(migrated.hud().enabled, "旧悬浮窗开关没接上");
        helper.assertTrue(Math.abs(migrated.hud().x - 0.35F) < 1.0E-5F, "旧悬浮窗位置没接上");
        helper.succeed();
    }

    /** 多本：新建 / 上限 / 切换 / 删除，以及拖拽排序（moveTask） */
    @GameTest(template = TEMPLATE)
    public static void noteMultiBook(GameTestHelper helper)
    {
        NoteShelf shelf = new NoteShelf();
        shelf.current().add("第一本的任务");
        for (int i = 0; i < NoteShelf.MAX_BOOKS + 3; i++)
        {
            shelf.addBook("第" + (i + 2) + "本");
        }
        helper.assertTrue(shelf.size() == NoteShelf.MAX_BOOKS,
                "本数没夹在 " + NoteShelf.MAX_BOOKS + "，实际 " + shelf.size());

        shelf.select(1);
        helper.assertTrue(shelf.selected() == 1, "切换失败");
        shelf.select(999);
        helper.assertTrue(shelf.selected() == shelf.size() - 1, "越界切换没夹住");

        NoteShelf single = new NoteShelf();
        helper.assertFalse(single.removeBook(0), "只剩一本时不该允许删除");
        helper.assertTrue(single.size() == 1, "删除不该把最后一本删掉");

        NoteShelf ordered = new NoteShelf();
        ordered.current().add("1");
        ordered.current().add("2");
        ordered.current().add("3");
        ordered.moveTask(2, 0);
        helper.assertTrue("3".equals(ordered.current().tasks().get(0).text), "拖拽排序（尾→首）失败");
        ordered.moveTask(0, 2);
        helper.assertTrue("3".equals(ordered.current().tasks().get(2).text), "拖拽排序（首→尾）失败");
        helper.succeed();
    }

    /** 自动更名：开着必须刷成当日日期且幂等；关着绝不能被动改；整册级调用只影响开着的那几本 */
    @GameTest(template = TEMPLATE)
    public static void noteAutoNameRule(GameTestHelper helper)
    {
        NoteBook auto = new NoteBook();
        auto.setName("01.01");
        auto.setAutoName(true);
        helper.assertTrue(auto.applyAutoName(), "开着自动更名时首次调用应该改名");
        helper.assertTrue(NoteDate.today().equals(auto.name()),
                "自动更名结果不是当日日期：" + auto.name() + "（期望 " + NoteDate.today() + "）");
        helper.assertFalse(auto.applyAutoName(), "重复调用不该再改（应当幂等）");

        NoteBook manual = new NoteBook();
        manual.setName("01.01");
        helper.assertFalse(manual.applyAutoName(), "自动更名关着时不该动名字");
        helper.assertTrue("01.01".equals(manual.name()), "手动起的名字被改掉了");

        NoteShelf shelf = new NoteShelf();
        shelf.current().setName("手动本");
        shelf.current().add("x");
        shelf.addBook("自动本");
        shelf.current().setName("01.01");
        shelf.current().setAutoName(true);
        helper.assertTrue(shelf.applyAutoName(), "整册调用应该把开着自动更名的那本改掉");
        helper.assertTrue(NoteDate.today().equals(shelf.current().name()), "自动本没被改名");
        helper.assertTrue("手动本".equals(shelf.books().get(0).name()), "手动本被误改");
        helper.succeed();
    }

    /** 服务端兜底：条数 / 本数 / 名称长度 / 名称换行 / 悬浮窗越界字段全都要被夹住 */
    @GameTest(template = TEMPLATE)
    public static void noteClampRules(GameTestHelper helper)
    {
        NoteShelf shelf = new NoteShelf();
        for (int b = 0; b < NoteShelf.MAX_BOOKS + 5; b++)
        {
            shelf.addBook("本" + b);
            for (int i = 0; i < NoteBook.MAX_TASKS + 5; i++)
            {
                shelf.current().add("任务" + i);
            }
        }
        shelf.current().setName("换\n行" + "y".repeat(80));
        shelf.hud().scale = 99.0F;
        shelf.hud().opacity = -1.0F;
        shelf.hud().background = 42;
        shelf.hud().x = 42.0F;
        shelf.clamp();

        helper.assertTrue(shelf.size() <= NoteShelf.MAX_BOOKS, "本数没夹住：" + shelf.size());
        for (NoteBook book : shelf.books())
        {
            helper.assertTrue(book.tasks().size() <= NoteBook.MAX_TASKS, "任务条数没夹住：" + book.tasks().size());
            helper.assertTrue(book.name().length() <= NoteBook.MAX_NAME, "名称没截断：" + book.name().length());
            helper.assertFalse(book.name().contains("\n"), "名称里的换行没清掉");
        }
        helper.assertTrue(shelf.hud().scale == NoteHud.MAX_SCALE, "缩放上限没夹住：" + shelf.hud().scale);
        helper.assertTrue(shelf.hud().opacity == NoteHud.MIN_OPACITY, "透明度下限没夹住：" + shelf.hud().opacity);
        helper.assertTrue(shelf.hud().background == NoteHud.BG_COUNT - 1,
                "背景档位没夹住：" + shelf.hud().background);
        helper.assertTrue(shelf.hud().x <= 1.5F, "位置没夹住：" + shelf.hud().x);
        helper.succeed();
    }

    /** 子任务：NBT / 封包往返、旧平铺存档兼容、深度与数量夹取 */
    @GameTest(template = TEMPLATE)
    public static void noteSubTaskStructure(GameTestHelper helper)
    {
        NoteBook book = new NoteBook();
        book.add("做贤者之石");
        int p = 0;
        helper.assertTrue(book.addChild(p, "热核") == 0, "挂第一个子任务应返回下标 0");
        helper.assertTrue("热核".equals(book.tasks().get(p).children().get(0).text), "子任务文字不对");
        book.addChild(p, "工业先锋");
        book.toggleChild(p, 1);
        helper.assertTrue(book.tasks().get(p).doneChildren() == 1, "已完成子任务计数不对");
        helper.assertTrue(book.tasks().get(p).totalDone() == 1
                && book.tasks().get(p).totalCount() == 3, "子树计数不对");

        // NBT 往返
        CompoundTag tag = new CompoundTag();
        book.tasks().get(p).save(tag);
        NoteTask back = NoteTask.load(tag);
        helper.assertTrue(back.childCount() == 2, "NBT 往返后子任务丢了");
        helper.assertTrue("工业先锋".equals(back.children().get(1).text) && back.children().get(1).done,
                "NBT 往返后子任务内容/勾选不对");

        // 封包往返
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), helper.getLevel().registryAccess());
        book.tasks().get(p).write(buf);
        NoteTask fromBuf = NoteTask.read(buf);
        helper.assertTrue(fromBuf.childCount() == 2
                && "热核".equals(fromBuf.children().get(0).text), "封包往返后子任务不对");
        helper.assertTrue(buf.readableBytes() == 0, "封包没读完（读写不对称）");

        // 旧平铺存档（没有 Children 字段）→ 读成主任务、无子任务
        CompoundTag legacy = new CompoundTag();
        new NoteTask("老任务", false).save(legacy);
        NoteTask legacyTask = NoteTask.load(legacy);
        helper.assertTrue(legacyTask.childCount() == 0 && "老任务".equals(legacyTask.text),
                "旧平铺任务应该读成无子任务的主任务");

        // 超过两级的嵌套会被夹掉
        NoteTask deep = new NoteTask("一级", false);
        NoteTask mid = new NoteTask("二级", false);
        mid.appendChild(new NoteTask("三级", false));
        deep.appendChild(mid);
        NoteTask deepBack = NoteTask.load(toTag(deep));
        helper.assertTrue(deepBack.childCount() == 1, "二级应该保留");
        helper.assertTrue(deepBack.children().get(0).childCount() == 0, "三级应该被夹掉（只开放两级）");

        // 数量上限
        NoteBook big = new NoteBook();
        big.add("主任务");
        for (int i = 0; i < NoteTask.MAX_CHILDREN + 5; i++)
        {
            big.addChild(0, "子" + i);
        }
        helper.assertTrue(big.tasks().get(0).childCount() == NoteTask.MAX_CHILDREN,
                "子任务数没夹在 " + NoteTask.MAX_CHILDREN);
        helper.succeed();
    }

    private static CompoundTag toTag(NoteTask task)
    {
        CompoundTag tag = new CompoundTag();
        task.save(tag);
        return tag;
    }

    /** 升降级与同级重排：Tab（降级）/ Shift+Tab（升级）/ 拖拽语义 */
    @GameTest(template = TEMPLATE)
    public static void noteSubTaskMove(GameTestHelper helper)
    {
        NoteBook book = new NoteBook();
        book.add("A");
        book.add("B");
        book.add("C");

        // B 降级成 A 的子任务
        helper.assertTrue(book.demoteTop(1), "demoteTop 应该成功");
        helper.assertTrue(book.tasks().size() == 2, "降级后主任务数应为 2");
        helper.assertTrue(book.tasks().get(0).childCount() == 1
                && "B".equals(book.tasks().get(0).children().get(0).text), "B 应该挂在 A 下面");

        // 再把 B 升级回主任务（插在 A 后面）
        helper.assertTrue(book.promoteChild(0, 0), "promoteChild 应该成功");
        helper.assertTrue(book.tasks().size() == 3
                && "B".equals(book.tasks().get(1).text), "升级后 B 应该回到 A 后面");

        // 子任务同级重排
        book.addChild(0, "子1");
        book.addChild(0, "子2");
        book.addChild(0, "子3");
        book.moveChild(0, 2, 0);
        helper.assertTrue("子3".equals(book.tasks().get(0).children().get(0).text), "子任务重排失败");

        // 主任务同级重排：子任务跟着走
        book.moveTask(0, 2);
        helper.assertTrue(book.tasks().get(2).childCount() == 3, "主任务拖拽应该带着子任务走");

        // 删除主任务连子任务一起删
        book.remove(2);
        helper.assertTrue(book.childCount() == 0, "删主任务应该连子任务一起删");

        // clearDone：完成的子任务会被清掉，未完成的保留（主任务不受影响）
        book.addChild(0, "已完成的子任务");
        book.addChild(0, "未完成的子任务");
        book.toggleChild(0, 0); // 第一个标成完成
        int removed = book.clearDone();
        helper.assertTrue(removed == 1, "clearDone 应该只清掉 1 条已完成的子任务");
        helper.assertTrue(book.tasks().size() == 2, "未完成的主任务应该保留");
        helper.assertTrue(book.childCount() == 1
                && "未完成的子任务".equals(book.tasks().get(0).children().get(0).text),
                "clearDone 应该保留未完成的子任务");
        helper.succeed();
    }

    /** 传送点数据（顺带覆盖）：名称 / 维度 / 坐标 / 面对方向 / 置顶状态 */
    @GameTest(template = TEMPLATE)
    public static void waypointNbtRoundTrip(GameTestHelper helper)
    {
        Waypoint wp = new Waypoint();
        wp.name = "测试点";
        wp.dimension = "minecraft:overworld";
        wp.x = 12.5;
        wp.y = 64.0;
        wp.z = -3.25;
        wp.yaw = 90.0F;
        wp.pitch = 12.5F;
        wp.pinned = true;

        Waypoint back = Waypoint.load(wp.save(new CompoundTag()));
        helper.assertTrue(wp.name.equals(back.name), "传送点名称没保住");
        helper.assertTrue(wp.dimension.equals(back.dimension), "传送点维度没保住");
        helper.assertTrue(Math.abs(wp.x - back.x) < 1.0E-6 && Math.abs(wp.z - back.z) < 1.0E-6,
                "传送点坐标没保住");
        helper.assertTrue(Math.abs(wp.yaw - back.yaw) < 1.0E-3, "传送点朝向没保住");
        helper.assertTrue(back.pinned, "传送点置顶状态没保住");
        helper.succeed();
    }
}

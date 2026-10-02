package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteDate;
import com.godofthings.note.NoteHud;
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
 * 自动更名的日期逻辑、以及服务端兜底 clamp。以前这些只能靠临时自检脚本，现在固化成测试，
 * 以后改数据结构、换版本时直接跑一遍就知道有没有跑偏。</p>
 *
 * <p>模板用 {@code data/godofthings/structure/note_data.nbt}（1×1×1 空气，只提供一块跑测试的地），
 * 所以 {@code @PrefixGameTestTemplate(false)} 让所有方法共用它、而不是各要一个结构文件。</p>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class NoteDataGameTest
{
    private static final String TEMPLATE = "note_data";

    /** 造一本有代表性的便签：多条任务 + 勾选状态 + 自定义名称 + 悬浮窗设置 */
    private static NoteBook sample()
    {
        NoteBook book = new NoteBook();
        book.add("买牛奶");
        book.add("写周报");
        book.add("测试勾选");
        book.toggle(2);
        book.setName("我的清单");
        book.setAutoName(false);
        book.hud().enabled = true;
        book.hud().x = 0.15F;
        book.hud().y = 0.25F;
        book.hud().scale = 1.5F;
        book.hud().background = NoteHud.BG_SPACE;
        book.hud().opacity = 0.7F;
        book.hud().showDone = false;
        return book;
    }

    private static boolean same(NoteBook a, NoteBook b)
    {
        if (a.tasks().size() != b.tasks().size()
                || !a.name().equals(b.name())
                || a.autoName() != b.autoName()
                || a.hud().enabled != b.hud().enabled
                || a.hud().showDone != b.hud().showDone
                || a.hud().background != b.hud().background
                || Math.abs(a.hud().x - b.hud().x) > 1.0E-5F
                || Math.abs(a.hud().y - b.hud().y) > 1.0E-5F
                || Math.abs(a.hud().scale - b.hud().scale) > 1.0E-5F
                || Math.abs(a.hud().opacity - b.hud().opacity) > 1.0E-5F)
        {
            return false;
        }
        for (int i = 0; i < a.tasks().size(); i++)
        {
            NoteTask left = a.tasks().get(i);
            NoteTask right = b.tasks().get(i);
            if (!left.text.equals(right.text) || left.done != right.done)
            {
                return false;
            }
        }
        return true;
    }

    /** 存档路径：NoteBook → CompoundTag → NoteBook（任务 / 名称 / 自动更名开关 / 悬浮窗都要活着回来） */
    @GameTest(template = TEMPLATE)
    public static void noteNbtRoundTrip(GameTestHelper helper)
    {
        NoteBook original = sample();
        CompoundTag tag = new CompoundTag();
        original.save(tag);

        NoteBook loaded = new NoteBook();
        loaded.load(tag);
        helper.assertTrue(same(original, loaded), "NBT 往返后便签内容不一致");
        helper.succeed();
    }

    /** 网络路径：NoteBook → RegistryFriendlyByteBuf → NoteBook，且缓冲区必须正好读完 */
    @GameTest(template = TEMPLATE)
    public static void notePacketRoundTrip(GameTestHelper helper)
    {
        NoteBook original = sample();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), helper.getLevel().registryAccess());
        original.write(buf);
        NoteBook decoded = NoteBook.read(buf);

        helper.assertTrue(same(original, decoded), "封包往返后便签内容不一致");
        helper.assertTrue(buf.readableBytes() == 0, "封包读完后还剩 " + buf.readableBytes() + " 字节（读写不对称）");
        helper.succeed();
    }

    /** 自动更名：开着必须刷成当日日期且幂等；关着绝不能被动改 */
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
        helper.succeed();
    }

    /** 服务端兜底：条数 / 名称长度 / 名称换行 / 悬浮窗越界字段全都要被夹住 */
    @GameTest(template = TEMPLATE)
    public static void noteClampRules(GameTestHelper helper)
    {
        NoteBook messy = new NoteBook();
        for (int i = 0; i < NoteBook.MAX_TASKS + 20; i++)
        {
            messy.add("任务" + i);
        }
        messy.setName("换\n行" + "y".repeat(80));
        messy.hud().scale = 99.0F;
        messy.hud().opacity = -1.0F;
        messy.hud().background = 42;
        messy.hud().x = 42.0F;
        messy.clamp();

        helper.assertTrue(messy.tasks().size() <= NoteBook.MAX_TASKS,
                "任务条数没夹住：" + messy.tasks().size());
        helper.assertTrue(messy.name().length() <= NoteBook.MAX_NAME, "名称没截断：" + messy.name().length());
        helper.assertFalse(messy.name().contains("\n"), "名称里的换行没清掉");
        helper.assertTrue(messy.hud().scale == NoteHud.MAX_SCALE, "缩放上限没夹住：" + messy.hud().scale);
        helper.assertTrue(messy.hud().opacity == NoteHud.MIN_OPACITY, "透明度下限没夹住：" + messy.hud().opacity);
        helper.assertTrue(messy.hud().background == NoteHud.BG_COUNT - 1,
                "背景档位没夹住：" + messy.hud().background);
        helper.assertTrue(messy.hud().x <= 1.5F, "位置没夹住：" + messy.hud().x);
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

package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.item.BlackBoxData;
import com.godofthings.menu.GodBlackBoxMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 神之黑盒回归测试（v5.17.4）。
 *
 * <p>覆盖两个审计出来的隐患：①{@code stillValid} 原先恒返回 true —— 黑盒扔到地上 / 放进箱子后
 * 界面仍然开着且可操作；②过滤存储槽没拦 {@code GodBlackBoxItem} —— 黑盒能被放进自己里面（套娃）。</p>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodBlackBoxGameTest
{
    private static final String TEMPLATE = "note_data";

    /** 黑盒不在身上时菜单必须失效；别的槽位冒出黑盒也不许「顶上」（防止张冠李戴） */
    @GameTest(template = TEMPLATE)
    public static void stillValidFollowsTheBox(GameTestHelper helper)
    {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Godofthings.GOD_BLACK_BOX.get()));
        GodBlackBoxMenu menu = new GodBlackBoxMenu(0, player.getInventory(), 0);
        helper.assertTrue(menu.stillValid(player), "黑盒在身上时菜单应当有效");

        // 黑盒被拿走 / 扔到地上 → 槽位空了 → 必须失效（否则留下一个能操作的空壳界面）
        player.getInventory().setItem(0, ItemStack.EMPTY);
        helper.assertFalse(menu.stillValid(player), "黑盒不在身上时菜单必须失效");

        // 另一个黑盒出现在别的槽位也不该让旧菜单复活
        player.getInventory().setItem(9, new ItemStack(Godofthings.GOD_BLACK_BOX.get()));
        helper.assertFalse(menu.stillValid(player), "别的槽位出现黑盒也不该让旧菜单复活");
        helper.succeed();
    }

    /** 不许黑盒套黑盒：过滤槽放置校验 / 数字键交换 / Shift 快捷移动三条路都要拦住 */
    @GameTest(template = TEMPLATE)
    public static void noBlackBoxInsideBlackBox(GameTestHelper helper)
    {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack open = new ItemStack(Godofthings.GOD_BLACK_BOX.get());
        ItemStack spare = new ItemStack(Godofthings.GOD_BLACK_BOX.get());
        player.getInventory().setItem(0, open);    // 快捷栏 0：正开着的黑盒
        player.getInventory().setItem(1, spare);   // 快捷栏 1：备用黑盒
        GodBlackBoxMenu menu = new GodBlackBoxMenu(0, player.getInventory(), 0);

        // ① 槽位校验
        helper.assertFalse(menu.getSlot(0).mayPlace(spare), "过滤槽不该允许放入神之黑盒");

        // ② 数字键交换（button = 快捷栏下标 1；原版 SWAP 不检查 mayPlace）
        menu.clicked(0, 1, ClickType.SWAP, player);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "数字键交换把黑盒塞进了过滤槽");

        // ③ Shift 快捷移动（菜单槽位 15 = 玩家主物品栏第一格）
        player.getInventory().setItem(1, ItemStack.EMPTY);
        player.getInventory().setItem(9, spare);
        menu.quickMoveStack(player, BlackBoxData.FILTER_SLOTS);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "Shift 快捷移动把黑盒塞进了过滤槽");
        helper.succeed();
    }
}

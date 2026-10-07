package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.backpack.GodBackpackContainer;
import com.godofthings.backpack.GodBackpackContents;
import com.godofthings.backpack.GodBackpackItem;
import com.godofthings.backpack.GodBackpackMenu;
import com.godofthings.backpack.GodBackpackSettings;
import com.godofthings.backpack.GodBackpackSorting;
import com.godofthings.backpack.SortBy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 神之背包回归测试（{@code gradlew runGameTestServer} 跑）。
 *
 * <p>覆盖四条最怕静默出错的数据层行为：120 格内容的 NBT 往返、设置组件的 NBT 往返、
 * 整理时「记忆格 / 忽略整理格原地不动」、以及「存入时记忆格优先」。</p>
 *
 * <p>模板沿用 {@code data/godofthings/structure/note_data.nbt}（1×1×1 空气），
 * 所以 {@code @PrefixGameTestTemplate(false)} 让四条共用它。</p>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodBackpackGameTest
{
    private static final String TEMPLATE = "note_data";

    /** 造一个「有代表性」的背包：普通堆叠、自定义名字、附魔（要 registry 的组件）、空格混在一起 */
    private static GodBackpackContents sampleContents(ServerLevel level)
    {
        List<ItemStack> items = new ArrayList<>(GodBackpackItem.SIZE);
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            // 奇数格留空，覆盖「空堆叠编码成空 map」这条路径
            if (i % 2 == 1)
            {
                items.add(ItemStack.EMPTY);
                continue;
            }
            if (i % 10 == 0)
            {
                ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
                named.set(DataComponents.CUSTOM_NAME, Component.literal("测试剑 " + i));
                items.add(named);
            }
            else if (i % 10 == 2)
            {
                ItemStack enchanted = new ItemStack(Items.DIAMOND_PICKAXE);
                enchanted.enchant(level.registryAccess().holderOrThrow(Enchantments.SHARPNESS), 3);
                items.add(enchanted);
            }
            else
            {
                items.add(new ItemStack(Items.STONE, i % 64 + 1));
            }
        }
        return new GodBackpackContents(items);
    }

    /** 从容器抓一份快照（拷贝，避免后续改动影响断言） */
    private static List<ItemStack> snapshot(GodBackpackContainer container)
    {
        List<ItemStack> list = new ArrayList<>(GodBackpackItem.SIZE);
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            list.add(container.getItem(i).copy());
        }
        return list;
    }

    /** 物品 → 总数量（整理只该重排，不该吞掉或复制物品） */
    private static Map<Item, Integer> totals(List<ItemStack> stacks)
    {
        Map<Item, Integer> map = new LinkedHashMap<>();
        for (ItemStack stack : stacks)
        {
            if (!stack.isEmpty())
            {
                map.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return map;
    }

    /** 存档路径：120 格内容（含空、含自定义名字、含附魔）→ NBT → 内容，逐格比对 */
    @GameTest(template = TEMPLATE)
    public static void contentsRoundTrip(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        ItemStack backpack = new ItemStack(Godofthings.GOD_BACKPACK_ITEM.get());
        GodBackpackContents original = sampleContents(level);
        GodBackpackItem.setContents(backpack, original);

        // 走真实存档路径：物品 → NBT → 物品（数据组件的 persistent codec）
        Tag saved = backpack.save(level.registryAccess());
        ItemStack loaded = ItemStack.parse(level.registryAccess(), saved).orElse(ItemStack.EMPTY);
        helper.assertFalse(loaded.isEmpty(), "背包没能在 NBT 往返后读回来");

        GodBackpackContents round = GodBackpackItem.contents(loaded);
        helper.assertTrue(round.items().size() == GodBackpackItem.SIZE,
                "往返后格数不是 " + GodBackpackItem.SIZE + "，实际 " + round.items().size());
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            ItemStack before = original.items().get(i);
            ItemStack after = round.items().get(i);
            helper.assertTrue(ItemStack.matches(before, after),
                    "第 " + i + " 格往返后不一致：" + before + " → " + after);
        }
        helper.succeed();
    }

    /** 存档路径：记忆格 + 忽略整理格 + 排序方式 + 三个开关 + 搜索词 → NBT → 设置 */
    @GameTest(template = TEMPLATE)
    public static void settingsRoundTrip(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        ItemStack backpack = new ItemStack(Godofthings.GOD_BACKPACK_ITEM.get());

        Map<Integer, ItemStack> memory = new LinkedHashMap<>();
        memory.put(3, new ItemStack(Items.DIAMOND_PICKAXE));
        memory.put(7, new ItemStack(Items.DIAMOND));
        memory.put(119, new ItemStack(Items.SHULKER_BOX));
        Set<Integer> noSort = new LinkedHashSet<>(List.of(0, 12, 55));
        GodBackpackSettings original = new GodBackpackSettings(
                memory, noSort, SortBy.TAG, true, true, "@minecraft 钻石", true);
        GodBackpackItem.setSettings(backpack, original);

        Tag saved = backpack.save(level.registryAccess());
        ItemStack loaded = ItemStack.parse(level.registryAccess(), saved).orElse(ItemStack.EMPTY);
        helper.assertFalse(loaded.isEmpty(), "背包没能在 NBT 往返后读回来");

        GodBackpackSettings round = GodBackpackItem.settings(loaded);
        helper.assertTrue(round.sortBy() == SortBy.TAG, "排序方式没保住：" + round.sortBy());
        helper.assertTrue(round.ignoreDurability(), "「忽略耐久」开关没保住");
        helper.assertTrue(round.ignoreNbt(), "「忽略 NBT」开关没保住");
        helper.assertTrue(round.keepSearch(), "「保留搜索词」开关没保住");
        helper.assertTrue("@minecraft 钻石".equals(round.searchPhrase()),
                "搜索词没保住：" + round.searchPhrase());
        helper.assertTrue(round.noSort().equals(noSort), "忽略整理格没保住：" + round.noSort());
        helper.assertTrue(round.memory().size() == memory.size(),
                "记忆格数量不对：" + round.memory().size() + "（期望 " + memory.size() + "）");
        for (Map.Entry<Integer, ItemStack> entry : memory.entrySet())
        {
            ItemStack remembered = round.memory().get(entry.getKey());
            helper.assertTrue(remembered != null && ItemStack.matches(remembered, entry.getValue()),
                    "第 " + entry.getKey() + " 格记住的物品没保住：" + remembered);
        }
        helper.succeed();
    }

    /** 整理：记忆格与忽略整理格原地不动，其余按当前排序键有序、空堆叠排最后，且物品一个不多一个不少 */
    @GameTest(template = TEMPLATE)
    public static void sortRespectsNoSortAndMemory(GameTestHelper helper)
    {
        ItemStack backpack = new ItemStack(Godofthings.GOD_BACKPACK_ITEM.get());
        GodBackpackContainer container = new GodBackpackContainer(backpack);

        // 乱序铺满 120 格（含空格）：5 种物品 × 数量 1..5
        Item[] pool = {Items.STONE, Items.DIRT, Items.DIAMOND, Items.APPLE, Items.GOLD_INGOT};
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            if (i % 11 == 0)
            {
                container.setItem(i, ItemStack.EMPTY);
            }
            else
            {
                container.setItem(i, new ItemStack(pool[(i * 7) % pool.length], i % 5 + 1));
            }
        }

        GodBackpackSettings settings = GodBackpackSettings.defaults()
                .withMemory(5, new ItemStack(Items.DIAMOND))
                .toggleNoSort(10);
        helper.assertTrue(settings.isMemory(5), "5 号格应该被标记成记忆格");
        helper.assertTrue(settings.isNoSort(10), "10 号格应该被标记成忽略整理格");

        ItemStack memoryBefore = container.getItem(5).copy();
        ItemStack noSortBefore = container.getItem(10).copy();
        Map<Item, Integer> totalsBefore = totals(snapshot(container));

        // ① 按名称整理
        GodBackpackSorting.sort(container, settings.withSortBy(SortBy.NAME));
        helper.assertTrue(ItemStack.matches(memoryBefore, container.getItem(5)), "记忆格 5 被整理动了");
        helper.assertTrue(ItemStack.matches(noSortBefore, container.getItem(10)), "忽略整理格 10 被整理动了");
        assertSortedBy(helper, container, SortBy.NAME, 5, 10);
        helper.assertTrue(totals(snapshot(container)).equals(totalsBefore), "按名称整理后物品总数对不上");

        // ② 换成按数量整理：多 → 少
        GodBackpackSorting.sort(container, settings.withSortBy(SortBy.COUNT));
        helper.assertTrue(ItemStack.matches(memoryBefore, container.getItem(5)), "按数量整理时记忆格 5 又被动了");
        helper.assertTrue(ItemStack.matches(noSortBefore, container.getItem(10)), "按数量整理时忽略整理格 10 又被动了");
        assertSortedBy(helper, container, SortBy.COUNT, 5, 10);
        helper.assertTrue(totals(snapshot(container)).equals(totalsBefore), "按数量整理后物品总数对不上");
        helper.succeed();
    }

    /** 断言：跳开被标记的两格之后，其余槽位按 sortBy 有序、空堆叠全在最后 */
    private static void assertSortedBy(GameTestHelper helper, GodBackpackContainer container,
                                       SortBy sortBy, int memorySlot, int noSortSlot)
    {
        boolean seenEmpty = false;
        String previousKey = null;
        int previousCount = Integer.MAX_VALUE;
        for (int slot = 0; slot < GodBackpackItem.SIZE; slot++)
        {
            if (slot == memorySlot || slot == noSortSlot)
            {
                continue;
            }
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty())
            {
                seenEmpty = true;
                continue;
            }
            helper.assertFalse(seenEmpty, "按 " + sortBy + " 整理后空堆叠没有排在最后（第 " + slot + " 格）");
            if (sortBy == SortBy.COUNT)
            {
                helper.assertTrue(stack.getCount() <= previousCount,
                        "按数量整理不是「多 → 少」：第 " + slot + " 格 " + stack.getCount() + " > " + previousCount);
                previousCount = stack.getCount();
                continue;
            }
            String key = keyOf(stack, sortBy);
            if (previousKey != null)
            {
                helper.assertTrue(previousKey.compareTo(key) <= 0,
                        "按 " + sortBy + " 整理不是升序：" + previousKey + " → " + key);
            }
            previousKey = key;
        }
    }

    /** 测试自己算一遍排序键（不复用实现里的比较器，避免「自己测自己」） */
    private static String keyOf(ItemStack stack, SortBy sortBy)
    {
        if (sortBy == SortBy.NAME)
        {
            return stack.getHoverName().getString();
        }
        if (sortBy == SortBy.MOD)
        {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        }
        return GodBackpackSorting.tagKey(stack);
    }

    /** 存入时记忆格优先：3 号格记住钻石 → 从玩家物品栏 shift-click 钻石 → 落进 3 号格 */
    @GameTest(template = TEMPLATE)
    public static void memoryPreferredOnInsert(GameTestHelper helper)
    {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack backpack = new ItemStack(Godofthings.GOD_BACKPACK_ITEM.get());
        GodBackpackItem.setSettings(backpack, GodBackpackSettings.defaults()
                .withMemory(3, new ItemStack(Items.DIAMOND)));
        // 背包拿在快捷栏 0 号槽（菜单构造时按槽位解析）
        player.getInventory().setItem(0, backpack);

        GodBackpackMenu menu = new GodBackpackMenu(0, player.getInventory(), backpack);
        // 玩家物品栏第一格（菜单槽位 54 = 物品栏下标 9）放 5 个钻石
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5));

        menu.quickMoveStack(player, 54);

        ItemStack landed = menu.getSlot(3).getItem();
        helper.assertTrue(landed.getItem() == Items.DIAMOND, "钻石没有落进记忆格 3，实际：" + landed);
        helper.assertTrue(landed.getCount() == 5, "记忆格 3 里的钻石数量不对：" + landed.getCount());
        helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "玩家物品栏里的钻石应该被搬空了");
        helper.succeed();
    }
}

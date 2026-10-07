package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.backpack.CuriosCompat;
import com.godofthings.backpack.GodBackpackContainer;
import com.godofthings.backpack.GodBackpackContents;
import com.godofthings.backpack.GodBackpackItem;
import com.godofthings.backpack.GodBackpackMenu;
import com.godofthings.backpack.GodBackpackSettings;
import com.godofthings.backpack.GodBackpackSorting;
import com.godofthings.backpack.SortBy;
import com.godofthings.network.GodBackpackActionPayload;
import com.godofthings.network.GodBackpackSyncPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
 * <p>覆盖四条最怕静默出错的数据层行为：120 格内容的编解码往返（NBT + 封包）、设置组件的编解码往返
 * （NBT + 封包 + 同步包 / 动作包）、整理时「记忆格 / 忽略整理格原地不动」、以及「存入时记忆格优先」。</p>
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

    /**
     * 逐字段比两份设置。
     *
     * <p>不能直接用记录的 {@code equals}：1.21.1 的 {@link ItemStack} 没有重写 {@code equals}
     * （是引用比较），记忆格里的堆叠一编解码就是新对象，记录相等会假阴性。堆叠一律用
     * {@link ItemStack#matches} 比。</p>
     */
    private static void assertSettingsEqual(GameTestHelper helper, String label,
                                            GodBackpackSettings expected, GodBackpackSettings actual)
    {
        helper.assertTrue(expected.sortBy() == actual.sortBy(),
                label + "：排序方式不一致 " + expected.sortBy() + " → " + actual.sortBy());
        helper.assertTrue(expected.ignoreDurability() == actual.ignoreDurability(), label + "：「忽略耐久」不一致");
        helper.assertTrue(expected.ignoreNbt() == actual.ignoreNbt(), label + "：「忽略 NBT」不一致");
        helper.assertTrue(expected.keepSearch() == actual.keepSearch(), label + "：「保留搜索词」不一致");
        helper.assertTrue(expected.searchPhrase().equals(actual.searchPhrase()),
                label + "：搜索词不一致 " + expected.searchPhrase() + " → " + actual.searchPhrase());
        helper.assertTrue(expected.noSort().equals(actual.noSort()),
                label + "：忽略整理格不一致 " + expected.noSort() + " → " + actual.noSort());
        helper.assertTrue(expected.memory().size() == actual.memory().size(),
                label + "：记忆格数量不一致 " + expected.memory().size() + " → " + actual.memory().size());
        for (Map.Entry<Integer, ItemStack> entry : expected.memory().entrySet())
        {
            ItemStack remembered = actual.memory().get(entry.getKey());
            helper.assertTrue(remembered != null && ItemStack.matches(remembered, entry.getValue()),
                    label + "：第 " + entry.getKey() + " 格记住的物品不一致 " + entry.getValue() + " → " + remembered);
        }
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

        // 网络路径：内容 → RegistryFriendlyByteBuf → 内容（数据组件的 networkSynchronized 走这条）
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        GodBackpackContents.STREAM_CODEC.encode(buf, original);
        GodBackpackContents networkRound = GodBackpackContents.STREAM_CODEC.decode(buf);
        helper.assertTrue(original.items().size() == networkRound.items().size(), "封包往返后格数变了");
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            helper.assertTrue(ItemStack.matches(original.items().get(i), networkRound.items().get(i)),
                    "第 " + i + " 格封包往返后不一致：" + original.items().get(i) + " → " + networkRound.items().get(i));
        }
        helper.assertTrue(buf.readableBytes() == 0,
                "内容封包读完后还剩 " + buf.readableBytes() + " 字节（读写不对称）");
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
        assertSettingsEqual(helper, "NBT 往返", original, round);

        // 网络路径：设置 → RegistryFriendlyByteBuf → 设置（同步包与「客户端动作」都靠它）
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        GodBackpackSettings.STREAM_CODEC.encode(buf, original);
        GodBackpackSettings networkRound = GodBackpackSettings.STREAM_CODEC.decode(buf);
        assertSettingsEqual(helper, "封包往返", original, networkRound);
        helper.assertTrue(buf.readableBytes() == 0,
                "设置封包读完后还剩 " + buf.readableBytes() + " 字节（读写不对称）");

        // 同步包整体往返（服务端 → 客户端：设置 + 滚动）
        GodBackpackSyncPayload sync = new GodBackpackSyncPayload(3, original, 5);
        buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        GodBackpackSyncPayload.STREAM_CODEC.encode(buf, sync);
        GodBackpackSyncPayload syncRound = GodBackpackSyncPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(syncRound.containerId() == 3 && syncRound.scroll() == 5,
                "同步包往返后 containerId / scroll 不对：" + syncRound.containerId() + " / " + syncRound.scroll());
        assertSettingsEqual(helper, "同步包往返", original, syncRound.settings());
        helper.assertTrue(buf.readableBytes() == 0,
                "同步包读完后还剩 " + buf.readableBytes() + " 字节（读写不对称）");

        // 动作包（客户端 → 服务端：标记记忆格 / 忽略整理格、搜索词、滚动）也顺手往返一次
        GodBackpackActionPayload action = new GodBackpackActionPayload(
                GodBackpackActionPayload.ACTION_SET_SEARCH, 0, "@minecraft 钻石");
        buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        GodBackpackActionPayload.STREAM_CODEC.encode(buf, action);
        GodBackpackActionPayload actionRound = GodBackpackActionPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(actionRound.equals(action), "动作包往返后不一致：" + actionRound);
        helper.assertTrue(buf.readableBytes() == 0,
                "动作包读完后还剩 " + buf.readableBytes() + " 字节（读写不对称）");
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

    /** 全选记忆 / 全选忽略整理：一次点全部标记，再点一次全部取消（智能切换） */
    @GameTest(template = TEMPLATE)
    public static void selectAllTogglesEverySlot(GameTestHelper helper)
    {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack backpack = new ItemStack(Godofthings.GOD_BACKPACK_ITEM.get());
        player.getInventory().setItem(0, backpack);
        GodBackpackMenu menu = new GodBackpackMenu(0, player.getInventory(), backpack);

        // 往背包里放三样东西（此时 scroll = 0，可见窗口下标 = 背包下标）
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND));
        menu.getSlot(5).set(new ItemStack(Items.IRON_INGOT));
        menu.getSlot(20).set(new ItemStack(Items.APPLE));

        // ① 全选记忆：有物品的格子全部变成记忆格，并记住各自的物品；空格不标记
        menu.selectAllMemory();
        GodBackpackSettings after = GodBackpackItem.settings(backpack);
        helper.assertTrue(after.isMemory(0) && after.isMemory(5) && after.isMemory(20),
                "全选记忆没有标记上「有物品」的格子");
        helper.assertTrue(!after.isMemory(1), "全选记忆不该标记空格");
        helper.assertTrue(after.memory().get(0).is(Items.DIAMOND), "记忆格 0 记住的物品不对");
        helper.assertTrue(after.memory().get(20).is(Items.APPLE), "记忆格 20 记住的物品不对");

        // ② 再点一次全选记忆：全部取消
        menu.selectAllMemory();
        after = GodBackpackItem.settings(backpack);
        helper.assertTrue(!after.isMemory(0) && !after.isMemory(5) && !after.isMemory(20),
                "再次点「全选记忆」应当把所有记忆标记取消");

        // ③ 全选忽略整理：120 格全覆盖；再点一次全部取消
        menu.selectAllNoSort();
        after = GodBackpackItem.settings(backpack);
        helper.assertTrue(after.isNoSort(0) && after.isNoSort(60) && after.isNoSort(119),
                "全选忽略整理没有覆盖全部格子");
        menu.selectAllNoSort();
        after = GodBackpackItem.settings(backpack);
        helper.assertTrue(!after.isNoSort(0) && !after.isNoSort(119),
                "再次点「全选忽略整理」应当把所有标记取消");
        helper.succeed();
    }

    /**
     * 快捷键（B）链路在<b>没装 Curios</b> 的环境下必须安静降级：反射工具返回空、饰品槽解析退化成空堆叠，
     * 全程不抛异常（GameTest 服务器没有 Curios，正好覆盖「模组未装直接跳过」分支，同 GodToolBeltGameTest）。
     */
    @GameTest(template = TEMPLATE)
    public static void openKeyWithoutCuriosIsSafe(GameTestHelper helper)
    {
        // gametest 运行目录里只有 AE2 + guideme，没装 Curios
        helper.assertFalse(CuriosCompat.isLoaded(), "gametest 环境不该装 Curios");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(CuriosCompat.findBackpackInBackSlot(player).isEmpty(),
                "没装 Curios 时查背部槽应返回空堆叠（而不是抛异常）");

        // 按「背包装备在饰品槽」开菜单：解析不到必须退化成空堆叠，不能崩
        GodBackpackMenu menu = new GodBackpackMenu(0, player.getInventory(), GodBackpackMenu.SLOT_CURIOS);
        helper.assertTrue(menu.backpackStack().isEmpty(), "没装 Curios 时饰品槽解析应该退化成空堆叠");
        helper.assertFalse(menu.stillValid(player), "解析不到背包时菜单不该算「还能用」");

        // 兜底顺序不受影响：物品栏里有背包时仍能解析出来（饰品槽 → 主物品栏 → 副手的第 2 档）
        ItemStack backpack = new ItemStack(Godofthings.GOD_BACKPACK_ITEM.get());
        player.getInventory().setItem(0, backpack);
        helper.assertTrue(menu.backpackStack() == backpack, "兜底应该能找到主物品栏里的背包");
        helper.assertTrue(menu.stillValid(player), "背包回到物品栏后菜单应重新可用");
        helper.succeed();
    }
}

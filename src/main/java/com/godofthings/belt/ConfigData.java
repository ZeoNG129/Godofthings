package com.godofthings.belt;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.godofthings.Godofthings;
import com.godofthings.belt.belt.ToolBeltItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 神之皮带的配置（移植自 ToolBelt 2.2.10 的 {@code dev.gigaherz.toolbelt.ConfigData}，BSD-3-Clause）。
 *
 * <p>[本地改动] 删掉与「皮带包 / 缝纫升级 / 铁砧升级 / Curios 槽位切换」相关的三个选项
 * （{@code enableSewingKitSupport} / {@code anvilUpgrading} / {@code gridCrafting} /
 * {@code customBeltSlotMode}），这几块功能整体没有移植；皮带槽恒为开启
 * （{@link #customBeltSlotEnabled} 变成常量，调用点不用改）。</p>
 *
 * <p>另外补了<b>内部白名单</b>：上游是靠本模组反射注入它的配置白名单（ToolBeltCompat），
 * 现在改成在 {@link #isItemStackAllowed(ItemStack)} 里直接放行本模组的五件工具。</p>
 */
public class ConfigData
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static
    {
        final Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static
    {
        final Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    private static Set<ItemStack> blackList = Sets.newHashSet();
    private static Set<ItemStack> whiteList = Sets.newHashSet();
    private static boolean allowAllNonStackableItems = true;

    public static boolean showBeltOnPlayers = true;
    public static float beltItemScale = 0.5f;

    public static boolean releaseToSwap = false;
    public static boolean clipMouseToCircle = true;
    public static boolean allowClickOutsideBounds = true;
    public static boolean displayEmptySlots = true;

    /** 皮带槽恒开启（上游按 Curios 是否存在三态切换；本移植没有 Curios 路径，恒为 true）。 */
    public static final boolean customBeltSlotEnabled = true;

    public static class ServerConfig
    {
        public final ModConfigSpec.ConfigValue<List<? extends String>> whitelist;
        public final ModConfigSpec.ConfigValue<List<? extends String>> blacklist;
        public final ModConfigSpec.BooleanValue allowAllNonStackableItems;

        ServerConfig(ModConfigSpec.Builder builder)
        {
            builder.push("general");
            whitelist = builder
                    .comment("List of items to force-allow placing in the belt. Takes precedence over blacklist.")
                    .defineList("whitelist", Lists.newArrayList(), o -> o instanceof String);
            blacklist = builder
                    .comment("List of items to disallow from placing in the belt. (whitelist takes precedence)")
                    .defineList("blacklist", Lists.newArrayList(), o -> o instanceof String);
            allowAllNonStackableItems = builder
                    .comment("When set to false, stackable items will no longer be allowed by default.")
                    .define("allowAllNonStackableItems", true);
            builder.pop();
        }
    }

    public static class ClientConfig
    {
        public final ModConfigSpec.BooleanValue showBeltOnPlayers;
        public final ModConfigSpec.DoubleValue beltItemScale;
        public final ModConfigSpec.BooleanValue releaseToSwap;
        public final ModConfigSpec.BooleanValue clipMouseToCircle;
        public final ModConfigSpec.BooleanValue allowClickOutsideBounds;
        public final ModConfigSpec.BooleanValue displayEmptySlots;

        ClientConfig(ModConfigSpec.Builder builder)
        {
            builder.comment("Options for customizing the display of tools on the player")
                    .push("display");
            showBeltOnPlayers = builder
                    .comment("If set to FALSE, the belts and tools will NOT draw on players.")
                    .define("showBeltOnPlayers", true);
            beltItemScale = builder
                    .comment("Changes the scale of items on the belt.")
                    .defineInRange("beltItemScale", 0.5, 0.1, 2.0);
            builder.pop();
            builder.comment("Options for customizing the radial menu")
                    .push("menu");
            releaseToSwap = builder
                    .comment("If set to TRUE, releasing the menu key will activate the swap. Requires a click otherwise (default).")
                    .define("releaseToSwap", false);
            clipMouseToCircle = builder
                    .comment("If set to TRUE, the radial menu will try to prevent the mouse from leaving the outer circle.")
                    .define("clipMouseToCircle", false);
            allowClickOutsideBounds = builder
                    .comment("If set to TRUE, the radial menu will allow clicking outside the outer circle to activate the items.")
                    .define("allowClickOutsideBounds", false);
            displayEmptySlots = builder
                    .comment("If set to TRUE, the radial menu will always display all the slots, even when empty.")
                    .define("displayEmptySlots", false);
            builder.pop();
        }
    }

    /** MOD 总线：配置文件加载（首次）时把值搬进静态字段。 */
    public static void onConfigLoad(ModConfigEvent.Loading event)
    {
        reload(event.getConfig());
    }

    /** MOD 总线：配置文件重载时同步静态字段。 */
    public static void onConfigReload(ModConfigEvent.Reloading event)
    {
        reload(event.getConfig());
    }

    private static void reload(ModConfig config)
    {
        if (config.getSpec() == CLIENT_SPEC)
            refreshClient();
        else if (config.getSpec() == SERVER_SPEC)
            refreshServer();
    }

    public static void refreshClient()
    {
        showBeltOnPlayers = CLIENT.showBeltOnPlayers.get();
        beltItemScale = (float) (double) CLIENT.beltItemScale.get();
        releaseToSwap = CLIENT.releaseToSwap.get();
        clipMouseToCircle = CLIENT.clipMouseToCircle.get();
        allowClickOutsideBounds = CLIENT.allowClickOutsideBounds.get();
        displayEmptySlots = CLIENT.displayEmptySlots.get();
    }

    public static void refreshServer()
    {
        blackList = SERVER.blacklist.get().stream().map(ConfigData::parseItemStack).collect(Collectors.toSet());
        whiteList = SERVER.whitelist.get().stream().map(ConfigData::parseItemStack).collect(Collectors.toSet());
        allowAllNonStackableItems = SERVER.allowAllNonStackableItems.getAsBoolean();
    }

    private static ItemStack parseItemStack(String itemString)
    {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemString));
        if (item == Items.AIR)
        {
            LOGGER.warn("Could not find item " + itemString);
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    /**
     * [本地改动] 内部白名单：本模组的五件工具默认就能放进皮带 ——
     * 替代上游由 ToolBeltCompat 反射注入外部模组配置白名单的做法（零配置即可用）。
     */
    private static boolean isGodTool(ItemStack stack)
    {
        Item item = stack.getItem();
        return item == Godofthings.GOD_INVITE.get()
                || item == Godofthings.GOD_CHANGE.get()
                || item == Godofthings.GOD_MEASURE.get()
                || item == Godofthings.GOD_BLACK_BOX.get()
                || item == Godofthings.GOD_BINDER.get();
    }

    public static boolean isItemStackAllowed(final ItemStack stack)
    {
        if (stack.getCount() <= 0)
            return true;

        // 皮带不许放进自己里面（必在内部白名单之前判断）
        if (stack.getItem() instanceof ToolBeltItem)
            return false;

        if (isGodTool(stack))
            return true;

        if (whiteList.stream().anyMatch((s) -> ItemStack.isSameItem(s, stack)))
            return true;

        if (blackList.stream().anyMatch((s) -> ItemStack.isSameItem(s, stack)))
            return false;

        return allowAllNonStackableItems && stack.getMaxStackSize() == 1;
    }
}

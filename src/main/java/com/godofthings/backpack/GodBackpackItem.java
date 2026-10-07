package com.godofthings.backpack;

import com.godofthings.Godofthings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 神之背包：120 格随身仓库（只有一档，没有升级槽 / 升级物品）。
 *
 * <p>内容与设置都存在物品的数据组件里（{@link Godofthings#GOD_BACKPACK_CONTENTS} /
 * {@link Godofthings#GOD_BACKPACK_SETTINGS}），所以背包掉在地上、放进箱子、跨维度都不会丢；
 * 两个组件都带 {@code networkSynchronized}，物品栏同步时设置会一起到客户端
 * （界面另外还会收 {@code GodBackpackSyncPayload} 做实时同步，见 {@link GodBackpackMenu}）。</p>
 *
 * <p><b>三种打开方式</b>：右键手持（主手 / 副手）、快捷键 <b>B</b>（背包在身上任何地方都能开，
 * 见 {@code com.godofthings.client.BackpackKeyHandler}）、以及装备在 Curios 背部槽时同样按 B 打开
 * （背部槽靠物品标签 {@code curios:back} 判定，标签由资源侧提供；反射解析见 {@link CuriosCompat}）。</p>
 *
 * <p>打开时把「背包在哪」写给客户端：主物品栏下标 / {@link #OFFHAND_SLOT} /
 * {@link GodBackpackMenu#SLOT_CURIOS}，客户端据此在本地找回同一个堆叠。</p>
 */
public class GodBackpackItem extends Item
{
    /** 总容量：120 = 13 行 × 9 列 + 3 格（界面可见 9 列 × 6 行，其余靠滚动） */
    public static final int SIZE = 120;

    /** 副手：物品栏槽位用 -1 表示（与神之黑盒同一套约定） */
    public static final int OFFHAND_SLOT = -1;

    public GodBackpackItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        // 只有主手 / 副手拿着的能打开（use 本身只对持握中的物品触发，这里再显式挡一道）
        if (hand != InteractionHand.MAIN_HAND && hand != InteractionHand.OFF_HAND)
        {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            openMenu(serverPlayer, hand == InteractionHand.MAIN_HAND
                    ? serverPlayer.getInventory().selected
                    : OFFHAND_SLOT);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * 打开背包界面（右键与快捷键两条路都走这里）。
     *
     * @param slot 背包在哪：0..35 主物品栏下标 / {@link #OFFHAND_SLOT} 副手 /
     *             {@link GodBackpackMenu#SLOT_CURIOS} Curios 背部槽
     */
    public static void openMenu(ServerPlayer player, int slot)
    {
        player.openMenu(new SimpleMenuProvider(
                        (id, inventory, owner) -> new GodBackpackMenu(id, inventory, slot),
                        Component.translatable("container.godofthings.god_backpack")),
                buf -> buf.writeVarInt(slot));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        // 一句提示：按 B 打开 + 能放进饰品栏背部槽（装了 Curios 才用得上，没装也无害）
        tooltip.add(Component.translatable("tooltip.godofthings.god_backpack.open").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** 背包内容（没有组件时 = 全空） */
    public static GodBackpackContents contents(ItemStack stack)
    {
        GodBackpackContents contents = stack.get(Godofthings.GOD_BACKPACK_CONTENTS.get());
        return contents == null ? GodBackpackContents.empty() : contents;
    }

    /** 背包设置（没有组件时 = 默认设置） */
    public static GodBackpackSettings settings(ItemStack stack)
    {
        GodBackpackSettings settings = stack.get(Godofthings.GOD_BACKPACK_SETTINGS.get());
        return settings == null ? GodBackpackSettings.defaults() : settings;
    }

    /** 写回内容（组件带持久化 + 网络同步 codec） */
    public static void setContents(ItemStack stack, GodBackpackContents contents)
    {
        stack.set(Godofthings.GOD_BACKPACK_CONTENTS.get(), contents);
    }

    /** 写回设置（组件带持久化 + 网络同步 codec） */
    public static void setSettings(ItemStack stack, GodBackpackSettings settings)
    {
        stack.set(Godofthings.GOD_BACKPACK_SETTINGS.get(), settings);
    }
}

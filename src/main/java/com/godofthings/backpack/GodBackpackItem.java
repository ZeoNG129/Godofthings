package com.godofthings.backpack;

import com.godofthings.Godofthings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 神之背包：120 格随身仓库（只有一档，没有升级槽 / 升级物品）。
 *
 * <p>内容与设置都存在物品的数据组件里（{@link Godofthings#GOD_BACKPACK_CONTENTS} /
 * {@link Godofthings#GOD_BACKPACK_SETTINGS}），所以背包掉在地上、放进箱子、跨维度都不会丢；
 * 两个组件都带 {@code networkSynchronized}，物品栏同步时设置会一起到客户端
 * （界面另外还会收 {@code GodBackpackSyncPayload} 做实时同步，见 {@link GodBackpackMenu}）。</p>
 *
 * <p>只有主手 / 副手拿着的背包能打开；打开时把「背包在哪个物品栏槽位」写给客户端
 * （主手 = 当前快捷栏槽位，副手 = {@link #OFFHAND_SLOT}），客户端据此在本地物品栏里找回同一个堆叠。</p>
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
            int slot = hand == InteractionHand.MAIN_HAND ? serverPlayer.getInventory().selected : OFFHAND_SLOT;
            serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inventory, owner) -> new GodBackpackMenu(id, inventory, slot),
                            Component.translatable("container.godofthings.god_backpack")),
                    buf -> buf.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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

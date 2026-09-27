package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.item.GodFavorWandItem;
import com.godofthings.item.WandAcceleration;
import com.godofthings.item.WandLootRefresh;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 神之工具的「荒辰移晷」附加功能（移植自万象担架）：
 * <ul>
 *   <li><b>战利品箱刷新</b>：潜行 + 右键战利品容器 → 用原表重刷（见 {@link WandLootRefresh}）</li>
 *   <li><b>生物加速</b>：潜行 + 右键生物 → 该生物被额外驱动 tick（受 3ms 预算约束）</li>
 *   <li><b>树叶掉落</b>：徒手/工具破坏树叶时有极小概率掉出神之工具</li>
 * </ul>
 * 倍率存在物品自身的 {@code CUSTOM_DATA} 里（键 {@code godofthings:wand_speed}），
 * 由 {@link #cycleSpeed} 切换（H 键 → 见 WandAccelerationHandler 的按键处理）。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class WandFeatureHandler
{
    private static final String TAG_SPEED = "godofthings:wand_speed";
    /** 树叶掉手杖的概率（上游默认 0.00001 = 十万分之一；这里调到 1/2000 让玩家真的能见到） */
    private static final float LEAF_DROP_CHANCE = 1.0F / 2000.0F;

    private WandFeatureHandler() {}

    private static boolean holdingWand(Player player)
    {
        return player.getMainHandItem().getItem() instanceof GodFavorWandItem
                || player.getOffhandItem().getItem() instanceof GodFavorWandItem;
    }

    private static ItemStack wandOf(Player player)
    {
        if (player.getMainHandItem().getItem() instanceof GodFavorWandItem)
        {
            return player.getMainHandItem();
        }
        return player.getOffhandItem().getItem() instanceof GodFavorWandItem ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    /** 当前配置的加速倍率（默认 {@link WandAcceleration#DEFAULT_SPEED}） */
    public static int getSpeed(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return WandAcceleration.DEFAULT_SPEED;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int v = tag.getInt(TAG_SPEED);
        return v <= 0 ? WandAcceleration.DEFAULT_SPEED : Math.min(v, WandAcceleration.MAX_SPEED);
    }

    /** H 键：倍率翻倍（超过上限回到默认），写进物品并 actionbar 提示 */
    public static void cycleSpeed(ServerPlayer player)
    {
        ItemStack stack = wandOf(player);
        if (stack.isEmpty())
        {
            return;
        }
        int next = getSpeed(stack) * 2;
        if (next > WandAcceleration.MAX_SPEED)
        {
            next = WandAcceleration.DEFAULT_SPEED;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_SPEED, next);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.displayClientMessage(Component.translatable("chat.godofthings.wand.speed", next)
                .withStyle(ChatFormatting.AQUA), true);
    }

    /** 右键方块：战利品容器优先刷新，其次才是加速 */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown()
                || !holdingWand(player)
                || !(player.level() instanceof ServerLevel level))
        {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        if (WandLootRefresh.isSupported(level.getBlockEntity(pos)))
        {
            boolean ok = WandLootRefresh.tryRefresh(player, pos);
            player.displayClientMessage(Component.translatable(ok
                    ? "chat.godofthings.wand.loot_refreshed"
                    : "chat.godofthings.wand.loot_no_source").withStyle(ok ? ChatFormatting.GOLD : ChatFormatting.RED), true);
            event.setCanceled(true);
            return;
        }
        int speed = WandAcceleration.mark(level, pos, getSpeed(wandOf(player)), false);
        player.displayClientMessage(Component.translatable("chat.godofthings.wand.accel",
                pos.getX(), pos.getY(), pos.getZ(), speed,
                WandAcceleration.DEFAULT_DURATION_TICKS / 20).withStyle(ChatFormatting.AQUA), true);
        event.setCanceled(true);
    }

    /** 右键生物：加速该生物 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown()
                || !holdingWand(player)
                || !(event.getTarget() instanceof LivingEntity target)
                || target instanceof Player)
        {
            return;
        }
        int speed = WandAcceleration.markEntity(player.serverLevel(), target, getSpeed(wandOf(player)));
        player.displayClientMessage(Component.translatable("chat.godofthings.wand.accel_mob",
                target.getDisplayName(), speed).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        event.setCanceled(true);
    }

    /** 破坏树叶时极小概率掉出神之工具 */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event)
    {
        if (!(event.getBreaker() instanceof ServerPlayer player) || !holdingWand(player))
        {
            return;
        }
        if (!event.getState().is(BlockTags.LEAVES))
        {
            return;
        }
        if (player.getRandom().nextFloat() >= LEAF_DROP_CHANCE)
        {
            return;
        }
        event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                (ServerLevel) event.getLevel(),
                event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
                new ItemStack(event.getLevel().getRandom().nextBoolean()
                        ? Godofthings.GOD_FAVOR_WAND.get() : Godofthings.GOD_FAVOR_WAND.get())));
    }
}
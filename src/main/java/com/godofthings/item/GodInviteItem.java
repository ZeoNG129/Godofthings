package com.godofthings.item;

import com.godofthings.Godofthings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 请神：对任意生物右键使用，使其获得无限血量（打不死）。
 * <p>
 * 「具备受击功能」的实现：不拦截伤害，让 {@link LivingEntity#hurt} 完整走完
 * 受伤动画 / 无敌帧 / 击退 / 音效流程；在 {@link LivingDamageEvent.Post}
 * （血量已扣减、死亡检查之前）把血量恢复满，因此永远打不死、死亡永不触发。
 * <p>
 * 标记存于 {@link LivingEntity#getPersistentData()}（键 {@link #MARKER_KEY}），随实体持久保存。
 */
public class GodInviteItem extends Item
{
    /** 生物请神标记键（存于 Entity.getPersistentData()，随实体保存）。 */
    public static final String MARKER_KEY = "QingShen";

    public GodInviteItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand)
    {
        if (player.level().isClientSide())
        {
            return InteractionResult.SUCCESS;
        }
        boolean invited = isInvited(target);
        if (invited)
        {
            // 再次右键：取消请神，恢复可被杀死的正常状态
            target.getPersistentData().remove(MARKER_KEY);
            target.setHealth(target.getMaxHealth());
            player.displayClientMessage(Component.translatable("message.godofthings.god_invite.remove", target.getDisplayName()), true);
        }
        else
        {
            // 请神：打上标记并立即回满血
            target.getPersistentData().putBoolean(MARKER_KEY, true);
            target.setHealth(target.getMaxHealth());
            player.displayClientMessage(Component.translatable("message.godofthings.god_invite.apply", target.getDisplayName()), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** 该生物是否已被请神（无限血量）。 */
    public static boolean isInvited(LivingEntity entity)
    {
        return entity.getPersistentData().getBoolean(MARKER_KEY);
    }

    @EventBusSubscriber(modid = Godofthings.MODID)
    public static class QingShenHandler
    {
        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent.Post event)
        {
            LivingEntity entity = event.getEntity();
            if (isInvited(entity) && entity.isAlive())
            {
                // 伤害已结算（受击反馈均已触发），回满血杜绝死亡
                entity.setHealth(entity.getMaxHealth());
            }
        }
    }
}

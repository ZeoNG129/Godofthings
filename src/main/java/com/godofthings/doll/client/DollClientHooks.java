package com.godofthings.doll.client;

import com.godofthings.doll.network.TwoIDialoguePayload;
import com.godofthings.doll.network.TwoIItemDialoguePayload;
import com.godofthings.doll.network.TwoIStoryLibraryStatePayload;
import com.godofthings.doll.network.TwoIStoryStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * 玩偶客户端包处理 —— 对应源码 {@code ClientStateReceiver} 的 TWO_I 段。
 *
 * <p><b>这个类为什么存在、为什么可以被 DollNetwork 引用</b>：
 * {@code Minecraft} / {@code Screen} 都是 {@code @OnlyIn(Dist.CLIENT)}，
 * 专用服务端加载任何直接引用它们的类都会被 {@code RuntimeDistCleaner} 拦下
 * （v7.0.0 曾把客户端逻辑直接写进 {@code DollNetwork} 的 lambda，拿着玩偶就闪退）。
 * 所以网络类只做 {@code enqueueWork(() -> DollClientHooks.handleXxx(payload))} 转发：
 * playToClient 的处理器只在客户端执行，本类在那一刻才首次类加载，服务端永不加载它。</p>
 *
 * <p>本类<b>没有</b> {@code @EventBusSubscriber}（没有订阅方法，也不需要被扫描），
 * 纯被动的静态工具类 —— 这也符合仓库规则「客户端专用类禁止挂注解」。</p>
 */
public final class DollClientHooks
{
    private DollClientHooks()
    {
    }

    /** 对话界面：已开着就更新好感/邀请状态，否则按 open 标志新建 */
    public static void handleDialogue(TwoIDialoguePayload payload)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof com.godofthings.doll.client.TwoIDialogueScreen screen
                && screen.entityId() == payload.entityId())
        {
            screen.updateGiftStatus(payload.inviteReady(), payload.giftScore());
            return;
        }
        if (minecraft.screen instanceof com.godofthings.doll.client.TwoIStoryScreen story
                && story.entityId() == payload.entityId())
        {
            story.updateGiftStatus(payload.inviteReady(), payload.giftScore());
            return;
        }
        if (payload.open())
        {
            minecraft.setScreen(new com.godofthings.doll.client.TwoIDialogueScreen(
                    payload.entityId(), payload.inviteReady(), payload.giftScore()));
        }
    }

    /** 物品形态的对话界面（右键玩偶物品，服务端校验主人后下发） */
    public static void handleItemDialogue(TwoIItemDialoguePayload payload)
    {
        Minecraft.getInstance().setScreen(com.godofthings.doll.client.TwoIDialogueScreen.forItem(
                payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                payload.bond(), payload.invitationUsed()));
    }

    /** 剧情推进：对话界面 → 进入故事；故事界面 → 处理选项结果 / 失效 */
    public static void handleStoryState(TwoIStoryStatePayload payload)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof com.godofthings.doll.client.TwoIDialogueScreen screen
                && screen.entityId() == payload.entityId())
        {
            if (payload.result() == 0)
            {
                screen.openStory(payload.sessionId(), payload.chapter(), payload.bond(), payload.round());
            }
            else if (payload.result() == 2 && payload.sessionId() == 0L)
            {
                screen.onStoryUnavailable();
            }
            return;
        }
        if (minecraft.screen instanceof com.godofthings.doll.client.TwoIStoryScreen screen
                && screen.entityId() == payload.entityId())
        {
            if (payload.result() == 1)
            {
                screen.onChoiceResult(payload.sessionId(), payload.bond(), payload.round());
            }
            else if (payload.result() == 2)
            {
                screen.onStoryUnavailable(payload.sessionId(), payload.round());
            }
        }
    }

    /** 故事库的章节解锁进度回包 */
    public static void handleStoryLibraryState(TwoIStoryLibraryStatePayload payload)
    {
        if (Minecraft.getInstance().screen instanceof com.godofthings.doll.client.TwoIStoryLibraryScreen screen)
        {
            screen.acceptLibrary(payload.requestId(), payload.completedMask());
        }
    }
}

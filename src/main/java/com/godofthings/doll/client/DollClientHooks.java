package com.godofthings.doll.client;

import com.godofthings.doll.network.TwoIDialoguePayload;
import com.godofthings.doll.network.TwoIItemDialoguePayload;
import com.godofthings.doll.network.TwoIStoryLibraryStatePayload;
import com.godofthings.doll.network.TwoIStoryStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * 客户端侧的玩偶网络包处理。
 *
 * <p>原 mod 把这些塞在 {@code ClientStateReceiver} 里，和胶囊、手杖、范围加速、
 * 万象方块等十几个系统的客户端处理混在一起。移植版独立成类，只处理玩偶这 4 个包。</p>
 *
 * <p><b>为什么单独一个类</b>：{@code DollNetwork} 是<b>双端共用</b>的，
 * 直接在里面 {@code import} 客户端界面类会让专用服务端在加载网络类时就
 * {@code NoClassDefFoundError}（本仓库踩过的坑）。所以网络层只做
 * {@code context.enqueueWork(() -> DollClientHooks.xxx(payload))}，
 * 真正的界面操作全部收在这里，由 NeoForge 保证只在客户端执行。</p>
 */
public final class DollClientHooks
{
    private DollClientHooks()
    {
    }

    /** 对话界面：已经开着就只更新状态，没开才新建 */
    public static void openDialogue(TwoIDialoguePayload payload)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TwoIDialogueScreen screen
                && screen.entityId() == payload.entityId())
        {
            screen.updateGiftStatus(payload.inviteReady(), payload.giftScore());
            return;
        }
        if (minecraft.screen instanceof TwoIStoryScreen story
                && story.entityId() == payload.entityId())
        {
            story.updateGiftStatus(payload.inviteReady(), payload.giftScore());
            return;
        }
        if (payload.open())
        {
            minecraft.setScreen(new TwoIDialogueScreen(
                    payload.entityId(), payload.inviteReady(), payload.giftScore()));
        }
    }

    /** 物品形态的对话界面（右键玩偶物品） */
    public static void openItemDialogue(TwoIItemDialoguePayload payload)
    {
        Minecraft.getInstance().setScreen(TwoIDialogueScreen.forItem(
                payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                payload.bond(), payload.invitationUsed()));
    }

    /** 剧情推进：对话界面 → 打开故事；故事界面 → 处理选择结果 */
    public static void onStoryState(TwoIStoryStatePayload payload)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TwoIDialogueScreen screen
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
        if (minecraft.screen instanceof TwoIStoryScreen screen
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

    /** 故事库的解锁进度回包 */
    public static void onStoryLibraryState(TwoIStoryLibraryStatePayload payload)
    {
        if (Minecraft.getInstance().screen instanceof TwoIStoryLibraryScreen screen)
        {
            screen.acceptLibrary(payload.requestId(), payload.completedMask());
        }
    }
}

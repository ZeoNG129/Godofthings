package com.godofthings.client;

import com.godofthings.client.screen.GodNoteScreen;
import com.godofthings.note.NoteShelf;
import com.godofthings.network.GodNoteMessages;
import net.minecraft.client.Minecraft;

/**
 * 客户端侧的神之便签镜像：服务端每次下发（登录后拉取、界面里改动后的回执）都整册替换，
 * 界面与悬浮窗都读这一份，避免两处各自维护状态。
 */
public final class ClientNoteCache
{
    private static NoteShelf shelf = new NoteShelf();
    private static boolean requested = false;
    /** 上一次发出去的整册签名（见 {@link #shouldPush}） */
    private static String lastSentSignature = "";

    private ClientNoteCache() {}

    /** 当前镜像（永不为 null） */
    public static NoteShelf shelf()
    {
        return shelf;
    }

    /** 用服务端下发的副本整册替换本地镜像 */
    public static void apply(NoteShelf incoming)
    {
        if (incoming != null)
        {
            shelf = incoming;
        }
    }

    /**
     * 进世界后只拉一次数据。
     * <p>由客户端主动发起（而不是服务端登录时推送），这样纯原版客户端连进来时服务端不会
     * 往它发它不认识的包。</p>
     */
    public static void requestOnce()
    {
        if (!requested)
        {
            requested = true;
            GodNoteMessages.requestSync();
        }
    }

    /** 离开世界时复位，下次进来重新拉 */
    public static void resetRequest()
    {
        requested = false;
    }

    /**
     * 「这次改动和上次发出去的一样吗」——一样就别再发一遍包。
     *
     * <p>整册替换的包里存的是内容本身，所以内容没变时重发纯属浪费（设置页连点、界面 tick 里的
     * 兜底提交都会走到 push）。签名在 {@link NoteShelf#signature()}（公共层）里维护，
     * **必须覆盖所有会变的内容**——曾经漏掉子任务，加子任务的推送被这里吞掉，用户实测
     * 「子任务重进界面就没了」「折叠按钮无效」。</p>
     */
    public static boolean shouldPush(NoteShelf shelf)
    {
        String signature = shelf.signature();
        if (signature.equals(lastSentSignature))
        {
            return false;
        }
        lastSentSignature = signature;
        return true;
    }

    /** 打开便签界面（物品右键 / 指令 / 快捷键最终都落到这里） */
    public static void openScreen()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
        {
            return;
        }
        mc.setScreen(new GodNoteScreen());
    }
}

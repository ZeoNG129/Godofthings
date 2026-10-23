package com.godofthings.doll.network;

import com.godofthings.Godofthings;
import com.godofthings.doll.entity.TwoIDollEntity;
import com.godofthings.doll.item.TwoIDollItem;
import com.godofthings.doll.init.DollComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 玩偶系统的网络通道。
 *
 * <p>原 mod 把 15 个 payload 全塞进一个 2397 行的 {@code Network} 类，和手杖、胶囊、
 * 范围加速、万象方块等十几套系统混在一起。移植时只把玩偶这 14 个抽出来独立成类，
 * 每个 payload 一个文件（放在同包的 record 里），本类只负责<b>注册</b>与<b>收发</b>。</p>
 *
 * <p>版本号 {@code "1"}：玩偶是全新通道，与主模组既有的 {@code "1"} 不冲突
 * （NeoForge 的 registrar 版本是按 payload 类型独立的）。</p>
 */
public final class DollNetwork
{
    private DollNetwork()
    {
    }

    public static void register(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");

        // ---- 服务端 → 客户端 ----
        registrar.playToClient(TwoIDialoguePayload.TYPE, TwoIDialoguePayload.STREAM_CODEC,
                DollNetwork::handleDialogue);
        registrar.playToClient(TwoIItemDialoguePayload.TYPE, TwoIItemDialoguePayload.STREAM_CODEC,
                DollNetwork::handleItemDialogue);
        registrar.playToClient(TwoIStoryStatePayload.TYPE, TwoIStoryStatePayload.STREAM_CODEC,
                DollNetwork::handleStoryState);
        registrar.playToClient(TwoIStoryLibraryStatePayload.TYPE, TwoIStoryLibraryStatePayload.STREAM_CODEC,
                DollNetwork::handleStoryLibraryState);

        // ---- 客户端 → 服务端 ----
        registrar.playToServer(TwoIDollReclaimPayload.TYPE, TwoIDollReclaimPayload.STREAM_CODEC,
                DollNetwork::handleReclaim);
        registrar.playToServer(TwoIDollGiftPayload.TYPE, TwoIDollGiftPayload.STREAM_CODEC,
                DollNetwork::handleGift);
        registrar.playToServer(TwoIDollInvitePayload.TYPE, TwoIDollInvitePayload.STREAM_CODEC,
                DollNetwork::handleInvite);
        registrar.playToServer(TwoIDollPatPayload.TYPE, TwoIDollPatPayload.STREAM_CODEC,
                DollNetwork::handlePat);
        registrar.playToServer(TwoIDollStoryStartPayload.TYPE, TwoIDollStoryStartPayload.STREAM_CODEC,
                DollNetwork::handleStoryStart);
        registrar.playToServer(TwoIDollStoryPayload.TYPE, TwoIDollStoryPayload.STREAM_CODEC,
                DollNetwork::handleStoryChoice);
        registrar.playToServer(TwoIDollDeployPayload.TYPE, TwoIDollDeployPayload.STREAM_CODEC,
                DollNetwork::handleDeploy);
        registrar.playToServer(TwoIDollRemoteReclaimPayload.TYPE, TwoIDollRemoteReclaimPayload.STREAM_CODEC,
                DollNetwork::handleRemoteReclaim);
        registrar.playToServer(TwoIInteractionPayload.TYPE, TwoIInteractionPayload.STREAM_CODEC,
                DollNetwork::handleInteraction);
        registrar.playToServer(TwoIStoryLibraryRequestPayload.TYPE, TwoIStoryLibraryRequestPayload.STREAM_CODEC,
                DollNetwork::handleStoryLibraryRequest);
    }

    // ---------------- 服务端 → 客户端 ----------------

    public static void sendTwoIDialogue(ServerPlayer player, int entityId, boolean inviteReady, int giftScore)
    {
        PacketDistributor.sendToPlayer(player,
                new TwoIDialoguePayload(entityId, inviteReady, giftScore, true));
    }

    public static void updateTwoIDialogue(ServerPlayer player, int entityId, boolean inviteReady, int giftScore)
    {
        PacketDistributor.sendToPlayer(player,
                new TwoIDialoguePayload(entityId, inviteReady, giftScore, false));
    }

    /** 物品形态的对话界面（右键玩偶物品时打开） */
    public static void sendTwoIItemDialogue(ServerPlayer player, InteractionHand hand, ItemStack stack)
    {
        if (!player.getUUID().equals(stack.get(DollComponents.TWO_I_OWNER.get())))
        {
            return;
        }
        PacketDistributor.sendToPlayer(player, new TwoIItemDialoguePayload(
                hand == InteractionHand.OFF_HAND,
                TwoIDollItem.getBond(stack),
                stack.getOrDefault(DollComponents.TWO_I_INVITATION_USED.get(), false)));
    }

    public static void sendTwoIStoryState(ServerPlayer player, int entityId, long sessionId,
                                      int chapter, int bond, int result)
    {
        sendTwoIStoryState(player, entityId, sessionId, chapter, bond, result, 0);
    }

    public static void sendTwoIStoryState(ServerPlayer player, int entityId, long sessionId,
                                      int chapter, int bond, int result, int round)
    {
        PacketDistributor.sendToPlayer(player,
                new TwoIStoryStatePayload(entityId, sessionId, chapter, bond, result, round));
    }

    public static void sendTwoIStoryLibraryState(ServerPlayer player, int requestId, int completedMask)
    {
        PacketDistributor.sendToPlayer(player, new TwoIStoryLibraryStatePayload(requestId, completedMask));
    }

    // ---------------- 客户端 → 服务端 ----------------

    public static void sendTwoIDollReclaim(int entityId)
    {
        PacketDistributor.sendToServer(new TwoIDollReclaimPayload(entityId));
    }

    public static void sendTwoIDollGift(int entityId)
    {
        PacketDistributor.sendToServer(new TwoIDollGiftPayload(entityId));
    }

    public static void sendTwoIDollInvite(int entityId)
    {
        PacketDistributor.sendToServer(new TwoIDollInvitePayload(entityId));
    }

    public static void sendTwoIDollPat(int entityId)
    {
        PacketDistributor.sendToServer(new TwoIDollPatPayload(entityId));
    }

    public static void sendTwoIDollStoryStart(int entityId)
    {
        PacketDistributor.sendToServer(new TwoIDollStoryStartPayload(entityId));
    }

    public static void sendTwoIDollStory(int entityId, long sessionId, int choice)
    {
        PacketDistributor.sendToServer(new TwoIDollStoryPayload(entityId, sessionId, choice));
    }

    public static void sendTwoIDollDeploy(InteractionHand hand)
    {
        PacketDistributor.sendToServer(new TwoIDollDeployPayload(hand == InteractionHand.OFF_HAND));
    }

    public static void sendTwoIDollRemoteReclaim()
    {
        PacketDistributor.sendToServer(new TwoIDollRemoteReclaimPayload());
    }

    public static void sendTwoIInteraction(int entityId, boolean active)
    {
        PacketDistributor.sendToServer(new TwoIInteractionPayload(entityId, active));
    }

    public static void requestTwoIStoryLibrary(int requestId)
    {
        PacketDistributor.sendToServer(new TwoIStoryLibraryRequestPayload(requestId));
    }

    // ---------------- 处理 ----------------

    private static void handleReclaim(TwoIDollReclaimPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player
                && player.serverLevel().getEntity(payload.entityId()) instanceof TwoIDollEntity doll
                && !doll.isRemoved() && doll.distanceToSqr(player) <= 64.0D)
        {
            doll.reclaim(player);
        }
    }

    private static void handleGift(TwoIDollGiftPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player
                && player.serverLevel().getEntity(payload.entityId()) instanceof TwoIDollEntity doll)
        {
            doll.acceptGift(player);
        }
    }

    private static void handleInvite(TwoIDollInvitePayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player
                && player.serverLevel().getEntity(payload.entityId()) instanceof TwoIDollEntity doll)
        {
            doll.acceptInvitation(player);
        }
    }

    private static void handlePat(TwoIDollPatPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player
                && player.serverLevel().getEntity(payload.entityId()) instanceof TwoIDollEntity doll)
        {
            doll.patBy(player);
        }
    }

    private static void handleStoryStart(TwoIDollStoryStartPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player)
        {
            if (player.serverLevel().getEntity(payload.entityId()) instanceof TwoIDollEntity doll)
            {
                doll.startStory(player);
            }
            else
            {
                sendTwoIStoryState(player, payload.entityId(), 0L, 0, 0, 2);
            }
        }
    }

    private static void handleStoryChoice(TwoIDollStoryPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player)
        {
            if (payload.choice() >= 0 && payload.choice() < 9)
            {
                if (player.serverLevel().getEntity(payload.entityId()) instanceof TwoIDollEntity doll)
                {
                    doll.storyChoice(player, payload.sessionId(), payload.choice());
                }
                else
                {
                    sendTwoIStoryState(player, payload.entityId(), payload.sessionId(),
                            0, 0, 2, payload.choice() / 3);
                }
            }
            else
            {
                sendTwoIStoryState(player, payload.entityId(), payload.sessionId(),
                        0, 0, 2, Math.clamp(payload.choice() / 3L, 0, 2));
            }
        }
    }

    private static void handleDeploy(TwoIDollDeployPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player)
        {
            TwoIDollItem.deploy(player,
                    payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        }
    }

    private static void handleRemoteReclaim(TwoIDollRemoteReclaimPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player))
        {
            return;
        }
        net.minecraft.world.phys.Vec3 start = player.getEyePosition();
        net.minecraft.world.phys.Vec3 direction = player.getLookAngle().normalize();
        TwoIDollEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (TwoIDollEntity doll : TwoIDollEntity.trackedFor(player.getServer(), player.getUUID()))
        {
            if (doll.isRemoved() || doll.level() != player.level())
            {
                continue;
            }
            net.minecraft.world.phys.Vec3 to = doll.getBoundingBox().getCenter().subtract(start);
            double projection = to.dot(direction);
            if (projection < 0.0D || projection >= bestDistance)
            {
                continue;
            }
            net.minecraft.world.phys.Vec3 point = start.add(direction.scale(projection));
            if (point.distanceToSqr(doll.getBoundingBox().getCenter()) <= 4.0D)
            {
                best = doll;
                bestDistance = projection;
            }
        }

        if (best != null)
        {
            best.reclaim(player);
        }
    }

    private static void handleInteraction(TwoIInteractionPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player
                && player.serverLevel().getEntity(payload.entityId()) instanceof TwoIDollEntity doll)
        {
            doll.keepInteraction(player, payload.active());
        }
    }

    private static void handleStoryLibraryRequest(TwoIStoryLibraryRequestPayload payload,
                                                  IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player)
        {
            sendTwoIStoryLibraryState(player, payload.requestId(),
                    com.godofthings.doll.entity.TwoIStoryData.get(player.getServer()).completedMask(player.getUUID()));
        }
    }

    // ---- 客户端侧处理 ----
    // 照源码 Network -> ClientStateReceiver 的结构：本类的方法体里【禁止】出现任何
    // net.minecraft.client.* 引用（Minecraft / Screen 都是 @OnlyIn(CLIENT)）。
    // DollNetwork 在专用服务端也会被加载（主类构造器的 addListener），而
    // RuntimeDistCleaner 在类转换期扫描常量池，见到 @OnlyIn 类引用就把整条 invokedynamic
    // 的 bootstrap 拦下来 —— v7.0.0 的「拿着玩偶闪退 / GameTest 建服失败」就是这个：
    // 曾经直接在 lambda 里写 Minecraft.getInstance()。
    // 处理器只在客户端执行， DollClientHooks 只会在那一刻首次类加载，服务端永不加载它。

    private static void handleDialogue(TwoIDialoguePayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> com.godofthings.doll.client.DollClientHooks.handleDialogue(payload));
    }

    private static void handleItemDialogue(TwoIItemDialoguePayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> com.godofthings.doll.client.DollClientHooks.handleItemDialogue(payload));
    }

    private static void handleStoryState(TwoIStoryStatePayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> com.godofthings.doll.client.DollClientHooks.handleStoryState(payload));
    }

    private static void handleStoryLibraryState(TwoIStoryLibraryStatePayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> com.godofthings.doll.client.DollClientHooks.handleStoryLibraryState(payload));
    }
}
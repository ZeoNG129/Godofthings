package com.godofthings.construction.network;

import com.godofthings.Godofthings;
import com.godofthings.construction.ConstructionActions;
import com.godofthings.construction.ConstructionForce;
import com.godofthings.construction.GodConstructionItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 神之建筑：「Ctrl 强制施工」的客户端 → 服务端包。
 *
 * <p>服务端读不到 Ctrl（原版只同步 Shift），所以在<b>客户端</b>的 {@code onItemUseFirst} 里判定
 * Ctrl 之后用这个包把「刚才那一下是要施工的」告诉服务端。包体只有三样东西：</p>
 * <ul>
 *   <li>{@code pos}：被点的方块坐标；</li>
 *   <li>{@code face}：被点的面（{@code UseOnContext#getClickedFace()}，建造 / 销毁都要它）；</li>
 *   <li>{@code action}：点下去那一刻的模式（0 建造 / 1 销毁 / 2 替换，见 {@link GodConstructionItem}）。</li>
 * </ul>
 *
 * <p>服务端收到后做三件事（见 {@link #handle}）：</p>
 * <ol>
 *   <li>{@link ConstructionForce#mark} 记一个一次性标记 —— 紧随其后的那一发原版右键会在
 *       {@code onItemUseFirst} 里把它消费掉并返回 SUCCESS，用来<b>挡住方块开界面</b>；</li>
 *   <li>{@link ConstructionActions#perform} <b>当场按模式发起作业</b>，等于在客户端按下 Ctrl 的那一刻
 *       就已经施工了，不依赖后面那一发右键（万一那发没到，标记也会自己过期）；</li>
 *   <li>兜底再关一次容器界面：万一某处插队让原版那发右键<b>先</b>到了、界面已经被打开，
 *       这里把菜单关掉（开着界面时玩家点不到世界里的方块，所以此刻开着的只可能是刚才那一下开的）。
 *       顺序正常时这一步不会触发（那时根本没有菜单打开）。</li>
 * </ol>
 *
 * <p>顺序为什么成立：客户端先跑 {@code Item#onItemUseFirst}（强制包在这里发出），
 * 原版那发 {@code ServerboundUseItemOnPacket} 是在它返回之后由
 * {@code MultiPlayerGameMode#startPrediction} 才发出的；两包走同一条连接、落到服务端主线程队列里
 * 按到达顺序处理。</p>
 *
 * <p>服务端只认<b>玩家手上拿着的本族物品</b>（主手优先、其次副手）：网络数据不可信，
 * 手上没有棒子就整个丢掉。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public record ConstructionForcePayload(BlockPos pos, Direction face, int action) implements CustomPacketPayload
{
    public static final Type<ConstructionForcePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "god_construction_force"));

    public static final StreamCodec<FriendlyByteBuf, ConstructionForcePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) ->
            {
                buf.writeBlockPos(payload.pos());
                buf.writeEnum(payload.face());
                buf.writeVarInt(payload.action());
            },
            buf -> new ConstructionForcePayload(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readVarInt()));

    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ConstructionForcePayload.TYPE, ConstructionForcePayload.STREAM_CODEC,
                ConstructionForcePayload::handle);
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    /** 客户端发：Ctrl + 右键「会开界面的方块」= 强制施工 */
    public static void send(BlockPos pos, Direction face, int action)
    {
        PacketDistributor.sendToServer(new ConstructionForcePayload(pos, face, action));
    }

    public static void handle(ConstructionForcePayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
            {
                return;
            }
            // 网络数据不可信：手上（主手优先、其次副手）没有本族物品就整个丢掉
            ItemStack wand = GodConstructionItem.findWand(player);
            if (!GodConstructionItem.isWand(wand))
            {
                return;
            }
            if (payload.face() == null)
            {
                return;
            }
            Level level = player.level();
            // 批量操作里一律用不可变坐标：后面 BFS 会到处 relative，可变 pos 被复用会串格
            BlockPos clickedPos = payload.pos().immutable();
            if (!level.mayInteract(player, clickedPos))
            {
                return; // 顺手挡掉越界 / 无权限（和真正施工时的判定同一套）
            }

            // 1) 一次性标记：紧随其后的那一发原版右键只用来挡界面，不再重复施工
            ConstructionForce.mark(player);
            // 2) 当场按模式发起作业（模式取客户端点下去那一刻的值，越界就退回物品上的权威值）
            int mode = payload.action() >= 0 && payload.action() < GodConstructionItem.MODE_COUNT
                    ? payload.action()
                    : GodConstructionItem.getMode(wand);
            ConstructionActions.perform(player, wand, level, clickedPos, payload.face(), mode);
            // 3) 兜底：顺序万一不成立（界面已经被那一发右键打开过），这里把它关掉
            if (player.hasContainerOpen())
            {
                player.closeContainer();
            }
        });
    }
}

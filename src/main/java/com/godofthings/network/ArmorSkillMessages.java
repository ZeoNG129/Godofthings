package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillCategory;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkillDef;
import com.godofthings.armor.skill.ArmorSkills;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

/**
 * 神之套装技能树的网络通道（两端都要注册）。
 * <ul>
 *   <li>C2S {@code ArmorSkillActionPayload}：单个技能操作
 *       （0=开/关，1=+1级，2=+10级，3=设为指定等级（拖动进度条用））。</li>
 *   <li>C2S {@code ArmorSkillBulkPayload}：整列操作（分类序号，-1=全部；等级 0=全关）。</li>
 *   <li>C2S {@code ArmorSkillRequestPayload}：打开界面时拉一次权威值。</li>
 *   <li>S2C {@code ArmorSkillSyncPayload}：服务端在登录 / 每次变更 / 收到请求时推送整张技能表。</li>
 * </ul>
 * 服务端为唯一权威：等级一律 <b>clamp 到 [0, 上限]</b>，<b>满级后再升级不再回绕到 1 级</b>。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ArmorSkillMessages
{
    /** 开 / 关 */
    public static final int ACTION_TOGGLE = 0;
    /** 等级 +1（到上限即停，不回绕） */
    public static final int ACTION_LEVEL_UP = 1;
    /** 等级 +10（到上限即停，不回绕） */
    public static final int ACTION_LEVEL_UP_10 = 2;
    /** 设为 value 指定的绝对等级（拖动进度条） */
    public static final int ACTION_SET_LEVEL = 3;

    /** 整列操作里代表"全部技能"的分类序号 */
    public static final int CATEGORY_ALL = -1;

    private ArmorSkillMessages() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ArmorSkillActionPayload.TYPE, ArmorSkillActionPayload.STREAM_CODEC, ArmorSkillActionPayload::handle);
        registrar.playToServer(ArmorSkillBulkPayload.TYPE, ArmorSkillBulkPayload.STREAM_CODEC, ArmorSkillBulkPayload::handle);
        registrar.playToServer(ArmorSkillRequestPayload.TYPE, ArmorSkillRequestPayload.STREAM_CODEC, ArmorSkillRequestPayload::handle);
        registrar.playToClient(ArmorSkillSyncPayload.TYPE, ArmorSkillSyncPayload.STREAM_CODEC, ArmorSkillSyncPayload::handle);
        registrar.playToServer(ArmorSkillZonePayload.TYPE, ArmorSkillZonePayload.STREAM_CODEC, ArmorSkillZonePayload::handle);
    }

    // ---- 客户端发送入口 ----

    public static void sendAction(String skillId, int action)
    {
        sendAction(skillId, action, 0);
    }

    public static void sendAction(String skillId, int action, int value)
    {
        PacketDistributor.sendToServer(new ArmorSkillActionPayload(skillId, action, value));
    }

    public static void sendBulk(int categoryOrdinal, int level)
    {
        PacketDistributor.sendToServer(new ArmorSkillBulkPayload(categoryOrdinal, level));
    }

    /** 客户端触发选区操作（true = 放置，false = 挖掘） */
    public static void sendZone(boolean place)
    {
        PacketDistributor.sendToServer(new ArmorSkillZonePayload(place));
    }

    public static void requestSync()
    {
        PacketDistributor.sendToServer(new ArmorSkillRequestPayload());
    }

    /** 服务端把权威技能表推给某个玩家。 */
    public static void sendSync(ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, new ArmorSkillSyncPayload(
                new HashMap<>(ArmorSkillData.get(player))));
    }

    // ---- 包定义 ----

    public record ArmorSkillActionPayload(String skillId, int action, int value) implements CustomPacketPayload
    {
        public static final Type<ArmorSkillActionPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "armor_skill_action"));

        public static final StreamCodec<ByteBuf, ArmorSkillActionPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ArmorSkillActionPayload::skillId,
                ByteBufCodecs.VAR_INT, ArmorSkillActionPayload::action,
                ByteBufCodecs.VAR_INT, ArmorSkillActionPayload::value,
                ArmorSkillActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ArmorSkillActionPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer player))
                {
                    return;
                }
                ArmorSkillDef def = ArmorSkills.get(msg.skillId());
                if (def == null)
                {
                    return; // 未定义的技能 id：忽略（客户端塞进来的非法值）
                }
                int current = ArmorSkillData.level(player, def.id());
                int next = switch (msg.action())
                {
                    // 升级到上限即停（不回绕到 1 级）
                    case ACTION_LEVEL_UP -> current <= 0 ? ArmorSkills.UNLOCK_LEVEL
                            : Math.min(def.maxLevel(), current + 1);
                    case ACTION_LEVEL_UP_10 -> current <= 0 ? ArmorSkills.UNLOCK_LEVEL
                            : Math.min(def.maxLevel(), current + 10);
                    case ACTION_SET_LEVEL -> Math.max(0, Math.min(def.maxLevel(), msg.value()));
                    default -> current; // 开/关：等级不变，只翻开关状态（下面单独处理）
                };
                if (msg.action() == ACTION_TOGGLE)
                {
                    // 用户要求：关闭后再打开必须保留等级，所以只翻开关、不碰等级
                    ArmorSkillData.setEnabled(player, def.id(), !ArmorSkillData.isEnabled(player, def.id()));
                }
                else
                {
                    ArmorSkillData.setLevel(player, def.id(), next);
                }
                com.godofthings.handler.ArmorSkillHandler.refresh(player);
                sendSync(player);
            });
        }
    }

    public record ArmorSkillBulkPayload(int categoryOrdinal, int level) implements CustomPacketPayload
    {
        public static final Type<ArmorSkillBulkPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "armor_skill_bulk"));

        public static final StreamCodec<ByteBuf, ArmorSkillBulkPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ArmorSkillBulkPayload::categoryOrdinal,
                ByteBufCodecs.VAR_INT, ArmorSkillBulkPayload::level,
                ArmorSkillBulkPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ArmorSkillBulkPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer player))
                {
                    return;
                }
                // >0 = 设为该等级；<0 = 只关闭（保留等级）；0 = 清除该列（重置）
                int raw = msg.level();
                int level = raw == 0 ? 0 : (raw < 0 ? -1 : Math.min(ArmorSkills.BASE_MAX_LEVEL, raw));
                ArmorSkillCategory[] categories = ArmorSkillCategory.values();
                if (msg.categoryOrdinal() == CATEGORY_ALL)
                {
                    for (ArmorSkillCategory category : categories)
                    {
                        if (ArmorSkills.isAvailable(category))
                        {
                            ArmorSkillData.setCategoryLevel(player, category, level);
                        }
                    }
                }
                else if (msg.categoryOrdinal() >= 0 && msg.categoryOrdinal() < categories.length)
                {
                    ArmorSkillCategory category = categories[msg.categoryOrdinal()];
                    if (ArmorSkills.isAvailable(category))
                    {
                        ArmorSkillData.setCategoryLevel(player, category, level);
                    }
                }
                com.godofthings.handler.ArmorSkillHandler.refresh(player);
                sendSync(player);
            });
        }
    }

    public record ArmorSkillRequestPayload() implements CustomPacketPayload
    {
        public static final Type<ArmorSkillRequestPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "armor_skill_request"));

        public static final StreamCodec<ByteBuf, ArmorSkillRequestPayload> STREAM_CODEC =
                StreamCodec.unit(new ArmorSkillRequestPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ArmorSkillRequestPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer player)
                {
                    sendSync(player);
                }
            });
        }
    }

    public record ArmorSkillSyncPayload(Map<String, Integer> levels) implements CustomPacketPayload
    {
        public static final Type<ArmorSkillSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "armor_skill_sync"));

        public static final StreamCodec<ByteBuf, ArmorSkillSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
                ArmorSkillSyncPayload::levels,
                ArmorSkillSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ArmorSkillSyncPayload msg, IPayloadContext ctx)
        {
            // 只写客户端镜像，不碰玩家附件（附件是服务端权威数据）
            ctx.enqueueWork(() -> ArmorSkillData.setClientLevels(msg.levels()));
        }
    }

    /** 选区操作触发（机械共鸣）：C2S */
    public record ArmorSkillZonePayload(boolean place) implements CustomPacketPayload
    {
        public static final Type<ArmorSkillZonePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "armor_skill_zone"));

        public static final StreamCodec<ByteBuf, ArmorSkillZonePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ArmorSkillZonePayload::place,
                ArmorSkillZonePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ArmorSkillZonePayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer player)
                {
                    com.godofthings.handler.ArmorSkillHandler.startZoneOperation(player, msg.place());
                }
            });
        }
    }
}

package com.godofthings.wondrous.network;

import com.godofthings.Godofthings;
import com.godofthings.wondrous.config.WondrousConfig;
import com.godofthings.wondrous.content.acceleration.PermanentAccelerationHistory;
import com.godofthings.wondrous.content.apotheosis.ApotheosisStaffSettings;
import com.godofthings.wondrous.content.apotheosis.ApotheosisTableData;
import com.godofthings.wondrous.content.entity.EntityTimerAcceleration;
import com.godofthings.wondrous.content.entity.StaffPhaseManager;
import com.godofthings.wondrous.content.item.StaffTutorialData;
import com.godofthings.wondrous.content.entity.TimeStopManager;
import com.godofthings.wondrous.content.entity.WondrousStaffAcceleration;
import com.godofthings.wondrous.content.entity.WondrousStaffSummoning;
import com.godofthings.wondrous.content.item.WondrousStaffItem;
import com.godofthings.wondrous.content.range.RangeAccelerationSavedData;
import com.godofthings.wondrous.init.ModItems;
import com.godofthings.wondrous.init.StaffComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 荒辰移晷之杖的网络通道（照抄上游 Network.java 中杖相关的 payload 段）。
 * [本地改动] 只移植杖本体功能：剔除 TwoI 对话 / 胶囊 / 星空 / 流体收集 /
 * 独立传送 / 无尽万物乱真表（Myriad）相关 payload；配置读取从 StretcherConfig
 * 换成 WondrousConfig；组件从 StretcherComponents 换成 StaffComponents；
 * 时间加速开关组件从 BeefTimeAccelerationEnabledComponent 换成
 * {@link StaffComponents#STAFF_TIME_ACCELERATION}。
 */
public final class WondrousNetwork
{
    private static final int MAX_RECLAIM_ENTRIES = 512;

    private WondrousNetwork()
    {
    }

    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, path);
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path)
    {
        return new CustomPacketPayload.Type<>(id(path));
    }

    // ------------------------------------------------------------------
    // 教程提示（首次手持杖 / 换挡 / 打开 X 界面）
    // ------------------------------------------------------------------

    public record StaffTutorialPayload() implements CustomPacketPayload
    {
        public static final Type<StaffTutorialPayload> TYPE = WondrousNetwork.type("staff_tutorial");
        public static final StreamCodec<RegistryFriendlyByteBuf, StaffTutorialPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> { }, buf -> new StaffTutorialPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    /** 玩家打开 X 杖配置界面时上行一次。 */
    public record StaffTutorialOpenedPayload() implements CustomPacketPayload
    {
        public static final Type<StaffTutorialOpenedPayload> TYPE = WondrousNetwork.type("staff_tutorial_opened");
        public static final StreamCodec<RegistryFriendlyByteBuf, StaffTutorialOpenedPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> { }, buf -> new StaffTutorialOpenedPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // 换挡 / 模式（Shift+滚轮与 X 界面）
    // ------------------------------------------------------------------

    public record WondrousStaffSpeedPayload(int speed, int mode, boolean accelerationEnabled, boolean offhand)
            implements CustomPacketPayload
    {
        public static final Type<WondrousStaffSpeedPayload> TYPE = WondrousNetwork.type("wondrous_staff_speed");

        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffSpeedPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, WondrousStaffSpeedPayload::speed,
                        ByteBufCodecs.VAR_INT, WondrousStaffSpeedPayload::mode,
                        ByteBufCodecs.BOOL, WondrousStaffSpeedPayload::accelerationEnabled,
                        ByteBufCodecs.BOOL, WondrousStaffSpeedPayload::offhand,
                        WondrousStaffSpeedPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // 时停
    // ------------------------------------------------------------------

    public record StaffTimeStopPayload(boolean offhand) implements CustomPacketPayload
    {
        public static final Type<StaffTimeStopPayload> TYPE = WondrousNetwork.type("staff_time_stop");
        public static final StreamCodec<RegistryFriendlyByteBuf, StaffTimeStopPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> buf.writeBoolean(value.offhand()),
                        buf -> new StaffTimeStopPayload(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record TimeStopStatePayload(int remaining) implements CustomPacketPayload
    {
        public static final Type<TimeStopStatePayload> TYPE = WondrousNetwork.type("time_stop_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, TimeStopStatePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, TimeStopStatePayload::remaining, TimeStopStatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // 实体计时器设置（生物加速的计时器模式）
    // ------------------------------------------------------------------

    public record EntityTimerSettingsPayload(boolean timers, int speed, boolean offhand) implements CustomPacketPayload
    {
        public static final Type<EntityTimerSettingsPayload> TYPE = WondrousNetwork.type("entity_timer_settings");
        public static final StreamCodec<RegistryFriendlyByteBuf, EntityTimerSettingsPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, EntityTimerSettingsPayload::timers,
                        ByteBufCodecs.VAR_INT, EntityTimerSettingsPayload::speed,
                        ByteBufCodecs.BOOL, EntityTimerSettingsPayload::offhand, EntityTimerSettingsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // X 界面的杖功能开关（召唤 / 战利品刷新 / 相位 / 辐射）
    // ------------------------------------------------------------------

    /** X 界面编辑的杖功能开关。 */
    public record WondrousStaffFeaturesPayload(boolean summonEnabled, boolean lootRefresh, boolean offhand)
            implements CustomPacketPayload
    {
        public static final Type<WondrousStaffFeaturesPayload> TYPE = WondrousNetwork.type("wondrous_staff_features");
        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffFeaturesPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::summonEnabled,
                        ByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::lootRefresh,
                        ByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::offhand,
                        WondrousStaffFeaturesPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    /** 客户端上行相位 / 相位交互开关。 */
    public record WondrousStaffPhasePayload(boolean phaseEnabled, boolean interactionEnabled, boolean offhand)
            implements CustomPacketPayload
    {
        public static final Type<WondrousStaffPhasePayload> TYPE = WondrousNetwork.type("wondrous_staff_phase");
        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffPhasePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, WondrousStaffPhasePayload::phaseEnabled,
                        ByteBufCodecs.BOOL, WondrousStaffPhasePayload::interactionEnabled,
                        ByteBufCodecs.BOOL, WondrousStaffPhasePayload::offhand,
                        WondrousStaffPhasePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    /** 服务端权威的相位状态；手持杖变化或登录时推送。 */
    public record StaffPhaseStatePayload(boolean phaseEnabled, boolean interactionEnabled)
            implements CustomPacketPayload
    {
        public static final Type<StaffPhaseStatePayload> TYPE = WondrousNetwork.type("staff_phase_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, StaffPhaseStatePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, StaffPhaseStatePayload::phaseEnabled,
                        ByteBufCodecs.BOOL, StaffPhaseStatePayload::interactionEnabled,
                        StaffPhaseStatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    /** 校验过的单杖辐射开关。 */
    public record WondrousStaffRadiationPayload(boolean enabled, boolean offhand) implements CustomPacketPayload
    {
        public static final Type<WondrousStaffRadiationPayload> TYPE = WondrousNetwork.type("wondrous_staff_radiation");
        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffRadiationPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, WondrousStaffRadiationPayload::enabled,
                        ByteBufCodecs.BOOL, WondrousStaffRadiationPayload::offhand, WondrousStaffRadiationPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // APOC 附魔台（Apothic Enchanting 桥接）
    // ------------------------------------------------------------------

    public record ApotheosisTierPayload(String tier) implements CustomPacketPayload
    {
        public static final Type<ApotheosisTierPayload> TYPE = WondrousNetwork.type("apotheosis_tier");
        public static final StreamCodec<RegistryFriendlyByteBuf, ApotheosisTierPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ApotheosisTierPayload::tier, ApotheosisTierPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record ApotheosisOptionsPayload(boolean selectionMode, int eterna, int quanta, int arcana,
                                           int clues, boolean offhand) implements CustomPacketPayload
    {
        public static final Type<ApotheosisOptionsPayload> TYPE = WondrousNetwork.type("apotheosis_options");
        public static final StreamCodec<RegistryFriendlyByteBuf, ApotheosisOptionsPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ApotheosisOptionsPayload::selectionMode,
                        ByteBufCodecs.VAR_INT, ApotheosisOptionsPayload::eterna,
                        ByteBufCodecs.VAR_INT, ApotheosisOptionsPayload::quanta,
                        ByteBufCodecs.VAR_INT, ApotheosisOptionsPayload::arcana,
                        ByteBufCodecs.VAR_INT, ApotheosisOptionsPayload::clues,
                        ByteBufCodecs.BOOL, ApotheosisOptionsPayload::offhand,
                        ApotheosisOptionsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record ApotheosisTablePayload(BlockPos pos, boolean offhand) implements CustomPacketPayload
    {
        public static final Type<ApotheosisTablePayload> TYPE = WondrousNetwork.type("apotheosis_table");
        public static final StreamCodec<RegistryFriendlyByteBuf, ApotheosisTablePayload> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, ApotheosisTablePayload::pos,
                        ByteBufCodecs.BOOL, ApotheosisTablePayload::offhand, ApotheosisTablePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // 召唤 / 回收
    // ------------------------------------------------------------------

    /** 上行选中的实体注册 ID 列表；上限由服务端把关。 */
    public record WondrousStaffSummonPayload(List<String> entityIds, boolean offhand) implements CustomPacketPayload
    {
        public static final Type<WondrousStaffSummonPayload> TYPE = WondrousNetwork.type("wondrous_staff_summon");
        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffSummonPayload> STREAM_CODEC =
                StreamCodec.of((buffer, value) -> {
                    int size = Math.min(value.entityIds().size(), WondrousStaffSummoning.MAX_SELECTION);
                    buffer.writeVarInt(size);
                    for (int i = 0; i < size; i++) buffer.writeUtf(value.entityIds().get(i), 256);
                    buffer.writeBoolean(value.offhand());
                }, buffer -> {
                    int size = buffer.readVarInt();
                    if (size < 0 || size > WondrousStaffSummoning.MAX_SELECTION)
                    {
                        throw new IllegalArgumentException("Invalid summon selection size: " + size);
                    }
                    List<String> ids = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) ids.add(buffer.readUtf(256));
                    return new WondrousStaffSummonPayload(ids, buffer.readBoolean());
                });

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record WondrousStaffRecallPayload(boolean offhand) implements CustomPacketPayload
    {
        public static final Type<WondrousStaffRecallPayload> TYPE = WondrousNetwork.type("wondrous_staff_recall");
        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffRecallPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> buf.writeBoolean(value.offhand()),
                        buf -> new WondrousStaffRecallPayload(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // 回收器（杖持有的个人模式）
    // ------------------------------------------------------------------

    /** 回收器界面里的一条范围 / 机器标记。 */
    public record ReclaimerEntry(boolean range, UUID id, UUID owner, String ownerName,
                                 ResourceLocation dimension, BlockPos pos, String label)
            implements CustomPacketPayload
    {
        public static final Type<ReclaimerEntry> TYPE = WondrousNetwork.type("reclaimer_entry");
        public static final StreamCodec<RegistryFriendlyByteBuf, ReclaimerEntry> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value.range());
                    buf.writeUUID(value.id());
                    buf.writeUUID(value.owner());
                    buf.writeUtf(value.ownerName(), 64);
                    ResourceLocation.STREAM_CODEC.encode(buf, value.dimension());
                    buf.writeBlockPos(value.pos());
                    buf.writeUtf(value.label(), 128);
                }, buf -> new ReclaimerEntry(buf.readBoolean(), buf.readUUID(), buf.readUUID(), buf.readUtf(64),
                        ResourceLocation.STREAM_CODEC.decode(buf), buf.readBlockPos(), buf.readUtf(128)));

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record ReclaimerStatePayload(List<ReclaimerEntry> entries, boolean personal) implements CustomPacketPayload
    {
        public static final Type<ReclaimerStatePayload> TYPE = WondrousNetwork.type("reclaimer_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, ReclaimerStatePayload> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value.personal());
                    int count = Math.min(MAX_RECLAIM_ENTRIES, value.entries().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) ReclaimerEntry.STREAM_CODEC.encode(buf, value.entries().get(i));
                }, buf -> {
                    boolean personal = buf.readBoolean();
                    int count = buf.readVarInt();
                    if (count < 0 || count > MAX_RECLAIM_ENTRIES) throw new IllegalArgumentException("Invalid reclaim count");
                    List<ReclaimerEntry> entries = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) entries.add(ReclaimerEntry.STREAM_CODEC.decode(buf));
                    return new ReclaimerStatePayload(entries, personal);
                });

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record ReclaimerRequestPayload(boolean personal) implements CustomPacketPayload
    {
        public static final Type<ReclaimerRequestPayload> TYPE = WondrousNetwork.type("reclaimer_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, ReclaimerRequestPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> buf.writeBoolean(value.personal()),
                        buf -> new ReclaimerRequestPayload(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record ReclaimerActionPayload(boolean range, UUID id, boolean personal) implements CustomPacketPayload
    {
        public static final Type<ReclaimerActionPayload> TYPE = WondrousNetwork.type("reclaimer_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, ReclaimerActionPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> {
                    buf.writeBoolean(value.range());
                    buf.writeUUID(value.id());
                    buf.writeBoolean(value.personal());
                }, buf -> new ReclaimerActionPayload(buf.readBoolean(), buf.readUUID(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // 维度级视觉时钟（客户端云层流动）
    // ------------------------------------------------------------------

    public record TimeAccelerationStatePayload(String dimension, int speed) implements CustomPacketPayload
    {
        public static final Type<TimeAccelerationStatePayload> TYPE = WondrousNetwork.type("time_acceleration_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, TimeAccelerationStatePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, TimeAccelerationStatePayload::dimension,
                        ByteBufCodecs.VAR_INT, TimeAccelerationStatePayload::speed,
                        TimeAccelerationStatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------
    // 注册
    // ------------------------------------------------------------------

    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("22");
        registrar.playToServer(StaffTutorialOpenedPayload.TYPE, StaffTutorialOpenedPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player)
                    {
                        StaffTutorialData.get(player.getServer()).markUiOpened(player.getUUID());
                    }
                }));
        registrar.playToClient(StaffTutorialPayload.TYPE, StaffTutorialPayload.STREAM_CODEC,
                (payload, context) -> ClientStateReceiver.handleStaffTutorial());
        registrar.playToClient(TimeAccelerationStatePayload.TYPE, TimeAccelerationStatePayload.STREAM_CODEC,
                (payload, context) -> ClientStateReceiver.handleTimeAcceleration(payload));
        registrar.playToClient(StaffPhaseStatePayload.TYPE, StaffPhaseStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientStateReceiver.handleStaffPhase(payload)));
        registrar.playToServer(WondrousStaffSpeedPayload.TYPE, WondrousStaffSpeedPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleWondrousStaffSpeed(payload, context)));
        registrar.playToServer(StaffTimeStopPayload.TYPE, StaffTimeStopPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleStaffTimeStop(payload, context)));
        registrar.playToClient(TimeStopStatePayload.TYPE, TimeStopStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientStateReceiver.handleTimeStop(payload)));
        registrar.playToServer(EntityTimerSettingsPayload.TYPE, EntityTimerSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player) || !WondrousConfig.serverStaffAcceleration()) return;
                    ItemStack held = player.getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                    if (!held.is(ModItems.WONDROUS_STAFF.get())) return;
                    held.set(StaffComponents.ENTITY_TIMER_MODE.get(), payload.timers());
                    held.set(StaffComponents.ENTITY_TIMER_SPEED.get(),
                            EntityTimerAcceleration.normalizeSpeed(payload.speed()));
                }));
        registrar.playToServer(WondrousStaffFeaturesPayload.TYPE, WondrousStaffFeaturesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleWondrousStaffFeatures(payload, context)));
        registrar.playToServer(WondrousStaffPhasePayload.TYPE, WondrousStaffPhasePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleWondrousStaffPhase(payload, context)));
        registrar.playToServer(WondrousStaffRadiationPayload.TYPE, WondrousStaffRadiationPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player)
                    {
                        com.godofthings.wondrous.compat.StaffRadiationProtection.configure(player, payload.offhand(), payload.enabled());
                    }
                }));
        registrar.playToServer(ApotheosisTierPayload.TYPE, ApotheosisTierPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleApotheosisTier(payload, context)));
        registrar.playToServer(ApotheosisOptionsPayload.TYPE, ApotheosisOptionsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleApotheosisOptions(payload, context)));
        registrar.playToServer(ApotheosisTablePayload.TYPE, ApotheosisTablePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleApotheosisTable(payload, context)));
        registrar.playToServer(WondrousStaffSummonPayload.TYPE, WondrousStaffSummonPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleWondrousStaffSummon(payload, context)));
        registrar.playToServer(WondrousStaffRecallPayload.TYPE, WondrousStaffRecallPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleWondrousStaffRecall(payload, context)));
        registrar.playToServer(ReclaimerRequestPayload.TYPE, ReclaimerRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        sendReclaimerState((ServerPlayer) context.player(), payload.personal())));
        registrar.playToServer(ReclaimerActionPayload.TYPE, ReclaimerActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleReclaimerAction(payload, context)));
        registrar.playToClient(ReclaimerStatePayload.TYPE, ReclaimerStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientStateReceiver.handleReclaimer(payload)));
        ApotheosisNetwork.register(registrar);
    }

    // ------------------------------------------------------------------
    // 服务端 handler
    // ------------------------------------------------------------------

    private static void handleWondrousStaffSpeed(WondrousStaffSpeedPayload payload,
                                                 net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!WondrousConfig.serverStaffAcceleration()) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() != ModItems.WONDROUS_STAFF.get()) return;
        int speed = switch (payload.speed()) {
            case 0, 2, 4, 16, 32, 64, 128, 256, 512, 1024 -> payload.speed();
            default -> WondrousStaffAcceleration.DEFAULT_GEAR;
        };
        int mode = payload.mode() >= 0 && payload.mode() < WondrousStaffAcceleration.STAFF_MODE_COUNT
                ? payload.mode()
                : WondrousStaffAcceleration.STAFF_MODE_NORMAL;
        held.set(StaffComponents.WONDROUS_STAFF_SPEED.get(), speed);
        WondrousStaffAcceleration.setMode(held, mode);
        held.set(StaffComponents.STAFF_TIME_ACCELERATION.get(), payload.accelerationEnabled());
        if (StaffTutorialData.get(player.getServer()).markHintShown(player.getUUID(),
                StaffTutorialData.HINT_GEAR_CHANGE))
        {
            sendStaffTutorial(player);
        }
    }

    private static void handleStaffTimeStop(StaffTimeStopPayload payload,
                                            net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player) || !WondrousConfig.serverTimeStop()) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.WONDROUS_STAFF.get())) return;
        TimeStopManager.start(player);
    }

    private static void handleWondrousStaffFeatures(WondrousStaffFeaturesPayload payload,
                                                    net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.WONDROUS_STAFF.get())) return;
        held.set(StaffComponents.WONDROUS_STAFF_SUMMON_ENABLED.get(), payload.summonEnabled());
        held.set(StaffComponents.WONDROUS_STAFF_LOOT_REFRESH.get(),
                payload.lootRefresh() && WondrousConfig.enableStaffLootRefresh());
    }

    private static void handleWondrousStaffPhase(WondrousStaffPhasePayload payload,
                                                 net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.WONDROUS_STAFF.get())) return;
        held.set(StaffComponents.WONDROUS_STAFF_PHASE_ENABLED.get(), payload.phaseEnabled());
        held.set(StaffComponents.WONDROUS_STAFF_PHASE_INTERACTION.get(), payload.interactionEnabled());
        StaffPhaseManager.apply(player);
    }

    private static boolean apotheosisAllowed()
    {
        // 加成 / 选台开关只需要 Apothic Enchanting（独立模组）。
        return WondrousConfig.enableApotheosisCompat() && ApotheosisStaffSettings.isEnchantingAvailable();
    }

    private static void handleApotheosisTier(ApotheosisTierPayload payload,
                                             net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player)
                || !WondrousConfig.enableApotheosisCompat()
                || !ApotheosisStaffSettings.isApotheosisLoaded()) return;
        if (!(player.getMainHandItem().getItem() instanceof WondrousStaffItem)
                && !(player.getOffhandItem().getItem() instanceof WondrousStaffItem)) return;
        String tier = payload.tier().toLowerCase(java.util.Locale.ROOT);
        if (!Set.of("haven", "frontier", "ascent", "summit", "pinnacle").contains(tier)) return;
        if (!ApotheosisStaffSettings.setTier(player, tier))
        {
            player.displayClientMessage(Component.translatable("msg.godofthings.apotheosis.failed"), true);
        }
        else
        {
            player.displayClientMessage(Component.translatable("msg.godofthings.apotheosis.tier_set", tier), true);
        }
        InteractionHand hand = player.getMainHandItem().getItem() instanceof WondrousStaffItem
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ApotheosisNetwork.sendSettings(player, hand);
    }

    private static void handleApotheosisOptions(ApotheosisOptionsPayload payload,
                                                net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player) || !apotheosisAllowed()) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.WONDROUS_STAFF.get())) return;
        var boost = new ApotheosisTableData.Boost(
                payload.eterna(), payload.quanta(), payload.arcana(), payload.clues()).normalized();
        ApotheosisStaffSettings.setBoost(held, boost);
        ApotheosisStaffSettings.setSelectionMode(held, payload.selectionMode());
        if (payload.selectionMode())
        {
            held.set(StaffComponents.RANGE_FILTER_MARKING_MODE.get(), false);
            held.set(StaffComponents.RANGE_PLACEMENT_MODE.get(), false);
        }
        ApotheosisTableData data = ApotheosisTableData.get(player.getServer());
        if (data.updateOwned(player.getUUID(), boost) > 0)
        {
            ApotheosisTableData.refreshOpenMenus(player.getServer());
        }
        player.inventoryMenu.broadcastChanges();
        ApotheosisNetwork.sendSettings(player, hand);
    }

    private static void handleApotheosisTable(ApotheosisTablePayload payload,
                                              net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ApotheosisStaffSettings.markTable(player, hand, payload.pos());
    }

    private static void handleWondrousStaffSummon(WondrousStaffSummonPayload payload,
                                                  net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.WONDROUS_STAFF.get())) return;
        WondrousStaffSummoning.summon(player, held, payload.entityIds());
    }

    private static void handleWondrousStaffRecall(WondrousStaffRecallPayload payload,
                                                  net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack held = player.getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        WondrousStaffSummoning.recall(player, held);
    }

    private static boolean canManage(ServerPlayer player, boolean personal)
    {
        if (player == null || (!personal && !WondrousConfig.serverRemoteReclaimer())) return false;
        var item = ModItems.WONDROUS_STAFF.get();
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    private static void sendReclaimerState(ServerPlayer player, boolean personal)
    {
        if (!canManage(player, personal)) return;
        List<ReclaimerEntry> result = new ArrayList<>();
        RangeAccelerationSavedData data = RangeAccelerationSavedData.get(player.getServer());
        for (RangeAccelerationSavedData.Field field : data.operatorFields())
        {
            if (personal && !field.owner().equals(player.getUUID())) continue;
            if (result.size() >= MAX_RECLAIM_ENTRIES) break;
            result.add(new ReclaimerEntry(true, field.id(), field.owner(), ownerName(player, field.owner()),
                    field.dimension(), field.center(), (field.name().isBlank() ? "时间流逝" : field.name())
                            + " · x" + field.speed()));
        }
        for (var entry : PermanentAccelerationHistory.get(player.getServer())
                .activeEntries(personal ? player.getUUID() : null))
        {
            if (result.size() >= MAX_RECLAIM_ENTRIES) break;
            result.add(new ReclaimerEntry(false, entry.id(), entry.owner(), ownerName(player, entry.owner()),
                    entry.dimension(), entry.pos(), entry.label()));
        }
        result.sort(java.util.Comparator.comparing(ReclaimerEntry::ownerName)
                .thenComparing(entry -> entry.owner().toString()).thenComparing(ReclaimerEntry::label));
        PacketDistributor.sendToPlayer(player, new ReclaimerStatePayload(result, personal));
    }

    private static String ownerName(ServerPlayer viewer, UUID owner)
    {
        ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(owner);
        if (online != null) return online.getGameProfile().getName();
        var cache = viewer.getServer().getProfileCache();
        return cache == null ? owner.toString()
                : cache.get(owner).map(com.mojang.authlib.GameProfile::getName).orElse(owner.toString());
    }

    private static void handleReclaimerAction(ReclaimerActionPayload payload,
                                              net.neoforged.neoforge.network.handling.IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!canManage(player, payload.personal())) return;
        boolean removed = false;
        if (payload.range())
        {
            var ranges = RangeAccelerationSavedData.get(player.getServer());
            removed = payload.personal()
                    ? ranges.reclaim(player.getServer(), player.getUUID(), payload.id()) != null
                    : ranges.reclaimByOperator(player.getServer(), payload.id()) != null;
        }
        else
        {
            removed = PermanentAccelerationHistory.get(player.getServer())
                    .reclaim(player.getServer(), payload.id(), payload.personal() ? player.getUUID() : null);
        }
        player.displayClientMessage(Component.translatable(removed
                ? "msg.godofthings.reclaimer.removed"
                : "msg.godofthings.reclaimer.missing"), true);
        sendReclaimerState(player, payload.personal());
    }

    // ------------------------------------------------------------------
    // 发送端
    // ------------------------------------------------------------------

    public static void sendWondrousStaffSpeed(int speed, int mode, boolean accelerationEnabled,
                                              InteractionHand hand)
    {
        PacketDistributor.sendToServer(new WondrousStaffSpeedPayload(
                speed, mode, accelerationEnabled, hand == InteractionHand.OFF_HAND));
    }

    public static void sendStaffTimeStop(InteractionHand hand)
    {
        PacketDistributor.sendToServer(new StaffTimeStopPayload(hand == InteractionHand.OFF_HAND));
    }

    public static void sendTimeStopState(ServerPlayer player, int remaining)
    {
        PacketDistributor.sendToPlayer(player, new TimeStopStatePayload(Math.max(0, remaining)));
    }

    public static void sendEntityTimerSettings(boolean timers, int speed, InteractionHand hand)
    {
        PacketDistributor.sendToServer(new EntityTimerSettingsPayload(timers, speed, hand == InteractionHand.OFF_HAND));
    }

    public static void sendWondrousStaffFeatures(boolean summonEnabled, boolean lootRefresh, InteractionHand hand)
    {
        PacketDistributor.sendToServer(new WondrousStaffFeaturesPayload(
                summonEnabled, lootRefresh, hand == InteractionHand.OFF_HAND));
    }

    public static void sendWondrousStaffPhase(boolean phaseEnabled, boolean interactionEnabled, InteractionHand hand)
    {
        PacketDistributor.sendToServer(new WondrousStaffPhasePayload(
                phaseEnabled, interactionEnabled, hand == InteractionHand.OFF_HAND));
    }

    public static void sendWondrousStaffRadiation(boolean enabled, InteractionHand hand)
    {
        PacketDistributor.sendToServer(new WondrousStaffRadiationPayload(enabled, hand == InteractionHand.OFF_HAND));
    }

    public static void sendStaffPhaseState(ServerPlayer player, boolean phaseEnabled, boolean interactionEnabled)
    {
        PacketDistributor.sendToPlayer(player, new StaffPhaseStatePayload(phaseEnabled, interactionEnabled));
    }

    public static void sendWondrousStaffSummon(List<String> entityIds, InteractionHand hand)
    {
        PacketDistributor.sendToServer(new WondrousStaffSummonPayload(
                List.copyOf(entityIds), hand == InteractionHand.OFF_HAND));
    }

    public static void recallWondrousStaff(InteractionHand hand)
    {
        PacketDistributor.sendToServer(new WondrousStaffRecallPayload(hand == InteractionHand.OFF_HAND));
    }

    public static void sendStaffTutorial(ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, new StaffTutorialPayload());
    }

    public static void sendStaffTutorialOpened()
    {
        PacketDistributor.sendToServer(new StaffTutorialOpenedPayload());
    }

    public static void sendTimeAccelerationState(ServerLevel level, int speed)
    {
        PacketDistributor.sendToPlayersInDimension(level, new TimeAccelerationStatePayload(
                level.dimension().location().toString(), Math.max(0, speed)));
    }

    public static void requestReclaimer()
    {
        PacketDistributor.sendToServer(new ReclaimerRequestPayload(false));
    }

    public static void requestPersonalReclaimer()
    {
        PacketDistributor.sendToServer(new ReclaimerRequestPayload(true));
    }

    public static void reclaimTarget(boolean range, UUID id, boolean personal)
    {
        PacketDistributor.sendToServer(new ReclaimerActionPayload(range, id, personal));
    }

    public static void sendApotheosisTier(String tier)
    {
        PacketDistributor.sendToServer(new ApotheosisTierPayload(tier));
    }

    public static void sendApotheosisOptions(boolean selectionMode, ApotheosisTableData.Boost boost,
                                             InteractionHand hand)
    {
        PacketDistributor.sendToServer(new ApotheosisOptionsPayload(selectionMode, boost.eterna(),
                boost.quanta(), boost.arcana(), boost.clues(), hand == InteractionHand.OFF_HAND));
    }
}

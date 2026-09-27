package com.godofthings.beef.stretcher.network;

import com.godofthings.beef.stretcher.UselessStretcherMod;
import com.godofthings.beef.stretcher.content.item.StaffTutorialData;
import com.godofthings.beef.stretcher.content.entity.WondrousStaffSummoning;
import com.godofthings.beef.stretcher.init.StretcherComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * 荒辰移晷之杖的网络包。
 *
 * <p>逐字照抄 UselessStretcher（万象担架）扩展模组的 {@code network.Network}，
 * 只裁掉「万象模具」（omniversal myriad）那套包：{@code MyriadStatePayload} /
 * {@code MyriadActionPayload} / {@code FullSlotsPayload} 及其处理器 —— 它们依赖
 * {@code OmniversalMyriadBlockEntity} 与 {@code OmniversalMyriadMenu}，
 * 属于本模组未移植的模具/机器子系统。杖相关的包（教学提示、加速状态、
 * 倍率/功能开关、召唤、云层时钟）与 {@link RangeNetwork} 全部保留。</p>
 */
public final class Network {
    private Network() {
    }

    public record StaffTutorialPayload() implements CustomPacketPayload {
        public static final Type<StaffTutorialPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(UselessStretcherMod.MODID, "staff_tutorial"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StaffTutorialPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> { }, buf -> new StaffTutorialPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Sent once when the player opens the X staff configuration screen. */
    public record StaffTutorialOpenedPayload() implements CustomPacketPayload {
        public static final Type<StaffTutorialOpenedPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(UselessStretcherMod.MODID, "staff_tutorial_opened"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StaffTutorialOpenedPayload> STREAM_CODEC =
                StreamCodec.of((buf, value) -> { }, buf -> new StaffTutorialOpenedPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record WondrousStaffSpeedPayload(int speed, int mode, boolean accelerationEnabled, boolean offhand)
            implements CustomPacketPayload {
        public static final Type<WondrousStaffSpeedPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(UselessStretcherMod.MODID, "wondrous_staff_speed"));

        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffSpeedPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, WondrousStaffSpeedPayload::speed,
                        ByteBufCodecs.VAR_INT, WondrousStaffSpeedPayload::mode,
                        ByteBufCodecs.BOOL, WondrousStaffSpeedPayload::accelerationEnabled,
                        ByteBufCodecs.BOOL, WondrousStaffSpeedPayload::offhand,
                        WondrousStaffSpeedPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Staff-only feature toggles edited by the X screen. */
    public record WondrousStaffFeaturesPayload(boolean summonEnabled,
                                                boolean lootRefresh, boolean offhand)
            implements CustomPacketPayload {
        public static final Type<WondrousStaffFeaturesPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(UselessStretcherMod.MODID,
                        "wondrous_staff_features"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffFeaturesPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::summonEnabled,
                        ByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::lootRefresh,
                        ByteBufCodecs.BOOL, WondrousStaffFeaturesPayload::offhand,
                        WondrousStaffFeaturesPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Selected entity registry IDs to summon. The server applies the hard count limit. */
    public record WondrousStaffSummonPayload(List<String> entityIds, boolean offhand)
            implements CustomPacketPayload {
        public static final Type<WondrousStaffSummonPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(UselessStretcherMod.MODID,
                        "wondrous_staff_summon"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WondrousStaffSummonPayload> STREAM_CODEC =
                StreamCodec.of((buffer, value) -> {
                    int size = Math.min(value.entityIds().size(), WondrousStaffSummoning.MAX_SELECTION);
                    buffer.writeVarInt(size);
                    for (int i = 0; i < size; i++) buffer.writeUtf(value.entityIds().get(i), 256);
                    buffer.writeBoolean(value.offhand());
                }, buffer -> {
                    int size = buffer.readVarInt();
                    if (size < 0 || size > WondrousStaffSummoning.MAX_SELECTION) {
                        throw new IllegalArgumentException("Invalid summon selection size: " + size);
                    }
                    List<String> ids = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) ids.add(buffer.readUtf(256));
                    return new WondrousStaffSummonPayload(ids, buffer.readBoolean());
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Dimension-wide visual clock used only for client cloud movement. */
    public record TimeAccelerationStatePayload(String dimension, int speed) implements CustomPacketPayload {
        public static final Type<TimeAccelerationStatePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(UselessStretcherMod.MODID,
                        "time_acceleration_state"));

        public static final StreamCodec<RegistryFriendlyByteBuf, TimeAccelerationStatePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, TimeAccelerationStatePayload::dimension,
                        ByteBufCodecs.VAR_INT, TimeAccelerationStatePayload::speed,
                        TimeAccelerationStatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        // 上游用 event.registrar("4")；本模组既有的 9 处网络注册统一用 "1"，
        // 通道版本是模组级设置而非功能逻辑，故这里保持一致。
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(StaffTutorialPayload.TYPE, StaffTutorialPayload.STREAM_CODEC,
                (payload, context) -> ClientStateReceiver.handleStaffTutorial());
        registrar.playToServer(StaffTutorialOpenedPayload.TYPE, StaffTutorialOpenedPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StaffTutorialData.get(player.getServer()).markUiOpened(player.getUUID());
                    }
                }));
        registrar.playToClient(TimeAccelerationStatePayload.TYPE, TimeAccelerationStatePayload.STREAM_CODEC,
                (payload, context) -> ClientStateReceiver.handleTimeAcceleration(payload));
        registrar.playToServer(WondrousStaffSpeedPayload.TYPE, WondrousStaffSpeedPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleWondrousStaffSpeed(payload, context)));
        registrar.playToServer(WondrousStaffFeaturesPayload.TYPE, WondrousStaffFeaturesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleWondrousStaffFeatures(payload, context)));
        registrar.playToServer(WondrousStaffSummonPayload.TYPE, WondrousStaffSummonPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleWondrousStaffSummon(payload, context)));
        RangeNetwork.register(registrar);
    }

    private static void handleWondrousStaffSpeed(WondrousStaffSpeedPayload payload,
                                                 net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() != com.godofthings.beef.stretcher.init.ModItems.WONDROUS_STAFF.get()) return;
        int speed = switch (payload.speed()) {
            case 0, 2, 4, 16, 32, 64, 128, 256, 512, 1024 -> payload.speed();
            default -> com.godofthings.beef.stretcher.content.entity.WondrousStaffAcceleration.DEFAULT_GEAR;
        };
        int mode = payload.mode() >= 0
                && payload.mode() < com.godofthings.beef.stretcher.content.entity.WondrousStaffAcceleration.STAFF_MODE_COUNT
                ? payload.mode()
                : com.godofthings.beef.stretcher.content.entity.WondrousStaffAcceleration.STAFF_MODE_NORMAL;
        held.set(com.godofthings.beef.stretcher.init.StretcherComponents.WONDROUS_STAFF_SPEED.get(), speed);
        com.godofthings.beef.stretcher.content.entity.WondrousStaffAcceleration.setMode(held, mode);
        held.set(com.godofthings.beef.core.component.UComponents.BeefTimeAccelerationEnabledComponent.get(),
                payload.accelerationEnabled());
        if (StaffTutorialData.get(player.getServer()).markHintShown(player.getUUID(),
                StaffTutorialData.HINT_GEAR_CHANGE)) {
            sendStaffTutorial(player);
        }
    }

    public static void sendWondrousStaffSpeed(int speed, int mode, boolean accelerationEnabled,
                                              InteractionHand hand) {
        PacketDistributor.sendToServer(new WondrousStaffSpeedPayload(
                speed, mode, accelerationEnabled, hand == InteractionHand.OFF_HAND));
    }

    private static void handleWondrousStaffFeatures(WondrousStaffFeaturesPayload payload,
                                                    net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(com.godofthings.beef.stretcher.init.ModItems.WONDROUS_STAFF.get())) return;
        held.set(StretcherComponents.WONDROUS_STAFF_SUMMON_ENABLED.get(), payload.summonEnabled());
        held.set(StretcherComponents.WONDROUS_STAFF_LOOT_REFRESH.get(),
                payload.lootRefresh()
                        && com.godofthings.beef.stretcher.config.StretcherConfig.enableStaffLootRefresh());
    }

    private static void handleWondrousStaffSummon(WondrousStaffSummonPayload payload,
                                                  net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(com.godofthings.beef.stretcher.init.ModItems.WONDROUS_STAFF.get())) return;
        WondrousStaffSummoning.summon(player, held, payload.entityIds());
    }

    public static void sendWondrousStaffFeatures(boolean summonEnabled, boolean lootRefresh,
                                                 InteractionHand hand) {
        PacketDistributor.sendToServer(new WondrousStaffFeaturesPayload(
                summonEnabled, lootRefresh, hand == InteractionHand.OFF_HAND));
    }

    public static void sendWondrousStaffSummon(List<String> entityIds, InteractionHand hand) {
        PacketDistributor.sendToServer(new WondrousStaffSummonPayload(
                List.copyOf(entityIds), hand == InteractionHand.OFF_HAND));
    }

    public static void sendStaffTutorial(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new StaffTutorialPayload());
    }

    public static void sendStaffTutorialOpened() {
        PacketDistributor.sendToServer(new StaffTutorialOpenedPayload());
    }

    public static void sendTimeAccelerationState(net.minecraft.server.level.ServerLevel level, int speed) {
        PacketDistributor.sendToPlayersInDimension(level, new TimeAccelerationStatePayload(
                level.dimension().location().toString(), Math.max(0, speed)));
    }
}

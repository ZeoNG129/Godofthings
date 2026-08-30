package com.direwolf20.justdirethings;

import com.direwolf20.justdirethings.common.network.PacketHandler;
import com.direwolf20.justdirethings.setup.Registration;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Just Dire Things 移植收编入口（非 @Mod，注册与网络初始化由 Godofthings 主类调用）。
 * mod_id 已合并为 godofthings。
 */
public class JustDireThings {
    public static final String MODID = "godofthings";

    public static void init(IEventBus modEventBus) {
        Registration.init(modEventBus);
        modEventBus.addListener(PacketHandler::registerNetworking);
    }
}

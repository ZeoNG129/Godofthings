package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.client.screen.GodBlackBoxScreen;
import com.godofthings.client.screen.GodFurnaceConfigScreen;
import com.godofthings.energy.CreativeEnergyCubeScreen;
import com.godofthings.client.screen.GodFurnaceScreen;
import com.godofthings.client.screen.GodChangeScreen;
import com.godofthings.client.screen.GodCraftScreen;
import com.godofthings.client.screen.GodCraftConfigScreen;
import com.godofthings.client.screen.GodCraftTemplateScreen;
import com.godofthings.client.screen.GodDevourerScreen;
import com.godofthings.client.screen.GodDropScreen;
import com.godofthings.client.screen.GodEnchantScreen;
import com.godofthings.client.screen.GodMinerScreen;
import com.godofthings.client.screen.GodRecordScreen;
import com.godofthings.client.screen.GodResourceScreen;
import com.godofthings.client.screen.GodTransmitterScreen;
import com.godofthings.client.screen.WaypointScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents
{
    // 1.20.1 为 FMLClientSetupEvent + MenuScreens.register，1.21.1 改为 MOD 总线 RegisterMenuScreensEvent
    @SubscribeEvent
    public static void onClientSetup(RegisterMenuScreensEvent event)
    {
        event.register(Godofthings.GOD_FURNACE_MENU.get(), GodFurnaceScreen::new);
        event.register(Godofthings.GOD_FURNACE_CONFIG_MENU.get(), GodFurnaceConfigScreen::new);
        event.register(Godofthings.GOD_MINER_MENU.get(), GodMinerScreen::new);
        event.register(Godofthings.GOD_RESOURCE_MENU.get(), GodResourceScreen::new);
        event.register(Godofthings.GOD_DEVOURER_MENU.get(), GodDevourerScreen::new);
        event.register(Godofthings.PORTABLE_DEVOURER_MENU.get(), GodDevourerScreen::new);
        event.register(Godofthings.GOD_DROP_MENU.get(), GodDropScreen::new);
        event.register(Godofthings.GOD_ENCHANT_MENU.get(), GodEnchantScreen::new);
        event.register(Godofthings.GOD_CHANGE_MENU.get(), GodChangeScreen::new);
        event.register(Godofthings.GOD_CRAFT_MENU.get(), GodCraftScreen::new);
        event.register(Godofthings.GOD_CRAFT_CONFIG_MENU.get(), GodCraftConfigScreen::new);
        event.register(Godofthings.GOD_CRAFT_TEMPLATE_MENU.get(), GodCraftTemplateScreen::new);
        event.register(Godofthings.CREATIVE_ENERGY_CUBE_MENU.get(), CreativeEnergyCubeScreen::new);
        event.register(Godofthings.GOD_RECORD_MENU.get(), GodRecordScreen::new);
        event.register(Godofthings.WAYPOINT_MENU.get(), WaypointScreen::new);
        event.register(Godofthings.GOD_BLACK_BOX_MENU.get(), GodBlackBoxScreen::new);
        event.register(Godofthings.GOD_TRANSMITTER_MENU.get(), GodTransmitterScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        WandKeyBindings.register(event);
    }
}

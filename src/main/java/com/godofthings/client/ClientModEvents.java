package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.client.screen.GodFurnaceConfigScreen;
import com.godofthings.client.screen.EnergyGeneratorScreen;
import com.godofthings.client.screen.EnergyRelayScreen;
import com.godofthings.client.screen.GodFurnaceScreen;
import com.godofthings.client.screen.GodChangeScreen;
import com.godofthings.client.screen.GodCraftScreen;
import com.godofthings.client.screen.GodCraftConfigScreen;
import com.godofthings.client.screen.GodCraftTemplateScreen;
import com.godofthings.client.screen.GodDropScreen;
import com.godofthings.client.screen.GodEnchantScreen;
import com.godofthings.client.screen.GodMinerScreen;
import com.godofthings.client.screen.GodResourceScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Godofthings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            MenuScreens.register(Godofthings.GOD_FURNACE_MENU.get(), GodFurnaceScreen::new);
            MenuScreens.register(Godofthings.GOD_FURNACE_CONFIG_MENU.get(), GodFurnaceConfigScreen::new);
            MenuScreens.register(Godofthings.GOD_MINER_MENU.get(), GodMinerScreen::new);
            MenuScreens.register(Godofthings.GOD_RESOURCE_MENU.get(), GodResourceScreen::new);
            MenuScreens.register(Godofthings.GOD_DROP_MENU.get(), GodDropScreen::new);
            MenuScreens.register(Godofthings.GOD_ENCHANT_MENU.get(), GodEnchantScreen::new);
            MenuScreens.register(Godofthings.GOD_CHANGE_MENU.get(), GodChangeScreen::new);
            MenuScreens.register(Godofthings.GOD_CRAFT_MENU.get(), GodCraftScreen::new);
            MenuScreens.register(Godofthings.GOD_CRAFT_CONFIG_MENU.get(), GodCraftConfigScreen::new);
            MenuScreens.register(Godofthings.GOD_CRAFT_TEMPLATE_MENU.get(), GodCraftTemplateScreen::new);
            MenuScreens.register(Godofthings.ENERGY_GENERATOR_MENU.get(), EnergyGeneratorScreen::new);
            MenuScreens.register(Godofthings.ENERGY_RELAY_MENU.get(), EnergyRelayScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        WandKeyBindings.register(event);
    }
}

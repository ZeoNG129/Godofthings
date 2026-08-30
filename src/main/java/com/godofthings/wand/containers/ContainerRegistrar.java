package com.godofthings.wand.containers;

import net.minecraftforge.fml.ModList;
import com.godofthings.wand.ConstructionWand;
import com.godofthings.wand.containers.handlers.HandlerAdvWirelessTerminal;
import com.godofthings.wand.containers.handlers.HandlerBotania;
import com.godofthings.wand.containers.handlers.HandlerBundle;
import com.godofthings.wand.containers.handlers.HandlerCapability;
import com.godofthings.wand.containers.handlers.HandlerDimensionsNet;
import com.godofthings.wand.containers.handlers.HandlerLightland;
import com.godofthings.wand.containers.handlers.HandlerNetTerminal;
import com.godofthings.wand.containers.handlers.HandlerPortableCell;
import com.godofthings.wand.containers.handlers.HandlerProjectE;
import com.godofthings.wand.containers.handlers.HandlerShulkerbox;
import com.godofthings.wand.containers.handlers.HandlerWirelessCraftingGrid;
import com.godofthings.wand.containers.handlers.HandlerWirelessGrid;
import com.godofthings.wand.containers.handlers.HandlerWirelessTerminal;


public class ContainerRegistrar
{
    public static void register() {
        // Normal backpack will be recognized as Capability
        if(ModList.get().isLoaded("l2backpack")) {
            ConstructionWand.instance.containerManager.register(new HandlerLightland());
            ConstructionWand.LOGGER.info("L2Backpack integration added");
        }

        ConstructionWand.instance.containerManager.register(new HandlerCapability());
        ConstructionWand.instance.containerManager.register(new HandlerShulkerbox());
        ConstructionWand.instance.containerManager.register(new HandlerBundle());

        if(ModList.get().isLoaded("botania")) {
            ConstructionWand.instance.containerManager.register(new HandlerBotania());
            ConstructionWand.LOGGER.info("Botania integration added");
        }

        if(ModList.get().isLoaded("curios")) {
            ConstructionWand.LOGGER.info("Curios integration added");
        }

        if(ModList.get().isLoaded("ae2")) {
            ConstructionWand.instance.containerManager.register(new HandlerPortableCell());
            ConstructionWand.instance.containerManager.register(new HandlerWirelessTerminal());
            ConstructionWand.LOGGER.info("Applied Energistics 2 integration added");
        }

        if(ModList.get().isLoaded("toms_storage")) {
            ConstructionWand.instance.containerManager.register(new HandlerAdvWirelessTerminal());
            ConstructionWand.LOGGER.info("Tom's Simple Storage integration added");
        }

        if(ModList.get().isLoaded("refinedstorage")) {
            if(ModList.get().isLoaded("refinedstorageaddons")) {
                ConstructionWand.instance.containerManager.register(new HandlerWirelessCraftingGrid());
                ConstructionWand.LOGGER.info("Refined Storage Addons integration added");
            }
            ConstructionWand.instance.containerManager.register(new HandlerWirelessGrid());
            ConstructionWand.LOGGER.info("Refined Storage integration added");
        }

        if(ModList.get().isLoaded("beyonddimensions")) {
            ConstructionWand.instance.containerManager.register(new HandlerDimensionsNet());
            ConstructionWand.instance.containerManager.register(new HandlerNetTerminal());
            ConstructionWand.LOGGER.info("Beyond Dimensions integration added");
        }

        if(ModList.get().isLoaded("projecte")) {
            ConstructionWand.instance.containerManager.register(new HandlerProjectE());
            ConstructionWand.LOGGER.info("ProjectE integration added");
        }
    }
}

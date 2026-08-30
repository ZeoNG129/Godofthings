package com.godofthings.wand.items;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import com.godofthings.wand.ConstructionWand;
import com.godofthings.wand.basics.option.WandOptions;
import com.godofthings.wand.crafting.RecipeWandUpgrade;
import com.godofthings.wand.items.core.ItemCoreAngel;
import com.godofthings.wand.items.core.ItemCoreDestruction;
import com.godofthings.wand.items.wand.ItemWand;
import com.godofthings.wand.items.wand.ItemWandInfinity;

@Mod.EventBusSubscriber(modid = ConstructionWand.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ConstructionWand.MODID);

    // Wands
    public static final RegistryObject<Item> WAND_INFINITY = ITEMS.register("infinity_wand", () -> new ItemWandInfinity(propWand()));

    // Cores
    public static final RegistryObject<Item> CORE_ANGEL = ITEMS.register("core_angel", () -> new ItemCoreAngel(propUpgrade()));
    public static final RegistryObject<Item> CORE_DESTRUCTION = ITEMS.register("core_destruction", () -> new ItemCoreDestruction(propUpgrade()));

    // Collections
    public static final RegistryObject<Item>[] WANDS = new RegistryObject[] {WAND_INFINITY};
    public static final RegistryObject<Item>[] CORES = new RegistryObject[] {CORE_ANGEL, CORE_DESTRUCTION};

    public static Item.Properties propWand() {
        return new Item.Properties();
    }

    private static Item.Properties propUpgrade() {
        return new Item.Properties().stacksTo(1);
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS, registry -> {
            registry.register("wand_upgrade", RecipeWandUpgrade.SERIALIZER);
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerModelProperties() {
        for(RegistryObject<Item> itemSupplier : WANDS) {
            Item item = itemSupplier.get();
            ItemProperties.register(
                    item, ConstructionWand.loc("using_core"),
                    (stack, world, entity, n) -> entity == null || !(stack.getItem() instanceof ItemWand) ? 0 :
                            new WandOptions(stack).cores.get().getColor() > -1 ? 1 : 0
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for(RegistryObject<Item> itemSupplier : WANDS) {
            Item item = itemSupplier.get();
            event.register((stack, layer) -> (layer == 1 && stack.getItem() instanceof ItemWand) ?
                    new WandOptions(stack).cores.get().getColor() : -1, item);
        }
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for(RegistryObject<Item> itemSupplier : WANDS) {
                event.accept(itemSupplier);
            }
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            for(RegistryObject<Item> itemSupplier : CORES) {
                event.accept(itemSupplier);
            }
        }
    }
}

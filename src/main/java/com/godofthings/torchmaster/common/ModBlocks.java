package com.godofthings.torchmaster.common;

import com.godofthings.Godofthings;
import com.godofthings.torchmaster.TorchmasterConfig;
import com.godofthings.torchmaster.common.blocks.EntityBlockingLightBlock;
import com.godofthings.torchmaster.common.items.TMItemBlock;
import com.godofthings.torchmaster.common.logic.entityblocking.dreadlamp.DreadLampEntityBlockingLight;
import com.godofthings.torchmaster.common.logic.entityblocking.megatorch.MegatorchEntityBlockingLight;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
   public static final RegistryObject<EntityBlockingLightBlock> blockMegaTorch = Godofthings.BLOCKS
      .register(
         "megatorch",
         () -> new EntityBlockingLightBlock(
               Properties.of().sound(SoundType.WOOD).strength(1.0F, 1.0F).lightLevel(blockState -> 15),
               pos -> "MT_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ(),
               MegatorchEntityBlockingLight::new,
               1.0F,
               MegatorchEntityBlockingLight.SHAPE,
               TorchmasterConfig.GENERAL.megaTorchRadius
            )
      );
   public static final RegistryObject<TMItemBlock> itemMegaTorch = fromBlock(blockMegaTorch, new net.minecraft.world.item.Item.Properties());
   public static final RegistryObject<EntityBlockingLightBlock> blockDreadLamp = Godofthings.BLOCKS
      .register(
         "dreadlamp",
         () -> new EntityBlockingLightBlock(
               Properties.of().sound(SoundType.LANTERN).strength(1.0F, 1.0F).lightLevel(blockState -> 15),
               pos -> "DL_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ(),
               DreadLampEntityBlockingLight::new,
               0.3F,
               DreadLampEntityBlockingLight.SHAPE,
               TorchmasterConfig.GENERAL.dreadLampRadius
            )
      );
   public static final RegistryObject<TMItemBlock> itemDreadLamp = fromBlock(blockDreadLamp, new net.minecraft.world.item.Item.Properties());

   private ModBlocks() {
   }

   public static void init() {
   }

   private static <B extends Block> RegistryObject<TMItemBlock> fromBlock(RegistryObject<B> block, net.minecraft.world.item.Item.Properties properties) {
      return Godofthings.ITEMS.register(block.getId().getPath(), () -> new TMItemBlock((Block)block.get(), properties));
   }
}

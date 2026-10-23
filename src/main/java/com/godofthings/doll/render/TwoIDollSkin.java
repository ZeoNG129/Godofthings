package com.godofthings.doll.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@EventBusSubscriber(
   modid = "godofthings",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public final class TwoIDollSkin {
   private static final ResourceLocation NORMALIZED = ResourceLocation.fromNamespaceAndPath("godofthings", "dynamic/two_i_normalized_skin");
   private static ResourceLocation texture = TwoIDollRenderer.TEXTURE;
   private static final ResourceLocation HALF = ResourceLocation.fromNamespaceAndPath("godofthings", "dynamic/two_i_half_blink");
   private static final ResourceLocation CLOSED = ResourceLocation.fromNamespaceAndPath("godofthings", "dynamic/two_i_closed_eyes");
   private static boolean blinkReady;

   private TwoIDollSkin() {
   }

   public static ResourceLocation texture() {
      return texture;
   }

   public static ResourceLocation texture(float blink) {
      return blinkReady && !(blink < 0.25F) ? (blink < 0.65F ? HALF : CLOSED) : texture;
   }

   @SubscribeEvent
   public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(new SimplePreparableReloadListener<NativeImage>() {
         protected NativeImage prepare(ResourceManager resources, ProfilerFiller profiler) {
            try {
               NativeImage var5;
               try (InputStream input = resources.open(TwoIDollRenderer.TEXTURE)) {
                  NativeImage source = NativeImage.read(input);

                  try {
                     var5 = TwoIDollSkin.normalizedCopy(source);
                  } catch (Throwable var9) {
                     if (source != null) {
                        try {
                           source.close();
                        } catch (Throwable var8) {
                           var9.addSuppressed(var8);
                        }
                     }

                     throw var9;
                  }

                  if (source != null) {
                     source.close();
                  }
               }

               return var5;
            } catch (Exception var11) {
               LogUtils.getLogger().warn("Could not prepare the 2i skin; using the resource directly", var11);
               return null;
            }
         }

         protected void apply(NativeImage image, ResourceManager resources, ProfilerFiller profiler) {
            TwoIDollHandRenderer.reload();
            TextureManager textures = Minecraft.getInstance().getTextureManager();
            textures.release(TwoIDollSkin.NORMALIZED);
            textures.release(TwoIDollSkin.HALF);
            textures.release(TwoIDollSkin.CLOSED);
            TwoIDollSkin.blinkReady = false;
            TwoIDollSkin.texture = TwoIDollRenderer.TEXTURE;
            if (image != null) {
               textures.register(TwoIDollSkin.HALF, new DynamicTexture(TwoIDollSkin.blinkCopy(image, false)));
               textures.register(TwoIDollSkin.CLOSED, new DynamicTexture(TwoIDollSkin.blinkCopy(image, true)));
               textures.register(TwoIDollSkin.NORMALIZED, new DynamicTexture(image));
               TwoIDollSkin.texture = TwoIDollSkin.NORMALIZED;
               TwoIDollSkin.blinkReady = true;
            }
         }
      });
   }

   public static NativeImage normalizedCopy(NativeImage source) {
      NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), false);
      copy.copyFrom(source);
      if (usesSlimArms(source)) {
         int scale = source.getWidth() / 64;
         remapArm(source, copy, 40, 16, scale, true);
         remapArm(source, copy, 32, 48, scale, true);
         remapArm(source, copy, 40, 32, scale, false);
         remapArm(source, copy, 48, 48, scale, false);
      }

      return copy;
   }

   public static NativeImage blinkCopy(NativeImage skin, boolean closed) {
      NativeImage copy = new NativeImage(skin.getWidth(), skin.getHeight(), false);
      copy.copyFrom(skin);
      if (skin.getWidth() % 64 == 0 && skin.getHeight() == skin.getWidth()) {
         int scale = skin.getWidth() / 64;

         for (int eye : new int[]{9, 13}) {
            for (int px = 0; px < 2 * scale; px++) {
               int x = eye * scale + px;
               int complexion = skin.getPixelRGBA(11 * scale, 15 * scale);
               int lash = skin.getPixelRGBA(8 * scale, 8 * scale);

               for (int py = 0; py < 3 * scale; py++) {
                  int y = 12 * scale + py;
                  int color = closed ? (py == 2 * scale ? lash : complexion) : (py < scale ? complexion : skin.getPixelRGBA(x, y));
                  copy.setPixelRGBA(x, y, color);
                  if (py >= scale && skin.getPixelRGBA(x + 32 * scale, y) >>> 24 != 0) {
                     copy.setPixelRGBA(x + 32 * scale, y, color);
                  }
               }
            }
         }

         return copy;
      } else {
         return copy;
      }
   }

   public static boolean usesSlimArms(NativeImage skin) {
      if (skin.getWidth() >= 64 && skin.getWidth() % 64 == 0 && skin.getHeight() == skin.getWidth()) {
         int scale = skin.getWidth() / 64;
         return allTransparent(skin, 54, 20, 2, 12, scale)
            && allTransparent(skin, 46, 52, 2, 12, scale)
            && !allTransparent(skin, 47, 16, 3, 4, scale)
            && !allTransparent(skin, 39, 48, 3, 4, scale);
      } else {
         return false;
      }
   }

   private static boolean allTransparent(NativeImage image, int x, int y, int width, int height, int scale) {
      for (int py = y * scale; py < (y + height) * scale; py++) {
         for (int px = x * scale; px < (x + width) * scale; px++) {
            if (image.getPixelRGBA(px, py) >>> 24 != 0) {
               return false;
            }
         }
      }

      return true;
   }

   private static void remapArm(NativeImage source, NativeImage target, int x, int y, int scale, boolean base) {
      face(source, target, x + 4, y, 3, 4, x + 4, y, 4, 4, scale, base);
      face(source, target, x + 7, y, 3, 4, x + 8, y, 4, 4, scale, base);
      face(source, target, x, y + 4, 4, 12, x, y + 4, 4, 12, scale, base);
      face(source, target, x + 4, y + 4, 3, 12, x + 4, y + 4, 4, 12, scale, base);
      face(source, target, x + 7, y + 4, 4, 12, x + 8, y + 4, 4, 12, scale, base);
      face(source, target, x + 11, y + 4, 3, 12, x + 12, y + 4, 4, 12, scale, base);
   }

   private static void face(
      NativeImage source, NativeImage target, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh, int scale, boolean opaqueBase
   ) {
      for (int y = 0; y < dh * scale; y++) {
         for (int x = 0; x < dw * scale; x++) {
            int color = source.getPixelRGBA(sx * scale + x * sw / dw, sy * scale + y * sh / dh);
            target.setPixelRGBA(dx * scale + x, dy * scale + y, opaqueBase ? color | 0xFF000000 : color);
         }
      }
   }
}

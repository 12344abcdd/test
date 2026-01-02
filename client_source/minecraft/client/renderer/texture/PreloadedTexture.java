package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PreloadedTexture extends SimpleTexture {
   @Nullable
   private CompletableFuture<SimpleTexture.TextureImage> future;

   public PreloadedTexture(ResourceManager resourceManager, ResourceLocation resourceLocation, Executor executor) {
      super(resourceLocation);
      this.future = CompletableFuture.supplyAsync(() -> {
         return SimpleTexture.TextureImage.load(resourceManager, resourceLocation);
      }, executor);
   }

   protected SimpleTexture.TextureImage getTextureImage(ResourceManager resourceManager) {
      if (this.future != null) {
         SimpleTexture.TextureImage textureImage = (SimpleTexture.TextureImage)this.future.join();
         this.future = null;
         return textureImage;
      } else {
         return SimpleTexture.TextureImage.load(resourceManager, this.location);
      }
   }

   public CompletableFuture<Void> getFuture() {
      return this.future == null ? CompletableFuture.completedFuture((Object)null) : this.future.thenApply((textureImage) -> {
         return null;
      });
   }

   public void reset(TextureManager textureManager, ResourceManager resourceManager, ResourceLocation resourceLocation, Executor executor) {
      this.future = CompletableFuture.supplyAsync(() -> {
         return SimpleTexture.TextureImage.load(resourceManager, this.location);
      }, Util.backgroundExecutor());
      this.future.thenRunAsync(() -> {
         textureManager.register((ResourceLocation)this.location, (AbstractTexture)this);
      }, executor(executor));
   }

   private static Executor executor(Executor executor) {
      return (runnable) -> {
         executor.execute(() -> {
            Objects.requireNonNull(runnable);
            RenderSystem.recordRenderCall(runnable::run);
         });
      };
   }
}

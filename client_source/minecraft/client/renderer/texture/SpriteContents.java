package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SpriteContents implements Stitcher.Entry, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation name;
   final int width;
   final int height;
   private final NativeImage originalImage;
   NativeImage[] byMipLevel;
   @Nullable
   private final SpriteContents.AnimatedTexture animatedTexture;
   private final ResourceMetadata metadata;

   public SpriteContents(ResourceLocation resourceLocation, FrameSize frameSize, NativeImage nativeImage, ResourceMetadata resourceMetadata) {
      this.name = resourceLocation;
      this.width = frameSize.width();
      this.height = frameSize.height();
      this.metadata = resourceMetadata;
      AnimationMetadataSection animationMetadataSection = (AnimationMetadataSection)resourceMetadata.getSection(AnimationMetadataSection.SERIALIZER).orElse(AnimationMetadataSection.EMPTY);
      this.animatedTexture = this.createAnimatedTexture(frameSize, nativeImage.getWidth(), nativeImage.getHeight(), animationMetadataSection);
      this.originalImage = nativeImage;
      this.byMipLevel = new NativeImage[]{this.originalImage};
   }

   public void increaseMipLevel(int i) {
      try {
         this.byMipLevel = MipmapGenerator.generateMipLevels(this.byMipLevel, i);
      } catch (Throwable var6) {
         CrashReport crashReport = CrashReport.forThrowable(var6, "Generating mipmaps for frame");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Sprite being mipmapped");
         crashReportCategory.setDetail("First frame", () -> {
            StringBuilder stringBuilder = new StringBuilder();
            if (stringBuilder.length() > 0) {
               stringBuilder.append(", ");
            }

            stringBuilder.append(this.originalImage.getWidth()).append("x").append(this.originalImage.getHeight());
            return stringBuilder.toString();
         });
         CrashReportCategory crashReportCategory2 = crashReport.addCategory("Frame being iterated");
         crashReportCategory2.setDetail("Sprite name", this.name);
         crashReportCategory2.setDetail("Sprite size", () -> {
            return this.width + " x " + this.height;
         });
         crashReportCategory2.setDetail("Sprite frames", () -> {
            return this.getFrameCount() + " frames";
         });
         crashReportCategory2.setDetail("Mipmap levels", i);
         throw new ReportedException(crashReport);
      }
   }

   private int getFrameCount() {
      return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
   }

   @Nullable
   private SpriteContents.AnimatedTexture createAnimatedTexture(FrameSize frameSize, int i, int j, AnimationMetadataSection animationMetadataSection) {
      int k = i / frameSize.width();
      int l = j / frameSize.height();
      int m = k * l;
      List<SpriteContents.FrameInfo> list = new ArrayList();
      animationMetadataSection.forEachFrame((ix, jx) -> {
         list.add(new SpriteContents.FrameInfo(ix, jx));
      });
      int n;
      if (list.isEmpty()) {
         for(n = 0; n < m; ++n) {
            list.add(new SpriteContents.FrameInfo(n, animationMetadataSection.getDefaultFrameTime()));
         }
      } else {
         n = 0;
         IntSet intSet = new IntOpenHashSet();

         for(Iterator iterator = list.iterator(); iterator.hasNext(); ++n) {
            SpriteContents.FrameInfo frameInfo = (SpriteContents.FrameInfo)iterator.next();
            boolean bl = true;
            if (frameInfo.time <= 0) {
               LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", new Object[]{this.name, n, frameInfo.time});
               bl = false;
            }

            if (frameInfo.index < 0 || frameInfo.index >= m) {
               LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", new Object[]{this.name, n, frameInfo.index});
               bl = false;
            }

            if (bl) {
               intSet.add(frameInfo.index);
            } else {
               iterator.remove();
            }
         }

         int[] is = IntStream.range(0, m).filter((ix) -> {
            return !intSet.contains(ix);
         }).toArray();
         if (is.length > 0) {
            LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(is));
         }
      }

      return list.size() <= 1 ? null : new SpriteContents.AnimatedTexture(ImmutableList.copyOf(list), k, animationMetadataSection.isInterpolatedFrames());
   }

   void upload(int i, int j, int k, int l, NativeImage[] nativeImages) {
      for(int m = 0; m < this.byMipLevel.length; ++m) {
         nativeImages[m].upload(m, i >> m, j >> m, k >> m, l >> m, this.width >> m, this.height >> m, this.byMipLevel.length > 1, false);
      }

   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public ResourceLocation name() {
      return this.name;
   }

   public IntStream getUniqueFrames() {
      return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
   }

   @Nullable
   public SpriteTicker createTicker() {
      return this.animatedTexture != null ? this.animatedTexture.createTicker() : null;
   }

   public ResourceMetadata metadata() {
      return this.metadata;
   }

   public void close() {
      NativeImage[] var1 = this.byMipLevel;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         NativeImage nativeImage = var1[var3];
         nativeImage.close();
      }

   }

   public String toString() {
      String var10000 = String.valueOf(this.name);
      return "SpriteContents{name=" + var10000 + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
   }

   public boolean isTransparent(int i, int j, int k) {
      int l = j;
      int m = k;
      if (this.animatedTexture != null) {
         l = j + this.animatedTexture.getFrameX(i) * this.width;
         m = k + this.animatedTexture.getFrameY(i) * this.height;
      }

      return (this.originalImage.getPixelRGBA(l, m) >> 24 & 255) == 0;
   }

   public void uploadFirstFrame(int i, int j) {
      if (this.animatedTexture != null) {
         this.animatedTexture.uploadFirstFrame(i, j);
      } else {
         this.upload(i, j, 0, 0, this.byMipLevel);
      }

   }

   @Environment(EnvType.CLIENT)
   private class AnimatedTexture {
      final List<SpriteContents.FrameInfo> frames;
      private final int frameRowSize;
      private final boolean interpolateFrames;

      AnimatedTexture(final List<SpriteContents.FrameInfo> list, final int i, final boolean bl) {
         this.frames = list;
         this.frameRowSize = i;
         this.interpolateFrames = bl;
      }

      int getFrameX(int i) {
         return i % this.frameRowSize;
      }

      int getFrameY(int i) {
         return i / this.frameRowSize;
      }

      void uploadFrame(int i, int j, int k) {
         int l = this.getFrameX(k) * SpriteContents.this.width;
         int m = this.getFrameY(k) * SpriteContents.this.height;
         SpriteContents.this.upload(i, j, l, m, SpriteContents.this.byMipLevel);
      }

      public SpriteTicker createTicker() {
         return SpriteContents.this.new Ticker(SpriteContents.this, this, this.interpolateFrames ? SpriteContents.this.new InterpolationData() : null);
      }

      public void uploadFirstFrame(int i, int j) {
         this.uploadFrame(i, j, ((SpriteContents.FrameInfo)this.frames.get(0)).index);
      }

      public IntStream getUniqueFrames() {
         return this.frames.stream().mapToInt((frameInfo) -> {
            return frameInfo.index;
         }).distinct();
      }
   }

   @Environment(EnvType.CLIENT)
   private static class FrameInfo {
      final int index;
      final int time;

      FrameInfo(int i, int j) {
         this.index = i;
         this.time = j;
      }
   }

   @Environment(EnvType.CLIENT)
   private class Ticker implements SpriteTicker {
      int frame;
      int subFrame;
      final SpriteContents.AnimatedTexture animationInfo;
      @Nullable
      private final SpriteContents.InterpolationData interpolationData;

      Ticker(final SpriteContents spriteContents, @Nullable final SpriteContents.AnimatedTexture animatedTexture, final SpriteContents.InterpolationData interpolationData) {
         this.animationInfo = animatedTexture;
         this.interpolationData = interpolationData;
      }

      public void tickAndUpload(int i, int j) {
         ++this.subFrame;
         SpriteContents.FrameInfo frameInfo = (SpriteContents.FrameInfo)this.animationInfo.frames.get(this.frame);
         if (this.subFrame >= frameInfo.time) {
            int k = frameInfo.index;
            this.frame = (this.frame + 1) % this.animationInfo.frames.size();
            this.subFrame = 0;
            int l = ((SpriteContents.FrameInfo)this.animationInfo.frames.get(this.frame)).index;
            if (k != l) {
               this.animationInfo.uploadFrame(i, j, l);
            }
         } else if (this.interpolationData != null) {
            if (!RenderSystem.isOnRenderThread()) {
               RenderSystem.recordRenderCall(() -> {
                  this.interpolationData.uploadInterpolatedFrame(i, j, this);
               });
            } else {
               this.interpolationData.uploadInterpolatedFrame(i, j, this);
            }
         }

      }

      public void close() {
         if (this.interpolationData != null) {
            this.interpolationData.close();
         }

      }
   }

   @Environment(EnvType.CLIENT)
   private final class InterpolationData implements AutoCloseable {
      private final NativeImage[] activeFrame;

      InterpolationData() {
         this.activeFrame = new NativeImage[SpriteContents.this.byMipLevel.length];

         for(int i = 0; i < this.activeFrame.length; ++i) {
            int j = SpriteContents.this.width >> i;
            int k = SpriteContents.this.height >> i;
            this.activeFrame[i] = new NativeImage(j, k, false);
         }

      }

      void uploadInterpolatedFrame(int i, int j, SpriteContents.Ticker ticker) {
         SpriteContents.AnimatedTexture animatedTexture = ticker.animationInfo;
         List<SpriteContents.FrameInfo> list = animatedTexture.frames;
         SpriteContents.FrameInfo frameInfo = (SpriteContents.FrameInfo)list.get(ticker.frame);
         double d = 1.0D - (double)ticker.subFrame / (double)frameInfo.time;
         int k = frameInfo.index;
         int l = ((SpriteContents.FrameInfo)list.get((ticker.frame + 1) % list.size())).index;
         if (k != l) {
            for(int m = 0; m < this.activeFrame.length; ++m) {
               int n = SpriteContents.this.width >> m;
               int o = SpriteContents.this.height >> m;

               for(int p = 0; p < o; ++p) {
                  for(int q = 0; q < n; ++q) {
                     int r = this.getPixel(animatedTexture, k, m, q, p);
                     int s = this.getPixel(animatedTexture, l, m, q, p);
                     int t = this.mix(d, r >> 16 & 255, s >> 16 & 255);
                     int u = this.mix(d, r >> 8 & 255, s >> 8 & 255);
                     int v = this.mix(d, r & 255, s & 255);
                     this.activeFrame[m].setPixelRGBA(q, p, r & -16777216 | t << 16 | u << 8 | v);
                  }
               }
            }

            SpriteContents.this.upload(i, j, 0, 0, this.activeFrame);
         }

      }

      private int getPixel(SpriteContents.AnimatedTexture animatedTexture, int i, int j, int k, int l) {
         return SpriteContents.this.byMipLevel[j].getPixelRGBA(k + (animatedTexture.getFrameX(i) * SpriteContents.this.width >> j), l + (animatedTexture.getFrameY(i) * SpriteContents.this.height >> j));
      }

      private int mix(double d, int i, int j) {
         return (int)(d * (double)i + (1.0D - d) * (double)j);
      }

      public void close() {
         NativeImage[] var1 = this.activeFrame;
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            NativeImage nativeImage = var1[var3];
            nativeImage.close();
         }

      }
   }
}

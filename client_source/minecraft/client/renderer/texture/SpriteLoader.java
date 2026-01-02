package net.minecraft.client.renderer.texture;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SpriteLoader {
   public static final Set<MetadataSectionSerializer<?>> DEFAULT_METADATA_SECTIONS;
   private static final Logger LOGGER;
   private final ResourceLocation location;
   private final int maxSupportedTextureSize;
   private final int minWidth;
   private final int minHeight;

   public SpriteLoader(ResourceLocation resourceLocation, int i, int j, int k) {
      this.location = resourceLocation;
      this.maxSupportedTextureSize = i;
      this.minWidth = j;
      this.minHeight = k;
   }

   public static SpriteLoader create(TextureAtlas textureAtlas) {
      return new SpriteLoader(textureAtlas.location(), textureAtlas.maxSupportedTextureSize(), textureAtlas.getWidth(), textureAtlas.getHeight());
   }

   public SpriteLoader.Preparations stitch(List<SpriteContents> list, int i, Executor executor) {
      int j = this.maxSupportedTextureSize;
      Stitcher<SpriteContents> stitcher = new Stitcher(j, j, i);
      int k = Integer.MAX_VALUE;
      int l = 1 << i;

      SpriteContents spriteContents;
      int m;
      for(Iterator var8 = list.iterator(); var8.hasNext(); stitcher.registerSprite(spriteContents)) {
         spriteContents = (SpriteContents)var8.next();
         k = Math.min(k, Math.min(spriteContents.width(), spriteContents.height()));
         m = Math.min(Integer.lowestOneBit(spriteContents.width()), Integer.lowestOneBit(spriteContents.height()));
         if (m < l) {
            LOGGER.warn("Texture {} with size {}x{} limits mip level from {} to {}", new Object[]{spriteContents.name(), spriteContents.width(), spriteContents.height(), Mth.log2(l), Mth.log2(m)});
            l = m;
         }
      }

      int n = Math.min(k, l);
      int o = Mth.log2(n);
      if (o < i) {
         LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", new Object[]{this.location, i, o, n});
         m = o;
      } else {
         m = i;
      }

      try {
         stitcher.stitch();
      } catch (StitcherException var16) {
         CrashReport crashReport = CrashReport.forThrowable(var16, "Stitching");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Stitcher");
         crashReportCategory.setDetail("Sprites", var16.getAllSprites().stream().map((entry) -> {
            return String.format(Locale.ROOT, "%s[%dx%d]", entry.name(), entry.width(), entry.height());
         }).collect(Collectors.joining(",")));
         crashReportCategory.setDetail("Max Texture Size", j);
         throw new ReportedException(crashReport);
      }

      int p = Math.max(stitcher.getWidth(), this.minWidth);
      int q = Math.max(stitcher.getHeight(), this.minHeight);
      Map<ResourceLocation, TextureAtlasSprite> map = this.getStitchedSprites(stitcher, p, q);
      TextureAtlasSprite textureAtlasSprite = (TextureAtlasSprite)map.get(MissingTextureAtlasSprite.getLocation());
      CompletableFuture completableFuture;
      if (m > 0) {
         completableFuture = CompletableFuture.runAsync(() -> {
            map.values().forEach((textureAtlasSprite) -> {
               textureAtlasSprite.contents().increaseMipLevel(m);
            });
         }, executor);
      } else {
         completableFuture = CompletableFuture.completedFuture((Object)null);
      }

      return new SpriteLoader.Preparations(p, q, m, textureAtlasSprite, map, completableFuture);
   }

   public static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(SpriteResourceLoader spriteResourceLoader, List<Function<SpriteResourceLoader, SpriteContents>> list, Executor executor) {
      List<CompletableFuture<SpriteContents>> list2 = list.stream().map((function) -> {
         return CompletableFuture.supplyAsync(() -> {
            return (SpriteContents)function.apply(spriteResourceLoader);
         }, executor);
      }).toList();
      return Util.sequence(list2).thenApply((listx) -> {
         return listx.stream().filter(Objects::nonNull).toList();
      });
   }

   public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(ResourceManager resourceManager, ResourceLocation resourceLocation, int i, Executor executor) {
      return this.loadAndStitch(resourceManager, resourceLocation, i, executor, DEFAULT_METADATA_SECTIONS);
   }

   public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(ResourceManager resourceManager, ResourceLocation resourceLocation, int i, Executor executor, Collection<MetadataSectionSerializer<?>> collection) {
      SpriteResourceLoader spriteResourceLoader = SpriteResourceLoader.create(collection);
      return CompletableFuture.supplyAsync(() -> {
         return SpriteSourceList.load(resourceManager, resourceLocation).list(resourceManager);
      }, executor).thenCompose((list) -> {
         return runSpriteSuppliers(spriteResourceLoader, list, executor);
      }).thenApply((list) -> {
         return this.stitch(list, i, executor);
      });
   }

   private Map<ResourceLocation, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> stitcher, int i, int j) {
      Map<ResourceLocation, TextureAtlasSprite> map = new HashMap();
      stitcher.gatherSprites((spriteContents, k, l) -> {
         map.put(spriteContents.name(), new TextureAtlasSprite(this.location, spriteContents, i, j, k, l));
      });
      return map;
   }

   static {
      DEFAULT_METADATA_SECTIONS = Set.of(AnimationMetadataSection.SERIALIZER);
      LOGGER = LogUtils.getLogger();
   }

   @Environment(EnvType.CLIENT)
   public static record Preparations(int width, int height, int mipLevel, TextureAtlasSprite missing, Map<ResourceLocation, TextureAtlasSprite> regions, CompletableFuture<Void> readyForUpload) {
      public Preparations(int i, int j, int k, TextureAtlasSprite textureAtlasSprite, Map<ResourceLocation, TextureAtlasSprite> map, CompletableFuture<Void> completableFuture) {
         this.width = i;
         this.height = j;
         this.mipLevel = k;
         this.missing = textureAtlasSprite;
         this.regions = map;
         this.readyForUpload = completableFuture;
      }

      public CompletableFuture<SpriteLoader.Preparations> waitForUpload() {
         return this.readyForUpload.thenApply((void_) -> {
            return this;
         });
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }

      public int mipLevel() {
         return this.mipLevel;
      }

      public TextureAtlasSprite missing() {
         return this.missing;
      }

      public Map<ResourceLocation, TextureAtlasSprite> regions() {
         return this.regions;
      }

      public CompletableFuture<Void> readyForUpload() {
         return this.readyForUpload;
      }
   }
}

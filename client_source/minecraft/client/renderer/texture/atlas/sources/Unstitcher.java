package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class Unstitcher implements SpriteSource {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<Unstitcher> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
      return instance.group(ResourceLocation.CODEC.fieldOf("resource").forGetter((unstitcher) -> {
         return unstitcher.resource;
      }), ExtraCodecs.nonEmptyList(Unstitcher.Region.CODEC.listOf()).fieldOf("regions").forGetter((unstitcher) -> {
         return unstitcher.regions;
      }), Codec.DOUBLE.optionalFieldOf("divisor_x", 1.0D).forGetter((unstitcher) -> {
         return unstitcher.xDivisor;
      }), Codec.DOUBLE.optionalFieldOf("divisor_y", 1.0D).forGetter((unstitcher) -> {
         return unstitcher.yDivisor;
      })).apply(instance, Unstitcher::new);
   });
   private final ResourceLocation resource;
   private final List<Unstitcher.Region> regions;
   private final double xDivisor;
   private final double yDivisor;

   public Unstitcher(ResourceLocation resourceLocation, List<Unstitcher.Region> list, double d, double e) {
      this.resource = resourceLocation;
      this.regions = list;
      this.xDivisor = d;
      this.yDivisor = e;
   }

   public void run(ResourceManager resourceManager, SpriteSource.Output output) {
      ResourceLocation resourceLocation = TEXTURE_ID_CONVERTER.idToFile(this.resource);
      Optional<Resource> optional = resourceManager.getResource(resourceLocation);
      if (optional.isPresent()) {
         LazyLoadedImage lazyLoadedImage = new LazyLoadedImage(resourceLocation, (Resource)optional.get(), this.regions.size());
         Iterator var6 = this.regions.iterator();

         while(var6.hasNext()) {
            Unstitcher.Region region = (Unstitcher.Region)var6.next();
            output.add(region.sprite, (SpriteSource.SpriteSupplier)(new Unstitcher.RegionInstance(lazyLoadedImage, region, this.xDivisor, this.yDivisor)));
         }
      } else {
         LOGGER.warn("Missing sprite: {}", resourceLocation);
      }

   }

   public SpriteSourceType type() {
      return SpriteSources.UNSTITCHER;
   }

   @Environment(EnvType.CLIENT)
   private static record Region(ResourceLocation sprite, double x, double y, double width, double height) {
      final ResourceLocation sprite;
      final double x;
      final double y;
      final double width;
      final double height;
      public static final Codec<Unstitcher.Region> CODEC = RecordCodecBuilder.create((instance) -> {
         return instance.group(ResourceLocation.CODEC.fieldOf("sprite").forGetter(Unstitcher.Region::sprite), Codec.DOUBLE.fieldOf("x").forGetter(Unstitcher.Region::x), Codec.DOUBLE.fieldOf("y").forGetter(Unstitcher.Region::y), Codec.DOUBLE.fieldOf("width").forGetter(Unstitcher.Region::width), Codec.DOUBLE.fieldOf("height").forGetter(Unstitcher.Region::height)).apply(instance, Unstitcher.Region::new);
      });

      private Region(ResourceLocation resourceLocation, double d, double e, double f, double g) {
         this.sprite = resourceLocation;
         this.x = d;
         this.y = e;
         this.width = f;
         this.height = g;
      }

      public ResourceLocation sprite() {
         return this.sprite;
      }

      public double x() {
         return this.x;
      }

      public double y() {
         return this.y;
      }

      public double width() {
         return this.width;
      }

      public double height() {
         return this.height;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class RegionInstance implements SpriteSource.SpriteSupplier {
      private final LazyLoadedImage image;
      private final Unstitcher.Region region;
      private final double xDivisor;
      private final double yDivisor;

      RegionInstance(LazyLoadedImage lazyLoadedImage, Unstitcher.Region region, double d, double e) {
         this.image = lazyLoadedImage;
         this.region = region;
         this.xDivisor = d;
         this.yDivisor = e;
      }

      public SpriteContents apply(SpriteResourceLoader spriteResourceLoader) {
         try {
            NativeImage nativeImage = this.image.get();
            double d = (double)nativeImage.getWidth() / this.xDivisor;
            double e = (double)nativeImage.getHeight() / this.yDivisor;
            int i = Mth.floor(this.region.x * d);
            int j = Mth.floor(this.region.y * e);
            int k = Mth.floor(this.region.width * d);
            int l = Mth.floor(this.region.height * e);
            NativeImage nativeImage2 = new NativeImage(NativeImage.Format.RGBA, k, l, false);
            nativeImage.copyRect(nativeImage2, i, j, 0, 0, k, l, false, false);
            SpriteContents var12 = new SpriteContents(this.region.sprite, new FrameSize(k, l), nativeImage2, ResourceMetadata.EMPTY);
            return var12;
         } catch (Exception var16) {
            Unstitcher.LOGGER.error("Failed to unstitch region {}", this.region.sprite, var16);
         } finally {
            this.image.release();
         }

         return MissingTextureAtlasSprite.create();
      }

      public void discard() {
         this.image.release();
      }

      // $FF: synthetic method
      public Object apply(final Object object) {
         return this.apply((SpriteResourceLoader)object);
      }
   }
}

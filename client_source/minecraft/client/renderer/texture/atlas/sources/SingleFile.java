package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SingleFile implements SpriteSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<SingleFile> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
      return instance.group(ResourceLocation.CODEC.fieldOf("resource").forGetter((singleFile) -> {
         return singleFile.resourceId;
      }), ResourceLocation.CODEC.optionalFieldOf("sprite").forGetter((singleFile) -> {
         return singleFile.spriteId;
      })).apply(instance, SingleFile::new);
   });
   private final ResourceLocation resourceId;
   private final Optional<ResourceLocation> spriteId;

   public SingleFile(ResourceLocation resourceLocation, Optional<ResourceLocation> optional) {
      this.resourceId = resourceLocation;
      this.spriteId = optional;
   }

   public void run(ResourceManager resourceManager, SpriteSource.Output output) {
      ResourceLocation resourceLocation = TEXTURE_ID_CONVERTER.idToFile(this.resourceId);
      Optional<Resource> optional = resourceManager.getResource(resourceLocation);
      if (optional.isPresent()) {
         output.add((ResourceLocation)this.spriteId.orElse(this.resourceId), (Resource)optional.get());
      } else {
         LOGGER.warn("Missing sprite: {}", resourceLocation);
      }

   }

   public SpriteSourceType type() {
      return SpriteSources.SINGLE_FILE;
   }
}

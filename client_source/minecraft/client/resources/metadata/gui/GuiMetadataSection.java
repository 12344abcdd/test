package net.minecraft.client.resources.metadata.gui;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.metadata.MetadataSectionType;

@Environment(EnvType.CLIENT)
public record GuiMetadataSection(GuiSpriteScaling scaling) {
   public static final GuiMetadataSection DEFAULT;
   public static final Codec<GuiMetadataSection> CODEC;
   public static final MetadataSectionType<GuiMetadataSection> TYPE;

   public GuiMetadataSection(GuiSpriteScaling guiSpriteScaling) {
      this.scaling = guiSpriteScaling;
   }

   public GuiSpriteScaling scaling() {
      return this.scaling;
   }

   static {
      DEFAULT = new GuiMetadataSection(GuiSpriteScaling.DEFAULT);
      CODEC = RecordCodecBuilder.create((instance) -> {
         return instance.group(GuiSpriteScaling.CODEC.optionalFieldOf("scaling", GuiSpriteScaling.DEFAULT).forGetter(GuiMetadataSection::scaling)).apply(instance, GuiMetadataSection::new);
      });
      TYPE = MetadataSectionType.fromCodec("gui", CODEC);
   }
}

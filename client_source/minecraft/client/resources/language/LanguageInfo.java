package net.minecraft.client.resources.language;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;

@Environment(EnvType.CLIENT)
public record LanguageInfo(String region, String name, boolean bidirectional) {
   public static final Codec<LanguageInfo> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(ExtraCodecs.NON_EMPTY_STRING.fieldOf("region").forGetter(LanguageInfo::region), ExtraCodecs.NON_EMPTY_STRING.fieldOf("name").forGetter(LanguageInfo::name), Codec.BOOL.optionalFieldOf("bidirectional", false).forGetter(LanguageInfo::bidirectional)).apply(instance, LanguageInfo::new);
   });

   public LanguageInfo(String string, String string2, boolean bl) {
      this.region = string;
      this.name = string2;
      this.bidirectional = bl;
   }

   public Component toComponent() {
      return Component.literal(this.name + " (" + this.region + ")");
   }

   public String region() {
      return this.region;
   }

   public String name() {
      return this.name;
   }

   public boolean bidirectional() {
      return this.bidirectional;
   }
}

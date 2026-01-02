package net.minecraft.client.particle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

@Environment(EnvType.CLIENT)
public class ParticleDescription {
   private final List<ResourceLocation> textures;

   private ParticleDescription(List<ResourceLocation> list) {
      this.textures = list;
   }

   public List<ResourceLocation> getTextures() {
      return this.textures;
   }

   public static ParticleDescription fromJson(JsonObject jsonObject) {
      JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "textures", (JsonArray)null);
      if (jsonArray == null) {
         return new ParticleDescription(List.of());
      } else {
         List<ResourceLocation> list = (List)Streams.stream(jsonArray).map((jsonElement) -> {
            return GsonHelper.convertToString(jsonElement, "texture");
         }).map(ResourceLocation::parse).collect(ImmutableList.toImmutableList());
         return new ParticleDescription(list);
      }
   }
}

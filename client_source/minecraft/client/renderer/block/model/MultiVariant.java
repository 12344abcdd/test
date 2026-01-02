package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class MultiVariant implements UnbakedModel {
   private final List<Variant> variants;

   public MultiVariant(List<Variant> list) {
      this.variants = list;
   }

   public List<Variant> getVariants() {
      return this.variants;
   }

   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (object instanceof MultiVariant) {
         MultiVariant multiVariant = (MultiVariant)object;
         return this.variants.equals(multiVariant.variants);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.variants.hashCode();
   }

   public Collection<ResourceLocation> getDependencies() {
      return (Collection)this.getVariants().stream().map(Variant::getModelLocation).collect(Collectors.toSet());
   }

   public void resolveParents(Function<ResourceLocation, UnbakedModel> function) {
      this.getVariants().stream().map(Variant::getModelLocation).distinct().forEach((resourceLocation) -> {
         ((UnbakedModel)function.apply(resourceLocation)).resolveParents(function);
      });
   }

   @Nullable
   public BakedModel bake(ModelBaker modelBaker, Function<Material, TextureAtlasSprite> function, ModelState modelState) {
      if (this.getVariants().isEmpty()) {
         return null;
      } else {
         WeightedBakedModel.Builder builder = new WeightedBakedModel.Builder();
         Iterator var5 = this.getVariants().iterator();

         while(var5.hasNext()) {
            Variant variant = (Variant)var5.next();
            BakedModel bakedModel = modelBaker.bake(variant.getModelLocation(), variant);
            builder.add(bakedModel, variant.getWeight());
         }

         return builder.build();
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Deserializer implements JsonDeserializer<MultiVariant> {
      public MultiVariant deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         List<Variant> list = Lists.newArrayList();
         if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            if (jsonArray.size() == 0) {
               throw new JsonParseException("Empty variant array");
            }

            Iterator var6 = jsonArray.iterator();

            while(var6.hasNext()) {
               JsonElement jsonElement2 = (JsonElement)var6.next();
               list.add((Variant)jsonDeserializationContext.deserialize(jsonElement2, Variant.class));
            }
         } else {
            list.add((Variant)jsonDeserializationContext.deserialize(jsonElement, Variant.class));
         }

         return new MultiVariant(list);
      }

      // $FF: synthetic method
      public Object deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         return this.deserialize(jsonElement, type, jsonDeserializationContext);
      }
   }
}

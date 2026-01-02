package net.minecraft.client.resources;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record PlayerSkin(ResourceLocation texture, @Nullable String textureUrl, @Nullable ResourceLocation capeTexture, @Nullable ResourceLocation elytraTexture, PlayerSkin.Model model, boolean secure) {
   public PlayerSkin(ResourceLocation resourceLocation, @Nullable String string, @Nullable ResourceLocation resourceLocation2, @Nullable ResourceLocation resourceLocation3, PlayerSkin.Model model, boolean bl) {
      this.texture = resourceLocation;
      this.textureUrl = string;
      this.capeTexture = resourceLocation2;
      this.elytraTexture = resourceLocation3;
      this.model = model;
      this.secure = bl;
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   @Nullable
   public String textureUrl() {
      return this.textureUrl;
   }

   @Nullable
   public ResourceLocation capeTexture() {
      return this.capeTexture;
   }

   @Nullable
   public ResourceLocation elytraTexture() {
      return this.elytraTexture;
   }

   public PlayerSkin.Model model() {
      return this.model;
   }

   public boolean secure() {
      return this.secure;
   }

   @Environment(EnvType.CLIENT)
   public static enum Model {
      SLIM("slim"),
      WIDE("default");

      private final String id;

      private Model(final String string2) {
         this.id = string2;
      }

      public static PlayerSkin.Model byName(@Nullable String string) {
         if (string == null) {
            return WIDE;
         } else {
            byte var2 = -1;
            switch(string.hashCode()) {
            case 3533117:
               if (string.equals("slim")) {
                  var2 = 0;
               }
            default:
               PlayerSkin.Model var10000;
               switch(var2) {
               case 0:
                  var10000 = SLIM;
                  break;
               default:
                  var10000 = WIDE;
               }

               return var10000;
            }
         }
      }

      public String id() {
         return this.id;
      }

      // $FF: synthetic method
      private static PlayerSkin.Model[] $values() {
         return new PlayerSkin.Model[]{SLIM, WIDE};
      }
   }
}

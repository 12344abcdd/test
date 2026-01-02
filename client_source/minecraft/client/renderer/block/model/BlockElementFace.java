package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record BlockElementFace(@Nullable Direction cullForDirection, int tintIndex, String texture, BlockFaceUV uv) {
   public static final int NO_TINT = -1;

   public BlockElementFace(@Nullable Direction direction, int i, String string, BlockFaceUV blockFaceUV) {
      this.cullForDirection = direction;
      this.tintIndex = i;
      this.texture = string;
      this.uv = blockFaceUV;
   }

   @Nullable
   public Direction cullForDirection() {
      return this.cullForDirection;
   }

   public int tintIndex() {
      return this.tintIndex;
   }

   public String texture() {
      return this.texture;
   }

   public BlockFaceUV uv() {
      return this.uv;
   }

   @Environment(EnvType.CLIENT)
   protected static class Deserializer implements JsonDeserializer<BlockElementFace> {
      private static final int DEFAULT_TINT_INDEX = -1;

      public BlockElementFace deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         JsonObject jsonObject = jsonElement.getAsJsonObject();
         Direction direction = this.getCullFacing(jsonObject);
         int i = this.getTintIndex(jsonObject);
         String string = this.getTexture(jsonObject);
         BlockFaceUV blockFaceUV = (BlockFaceUV)jsonDeserializationContext.deserialize(jsonObject, BlockFaceUV.class);
         return new BlockElementFace(direction, i, string, blockFaceUV);
      }

      protected int getTintIndex(JsonObject jsonObject) {
         return GsonHelper.getAsInt(jsonObject, "tintindex", -1);
      }

      private String getTexture(JsonObject jsonObject) {
         return GsonHelper.getAsString(jsonObject, "texture");
      }

      @Nullable
      private Direction getCullFacing(JsonObject jsonObject) {
         String string = GsonHelper.getAsString(jsonObject, "cullface", "");
         return Direction.byName(string);
      }

      // $FF: synthetic method
      public Object deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         return this.deserialize(jsonElement, type, jsonDeserializationContext);
      }
   }
}

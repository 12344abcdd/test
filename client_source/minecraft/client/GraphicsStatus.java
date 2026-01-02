package net.minecraft.client;

import java.util.function.IntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.OptionEnum;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;

@Environment(EnvType.CLIENT)
public enum GraphicsStatus implements OptionEnum {
   FAST(0, "options.graphics.fast"),
   FANCY(1, "options.graphics.fancy"),
   FABULOUS(2, "options.graphics.fabulous");

   private static final IntFunction<GraphicsStatus> BY_ID = ByIdMap.continuous(GraphicsStatus::getId, values(), OutOfBoundsStrategy.WRAP);
   private final int id;
   private final String key;

   private GraphicsStatus(final int j, final String string2) {
      this.id = j;
      this.key = string2;
   }

   public int getId() {
      return this.id;
   }

   public String getKey() {
      return this.key;
   }

   public String toString() {
      String var10000;
      switch(this.ordinal()) {
      case 0:
         var10000 = "fast";
         break;
      case 1:
         var10000 = "fancy";
         break;
      case 2:
         var10000 = "fabulous";
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public static GraphicsStatus byId(int i) {
      return (GraphicsStatus)BY_ID.apply(i);
   }

   // $FF: synthetic method
   private static GraphicsStatus[] $values() {
      return new GraphicsStatus[]{FAST, FANCY, FABULOUS};
   }
}

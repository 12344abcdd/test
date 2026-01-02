package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModelUtils {
   public static float rotlerpRad(float f, float g, float h) {
      float i;
      for(i = g - f; i < -3.1415927F; i += 6.2831855F) {
      }

      while(i >= 3.1415927F) {
         i -= 6.2831855F;
      }

      return f + h * i;
   }
}

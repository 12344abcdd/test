package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum Whence {
   ABSOLUTE,
   RELATIVE,
   END;

   // $FF: synthetic method
   private static Whence[] $values() {
      return new Whence[]{ABSOLUTE, RELATIVE, END};
   }
}

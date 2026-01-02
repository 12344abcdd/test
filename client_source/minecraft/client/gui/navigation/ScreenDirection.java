package net.minecraft.client.gui.navigation;

import it.unimi.dsi.fastutil.ints.IntComparator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum ScreenDirection {
   UP,
   DOWN,
   LEFT,
   RIGHT;

   private final IntComparator coordinateValueComparator = (ix, j) -> {
      return ix == j ? 0 : (this.isBefore(ix, j) ? -1 : 1);
   };

   public ScreenAxis getAxis() {
      ScreenAxis var10000;
      switch(this.ordinal()) {
      case 0:
      case 1:
         var10000 = ScreenAxis.VERTICAL;
         break;
      case 2:
      case 3:
         var10000 = ScreenAxis.HORIZONTAL;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public ScreenDirection getOpposite() {
      ScreenDirection var10000;
      switch(this.ordinal()) {
      case 0:
         var10000 = DOWN;
         break;
      case 1:
         var10000 = UP;
         break;
      case 2:
         var10000 = RIGHT;
         break;
      case 3:
         var10000 = LEFT;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public boolean isPositive() {
      boolean var10000;
      switch(this.ordinal()) {
      case 0:
      case 2:
         var10000 = false;
         break;
      case 1:
      case 3:
         var10000 = true;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public boolean isAfter(int i, int j) {
      if (this.isPositive()) {
         return i > j;
      } else {
         return j > i;
      }
   }

   public boolean isBefore(int i, int j) {
      if (this.isPositive()) {
         return i < j;
      } else {
         return j < i;
      }
   }

   public IntComparator coordinateValueComparator() {
      return this.coordinateValueComparator;
   }

   // $FF: synthetic method
   private static ScreenDirection[] $values() {
      return new ScreenDirection[]{UP, DOWN, LEFT, RIGHT};
   }
}

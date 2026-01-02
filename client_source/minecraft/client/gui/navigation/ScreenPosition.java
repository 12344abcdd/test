package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record ScreenPosition(int x, int y) {
   public ScreenPosition(int i, int j) {
      this.x = i;
      this.y = j;
   }

   public static ScreenPosition of(ScreenAxis screenAxis, int i, int j) {
      ScreenPosition var10000;
      switch(screenAxis) {
      case HORIZONTAL:
         var10000 = new ScreenPosition(i, j);
         break;
      case VERTICAL:
         var10000 = new ScreenPosition(j, i);
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public ScreenPosition step(ScreenDirection screenDirection) {
      ScreenPosition var10000;
      switch(screenDirection) {
      case DOWN:
         var10000 = new ScreenPosition(this.x, this.y + 1);
         break;
      case UP:
         var10000 = new ScreenPosition(this.x, this.y - 1);
         break;
      case LEFT:
         var10000 = new ScreenPosition(this.x - 1, this.y);
         break;
      case RIGHT:
         var10000 = new ScreenPosition(this.x + 1, this.y);
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public int getCoordinate(ScreenAxis screenAxis) {
      int var10000;
      switch(screenAxis) {
      case HORIZONTAL:
         var10000 = this.x;
         break;
      case VERTICAL:
         var10000 = this.y;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }
}

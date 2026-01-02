package net.minecraft.client.gui.navigation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record ScreenRectangle(ScreenPosition position, int width, int height) {
   private static final ScreenRectangle EMPTY = new ScreenRectangle(0, 0, 0, 0);

   public ScreenRectangle(int i, int j, int k, int l) {
      this(new ScreenPosition(i, j), k, l);
   }

   public ScreenRectangle(ScreenPosition screenPosition, int i, int j) {
      this.position = screenPosition;
      this.width = i;
      this.height = j;
   }

   public static ScreenRectangle empty() {
      return EMPTY;
   }

   public static ScreenRectangle of(ScreenAxis screenAxis, int i, int j, int k, int l) {
      ScreenRectangle var10000;
      switch(screenAxis) {
      case HORIZONTAL:
         var10000 = new ScreenRectangle(i, j, k, l);
         break;
      case VERTICAL:
         var10000 = new ScreenRectangle(j, i, l, k);
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public ScreenRectangle step(ScreenDirection screenDirection) {
      return new ScreenRectangle(this.position.step(screenDirection), this.width, this.height);
   }

   public int getLength(ScreenAxis screenAxis) {
      int var10000;
      switch(screenAxis) {
      case HORIZONTAL:
         var10000 = this.width;
         break;
      case VERTICAL:
         var10000 = this.height;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public int getBoundInDirection(ScreenDirection screenDirection) {
      ScreenAxis screenAxis = screenDirection.getAxis();
      return screenDirection.isPositive() ? this.position.getCoordinate(screenAxis) + this.getLength(screenAxis) - 1 : this.position.getCoordinate(screenAxis);
   }

   public ScreenRectangle getBorder(ScreenDirection screenDirection) {
      int i = this.getBoundInDirection(screenDirection);
      ScreenAxis screenAxis = screenDirection.getAxis().orthogonal();
      int j = this.getBoundInDirection(screenAxis.getNegative());
      int k = this.getLength(screenAxis);
      return of(screenDirection.getAxis(), i, j, 1, k).step(screenDirection);
   }

   public boolean overlaps(ScreenRectangle screenRectangle) {
      return this.overlapsInAxis(screenRectangle, ScreenAxis.HORIZONTAL) && this.overlapsInAxis(screenRectangle, ScreenAxis.VERTICAL);
   }

   public boolean overlapsInAxis(ScreenRectangle screenRectangle, ScreenAxis screenAxis) {
      int i = this.getBoundInDirection(screenAxis.getNegative());
      int j = screenRectangle.getBoundInDirection(screenAxis.getNegative());
      int k = this.getBoundInDirection(screenAxis.getPositive());
      int l = screenRectangle.getBoundInDirection(screenAxis.getPositive());
      return Math.max(i, j) <= Math.min(k, l);
   }

   public int getCenterInAxis(ScreenAxis screenAxis) {
      return (this.getBoundInDirection(screenAxis.getPositive()) + this.getBoundInDirection(screenAxis.getNegative())) / 2;
   }

   @Nullable
   public ScreenRectangle intersection(ScreenRectangle screenRectangle) {
      int i = Math.max(this.left(), screenRectangle.left());
      int j = Math.max(this.top(), screenRectangle.top());
      int k = Math.min(this.right(), screenRectangle.right());
      int l = Math.min(this.bottom(), screenRectangle.bottom());
      return i < k && j < l ? new ScreenRectangle(i, j, k - i, l - j) : null;
   }

   public int top() {
      return this.position.y();
   }

   public int bottom() {
      return this.position.y() + this.height;
   }

   public int left() {
      return this.position.x();
   }

   public int right() {
      return this.position.x() + this.width;
   }

   public boolean containsPoint(int i, int j) {
      return i >= this.left() && i < this.right() && j >= this.top() && j < this.bottom();
   }

   public ScreenPosition position() {
      return this.position;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }
}

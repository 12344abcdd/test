package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;

@Environment(EnvType.CLIENT)
public class LinearLayout implements Layout {
   private final GridLayout wrapped;
   private final LinearLayout.Orientation orientation;
   private int nextChildIndex;

   private LinearLayout(LinearLayout.Orientation orientation) {
      this(0, 0, orientation);
   }

   public LinearLayout(int i, int j, LinearLayout.Orientation orientation) {
      this.nextChildIndex = 0;
      this.wrapped = new GridLayout(i, j);
      this.orientation = orientation;
   }

   public LinearLayout spacing(int i) {
      this.orientation.setSpacing(this.wrapped, i);
      return this;
   }

   public LayoutSettings newCellSettings() {
      return this.wrapped.newCellSettings();
   }

   public LayoutSettings defaultCellSetting() {
      return this.wrapped.defaultCellSetting();
   }

   public <T extends LayoutElement> T addChild(T layoutElement, LayoutSettings layoutSettings) {
      return this.orientation.addChild(this.wrapped, layoutElement, this.nextChildIndex++, layoutSettings);
   }

   public <T extends LayoutElement> T addChild(T layoutElement) {
      return this.addChild(layoutElement, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T layoutElement, Consumer<LayoutSettings> consumer) {
      return this.orientation.addChild(this.wrapped, layoutElement, this.nextChildIndex++, (LayoutSettings)Util.make(this.newCellSettings(), consumer));
   }

   public void visitChildren(Consumer<LayoutElement> consumer) {
      this.wrapped.visitChildren(consumer);
   }

   public void arrangeElements() {
      this.wrapped.arrangeElements();
   }

   public int getWidth() {
      return this.wrapped.getWidth();
   }

   public int getHeight() {
      return this.wrapped.getHeight();
   }

   public void setX(int i) {
      this.wrapped.setX(i);
   }

   public void setY(int i) {
      this.wrapped.setY(i);
   }

   public int getX() {
      return this.wrapped.getX();
   }

   public int getY() {
      return this.wrapped.getY();
   }

   public static LinearLayout vertical() {
      return new LinearLayout(LinearLayout.Orientation.VERTICAL);
   }

   public static LinearLayout horizontal() {
      return new LinearLayout(LinearLayout.Orientation.HORIZONTAL);
   }

   @Environment(EnvType.CLIENT)
   public static enum Orientation {
      HORIZONTAL,
      VERTICAL;

      void setSpacing(GridLayout gridLayout, int i) {
         switch(this.ordinal()) {
         case 0:
            gridLayout.columnSpacing(i);
            break;
         case 1:
            gridLayout.rowSpacing(i);
         }

      }

      public <T extends LayoutElement> T addChild(GridLayout gridLayout, T layoutElement, int i, LayoutSettings layoutSettings) {
         LayoutElement var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = (LayoutElement)gridLayout.addChild(layoutElement, 0, i, (LayoutSettings)layoutSettings);
            break;
         case 1:
            var10000 = (LayoutElement)gridLayout.addChild(layoutElement, i, 0, (LayoutSettings)layoutSettings);
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      // $FF: synthetic method
      private static LinearLayout.Orientation[] $values() {
         return new LinearLayout.Orientation[]{HORIZONTAL, VERTICAL};
      }
   }
}

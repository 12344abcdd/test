package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;

@Environment(EnvType.CLIENT)
public class EqualSpacingLayout extends AbstractLayout {
   private final EqualSpacingLayout.Orientation orientation;
   private final List<EqualSpacingLayout.ChildContainer> children;
   private final LayoutSettings defaultChildLayoutSettings;

   public EqualSpacingLayout(int i, int j, EqualSpacingLayout.Orientation orientation) {
      this(0, 0, i, j, orientation);
   }

   public EqualSpacingLayout(int i, int j, int k, int l, EqualSpacingLayout.Orientation orientation) {
      super(i, j, k, l);
      this.children = new ArrayList();
      this.defaultChildLayoutSettings = LayoutSettings.defaults();
      this.orientation = orientation;
   }

   public void arrangeElements() {
      super.arrangeElements();
      if (!this.children.isEmpty()) {
         int i = 0;
         int j = this.orientation.getSecondaryLength((LayoutElement)this);

         EqualSpacingLayout.ChildContainer childContainer;
         for(Iterator var3 = this.children.iterator(); var3.hasNext(); j = Math.max(j, this.orientation.getSecondaryLength(childContainer))) {
            childContainer = (EqualSpacingLayout.ChildContainer)var3.next();
            i += this.orientation.getPrimaryLength(childContainer);
         }

         int k = this.orientation.getPrimaryLength((LayoutElement)this) - i;
         int l = this.orientation.getPrimaryPosition(this);
         Iterator<EqualSpacingLayout.ChildContainer> iterator = this.children.iterator();
         EqualSpacingLayout.ChildContainer childContainer2 = (EqualSpacingLayout.ChildContainer)iterator.next();
         this.orientation.setPrimaryPosition(childContainer2, l);
         l += this.orientation.getPrimaryLength(childContainer2);
         EqualSpacingLayout.ChildContainer childContainer3;
         if (this.children.size() >= 2) {
            for(Divisor divisor = new Divisor(k, this.children.size() - 1); divisor.hasNext(); l += this.orientation.getPrimaryLength(childContainer3)) {
               l += divisor.nextInt();
               childContainer3 = (EqualSpacingLayout.ChildContainer)iterator.next();
               this.orientation.setPrimaryPosition(childContainer3, l);
            }
         }

         int m = this.orientation.getSecondaryPosition(this);
         Iterator var13 = this.children.iterator();

         while(var13.hasNext()) {
            EqualSpacingLayout.ChildContainer childContainer4 = (EqualSpacingLayout.ChildContainer)var13.next();
            this.orientation.setSecondaryPosition(childContainer4, m, j);
         }

         switch(this.orientation.ordinal()) {
         case 0:
            this.height = j;
            break;
         case 1:
            this.width = j;
         }

      }
   }

   public void visitChildren(Consumer<LayoutElement> consumer) {
      this.children.forEach((childContainer) -> {
         consumer.accept(childContainer.child);
      });
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public <T extends LayoutElement> T addChild(T layoutElement) {
      return this.addChild(layoutElement, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T layoutElement, LayoutSettings layoutSettings) {
      this.children.add(new EqualSpacingLayout.ChildContainer(layoutElement, layoutSettings));
      return layoutElement;
   }

   public <T extends LayoutElement> T addChild(T layoutElement, Consumer<LayoutSettings> consumer) {
      return this.addChild(layoutElement, (LayoutSettings)Util.make(this.newChildLayoutSettings(), consumer));
   }

   @Environment(EnvType.CLIENT)
   public static enum Orientation {
      HORIZONTAL,
      VERTICAL;

      int getPrimaryLength(LayoutElement layoutElement) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = layoutElement.getWidth();
            break;
         case 1:
            var10000 = layoutElement.getHeight();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      int getPrimaryLength(EqualSpacingLayout.ChildContainer childContainer) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = childContainer.getWidth();
            break;
         case 1:
            var10000 = childContainer.getHeight();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      int getSecondaryLength(LayoutElement layoutElement) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = layoutElement.getHeight();
            break;
         case 1:
            var10000 = layoutElement.getWidth();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      int getSecondaryLength(EqualSpacingLayout.ChildContainer childContainer) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = childContainer.getHeight();
            break;
         case 1:
            var10000 = childContainer.getWidth();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      void setPrimaryPosition(EqualSpacingLayout.ChildContainer childContainer, int i) {
         switch(this.ordinal()) {
         case 0:
            childContainer.setX(i, childContainer.getWidth());
            break;
         case 1:
            childContainer.setY(i, childContainer.getHeight());
         }

      }

      void setSecondaryPosition(EqualSpacingLayout.ChildContainer childContainer, int i, int j) {
         switch(this.ordinal()) {
         case 0:
            childContainer.setY(i, j);
            break;
         case 1:
            childContainer.setX(i, j);
         }

      }

      int getPrimaryPosition(LayoutElement layoutElement) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = layoutElement.getX();
            break;
         case 1:
            var10000 = layoutElement.getY();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      int getSecondaryPosition(LayoutElement layoutElement) {
         int var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = layoutElement.getY();
            break;
         case 1:
            var10000 = layoutElement.getX();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      // $FF: synthetic method
      private static EqualSpacingLayout.Orientation[] $values() {
         return new EqualSpacingLayout.Orientation[]{HORIZONTAL, VERTICAL};
      }
   }

   @Environment(EnvType.CLIENT)
   private static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement layoutElement, LayoutSettings layoutSettings) {
         super(layoutElement, layoutSettings);
      }
   }
}

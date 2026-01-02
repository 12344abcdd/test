package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ComponentPath {
   static ComponentPath leaf(GuiEventListener guiEventListener) {
      return new ComponentPath.Leaf(guiEventListener);
   }

   @Nullable
   static ComponentPath path(ContainerEventHandler containerEventHandler, @Nullable ComponentPath componentPath) {
      return componentPath == null ? null : new ComponentPath.Path(containerEventHandler, componentPath);
   }

   static ComponentPath path(GuiEventListener guiEventListener, ContainerEventHandler... containerEventHandlers) {
      ComponentPath componentPath = leaf(guiEventListener);
      ContainerEventHandler[] var3 = containerEventHandlers;
      int var4 = containerEventHandlers.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ContainerEventHandler containerEventHandler = var3[var5];
         componentPath = path(containerEventHandler, componentPath);
      }

      return componentPath;
   }

   GuiEventListener component();

   void applyFocus(boolean bl);

   @Environment(EnvType.CLIENT)
   public static record Leaf(GuiEventListener component) implements ComponentPath {
      public Leaf(GuiEventListener guiEventListener) {
         this.component = guiEventListener;
      }

      public void applyFocus(boolean bl) {
         this.component.setFocused(bl);
      }

      public GuiEventListener component() {
         return this.component;
      }
   }

   @Environment(EnvType.CLIENT)
   public static record Path(ContainerEventHandler component, ComponentPath childPath) implements ComponentPath {
      public Path(ContainerEventHandler containerEventHandler, ComponentPath componentPath) {
         this.component = containerEventHandler;
         this.childPath = componentPath;
      }

      public void applyFocus(boolean bl) {
         if (!bl) {
            this.component.setFocused((GuiEventListener)null);
         } else {
            this.component.setFocused(this.childPath.component());
         }

         this.childPath.applyFocus(bl);
      }

      public ContainerEventHandler component() {
         return this.component;
      }

      public ComponentPath childPath() {
         return this.childPath;
      }

      // $FF: synthetic method
      public GuiEventListener component() {
         return this.component();
      }
   }
}

package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class ObjectSelectionList<E extends ObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
   private static final Component USAGE_NARRATION = Component.translatable("narration.selection.usage");

   public ObjectSelectionList(Minecraft minecraft, int i, int j, int k, int l) {
      super(minecraft, i, j, k, l);
   }

   @Nullable
   public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
      if (this.getItemCount() == 0) {
         return null;
      } else if (this.isFocused() && focusNavigationEvent instanceof FocusNavigationEvent.ArrowNavigation) {
         FocusNavigationEvent.ArrowNavigation arrowNavigation = (FocusNavigationEvent.ArrowNavigation)focusNavigationEvent;
         E entry = (ObjectSelectionList.Entry)this.nextEntry(arrowNavigation.direction());
         return entry != null ? ComponentPath.path((ContainerEventHandler)this, (ComponentPath)ComponentPath.leaf(entry)) : null;
      } else if (!this.isFocused()) {
         E entry2 = (ObjectSelectionList.Entry)this.getSelected();
         if (entry2 == null) {
            entry2 = (ObjectSelectionList.Entry)this.nextEntry(focusNavigationEvent.getVerticalDirectionForInitialFocus());
         }

         return entry2 == null ? null : ComponentPath.path((ContainerEventHandler)this, (ComponentPath)ComponentPath.leaf(entry2));
      } else {
         return null;
      }
   }

   public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      E entry = (ObjectSelectionList.Entry)this.getHovered();
      if (entry != null) {
         this.narrateListElementPosition(narrationElementOutput.nest(), entry);
         entry.updateNarration(narrationElementOutput);
      } else {
         E entry2 = (ObjectSelectionList.Entry)this.getSelected();
         if (entry2 != null) {
            this.narrateListElementPosition(narrationElementOutput.nest(), entry2);
            entry2.updateNarration(narrationElementOutput);
         }
      }

      if (this.isFocused()) {
         narrationElementOutput.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }

   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry<E extends ObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements NarrationSupplier {
      public abstract Component getNarration();

      public boolean mouseClicked(double d, double e, int i) {
         return true;
      }

      public void updateNarration(NarrationElementOutput narrationElementOutput) {
         narrationElementOutput.add(NarratedElementType.TITLE, this.getNarration());
      }
   }
}

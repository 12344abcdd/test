package net.minecraft.client.gui.components;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class ContainerObjectSelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
   public ContainerObjectSelectionList(Minecraft minecraft, int i, int j, int k, int l) {
      super(minecraft, i, j, k, l);
   }

   @Nullable
   public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
      if (this.getItemCount() == 0) {
         return null;
      } else if (!(focusNavigationEvent instanceof FocusNavigationEvent.ArrowNavigation)) {
         return super.nextFocusPath(focusNavigationEvent);
      } else {
         FocusNavigationEvent.ArrowNavigation arrowNavigation = (FocusNavigationEvent.ArrowNavigation)focusNavigationEvent;
         E entry = (ContainerObjectSelectionList.Entry)this.getFocused();
         if (arrowNavigation.direction().getAxis() == ScreenAxis.HORIZONTAL && entry != null) {
            return ComponentPath.path((ContainerEventHandler)this, (ComponentPath)entry.nextFocusPath(focusNavigationEvent));
         } else {
            int i = -1;
            ScreenDirection screenDirection = arrowNavigation.direction();
            if (entry != null) {
               i = entry.children().indexOf(entry.getFocused());
            }

            if (i == -1) {
               switch(screenDirection) {
               case LEFT:
                  i = Integer.MAX_VALUE;
                  screenDirection = ScreenDirection.DOWN;
                  break;
               case RIGHT:
                  i = 0;
                  screenDirection = ScreenDirection.DOWN;
                  break;
               default:
                  i = 0;
               }
            }

            ContainerObjectSelectionList.Entry entry2 = entry;

            ComponentPath componentPath;
            do {
               entry2 = (ContainerObjectSelectionList.Entry)this.nextEntry(screenDirection, (entryx) -> {
                  return !entryx.children().isEmpty();
               }, entry2);
               if (entry2 == null) {
                  return null;
               }

               componentPath = entry2.focusPathAtIndex(arrowNavigation, i);
            } while(componentPath == null);

            return ComponentPath.path((ContainerEventHandler)this, (ComponentPath)componentPath);
         }
      }
   }

   public void setFocused(@Nullable GuiEventListener guiEventListener) {
      if (this.getFocused() != guiEventListener) {
         super.setFocused(guiEventListener);
         if (guiEventListener == null) {
            this.setSelected((AbstractSelectionList.Entry)null);
         }

      }
   }

   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.isFocused() ? NarratableEntry.NarrationPriority.FOCUSED : super.narrationPriority();
   }

   protected boolean isSelectedItem(int i) {
      return false;
   }

   public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      E entry = (ContainerObjectSelectionList.Entry)this.getHovered();
      if (entry != null) {
         entry.updateNarration(narrationElementOutput.nest());
         this.narrateListElementPosition(narrationElementOutput, entry);
      } else {
         E entry2 = (ContainerObjectSelectionList.Entry)this.getFocused();
         if (entry2 != null) {
            entry2.updateNarration(narrationElementOutput.nest());
            this.narrateListElementPosition(narrationElementOutput, entry2);
         }
      }

      narrationElementOutput.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.component_list.usage"));
   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements ContainerEventHandler {
      @Nullable
      private GuiEventListener focused;
      @Nullable
      private NarratableEntry lastNarratable;
      private boolean dragging;

      public boolean isDragging() {
         return this.dragging;
      }

      public void setDragging(boolean bl) {
         this.dragging = bl;
      }

      public boolean mouseClicked(double d, double e, int i) {
         return ContainerEventHandler.super.mouseClicked(d, e, i);
      }

      public void setFocused(@Nullable GuiEventListener guiEventListener) {
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         if (guiEventListener != null) {
            guiEventListener.setFocused(true);
         }

         this.focused = guiEventListener;
      }

      @Nullable
      public GuiEventListener getFocused() {
         return this.focused;
      }

      @Nullable
      public ComponentPath focusPathAtIndex(FocusNavigationEvent focusNavigationEvent, int i) {
         if (this.children().isEmpty()) {
            return null;
         } else {
            ComponentPath componentPath = ((GuiEventListener)this.children().get(Math.min(i, this.children().size() - 1))).nextFocusPath(focusNavigationEvent);
            return ComponentPath.path((ContainerEventHandler)this, (ComponentPath)componentPath);
         }
      }

      @Nullable
      public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
         if (focusNavigationEvent instanceof FocusNavigationEvent.ArrowNavigation) {
            FocusNavigationEvent.ArrowNavigation arrowNavigation = (FocusNavigationEvent.ArrowNavigation)focusNavigationEvent;
            byte var10000;
            switch(arrowNavigation.direction()) {
            case LEFT:
               var10000 = -1;
               break;
            case RIGHT:
               var10000 = 1;
               break;
            case UP:
            case DOWN:
               var10000 = 0;
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
            }

            int i = var10000;
            if (i == 0) {
               return null;
            }

            int j = Mth.clamp(i + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1);

            for(int k = j; k >= 0 && k < this.children().size(); k += i) {
               GuiEventListener guiEventListener = (GuiEventListener)this.children().get(k);
               ComponentPath componentPath = guiEventListener.nextFocusPath(focusNavigationEvent);
               if (componentPath != null) {
                  return ComponentPath.path((ContainerEventHandler)this, (ComponentPath)componentPath);
               }
            }
         }

         return ContainerEventHandler.super.nextFocusPath(focusNavigationEvent);
      }

      public abstract List<? extends NarratableEntry> narratables();

      void updateNarration(NarrationElementOutput narrationElementOutput) {
         List<? extends NarratableEntry> list = this.narratables();
         Screen.NarratableSearchResult narratableSearchResult = Screen.findNarratableWidget(list, this.lastNarratable);
         if (narratableSearchResult != null) {
            if (narratableSearchResult.priority.isTerminal()) {
               this.lastNarratable = narratableSearchResult.entry;
            }

            if (list.size() > 1) {
               narrationElementOutput.add(NarratedElementType.POSITION, (Component)Component.translatable("narrator.position.object_list", new Object[]{narratableSearchResult.index + 1, list.size()}));
               if (narratableSearchResult.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                  narrationElementOutput.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.component_list.usage"));
               }
            }

            narratableSearchResult.entry.updateNarration(narrationElementOutput.nest());
         }

      }
   }
}

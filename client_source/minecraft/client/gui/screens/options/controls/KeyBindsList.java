package net.minecraft.client.gui.screens.options.controls;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class KeyBindsList extends ContainerObjectSelectionList<KeyBindsList.Entry> {
   private static final int ITEM_HEIGHT = 20;
   final KeyBindsScreen keyBindsScreen;
   private int maxNameWidth;

   public KeyBindsList(KeyBindsScreen keyBindsScreen, Minecraft minecraft) {
      super(minecraft, keyBindsScreen.width, keyBindsScreen.layout.getContentHeight(), keyBindsScreen.layout.getHeaderHeight(), 20);
      this.keyBindsScreen = keyBindsScreen;
      KeyMapping[] keyMappings = (KeyMapping[])ArrayUtils.clone(minecraft.options.keyMappings);
      Arrays.sort(keyMappings);
      String string = null;
      KeyMapping[] var5 = keyMappings;
      int var6 = keyMappings.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         KeyMapping keyMapping = var5[var7];
         String string2 = keyMapping.getCategory();
         if (!string2.equals(string)) {
            string = string2;
            this.addEntry(new KeyBindsList.CategoryEntry(Component.translatable(string2)));
         }

         Component component = Component.translatable(keyMapping.getName());
         int i = minecraft.font.width((FormattedText)component);
         if (i > this.maxNameWidth) {
            this.maxNameWidth = i;
         }

         this.addEntry(new KeyBindsList.KeyEntry(keyMapping, component));
      }

   }

   public void resetMappingAndUpdateButtons() {
      KeyMapping.resetMapping();
      this.refreshEntries();
   }

   public void refreshEntries() {
      this.children().forEach(KeyBindsList.Entry::refreshEntry);
   }

   public int getRowWidth() {
      return 340;
   }

   @Environment(EnvType.CLIENT)
   public class CategoryEntry extends KeyBindsList.Entry {
      final Component name;
      private final int width;

      public CategoryEntry(final Component component) {
         this.name = component;
         this.width = KeyBindsList.this.minecraft.font.width((FormattedText)this.name);
      }

      public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
         Font var10001 = KeyBindsList.this.minecraft.font;
         Component var10002 = this.name;
         int var10003 = KeyBindsList.this.width / 2 - this.width / 2;
         int var10004 = j + m;
         Objects.requireNonNull(KeyBindsList.this.minecraft.font);
         guiGraphics.drawString(var10001, (Component)var10002, var10003, var10004 - 9 - 1, -1, false);
      }

      @Nullable
      public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
         return null;
      }

      public List<? extends GuiEventListener> children() {
         return Collections.emptyList();
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            // $FF: synthetic field
            final KeyBindsList.CategoryEntry field_33831;

            {
               this.field_33831 = categoryEntry;
            }

            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            public void updateNarration(NarrationElementOutput narrationElementOutput) {
               narrationElementOutput.add(NarratedElementType.TITLE, this.field_33831.name);
            }
         });
      }

      protected void refreshEntry() {
      }
   }

   @Environment(EnvType.CLIENT)
   public class KeyEntry extends KeyBindsList.Entry {
      private static final Component RESET_BUTTON_TITLE = Component.translatable("controls.reset");
      private static final int PADDING = 10;
      private final KeyMapping key;
      private final Component name;
      private final Button changeButton;
      private final Button resetButton;
      private boolean hasCollision = false;

      KeyEntry(final KeyMapping keyMapping, final Component component) {
         this.key = keyMapping;
         this.name = component;
         this.changeButton = Button.builder(component, (button) -> {
            KeyBindsList.this.keyBindsScreen.selectedKey = keyMapping;
            KeyBindsList.this.resetMappingAndUpdateButtons();
         }).bounds(0, 0, 75, 20).createNarration((supplier) -> {
            return keyMapping.isUnbound() ? Component.translatable("narrator.controls.unbound", new Object[]{component}) : Component.translatable("narrator.controls.bound", new Object[]{component, supplier.get()});
         }).build();
         this.resetButton = Button.builder(RESET_BUTTON_TITLE, (button) -> {
            KeyBindsList.this.minecraft.options.setKey(keyMapping, keyMapping.getDefaultKey());
            KeyBindsList.this.resetMappingAndUpdateButtons();
         }).bounds(0, 0, 50, 20).createNarration((supplier) -> {
            return Component.translatable("narrator.controls.reset", new Object[]{component});
         }).build();
         this.refreshEntry();
      }

      public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
         int p = KeyBindsList.this.getScrollbarPosition() - this.resetButton.getWidth() - 10;
         int q = j - 2;
         this.resetButton.setPosition(p, q);
         this.resetButton.render(guiGraphics, n, o, f);
         int r = p - 5 - this.changeButton.getWidth();
         this.changeButton.setPosition(r, q);
         this.changeButton.render(guiGraphics, n, o, f);
         Font var10001 = KeyBindsList.this.minecraft.font;
         Component var10002 = this.name;
         int var10004 = j + m / 2;
         Objects.requireNonNull(KeyBindsList.this.minecraft.font);
         guiGraphics.drawString(var10001, (Component)var10002, k, var10004 - 9 / 2, -1);
         if (this.hasCollision) {
            int s = true;
            int t = this.changeButton.getX() - 6;
            guiGraphics.fill(t, j - 1, t + 3, j + m, -65536);
         }

      }

      public List<? extends GuiEventListener> children() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      protected void refreshEntry() {
         this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
         this.resetButton.active = !this.key.isDefault();
         this.hasCollision = false;
         MutableComponent mutableComponent = Component.empty();
         if (!this.key.isUnbound()) {
            KeyMapping[] var2 = KeyBindsList.this.minecraft.options.keyMappings;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               KeyMapping keyMapping = var2[var4];
               if (keyMapping != this.key && this.key.same(keyMapping)) {
                  if (this.hasCollision) {
                     mutableComponent.append(", ");
                  }

                  this.hasCollision = true;
                  mutableComponent.append(Component.translatable(keyMapping.getName()));
               }
            }
         }

         if (this.hasCollision) {
            this.changeButton.setMessage(Component.literal("[ ").append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE)).append(" ]").withStyle(ChatFormatting.RED));
            this.changeButton.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", new Object[]{mutableComponent})));
         } else {
            this.changeButton.setTooltip((Tooltip)null);
         }

         if (KeyBindsList.this.keyBindsScreen.selectedKey == this.key) {
            this.changeButton.setMessage(Component.literal("> ").append(this.changeButton.getMessage().copy().withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.UNDERLINE})).append(" <").withStyle(ChatFormatting.YELLOW));
         }

      }
   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry extends ContainerObjectSelectionList.Entry<KeyBindsList.Entry> {
      abstract void refreshEntry();
   }
}

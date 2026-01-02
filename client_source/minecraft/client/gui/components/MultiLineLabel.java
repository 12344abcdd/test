package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface MultiLineLabel {
   MultiLineLabel EMPTY = new MultiLineLabel() {
      public void renderCentered(GuiGraphics guiGraphics, int i, int j) {
      }

      public void renderCentered(GuiGraphics guiGraphics, int i, int j, int k, int l) {
      }

      public void renderLeftAligned(GuiGraphics guiGraphics, int i, int j, int k, int l) {
      }

      public int renderLeftAlignedNoShadow(GuiGraphics guiGraphics, int i, int j, int k, int l) {
         return j;
      }

      public int getLineCount() {
         return 0;
      }

      public int getWidth() {
         return 0;
      }
   };

   static MultiLineLabel create(Font font, Component... components) {
      return create(font, Integer.MAX_VALUE, Integer.MAX_VALUE, components);
   }

   static MultiLineLabel create(Font font, int i, Component... components) {
      return create(font, i, Integer.MAX_VALUE, components);
   }

   static MultiLineLabel create(Font font, Component component, int i) {
      return create(font, i, Integer.MAX_VALUE, component);
   }

   static MultiLineLabel create(Font font, int i, int j, Component... components) {
      return components.length == 0 ? EMPTY : new MultiLineLabel() {
         @Nullable
         private List<MultiLineLabel.TextAndWidth> cachedTextAndWidth;
         @Nullable
         private Language splitWithLanguage;

         public void renderCentered(GuiGraphics guiGraphics, int i, int j) {
            Objects.requireNonNull(font);
            this.renderCentered(guiGraphics, ix, jx, 9, -1);
         }

         public void renderCentered(GuiGraphics guiGraphics, int i, int j, int k, int l) {
            int m = jx;

            for(Iterator var7 = this.getSplitMessage().iterator(); var7.hasNext(); m += k) {
               MultiLineLabel.TextAndWidth textAndWidth = (MultiLineLabel.TextAndWidth)var7.next();
               guiGraphics.drawCenteredString(font, textAndWidth.text, ix, m, l);
            }

         }

         public void renderLeftAligned(GuiGraphics guiGraphics, int i, int j, int k, int l) {
            int m = jx;

            for(Iterator var7 = this.getSplitMessage().iterator(); var7.hasNext(); m += k) {
               MultiLineLabel.TextAndWidth textAndWidth = (MultiLineLabel.TextAndWidth)var7.next();
               guiGraphics.drawString(font, textAndWidth.text, ix, m, l);
            }

         }

         public int renderLeftAlignedNoShadow(GuiGraphics guiGraphics, int i, int j, int k, int l) {
            int m = jx;

            for(Iterator var7 = this.getSplitMessage().iterator(); var7.hasNext(); m += k) {
               MultiLineLabel.TextAndWidth textAndWidth = (MultiLineLabel.TextAndWidth)var7.next();
               guiGraphics.drawString(font, textAndWidth.text, ix, m, l, false);
            }

            return m;
         }

         private List<MultiLineLabel.TextAndWidth> getSplitMessage() {
            Language language = Language.getInstance();
            if (this.cachedTextAndWidth != null && language == this.splitWithLanguage) {
               return this.cachedTextAndWidth;
            } else {
               this.splitWithLanguage = language;
               List<FormattedCharSequence> list = new ArrayList();
               Component[] var3 = components;
               int var4 = var3.length;

               for(int var5 = 0; var5 < var4; ++var5) {
                  Component component = var3[var5];
                  list.addAll(font.split(component, i));
               }

               this.cachedTextAndWidth = new ArrayList();
               Iterator var7 = list.subList(0, Math.min(list.size(), j)).iterator();

               while(var7.hasNext()) {
                  FormattedCharSequence formattedCharSequence = (FormattedCharSequence)var7.next();
                  this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth(formattedCharSequence, font.width(formattedCharSequence)));
               }

               return this.cachedTextAndWidth;
            }
         }

         public int getLineCount() {
            return this.getSplitMessage().size();
         }

         public int getWidth() {
            return Math.min(i, this.getSplitMessage().stream().mapToInt(MultiLineLabel.TextAndWidth::width).max().orElse(0));
         }
      };
   }

   void renderCentered(GuiGraphics guiGraphics, int i, int j);

   void renderCentered(GuiGraphics guiGraphics, int i, int j, int k, int l);

   void renderLeftAligned(GuiGraphics guiGraphics, int i, int j, int k, int l);

   int renderLeftAlignedNoShadow(GuiGraphics guiGraphics, int i, int j, int k, int l);

   int getLineCount();

   int getWidth();

   @Environment(EnvType.CLIENT)
   public static record TextAndWidth(FormattedCharSequence text, int width) {
      final FormattedCharSequence text;

      public TextAndWidth(FormattedCharSequence formattedCharSequence, int i) {
         this.text = formattedCharSequence;
         this.width = i;
      }

      public FormattedCharSequence text() {
         return this.text;
      }

      public int width() {
         return this.width;
      }
   }
}

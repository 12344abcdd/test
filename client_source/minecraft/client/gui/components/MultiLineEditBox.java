package net.minecraft.client.gui.components;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.StringUtil;

@Environment(EnvType.CLIENT)
public class MultiLineEditBox extends AbstractScrollWidget {
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_INSERT_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   private static final int TEXT_COLOR = -2039584;
   private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
   private static final int CURSOR_BLINK_INTERVAL_MS = 300;
   private final Font font;
   private final Component placeholder;
   private final MultilineTextField textField;
   private long focusedTime = Util.getMillis();

   public MultiLineEditBox(Font font, int i, int j, int k, int l, Component component, Component component2) {
      super(i, j, k, l, component2);
      this.font = font;
      this.placeholder = component;
      this.textField = new MultilineTextField(font, k - this.totalInnerPadding());
      this.textField.setCursorListener(this::scrollToCursor);
   }

   public void setCharacterLimit(int i) {
      this.textField.setCharacterLimit(i);
   }

   public void setValueListener(Consumer<String> consumer) {
      this.textField.setValueListener(consumer);
   }

   public void setValue(String string) {
      this.textField.setValue(string);
   }

   public String getValue() {
      return this.textField.value();
   }

   public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      narrationElementOutput.add(NarratedElementType.TITLE, (Component)Component.translatable("gui.narrate.editBox", new Object[]{this.getMessage(), this.getValue()}));
   }

   public boolean mouseClicked(double d, double e, int i) {
      if (this.withinContentAreaPoint(d, e) && i == 0) {
         this.textField.setSelecting(Screen.hasShiftDown());
         this.seekCursorScreen(d, e);
         return true;
      } else {
         return super.mouseClicked(d, e, i);
      }
   }

   public boolean mouseDragged(double d, double e, int i, double f, double g) {
      if (super.mouseDragged(d, e, i, f, g)) {
         return true;
      } else if (this.withinContentAreaPoint(d, e) && i == 0) {
         this.textField.setSelecting(true);
         this.seekCursorScreen(d, e);
         this.textField.setSelecting(Screen.hasShiftDown());
         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(int i, int j, int k) {
      return this.textField.keyPressed(i);
   }

   public boolean charTyped(char c, int i) {
      if (this.visible && this.isFocused() && StringUtil.isAllowedChatCharacter(c)) {
         this.textField.insertText(Character.toString(c));
         return true;
      } else {
         return false;
      }
   }

   protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
      String string = this.textField.value();
      if (string.isEmpty() && !this.isFocused()) {
         guiGraphics.drawWordWrap(this.font, this.placeholder, this.getX() + this.innerPadding(), this.getY() + this.innerPadding(), this.width - this.totalInnerPadding(), -857677600);
      } else {
         int k = this.textField.cursor();
         boolean bl = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
         boolean bl2 = k < string.length();
         int l = 0;
         int m = 0;
         int n = this.getY() + this.innerPadding();

         int var10002;
         int var10004;
         for(Iterator var12 = this.textField.iterateLines().iterator(); var12.hasNext(); n += 9) {
            MultilineTextField.StringView stringView = (MultilineTextField.StringView)var12.next();
            Objects.requireNonNull(this.font);
            boolean bl3 = this.withinContentAreaTopBottom(n, n + 9);
            if (bl && bl2 && k >= stringView.beginIndex() && k <= stringView.endIndex()) {
               if (bl3) {
                  l = guiGraphics.drawString(this.font, string.substring(stringView.beginIndex(), k), this.getX() + this.innerPadding(), n, -2039584) - 1;
                  var10002 = n - 1;
                  int var10003 = l + 1;
                  var10004 = n + 1;
                  Objects.requireNonNull(this.font);
                  guiGraphics.fill(l, var10002, var10003, var10004 + 9, -3092272);
                  guiGraphics.drawString(this.font, string.substring(k, stringView.endIndex()), l, n, -2039584);
               }
            } else {
               if (bl3) {
                  l = guiGraphics.drawString(this.font, string.substring(stringView.beginIndex(), stringView.endIndex()), this.getX() + this.innerPadding(), n, -2039584) - 1;
               }

               m = n;
            }

            Objects.requireNonNull(this.font);
         }

         if (bl && !bl2) {
            Objects.requireNonNull(this.font);
            if (this.withinContentAreaTopBottom(m, m + 9)) {
               guiGraphics.drawString(this.font, "_", l, m, -3092272);
            }
         }

         if (this.textField.hasSelection()) {
            MultilineTextField.StringView stringView2 = this.textField.getSelected();
            int o = this.getX() + this.innerPadding();
            n = this.getY() + this.innerPadding();
            Iterator var20 = this.textField.iterateLines().iterator();

            while(var20.hasNext()) {
               MultilineTextField.StringView stringView3 = (MultilineTextField.StringView)var20.next();
               if (stringView2.beginIndex() > stringView3.endIndex()) {
                  Objects.requireNonNull(this.font);
                  n += 9;
               } else {
                  if (stringView3.beginIndex() > stringView2.endIndex()) {
                     break;
                  }

                  Objects.requireNonNull(this.font);
                  if (this.withinContentAreaTopBottom(n, n + 9)) {
                     int p = this.font.width(string.substring(stringView3.beginIndex(), Math.max(stringView2.beginIndex(), stringView3.beginIndex())));
                     int q;
                     if (stringView2.endIndex() > stringView3.endIndex()) {
                        q = this.width - this.innerPadding();
                     } else {
                        q = this.font.width(string.substring(stringView3.beginIndex(), stringView2.endIndex()));
                     }

                     var10002 = o + p;
                     var10004 = o + q;
                     Objects.requireNonNull(this.font);
                     this.renderHighlight(guiGraphics, var10002, n, var10004, n + 9);
                  }

                  Objects.requireNonNull(this.font);
                  n += 9;
               }
            }
         }

      }
   }

   protected void renderDecorations(GuiGraphics guiGraphics) {
      super.renderDecorations(guiGraphics);
      if (this.textField.hasCharacterLimit()) {
         int i = this.textField.characterLimit();
         Component component = Component.translatable("gui.multiLineEditBox.character_limit", new Object[]{this.textField.value().length(), i});
         guiGraphics.drawString(this.font, (Component)component, this.getX() + this.width - this.font.width((FormattedText)component), this.getY() + this.height + 4, 10526880);
      }

   }

   public int getInnerHeight() {
      Objects.requireNonNull(this.font);
      return 9 * this.textField.getLineCount();
   }

   protected boolean scrollbarVisible() {
      return (double)this.textField.getLineCount() > this.getDisplayableLineCount();
   }

   protected double scrollRate() {
      Objects.requireNonNull(this.font);
      return 9.0D / 2.0D;
   }

   private void renderHighlight(GuiGraphics guiGraphics, int i, int j, int k, int l) {
      guiGraphics.fill(RenderType.guiTextHighlight(), i, j, k, l, -16776961);
   }

   private void scrollToCursor() {
      double d = this.scrollAmount();
      MultilineTextField var10000 = this.textField;
      Objects.requireNonNull(this.font);
      MultilineTextField.StringView stringView = var10000.getLineView((int)(d / 9.0D));
      int var5;
      if (this.textField.cursor() <= stringView.beginIndex()) {
         var5 = this.textField.getLineAtCursor();
         Objects.requireNonNull(this.font);
         d = (double)(var5 * 9);
      } else {
         var10000 = this.textField;
         double var10001 = d + (double)this.height;
         Objects.requireNonNull(this.font);
         MultilineTextField.StringView stringView2 = var10000.getLineView((int)(var10001 / 9.0D) - 1);
         if (this.textField.cursor() > stringView2.endIndex()) {
            var5 = this.textField.getLineAtCursor();
            Objects.requireNonNull(this.font);
            var5 = var5 * 9 - this.height;
            Objects.requireNonNull(this.font);
            d = (double)(var5 + 9 + this.totalInnerPadding());
         }
      }

      this.setScrollAmount(d);
   }

   private double getDisplayableLineCount() {
      double var10000 = (double)(this.height - this.totalInnerPadding());
      Objects.requireNonNull(this.font);
      return var10000 / 9.0D;
   }

   private void seekCursorScreen(double d, double e) {
      double f = d - (double)this.getX() - (double)this.innerPadding();
      double g = e - (double)this.getY() - (double)this.innerPadding() + this.scrollAmount();
      this.textField.seekCursorToPoint(f, g);
   }

   public void setFocused(boolean bl) {
      super.setFocused(bl);
      if (bl) {
         this.focusedTime = Util.getMillis();
      }

   }
}

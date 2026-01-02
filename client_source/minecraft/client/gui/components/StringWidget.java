package net.minecraft.client.gui.components;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

@Environment(EnvType.CLIENT)
public class StringWidget extends AbstractStringWidget {
   private float alignX;

   public StringWidget(Component component, Font font) {
      int var10003 = font.width(component.getVisualOrderText());
      Objects.requireNonNull(font);
      this(0, 0, var10003, 9, component, font);
   }

   public StringWidget(int i, int j, Component component, Font font) {
      this(0, 0, i, j, component, font);
   }

   public StringWidget(int i, int j, int k, int l, Component component, Font font) {
      super(i, j, k, l, component, font);
      this.alignX = 0.5F;
      this.active = false;
   }

   public StringWidget setColor(int i) {
      super.setColor(i);
      return this;
   }

   private StringWidget horizontalAlignment(float f) {
      this.alignX = f;
      return this;
   }

   public StringWidget alignLeft() {
      return this.horizontalAlignment(0.0F);
   }

   public StringWidget alignCenter() {
      return this.horizontalAlignment(0.5F);
   }

   public StringWidget alignRight() {
      return this.horizontalAlignment(1.0F);
   }

   public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
      Component component = this.getMessage();
      Font font = this.getFont();
      int k = this.getWidth();
      int l = font.width((FormattedText)component);
      int m = this.getX() + Math.round(this.alignX * (float)(k - l));
      int var10000 = this.getY();
      int var10001 = this.getHeight();
      Objects.requireNonNull(font);
      int n = var10000 + (var10001 - 9) / 2;
      FormattedCharSequence formattedCharSequence = l > k ? this.clipText(component, k) : component.getVisualOrderText();
      guiGraphics.drawString(font, formattedCharSequence, m, n, this.getColor());
   }

   private FormattedCharSequence clipText(Component component, int i) {
      Font font = this.getFont();
      FormattedText formattedText = font.substrByWidth(component, i - font.width((FormattedText)CommonComponents.ELLIPSIS));
      return Language.getInstance().getVisualOrder(FormattedText.composite(new FormattedText[]{formattedText, CommonComponents.ELLIPSIS}));
   }

   // $FF: synthetic method
   public AbstractStringWidget setColor(final int i) {
      return this.setColor(i);
   }
}

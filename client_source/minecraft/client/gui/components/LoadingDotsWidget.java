package net.minecraft.client.gui.components;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LoadingDotsWidget extends AbstractWidget {
   private final Font font;

   public LoadingDotsWidget(Font font, Component component) {
      int var10003 = font.width((FormattedText)component);
      Objects.requireNonNull(font);
      super(0, 0, var10003, 9 * 3, component);
      this.font = font;
   }

   protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
      int k = this.getX() + this.getWidth() / 2;
      int l = this.getY() + this.getHeight() / 2;
      Component component = this.getMessage();
      Font var10001 = this.font;
      int var10003 = k - this.font.width((FormattedText)component) / 2;
      Objects.requireNonNull(this.font);
      guiGraphics.drawString(var10001, (Component)component, var10003, l - 9, -1, false);
      String string = LoadingDotsText.get(Util.getMillis());
      var10001 = this.font;
      var10003 = k - this.font.width(string) / 2;
      Objects.requireNonNull(this.font);
      guiGraphics.drawString(var10001, string, var10003, l + 9, -8355712, false);
   }

   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }

   public void playDownSound(SoundManager soundManager) {
   }

   public boolean isActive() {
      return false;
   }

   @Nullable
   public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
      return null;
   }
}

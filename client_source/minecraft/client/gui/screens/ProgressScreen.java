package net.minecraft.client.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProgressListener;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ProgressScreen extends Screen implements ProgressListener {
   @Nullable
   private Component header;
   @Nullable
   private Component stage;
   private int progress;
   private boolean stop;
   private final boolean clearScreenAfterStop;

   public ProgressScreen(boolean bl) {
      super(GameNarrator.NO_TITLE);
      this.clearScreenAfterStop = bl;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   protected boolean shouldNarrateNavigation() {
      return false;
   }

   public void progressStartNoAbort(Component component) {
      this.progressStart(component);
   }

   public void progressStart(Component component) {
      this.header = component;
      this.progressStage(Component.translatable("menu.working"));
   }

   public void progressStage(Component component) {
      this.stage = component;
      this.progressStagePercentage(0);
   }

   public void progressStagePercentage(int i) {
      this.progress = i;
   }

   public void stop() {
      this.stop = true;
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      if (this.stop) {
         if (this.clearScreenAfterStop) {
            this.minecraft.setScreen((Screen)null);
         }

      } else {
         super.render(guiGraphics, i, j, f);
         if (this.header != null) {
            guiGraphics.drawCenteredString(this.font, (Component)this.header, this.width / 2, 70, 16777215);
         }

         if (this.stage != null && this.progress != 0) {
            guiGraphics.drawCenteredString(this.font, (Component)Component.empty().append(this.stage).append(" " + this.progress + "%"), this.width / 2, 90, 16777215);
         }

      }
   }
}

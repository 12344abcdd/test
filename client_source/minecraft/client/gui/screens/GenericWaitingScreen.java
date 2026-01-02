package net.minecraft.client.gui.screens;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GenericWaitingScreen extends Screen {
   private static final int TITLE_Y = 80;
   private static final int MESSAGE_Y = 120;
   private static final int MESSAGE_MAX_WIDTH = 360;
   @Nullable
   private final Component messageText;
   private final Component buttonLabel;
   private final Runnable buttonCallback;
   @Nullable
   private MultiLineLabel message;
   private Button button;
   private int disableButtonTicks;

   public static GenericWaitingScreen createWaiting(Component component, Component component2, Runnable runnable) {
      return new GenericWaitingScreen(component, (Component)null, component2, runnable, 0);
   }

   public static GenericWaitingScreen createCompleted(Component component, Component component2, Component component3, Runnable runnable) {
      return new GenericWaitingScreen(component, component2, component3, runnable, 20);
   }

   protected GenericWaitingScreen(Component component, @Nullable Component component2, Component component3, Runnable runnable, int i) {
      super(component);
      this.messageText = component2;
      this.buttonLabel = component3;
      this.buttonCallback = runnable;
      this.disableButtonTicks = i;
   }

   protected void init() {
      super.init();
      if (this.messageText != null) {
         this.message = MultiLineLabel.create(this.font, this.messageText, 360);
      }

      int i = true;
      int j = true;
      int k = this.message != null ? this.message.getLineCount() : 1;
      int var10000 = Math.max(k, 5);
      Objects.requireNonNull(this.font);
      int l = var10000 * 9;
      int m = Math.min(120 + l, this.height - 40);
      this.button = (Button)this.addRenderableWidget(Button.builder(this.buttonLabel, (button) -> {
         this.onClose();
      }).bounds((this.width - 150) / 2, m, 150, 20).build());
   }

   public void tick() {
      if (this.disableButtonTicks > 0) {
         --this.disableButtonTicks;
      }

      this.button.active = this.disableButtonTicks == 0;
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      guiGraphics.drawCenteredString(this.font, (Component)this.title, this.width / 2, 80, 16777215);
      if (this.message == null) {
         String string = LoadingDotsText.get(Util.getMillis());
         guiGraphics.drawCenteredString(this.font, (String)string, this.width / 2, 120, 10526880);
      } else {
         this.message.renderCentered(guiGraphics, this.width / 2, 120);
      }

   }

   public boolean shouldCloseOnEsc() {
      return this.message != null && this.button.active;
   }

   public void onClose() {
      this.buttonCallback.run();
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(new Component[]{this.title, this.messageText != null ? this.messageText : CommonComponents.EMPTY});
   }
}

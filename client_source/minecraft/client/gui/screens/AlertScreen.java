package net.minecraft.client.gui.screens;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class AlertScreen extends Screen {
   private static final int LABEL_Y = 90;
   private final Component messageText;
   private MultiLineLabel message;
   private final Runnable callback;
   private final Component okButton;
   private final boolean shouldCloseOnEsc;

   public AlertScreen(Runnable runnable, Component component, Component component2) {
      this(runnable, component, component2, CommonComponents.GUI_BACK, true);
   }

   public AlertScreen(Runnable runnable, Component component, Component component2, Component component3, boolean bl) {
      super(component);
      this.message = MultiLineLabel.EMPTY;
      this.callback = runnable;
      this.messageText = component2;
      this.okButton = component3;
      this.shouldCloseOnEsc = bl;
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(new Component[]{super.getNarrationMessage(), this.messageText});
   }

   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.messageText, this.width - 50);
      int var10000 = this.message.getLineCount();
      Objects.requireNonNull(this.font);
      int i = var10000 * 9;
      int j = Mth.clamp(90 + i + 12, this.height / 6 + 96, this.height - 24);
      int k = true;
      this.addRenderableWidget(Button.builder(this.okButton, (button) -> {
         this.callback.run();
      }).bounds((this.width - 150) / 2, j, 150, 20).build());
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      guiGraphics.drawCenteredString(this.font, (Component)this.title, this.width / 2, 70, 16777215);
      this.message.renderCentered(guiGraphics, this.width / 2, 90);
   }

   public boolean shouldCloseOnEsc() {
      return this.shouldCloseOnEsc;
   }
}

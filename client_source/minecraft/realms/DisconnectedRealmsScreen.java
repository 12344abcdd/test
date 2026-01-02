package net.minecraft.realms;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class DisconnectedRealmsScreen extends RealmsScreen {
   private final Component reason;
   private MultiLineLabel message;
   private final Screen parent;
   private int textHeight;

   public DisconnectedRealmsScreen(Screen screen, Component component, Component component2) {
      super(component);
      this.message = MultiLineLabel.EMPTY;
      this.parent = screen;
      this.reason = component2;
   }

   public void init() {
      this.minecraft.getDownloadedPackSource().cleanupAfterDisconnect();
      this.message = MultiLineLabel.create(this.font, this.reason, this.width - 50);
      int var10001 = this.message.getLineCount();
      Objects.requireNonNull(this.font);
      this.textHeight = var10001 * 9;
      Button.Builder var1 = Button.builder(CommonComponents.GUI_BACK, (button) -> {
         this.minecraft.setScreen(this.parent);
      });
      int var10002 = this.width / 2 - 100;
      int var10003 = this.height / 2 + this.textHeight / 2;
      Objects.requireNonNull(this.font);
      this.addRenderableWidget(var1.bounds(var10002, var10003 + 9, 200, 20).build());
   }

   public Component getNarrationMessage() {
      return Component.empty().append(this.title).append(": ").append(this.reason);
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      Font var10001 = this.font;
      Component var10002 = this.title;
      int var10003 = this.width / 2;
      int var10004 = this.height / 2 - this.textHeight / 2;
      Objects.requireNonNull(this.font);
      guiGraphics.drawCenteredString(var10001, var10002, var10003, var10004 - 9 * 2, 11184810);
      this.message.renderCentered(guiGraphics, this.width / 2, this.height / 2 - this.textHeight / 2);
   }
}

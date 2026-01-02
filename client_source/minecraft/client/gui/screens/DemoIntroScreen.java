package net.minecraft.client.gui.screens;

import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonLinks;

@Environment(EnvType.CLIENT)
public class DemoIntroScreen extends Screen {
   private static final ResourceLocation DEMO_BACKGROUND_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png");
   private MultiLineLabel movementMessage;
   private MultiLineLabel durationMessage;

   public DemoIntroScreen() {
      super(Component.translatable("demo.help.title"));
      this.movementMessage = MultiLineLabel.EMPTY;
      this.durationMessage = MultiLineLabel.EMPTY;
   }

   protected void init() {
      int i = true;
      this.addRenderableWidget(Button.builder(Component.translatable("demo.help.buy"), (button) -> {
         button.active = false;
         Util.getPlatform().openUri(CommonLinks.BUY_MINECRAFT_JAVA);
      }).bounds(this.width / 2 - 116, this.height / 2 + 62 + -16, 114, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("demo.help.later"), (button) -> {
         this.minecraft.setScreen((Screen)null);
         this.minecraft.mouseHandler.grabMouse();
      }).bounds(this.width / 2 + 2, this.height / 2 + 62 + -16, 114, 20).build());
      Options options = this.minecraft.options;
      this.movementMessage = MultiLineLabel.create(this.font, Component.translatable("demo.help.movementShort", new Object[]{options.keyUp.getTranslatedKeyMessage(), options.keyLeft.getTranslatedKeyMessage(), options.keyDown.getTranslatedKeyMessage(), options.keyRight.getTranslatedKeyMessage()}), Component.translatable("demo.help.movementMouse"), Component.translatable("demo.help.jump", new Object[]{options.keyJump.getTranslatedKeyMessage()}), Component.translatable("demo.help.inventory", new Object[]{options.keyInventory.getTranslatedKeyMessage()}));
      this.durationMessage = MultiLineLabel.create(this.font, Component.translatable("demo.help.fullWrapped"), 218);
   }

   public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
      super.renderBackground(guiGraphics, i, j, f);
      int k = (this.width - 248) / 2;
      int l = (this.height - 166) / 2;
      guiGraphics.blit(DEMO_BACKGROUND_LOCATION, k, l, 0, 0, 248, 166);
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      int k = (this.width - 248) / 2 + 10;
      int l = (this.height - 166) / 2 + 8;
      guiGraphics.drawString(this.font, this.title, k, l, 2039583, false);
      l = this.movementMessage.renderLeftAlignedNoShadow(guiGraphics, k, l + 12, 12, 5197647);
      MultiLineLabel var10000 = this.durationMessage;
      int var10003 = l + 20;
      Objects.requireNonNull(this.font);
      var10000.renderLeftAlignedNoShadow(guiGraphics, k, var10003, 9, 2039583);
   }
}

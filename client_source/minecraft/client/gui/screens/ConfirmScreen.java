package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Iterator;
import java.util.List;
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
public class ConfirmScreen extends Screen {
   private static final int MARGIN = 20;
   private final Component message;
   private MultiLineLabel multilineMessage;
   protected Component yesButton;
   protected Component noButton;
   private int delayTicker;
   protected final BooleanConsumer callback;
   private final List<Button> exitButtons;

   public ConfirmScreen(BooleanConsumer booleanConsumer, Component component, Component component2) {
      this(booleanConsumer, component, component2, CommonComponents.GUI_YES, CommonComponents.GUI_NO);
   }

   public ConfirmScreen(BooleanConsumer booleanConsumer, Component component, Component component2, Component component3, Component component4) {
      super(component);
      this.multilineMessage = MultiLineLabel.EMPTY;
      this.exitButtons = Lists.newArrayList();
      this.callback = booleanConsumer;
      this.message = component2;
      this.yesButton = component3;
      this.noButton = component4;
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(new Component[]{super.getNarrationMessage(), this.message});
   }

   protected void init() {
      super.init();
      this.multilineMessage = MultiLineLabel.create(this.font, this.message, this.width - 50);
      int i = Mth.clamp(this.messageTop() + this.messageHeight() + 20, this.height / 6 + 96, this.height - 24);
      this.exitButtons.clear();
      this.addButtons(i);
   }

   protected void addButtons(int i) {
      this.addExitButton(Button.builder(this.yesButton, (button) -> {
         this.callback.accept(true);
      }).bounds(this.width / 2 - 155, i, 150, 20).build());
      this.addExitButton(Button.builder(this.noButton, (button) -> {
         this.callback.accept(false);
      }).bounds(this.width / 2 - 155 + 160, i, 150, 20).build());
   }

   protected void addExitButton(Button button) {
      this.exitButtons.add((Button)this.addRenderableWidget(button));
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.titleTop(), 16777215);
      this.multilineMessage.renderCentered(guiGraphics, this.width / 2, this.messageTop());
   }

   private int titleTop() {
      int i = (this.height - this.messageHeight()) / 2;
      int var10000 = i - 20;
      Objects.requireNonNull(this.font);
      return Mth.clamp(var10000 - 9, 10, 80);
   }

   private int messageTop() {
      return this.titleTop() + 20;
   }

   private int messageHeight() {
      int var10000 = this.multilineMessage.getLineCount();
      Objects.requireNonNull(this.font);
      return var10000 * 9;
   }

   public void setDelay(int i) {
      this.delayTicker = i;

      Button button;
      for(Iterator var2 = this.exitButtons.iterator(); var2.hasNext(); button.active = false) {
         button = (Button)var2.next();
      }

   }

   public void tick() {
      super.tick();
      Button button;
      if (--this.delayTicker == 0) {
         for(Iterator var1 = this.exitButtons.iterator(); var1.hasNext(); button.active = true) {
            button = (Button)var1.next();
         }
      }

   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean keyPressed(int i, int j, int k) {
      if (i == 256) {
         this.callback.accept(false);
         return true;
      } else {
         return super.keyPressed(i, j, k);
      }
   }
}

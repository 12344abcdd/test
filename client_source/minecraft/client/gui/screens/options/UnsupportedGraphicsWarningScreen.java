package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;

@Environment(EnvType.CLIENT)
public class UnsupportedGraphicsWarningScreen extends Screen {
   private static final int BUTTON_PADDING = 20;
   private static final int BUTTON_MARGIN = 5;
   private static final int BUTTON_HEIGHT = 20;
   private final Component narrationMessage;
   private final List<Component> message;
   private final ImmutableList<UnsupportedGraphicsWarningScreen.ButtonOption> buttonOptions;
   private MultiLineLabel messageLines;
   private int contentTop;
   private int buttonWidth;

   protected UnsupportedGraphicsWarningScreen(Component component, List<Component> list, ImmutableList<UnsupportedGraphicsWarningScreen.ButtonOption> immutableList) {
      super(component);
      this.messageLines = MultiLineLabel.EMPTY;
      this.message = list;
      this.narrationMessage = CommonComponents.joinForNarration(new Component[]{component, ComponentUtils.formatList(list, CommonComponents.EMPTY)});
      this.buttonOptions = immutableList;
   }

   public Component getNarrationMessage() {
      return this.narrationMessage;
   }

   public void init() {
      UnsupportedGraphicsWarningScreen.ButtonOption buttonOption;
      for(UnmodifiableIterator var1 = this.buttonOptions.iterator(); var1.hasNext(); this.buttonWidth = Math.max(this.buttonWidth, 20 + this.font.width((FormattedText)buttonOption.message) + 20)) {
         buttonOption = (UnsupportedGraphicsWarningScreen.ButtonOption)var1.next();
      }

      int i = 5 + this.buttonWidth + 5;
      int j = i * this.buttonOptions.size();
      this.messageLines = MultiLineLabel.create(this.font, j, (Component[])this.message.toArray(new Component[0]));
      int var10000 = this.messageLines.getLineCount();
      Objects.requireNonNull(this.font);
      int k = var10000 * 9;
      this.contentTop = (int)((double)this.height / 2.0D - (double)k / 2.0D);
      var10000 = this.contentTop + k;
      Objects.requireNonNull(this.font);
      int l = var10000 + 9 * 2;
      int m = (int)((double)this.width / 2.0D - (double)j / 2.0D);

      for(UnmodifiableIterator var6 = this.buttonOptions.iterator(); var6.hasNext(); m += i) {
         UnsupportedGraphicsWarningScreen.ButtonOption buttonOption2 = (UnsupportedGraphicsWarningScreen.ButtonOption)var6.next();
         this.addRenderableWidget(Button.builder(buttonOption2.message, buttonOption2.onPress).bounds(m, l, this.buttonWidth, 20).build());
      }

   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      Font var10001 = this.font;
      Component var10002 = this.title;
      int var10003 = this.width / 2;
      int var10004 = this.contentTop;
      Objects.requireNonNull(this.font);
      guiGraphics.drawCenteredString(var10001, (Component)var10002, var10003, var10004 - 9 * 2, -1);
      this.messageLines.renderCentered(guiGraphics, this.width / 2, this.contentTop);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   public static final class ButtonOption {
      final Component message;
      final Button.OnPress onPress;

      public ButtonOption(Component component, Button.OnPress onPress) {
         this.message = component;
         this.onPress = onPress;
      }
   }
}

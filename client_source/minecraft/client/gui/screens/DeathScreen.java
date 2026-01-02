package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DeathScreen extends Screen {
   private static final ResourceLocation DRAFT_REPORT_SPRITE = ResourceLocation.withDefaultNamespace("icon/draft_report");
   private int delayTicker;
   private final Component causeOfDeath;
   private final boolean hardcore;
   private Component deathScore;
   private final List<Button> exitButtons = Lists.newArrayList();
   @Nullable
   private Button exitToTitleButton;

   public DeathScreen(@Nullable Component component, boolean bl) {
      super(Component.translatable(bl ? "deathScreen.title.hardcore" : "deathScreen.title"));
      this.causeOfDeath = component;
      this.hardcore = bl;
   }

   protected void init() {
      this.delayTicker = 0;
      this.exitButtons.clear();
      Component component = this.hardcore ? Component.translatable("deathScreen.spectate") : Component.translatable("deathScreen.respawn");
      this.exitButtons.add((Button)this.addRenderableWidget(Button.builder(component, (button) -> {
         this.minecraft.player.respawn();
         button.active = false;
      }).bounds(this.width / 2 - 100, this.height / 4 + 72, 200, 20).build()));
      this.exitToTitleButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("deathScreen.titleScreen"), (button) -> {
         this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::handleExitToTitleScreen, true);
      }).bounds(this.width / 2 - 100, this.height / 4 + 96, 200, 20).build());
      this.exitButtons.add(this.exitToTitleButton);
      this.setButtonsActive(false);
      this.deathScore = Component.translatable("deathScreen.score.value", new Object[]{Component.literal(Integer.toString(this.minecraft.player.getScore())).withStyle(ChatFormatting.YELLOW)});
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void handleExitToTitleScreen() {
      if (this.hardcore) {
         this.exitToTitleScreen();
      } else {
         ConfirmScreen confirmScreen = new DeathScreen.TitleConfirmScreen((bl) -> {
            if (bl) {
               this.exitToTitleScreen();
            } else {
               this.minecraft.player.respawn();
               this.minecraft.setScreen((Screen)null);
            }

         }, Component.translatable("deathScreen.quit.confirm"), CommonComponents.EMPTY, Component.translatable("deathScreen.titleScreen"), Component.translatable("deathScreen.respawn"));
         this.minecraft.setScreen(confirmScreen);
         confirmScreen.setDelay(20);
      }
   }

   private void exitToTitleScreen() {
      if (this.minecraft.level != null) {
         this.minecraft.level.disconnect();
      }

      this.minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
      this.minecraft.setScreen(new TitleScreen());
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      guiGraphics.pose().pushPose();
      guiGraphics.pose().scale(2.0F, 2.0F, 2.0F);
      guiGraphics.drawCenteredString(this.font, (Component)this.title, this.width / 2 / 2, 30, 16777215);
      guiGraphics.pose().popPose();
      if (this.causeOfDeath != null) {
         guiGraphics.drawCenteredString(this.font, (Component)this.causeOfDeath, this.width / 2, 85, 16777215);
      }

      guiGraphics.drawCenteredString(this.font, (Component)this.deathScore, this.width / 2, 100, 16777215);
      if (this.causeOfDeath != null && j > 85) {
         Objects.requireNonNull(this.font);
         if (j < 85 + 9) {
            Style style = this.getClickedComponentStyleAt(i);
            guiGraphics.renderComponentHoverEffect(this.font, style, i, j);
         }
      }

      if (this.exitToTitleButton != null && this.minecraft.getReportingContext().hasDraftReport()) {
         guiGraphics.blitSprite(DRAFT_REPORT_SPRITE, this.exitToTitleButton.getX() + this.exitToTitleButton.getWidth() - 17, this.exitToTitleButton.getY() + 3, 15, 15);
      }

   }

   public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
      renderDeathBackground(guiGraphics, this.width, this.height);
   }

   static void renderDeathBackground(GuiGraphics guiGraphics, int i, int j) {
      guiGraphics.fillGradient(0, 0, i, j, 1615855616, -1602211792);
   }

   @Nullable
   private Style getClickedComponentStyleAt(int i) {
      if (this.causeOfDeath == null) {
         return null;
      } else {
         int j = this.minecraft.font.width((FormattedText)this.causeOfDeath);
         int k = this.width / 2 - j / 2;
         int l = this.width / 2 + j / 2;
         return i >= k && i <= l ? this.minecraft.font.getSplitter().componentStyleAtWidth((FormattedText)this.causeOfDeath, i - k) : null;
      }
   }

   public boolean mouseClicked(double d, double e, int i) {
      if (this.causeOfDeath != null && e > 85.0D) {
         Objects.requireNonNull(this.font);
         if (e < (double)(85 + 9)) {
            Style style = this.getClickedComponentStyleAt((int)d);
            if (style != null && style.getClickEvent() != null && style.getClickEvent().getAction() == Action.OPEN_URL) {
               this.handleComponentClicked(style);
               return false;
            }
         }
      }

      return super.mouseClicked(d, e, i);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      super.tick();
      ++this.delayTicker;
      if (this.delayTicker == 20) {
         this.setButtonsActive(true);
      }

   }

   private void setButtonsActive(boolean bl) {
      Button button;
      for(Iterator var2 = this.exitButtons.iterator(); var2.hasNext(); button.active = bl) {
         button = (Button)var2.next();
      }

   }

   @Environment(EnvType.CLIENT)
   public static class TitleConfirmScreen extends ConfirmScreen {
      public TitleConfirmScreen(BooleanConsumer booleanConsumer, Component component, Component component2, Component component3, Component component4) {
         super(booleanConsumer, component, component2, component3, component4);
      }

      public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
         DeathScreen.renderDeathBackground(guiGraphics, this.width, this.height);
      }
   }
}

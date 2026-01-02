package net.minecraft.client.gui.screens.telemetry;

import java.net.URI;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TelemetryInfoScreen extends Screen {
   private static final Component TITLE = Component.translatable("telemetry_info.screen.title");
   private static final Component DESCRIPTION = Component.translatable("telemetry_info.screen.description").withColor(-4539718);
   private static final Component BUTTON_PRIVACY_STATEMENT = Component.translatable("telemetry_info.button.privacy_statement");
   private static final Component BUTTON_GIVE_FEEDBACK = Component.translatable("telemetry_info.button.give_feedback");
   private static final Component BUTTON_VIEW_DATA = Component.translatable("telemetry_info.button.show_data");
   private static final Component CHECKBOX_OPT_IN = Component.translatable("telemetry_info.opt_in.description");
   private static final int SPACING = 8;
   private static final boolean EXTRA_TELEMETRY_AVAILABLE = Minecraft.getInstance().extraTelemetryAvailable();
   private final Screen lastScreen;
   private final Options options;
   private final HeaderAndFooterLayout layout;
   @Nullable
   private TelemetryEventWidget telemetryEventWidget;
   @Nullable
   private MultiLineTextWidget description;
   private double savedScroll;

   public TelemetryInfoScreen(Screen screen, Options options) {
      super(TITLE);
      Objects.requireNonNull(Minecraft.getInstance().font);
      this.layout = new HeaderAndFooterLayout(this, 16 + 9 * 5 + 20, EXTRA_TELEMETRY_AVAILABLE ? 33 + Checkbox.getBoxSize(Minecraft.getInstance().font) : 33);
      this.lastScreen = screen;
      this.options = options;
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(new Component[]{super.getNarrationMessage(), DESCRIPTION});
   }

   protected void init() {
      LinearLayout linearLayout = (LinearLayout)this.layout.addToHeader(LinearLayout.vertical().spacing(4));
      linearLayout.defaultCellSetting().alignHorizontallyCenter();
      linearLayout.addChild(new StringWidget(TITLE, this.font));
      this.description = (MultiLineTextWidget)linearLayout.addChild((new MultiLineTextWidget(DESCRIPTION, this.font)).setCentered(true));
      LinearLayout linearLayout2 = (LinearLayout)linearLayout.addChild(LinearLayout.horizontal().spacing(8));
      linearLayout2.addChild(Button.builder(BUTTON_PRIVACY_STATEMENT, this::openPrivacyStatementLink).build());
      linearLayout2.addChild(Button.builder(BUTTON_GIVE_FEEDBACK, this::openFeedbackLink).build());
      LinearLayout linearLayout3 = (LinearLayout)this.layout.addToFooter(LinearLayout.vertical().spacing(4));
      if (EXTRA_TELEMETRY_AVAILABLE) {
         linearLayout3.addChild(this.createTelemetryCheckbox());
      }

      LinearLayout linearLayout4 = (LinearLayout)linearLayout3.addChild(LinearLayout.horizontal().spacing(8));
      linearLayout4.addChild(Button.builder(BUTTON_VIEW_DATA, this::openDataFolder).build());
      linearLayout4.addChild(Button.builder(CommonComponents.GUI_DONE, (button) -> {
         this.onClose();
      }).build());
      LinearLayout linearLayout5 = (LinearLayout)this.layout.addToContents(LinearLayout.vertical().spacing(8));
      this.telemetryEventWidget = (TelemetryEventWidget)linearLayout5.addChild(new TelemetryEventWidget(0, 0, this.width - 40, this.layout.getContentHeight(), this.font));
      this.telemetryEventWidget.setOnScrolledListener((d) -> {
         this.savedScroll = d;
      });
      this.layout.visitWidgets((guiEventListener) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
      });
      this.repositionElements();
   }

   protected void repositionElements() {
      if (this.telemetryEventWidget != null) {
         this.telemetryEventWidget.setScrollAmount(this.savedScroll);
         this.telemetryEventWidget.setWidth(this.width - 40);
         this.telemetryEventWidget.setHeight(this.layout.getContentHeight());
         this.telemetryEventWidget.updateLayout();
      }

      if (this.description != null) {
         this.description.setMaxWidth(this.width - 16);
      }

      this.layout.arrangeElements();
   }

   protected void setInitialFocus() {
      if (this.telemetryEventWidget != null) {
         this.setInitialFocus(this.telemetryEventWidget);
      }

   }

   private AbstractWidget createTelemetryCheckbox() {
      OptionInstance<Boolean> optionInstance = this.options.telemetryOptInExtra();
      return Checkbox.builder(CHECKBOX_OPT_IN, this.font).selected(optionInstance).onValueChange(this::onOptInChanged).build();
   }

   private void onOptInChanged(AbstractWidget abstractWidget, boolean bl) {
      if (this.telemetryEventWidget != null) {
         this.telemetryEventWidget.onOptInChanged(bl);
      }

   }

   private void openPrivacyStatementLink(Button button) {
      ConfirmLinkScreen.confirmLinkNow(this, (URI)CommonLinks.PRIVACY_STATEMENT);
   }

   private void openFeedbackLink(Button button) {
      ConfirmLinkScreen.confirmLinkNow(this, (URI)CommonLinks.RELEASE_FEEDBACK);
   }

   private void openDataFolder(Button button) {
      Util.getPlatform().openPath(this.minecraft.getTelemetryManager().getLogDirectory());
   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }
}

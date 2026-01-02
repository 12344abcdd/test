package net.minecraft.client.gui.screens.reporting;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.NameReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class NameReportScreen extends AbstractReportScreen<NameReport.Builder> {
   private static final Component TITLE = Component.translatable("gui.abuseReport.name.title");
   private MultiLineEditBox commentBox;

   private NameReportScreen(Screen screen, ReportingContext reportingContext, NameReport.Builder builder) {
      super(TITLE, screen, reportingContext, builder);
   }

   public NameReportScreen(Screen screen, ReportingContext reportingContext, UUID uUID, String string) {
      this(screen, reportingContext, new NameReport.Builder(uUID, string, reportingContext.sender().reportLimits()));
   }

   public NameReportScreen(Screen screen, ReportingContext reportingContext, NameReport nameReport) {
      this(screen, reportingContext, new NameReport.Builder(nameReport, reportingContext.sender().reportLimits()));
   }

   protected void addContent() {
      Component component = Component.literal(((NameReport)((NameReport.Builder)this.reportBuilder).report()).getReportedName()).withStyle(ChatFormatting.YELLOW);
      this.layout.addChild(new StringWidget(Component.translatable("gui.abuseReport.name.reporting", new Object[]{component}), this.font), (Consumer)((layoutSettings) -> {
         layoutSettings.alignHorizontallyLeft().padding(0, 8);
      }));
      Objects.requireNonNull(this.font);
      this.commentBox = this.createCommentBox(280, 9 * 8, (string) -> {
         ((NameReport.Builder)this.reportBuilder).setComments(string);
         this.onReportChanged();
      });
      this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, MORE_COMMENTS_LABEL, (layoutSettings) -> {
         layoutSettings.paddingBottom(12);
      }));
   }

   public boolean mouseReleased(double d, double e, int i) {
      return super.mouseReleased(d, e, i) ? true : this.commentBox.mouseReleased(d, e, i);
   }
}

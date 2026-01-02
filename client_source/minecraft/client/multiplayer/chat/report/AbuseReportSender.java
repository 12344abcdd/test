package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;

@Environment(EnvType.CLIENT)
public interface AbuseReportSender {
   static AbuseReportSender create(ReportEnvironment reportEnvironment, UserApiService userApiService) {
      return new AbuseReportSender.Services(reportEnvironment, userApiService);
   }

   CompletableFuture<Unit> send(UUID uUID, ReportType reportType, AbuseReport abuseReport);

   boolean isEnabled();

   default AbuseReportLimits reportLimits() {
      return AbuseReportLimits.DEFAULTS;
   }

   @Environment(EnvType.CLIENT)
   public static record Services(ReportEnvironment environment, UserApiService userApiService) implements AbuseReportSender {
      private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
      private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
      private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

      public Services(ReportEnvironment reportEnvironment, UserApiService userApiService) {
         this.environment = reportEnvironment;
         this.userApiService = userApiService;
      }

      public CompletableFuture<Unit> send(UUID uUID, ReportType reportType, AbuseReport abuseReport) {
         return CompletableFuture.supplyAsync(() -> {
            AbuseReportRequest abuseReportRequest = new AbuseReportRequest(1, uUID, abuseReport, this.environment.clientInfo(), this.environment.thirdPartyServerInfo(), this.environment.realmInfo(), reportType.backendName());

            Component component;
            try {
               this.userApiService.reportAbuse(abuseReportRequest);
               return Unit.INSTANCE;
            } catch (MinecraftClientHttpException var7) {
               component = this.getHttpErrorDescription(var7);
               throw new CompletionException(new AbuseReportSender.SendException(component, var7));
            } catch (MinecraftClientException var8) {
               component = this.getErrorDescription(var8);
               throw new CompletionException(new AbuseReportSender.SendException(component, var8));
            }
         }, Util.ioPool());
      }

      public boolean isEnabled() {
         return this.userApiService.canSendReports();
      }

      private Component getHttpErrorDescription(MinecraftClientHttpException minecraftClientHttpException) {
         return Component.translatable("gui.abuseReport.send.error_message", new Object[]{minecraftClientHttpException.getMessage()});
      }

      private Component getErrorDescription(MinecraftClientException minecraftClientException) {
         Component var10000;
         switch(minecraftClientException.getType()) {
         case SERVICE_UNAVAILABLE:
            var10000 = SERVICE_UNAVAILABLE_TEXT;
            break;
         case HTTP_ERROR:
            var10000 = HTTP_ERROR_TEXT;
            break;
         case JSON_ERROR:
            var10000 = JSON_ERROR_TEXT;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      public AbuseReportLimits reportLimits() {
         return this.userApiService.getAbuseReportLimits();
      }

      public ReportEnvironment environment() {
         return this.environment;
      }

      public UserApiService userApiService() {
         return this.userApiService;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class SendException extends ThrowingComponent {
      public SendException(Component component, Throwable throwable) {
         super(component, throwable);
      }
   }
}

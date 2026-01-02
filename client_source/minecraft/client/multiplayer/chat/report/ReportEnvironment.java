package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ClientInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.RealmInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ThirdPartyServerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record ReportEnvironment(String clientVersion, @Nullable ReportEnvironment.Server server) {
   public ReportEnvironment(String string, @Nullable ReportEnvironment.Server server) {
      this.clientVersion = string;
      this.server = server;
   }

   public static ReportEnvironment local() {
      return create((ReportEnvironment.Server)null);
   }

   public static ReportEnvironment thirdParty(String string) {
      return create(new ReportEnvironment.Server.ThirdParty(string));
   }

   public static ReportEnvironment realm(RealmsServer realmsServer) {
      return create(new ReportEnvironment.Server.Realm(realmsServer));
   }

   public static ReportEnvironment create(@Nullable ReportEnvironment.Server server) {
      return new ReportEnvironment(getClientVersion(), server);
   }

   public ClientInfo clientInfo() {
      return new ClientInfo(this.clientVersion, Locale.getDefault().toLanguageTag());
   }

   @Nullable
   public ThirdPartyServerInfo thirdPartyServerInfo() {
      ReportEnvironment.Server var2 = this.server;
      if (var2 instanceof ReportEnvironment.Server.ThirdParty) {
         ReportEnvironment.Server.ThirdParty thirdParty = (ReportEnvironment.Server.ThirdParty)var2;
         return new ThirdPartyServerInfo(thirdParty.ip);
      } else {
         return null;
      }
   }

   @Nullable
   public RealmInfo realmInfo() {
      ReportEnvironment.Server var2 = this.server;
      if (var2 instanceof ReportEnvironment.Server.Realm) {
         ReportEnvironment.Server.Realm realm = (ReportEnvironment.Server.Realm)var2;
         return new RealmInfo(String.valueOf(realm.realmId()), realm.slotId());
      } else {
         return null;
      }
   }

   private static String getClientVersion() {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("1.21.1");
      if (Minecraft.checkModStatus().shouldReportAsModified()) {
         stringBuilder.append(" (modded)");
      }

      return stringBuilder.toString();
   }

   public String clientVersion() {
      return this.clientVersion;
   }

   @Nullable
   public ReportEnvironment.Server server() {
      return this.server;
   }

   @Environment(EnvType.CLIENT)
   public interface Server {
      @Environment(EnvType.CLIENT)
      public static record Realm(long realmId, int slotId) implements ReportEnvironment.Server {
         public Realm(RealmsServer realmsServer) {
            this(realmsServer.id, realmsServer.activeSlot);
         }

         public Realm(long l, int i) {
            this.realmId = l;
            this.slotId = i;
         }

         public long realmId() {
            return this.realmId;
         }

         public int slotId() {
            return this.slotId;
         }
      }

      @Environment(EnvType.CLIENT)
      public static record ThirdParty(String ip) implements ReportEnvironment.Server {
         final String ip;

         public ThirdParty(String string) {
            this.ip = string;
         }

         public String ip() {
            return this.ip;
         }
      }
   }
}

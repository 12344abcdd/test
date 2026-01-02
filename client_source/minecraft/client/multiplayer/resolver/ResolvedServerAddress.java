package net.minecraft.client.multiplayer.resolver;

import java.net.InetSocketAddress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface ResolvedServerAddress {
   String getHostName();

   String getHostIp();

   int getPort();

   InetSocketAddress asInetSocketAddress();

   static ResolvedServerAddress from(InetSocketAddress inetSocketAddress) {
      return new ResolvedServerAddress() {
         public String getHostName() {
            return inetSocketAddress.getAddress().getHostName();
         }

         public String getHostIp() {
            return inetSocketAddress.getAddress().getHostAddress();
         }

         public int getPort() {
            return inetSocketAddress.getPort();
         }

         public InetSocketAddress asInetSocketAddress() {
            return inetSocketAddress;
         }
      };
   }
}

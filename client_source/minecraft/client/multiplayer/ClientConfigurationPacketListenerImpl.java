package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundResetChatPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientConfigurationPacketListenerImpl extends ClientCommonPacketListenerImpl implements ClientConfigurationPacketListener, TickablePacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final GameProfile localGameProfile;
   private FeatureFlagSet enabledFeatures;
   private final Frozen receivedRegistries;
   private final RegistryDataCollector registryDataCollector = new RegistryDataCollector();
   @Nullable
   private KnownPacksManager knownPacks;
   @Nullable
   protected ChatComponent.State chatState;

   public ClientConfigurationPacketListenerImpl(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
      super(minecraft, connection, commonListenerCookie);
      this.localGameProfile = commonListenerCookie.localGameProfile();
      this.receivedRegistries = commonListenerCookie.receivedRegistries();
      this.enabledFeatures = commonListenerCookie.enabledFeatures();
      this.chatState = commonListenerCookie.chatState();
   }

   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   protected void handleCustomPayload(CustomPacketPayload customPacketPayload) {
      this.handleUnknownCustomPayload(customPacketPayload);
   }

   private void handleUnknownCustomPayload(CustomPacketPayload customPacketPayload) {
      LOGGER.warn("Unknown custom packet payload: {}", customPacketPayload.type().id());
   }

   public void handleRegistryData(ClientboundRegistryDataPacket clientboundRegistryDataPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundRegistryDataPacket, this, this.minecraft);
      this.registryDataCollector.appendContents(clientboundRegistryDataPacket.registry(), clientboundRegistryDataPacket.entries());
   }

   public void handleUpdateTags(ClientboundUpdateTagsPacket clientboundUpdateTagsPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundUpdateTagsPacket, this, this.minecraft);
      this.registryDataCollector.appendTags(clientboundUpdateTagsPacket.getTags());
   }

   public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket clientboundUpdateEnabledFeaturesPacket) {
      this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(clientboundUpdateEnabledFeaturesPacket.features());
   }

   public void handleSelectKnownPacks(ClientboundSelectKnownPacks clientboundSelectKnownPacks) {
      PacketUtils.ensureRunningOnSameThread(clientboundSelectKnownPacks, this, this.minecraft);
      if (this.knownPacks == null) {
         this.knownPacks = new KnownPacksManager();
      }

      List<KnownPack> list = this.knownPacks.trySelectingPacks(clientboundSelectKnownPacks.knownPacks());
      this.send(new ServerboundSelectKnownPacks(list));
   }

   public void handleResetChat(ClientboundResetChatPacket clientboundResetChatPacket) {
      this.chatState = null;
   }

   private <T> T runWithResources(Function<ResourceProvider, T> function) {
      if (this.knownPacks == null) {
         return function.apply(ResourceProvider.EMPTY);
      } else {
         CloseableResourceManager closeableResourceManager = this.knownPacks.createResourceManager();

         Object var3;
         try {
            var3 = function.apply(closeableResourceManager);
         } catch (Throwable var6) {
            if (closeableResourceManager != null) {
               try {
                  closeableResourceManager.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (closeableResourceManager != null) {
            closeableResourceManager.close();
         }

         return var3;
      }
   }

   public void handleConfigurationFinished(ClientboundFinishConfigurationPacket clientboundFinishConfigurationPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundFinishConfigurationPacket, this, this.minecraft);
      Frozen frozen = (Frozen)this.runWithResources((resourceProvider) -> {
         return this.registryDataCollector.collectGameRegistries(resourceProvider, this.receivedRegistries, this.connection.isMemoryConnection());
      });
      this.connection.setupInboundProtocol(GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(frozen)), new ClientPacketListener(this.minecraft, this.connection, new CommonListenerCookie(this.localGameProfile, this.telemetryManager, frozen, this.enabledFeatures, this.serverBrand, this.serverData, this.postDisconnectScreen, this.serverCookies, this.chatState, this.strictErrorHandling, this.customReportDetails, this.serverLinks)));
      this.connection.send(ServerboundFinishConfigurationPacket.INSTANCE);
      this.connection.setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(frozen)));
   }

   public void tick() {
      this.sendDeferredPackets();
   }

   public void onDisconnect(DisconnectionDetails disconnectionDetails) {
      super.onDisconnect(disconnectionDetails);
      this.minecraft.clearDownloadedResourcePacks();
   }
}

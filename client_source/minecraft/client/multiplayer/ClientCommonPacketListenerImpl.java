package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.ServerLinks.Entry;
import net.minecraft.server.ServerLinks.KnownLinkType;
import net.minecraft.server.ServerLinks.UntrustedEntry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public abstract class ClientCommonPacketListenerImpl implements ClientCommonPacketListener {
   private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Minecraft minecraft;
   protected final Connection connection;
   @Nullable
   protected final ServerData serverData;
   @Nullable
   protected String serverBrand;
   protected final WorldSessionTelemetryManager telemetryManager;
   @Nullable
   protected final Screen postDisconnectScreen;
   protected boolean isTransferring;
   /** @deprecated */
   @Deprecated(
      forRemoval = true
   )
   protected final boolean strictErrorHandling;
   private final List<ClientCommonPacketListenerImpl.DeferredPacket> deferredPackets = new ArrayList();
   protected final Map<ResourceLocation, byte[]> serverCookies;
   protected Map<String, String> customReportDetails;
   protected ServerLinks serverLinks;

   protected ClientCommonPacketListenerImpl(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
      this.minecraft = minecraft;
      this.connection = connection;
      this.serverData = commonListenerCookie.serverData();
      this.serverBrand = commonListenerCookie.serverBrand();
      this.telemetryManager = commonListenerCookie.telemetryManager();
      this.postDisconnectScreen = commonListenerCookie.postDisconnectScreen();
      this.serverCookies = commonListenerCookie.serverCookies();
      this.strictErrorHandling = commonListenerCookie.strictErrorHandling();
      this.customReportDetails = commonListenerCookie.customReportDetails();
      this.serverLinks = commonListenerCookie.serverLinks();
   }

   public void onPacketError(Packet packet, Exception exception) {
      LOGGER.error("Failed to handle packet {}", packet, exception);
      Optional<Path> optional = this.storeDisconnectionReport(packet, exception);
      Optional<URI> optional2 = this.serverLinks.findKnownType(KnownLinkType.BUG_REPORT).map(Entry::link);
      if (this.strictErrorHandling) {
         this.connection.disconnect(new DisconnectionDetails(Component.translatable("disconnect.packetError"), optional, optional2));
      }

   }

   public DisconnectionDetails createDisconnectionInfo(Component component, Throwable throwable) {
      Optional<Path> optional = this.storeDisconnectionReport((Packet)null, throwable);
      Optional<URI> optional2 = this.serverLinks.findKnownType(KnownLinkType.BUG_REPORT).map(Entry::link);
      return new DisconnectionDetails(component, optional, optional2);
   }

   private Optional<Path> storeDisconnectionReport(@Nullable Packet packet, Throwable throwable) {
      CrashReport crashReport = CrashReport.forThrowable(throwable, "Packet handling error");
      PacketUtils.fillCrashReport(crashReport, this, packet);
      Path path = this.minecraft.gameDirectory.toPath().resolve("debug");
      Path path2 = path.resolve("disconnect-" + Util.getFilenameFormattedDateTime() + "-client.txt");
      Optional<Entry> optional = this.serverLinks.findKnownType(KnownLinkType.BUG_REPORT);
      List<String> list = (List)optional.map((entry) -> {
         return List.of("Server bug reporting link: " + String.valueOf(entry.link()));
      }).orElse(List.of());
      return crashReport.saveToFile(path2, ReportType.NETWORK_PROTOCOL_ERROR, list) ? Optional.of(path2) : Optional.empty();
   }

   public boolean shouldHandleMessage(Packet<?> packet) {
      if (super.shouldHandleMessage(packet)) {
         return true;
      } else {
         return this.isTransferring && (packet instanceof ClientboundStoreCookiePacket || packet instanceof ClientboundTransferPacket);
      }
   }

   public void handleKeepAlive(ClientboundKeepAlivePacket clientboundKeepAlivePacket) {
      this.sendWhen(new ServerboundKeepAlivePacket(clientboundKeepAlivePacket.getId()), () -> {
         return !RenderSystem.isFrozenAtPollEvents();
      }, Duration.ofMinutes(1L));
   }

   public void handlePing(ClientboundPingPacket clientboundPingPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundPingPacket, this, this.minecraft);
      this.send(new ServerboundPongPacket(clientboundPingPacket.getId()));
   }

   public void handleCustomPayload(ClientboundCustomPayloadPacket clientboundCustomPayloadPacket) {
      CustomPacketPayload customPacketPayload = clientboundCustomPayloadPacket.payload();
      if (!(customPacketPayload instanceof DiscardedPayload)) {
         PacketUtils.ensureRunningOnSameThread(clientboundCustomPayloadPacket, this, this.minecraft);
         if (customPacketPayload instanceof BrandPayload) {
            BrandPayload brandPayload = (BrandPayload)customPacketPayload;
            this.serverBrand = brandPayload.brand();
            this.telemetryManager.onServerBrandReceived(brandPayload.brand());
         } else {
            this.handleCustomPayload(customPacketPayload);
         }

      }
   }

   protected abstract void handleCustomPayload(CustomPacketPayload customPacketPayload);

   public void handleResourcePackPush(ClientboundResourcePackPushPacket clientboundResourcePackPushPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundResourcePackPushPacket, this, this.minecraft);
      UUID uUID = clientboundResourcePackPushPacket.id();
      URL uRL = parseResourcePackUrl(clientboundResourcePackPushPacket.url());
      if (uRL == null) {
         this.connection.send(new ServerboundResourcePackPacket(uUID, Action.INVALID_URL));
      } else {
         String string = clientboundResourcePackPushPacket.hash();
         boolean bl = clientboundResourcePackPushPacket.required();
         ServerData.ServerPackStatus serverPackStatus = this.serverData != null ? this.serverData.getResourcePackStatus() : ServerData.ServerPackStatus.PROMPT;
         if (serverPackStatus != ServerData.ServerPackStatus.PROMPT && (!bl || serverPackStatus != ServerData.ServerPackStatus.DISABLED)) {
            this.minecraft.getDownloadedPackSource().pushPack(uUID, uRL, string);
         } else {
            this.minecraft.setScreen(this.addOrUpdatePackPrompt(uUID, uRL, string, bl, (Component)clientboundResourcePackPushPacket.prompt().orElse((Object)null)));
         }

      }
   }

   public void handleResourcePackPop(ClientboundResourcePackPopPacket clientboundResourcePackPopPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundResourcePackPopPacket, this, this.minecraft);
      clientboundResourcePackPopPacket.id().ifPresentOrElse((uUID) -> {
         this.minecraft.getDownloadedPackSource().popPack(uUID);
      }, () -> {
         this.minecraft.getDownloadedPackSource().popAll();
      });
   }

   static Component preparePackPrompt(Component component, @Nullable Component component2) {
      return (Component)(component2 == null ? component : Component.translatable("multiplayer.texturePrompt.serverPrompt", new Object[]{component, component2}));
   }

   @Nullable
   private static URL parseResourcePackUrl(String string) {
      try {
         URL uRL = new URL(string);
         String string2 = uRL.getProtocol();
         return !"http".equals(string2) && !"https".equals(string2) ? null : uRL;
      } catch (MalformedURLException var3) {
         return null;
      }
   }

   public void handleRequestCookie(ClientboundCookieRequestPacket clientboundCookieRequestPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundCookieRequestPacket, this, this.minecraft);
      this.connection.send(new ServerboundCookieResponsePacket(clientboundCookieRequestPacket.key(), (byte[])this.serverCookies.get(clientboundCookieRequestPacket.key())));
   }

   public void handleStoreCookie(ClientboundStoreCookiePacket clientboundStoreCookiePacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundStoreCookiePacket, this, this.minecraft);
      this.serverCookies.put(clientboundStoreCookiePacket.key(), clientboundStoreCookiePacket.payload());
   }

   public void handleCustomReportDetails(ClientboundCustomReportDetailsPacket clientboundCustomReportDetailsPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundCustomReportDetailsPacket, this, this.minecraft);
      this.customReportDetails = clientboundCustomReportDetailsPacket.details();
   }

   public void handleServerLinks(ClientboundServerLinksPacket clientboundServerLinksPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundServerLinksPacket, this, this.minecraft);
      List<UntrustedEntry> list = clientboundServerLinksPacket.links();
      Builder<Entry> builder = ImmutableList.builderWithExpectedSize(list.size());
      Iterator var4 = list.iterator();

      while(var4.hasNext()) {
         UntrustedEntry untrustedEntry = (UntrustedEntry)var4.next();

         try {
            URI uRI = Util.parseAndValidateUntrustedUri(untrustedEntry.link());
            builder.add(new Entry(untrustedEntry.type(), uRI));
         } catch (Exception var7) {
            LOGGER.warn("Received invalid link for type {}:{}", new Object[]{untrustedEntry.type(), untrustedEntry.link(), var7});
         }
      }

      this.serverLinks = new ServerLinks(builder.build());
   }

   public void handleTransfer(ClientboundTransferPacket clientboundTransferPacket) {
      this.isTransferring = true;
      PacketUtils.ensureRunningOnSameThread(clientboundTransferPacket, this, this.minecraft);
      if (this.serverData == null) {
         throw new IllegalStateException("Cannot transfer to server from singleplayer");
      } else {
         this.connection.disconnect(Component.translatable("disconnect.transfer"));
         this.connection.setReadOnly();
         this.connection.handleDisconnection();
         ServerAddress serverAddress = new ServerAddress(clientboundTransferPacket.host(), clientboundTransferPacket.port());
         ConnectScreen.startConnecting((Screen)Objects.requireNonNullElseGet(this.postDisconnectScreen, TitleScreen::new), this.minecraft, serverAddress, this.serverData, false, new TransferState(this.serverCookies));
      }
   }

   public void handleDisconnect(ClientboundDisconnectPacket clientboundDisconnectPacket) {
      this.connection.disconnect(clientboundDisconnectPacket.reason());
   }

   protected void sendDeferredPackets() {
      Iterator iterator = this.deferredPackets.iterator();

      while(iterator.hasNext()) {
         ClientCommonPacketListenerImpl.DeferredPacket deferredPacket = (ClientCommonPacketListenerImpl.DeferredPacket)iterator.next();
         if (deferredPacket.sendCondition().getAsBoolean()) {
            this.send(deferredPacket.packet);
            iterator.remove();
         } else if (deferredPacket.expirationTime() <= Util.getMillis()) {
            iterator.remove();
         }
      }

   }

   public void send(Packet<?> packet) {
      this.connection.send(packet);
   }

   public void onDisconnect(DisconnectionDetails disconnectionDetails) {
      this.telemetryManager.onDisconnect();
      this.minecraft.disconnect(this.createDisconnectScreen(disconnectionDetails), this.isTransferring);
      LOGGER.warn("Client disconnected with reason: {}", disconnectionDetails.reason().getString());
   }

   public void fillListenerSpecificCrashDetails(CrashReport crashReport, CrashReportCategory crashReportCategory) {
      crashReportCategory.setDetail("Server type", () -> {
         return this.serverData != null ? this.serverData.type().toString() : "<none>";
      });
      crashReportCategory.setDetail("Server brand", () -> {
         return this.serverBrand;
      });
      if (!this.customReportDetails.isEmpty()) {
         CrashReportCategory crashReportCategory2 = crashReport.addCategory("Custom Server Details");
         Map var10000 = this.customReportDetails;
         Objects.requireNonNull(crashReportCategory2);
         var10000.forEach(crashReportCategory2::setDetail);
      }

   }

   protected Screen createDisconnectScreen(DisconnectionDetails disconnectionDetails) {
      Screen screen = (Screen)Objects.requireNonNullElseGet(this.postDisconnectScreen, () -> {
         return new JoinMultiplayerScreen(new TitleScreen());
      });
      return (Screen)(this.serverData != null && this.serverData.isRealm() ? new DisconnectedRealmsScreen(screen, GENERIC_DISCONNECT_MESSAGE, disconnectionDetails.reason()) : new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, disconnectionDetails));
   }

   @Nullable
   public String serverBrand() {
      return this.serverBrand;
   }

   private void sendWhen(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier booleanSupplier, Duration duration) {
      if (booleanSupplier.getAsBoolean()) {
         this.send(packet);
      } else {
         this.deferredPackets.add(new ClientCommonPacketListenerImpl.DeferredPacket(packet, booleanSupplier, Util.getMillis() + duration.toMillis()));
      }

   }

   private Screen addOrUpdatePackPrompt(UUID uUID, URL uRL, String string, boolean bl, @Nullable Component component) {
      Screen screen = this.minecraft.screen;
      if (screen instanceof ClientCommonPacketListenerImpl.PackConfirmScreen) {
         ClientCommonPacketListenerImpl.PackConfirmScreen packConfirmScreen = (ClientCommonPacketListenerImpl.PackConfirmScreen)screen;
         return packConfirmScreen.update(this.minecraft, uUID, uRL, string, bl, component);
      } else {
         return new ClientCommonPacketListenerImpl.PackConfirmScreen(this.minecraft, screen, List.of(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(uUID, uRL, string)), bl, component);
      }
   }

   @Environment(EnvType.CLIENT)
   private static record DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
      final Packet<? extends ServerboundPacketListener> packet;

      DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier booleanSupplier, long l) {
         this.packet = packet;
         this.sendCondition = booleanSupplier;
         this.expirationTime = l;
      }

      public Packet<? extends ServerboundPacketListener> packet() {
         return this.packet;
      }

      public BooleanSupplier sendCondition() {
         return this.sendCondition;
      }

      public long expirationTime() {
         return this.expirationTime;
      }
   }

   @Environment(EnvType.CLIENT)
   private class PackConfirmScreen extends ConfirmScreen {
      private final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> requests;
      @Nullable
      private final Screen parentScreen;

      PackConfirmScreen(final Minecraft minecraft, @Nullable final Screen screen, final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> list, final boolean bl, @Nullable final Component component) {
         super((bl2) -> {
            minecraft.setScreen(screen);
            DownloadedPackSource downloadedPackSource = minecraft.getDownloadedPackSource();
            if (bl2) {
               if (ClientCommonPacketListenerImpl.this.serverData != null) {
                  ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
               }

               downloadedPackSource.allowServerPacks();
            } else {
               downloadedPackSource.rejectServerPacks();
               if (bl) {
                  ClientCommonPacketListenerImpl.this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
               } else if (ClientCommonPacketListenerImpl.this.serverData != null) {
                  ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
               }
            }

            Iterator var7 = list.iterator();

            while(var7.hasNext()) {
               ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest pendingRequest = (ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest)var7.next();
               downloadedPackSource.pushPack(pendingRequest.id, pendingRequest.url, pendingRequest.hash);
            }

            if (ClientCommonPacketListenerImpl.this.serverData != null) {
               ServerList.saveSingleServer(ClientCommonPacketListenerImpl.this.serverData);
            }

         }, bl ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"), ClientCommonPacketListenerImpl.preparePackPrompt(bl ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}) : Component.translatable("multiplayer.texturePrompt.line2"), component), bl ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES, bl ? CommonComponents.GUI_DISCONNECT : CommonComponents.GUI_NO);
         this.requests = list;
         this.parentScreen = screen;
      }

      public ClientCommonPacketListenerImpl.PackConfirmScreen update(Minecraft minecraft, UUID uUID, URL uRL, String string, boolean bl, @Nullable Component component) {
         List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> list = ImmutableList.builderWithExpectedSize(this.requests.size() + 1).addAll(this.requests).add(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(uUID, uRL, string)).build();
         return ClientCommonPacketListenerImpl.this.new PackConfirmScreen(minecraft, this.parentScreen, list, bl, component);
      }

      @Environment(EnvType.CLIENT)
      private static record PendingRequest(UUID id, URL url, String hash) {
         final UUID id;
         final URL url;
         final String hash;

         PendingRequest(UUID uUID, URL uRL, String string) {
            this.id = uUID;
            this.url = uRL;
            this.hash = string;
         }

         public UUID id() {
            return this.id;
         }

         public URL url() {
            return this.url;
         }

         public String hash() {
            return this.hash;
         }
      }
   }
}

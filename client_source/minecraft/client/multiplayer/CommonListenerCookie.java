package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record CommonListenerCookie(GameProfile localGameProfile, WorldSessionTelemetryManager telemetryManager, Frozen receivedRegistries, FeatureFlagSet enabledFeatures, @Nullable String serverBrand, @Nullable ServerData serverData, @Nullable Screen postDisconnectScreen, Map<ResourceLocation, byte[]> serverCookies, @Nullable ChatComponent.State chatState, boolean strictErrorHandling, Map<String, String> customReportDetails, ServerLinks serverLinks) {
   public CommonListenerCookie(GameProfile gameProfile, WorldSessionTelemetryManager worldSessionTelemetryManager, Frozen frozen, FeatureFlagSet featureFlagSet, @Nullable String string, @Nullable ServerData serverData, @Nullable Screen screen, Map<ResourceLocation, byte[]> map, @Nullable ChatComponent.State state, @Deprecated(forRemoval = true) boolean bl, Map<String, String> map2, ServerLinks serverLinks) {
      this.localGameProfile = gameProfile;
      this.telemetryManager = worldSessionTelemetryManager;
      this.receivedRegistries = frozen;
      this.enabledFeatures = featureFlagSet;
      this.serverBrand = string;
      this.serverData = serverData;
      this.postDisconnectScreen = screen;
      this.serverCookies = map;
      this.chatState = state;
      this.strictErrorHandling = bl;
      this.customReportDetails = map2;
      this.serverLinks = serverLinks;
   }

   public GameProfile localGameProfile() {
      return this.localGameProfile;
   }

   public WorldSessionTelemetryManager telemetryManager() {
      return this.telemetryManager;
   }

   public Frozen receivedRegistries() {
      return this.receivedRegistries;
   }

   public FeatureFlagSet enabledFeatures() {
      return this.enabledFeatures;
   }

   @Nullable
   public String serverBrand() {
      return this.serverBrand;
   }

   @Nullable
   public ServerData serverData() {
      return this.serverData;
   }

   @Nullable
   public Screen postDisconnectScreen() {
      return this.postDisconnectScreen;
   }

   public Map<ResourceLocation, byte[]> serverCookies() {
      return this.serverCookies;
   }

   @Nullable
   public ChatComponent.State chatState() {
      return this.chatState;
   }

   /** @deprecated */
   @Deprecated(
      forRemoval = true
   )
   public boolean strictErrorHandling() {
      return this.strictErrorHandling;
   }

   public Map<String, String> customReportDetails() {
      return this.customReportDetails;
   }

   public ServerLinks serverLinks() {
      return this.serverLinks;
   }
}

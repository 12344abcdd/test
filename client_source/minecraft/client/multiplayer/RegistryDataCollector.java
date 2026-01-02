package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.RegistrySynchronization.PackedRegistryEntry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagNetworkSerialization.NetworkPayload;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RegistryDataCollector {
   @Nullable
   private RegistryDataCollector.ContentsCollector contentsCollector;
   @Nullable
   private TagCollector tagCollector;

   public void appendContents(ResourceKey<? extends Registry<?>> resourceKey, List<PackedRegistryEntry> list) {
      if (this.contentsCollector == null) {
         this.contentsCollector = new RegistryDataCollector.ContentsCollector();
      }

      this.contentsCollector.append(resourceKey, list);
   }

   public void appendTags(Map<ResourceKey<? extends Registry<?>>, NetworkPayload> map) {
      if (this.tagCollector == null) {
         this.tagCollector = new TagCollector();
      }

      TagCollector var10001 = this.tagCollector;
      Objects.requireNonNull(var10001);
      map.forEach(var10001::append);
   }

   public Frozen collectGameRegistries(ResourceProvider resourceProvider, RegistryAccess registryAccess, boolean bl) {
      LayeredRegistryAccess<ClientRegistryLayer> layeredRegistryAccess = ClientRegistryLayer.createRegistryAccess();
      Object registryAccess2;
      if (this.contentsCollector != null) {
         Frozen frozen = layeredRegistryAccess.getAccessForLoading(ClientRegistryLayer.REMOTE);
         Frozen frozen2 = this.contentsCollector.loadRegistries(resourceProvider, frozen).freeze();
         registryAccess2 = layeredRegistryAccess.replaceFrom(ClientRegistryLayer.REMOTE, new Frozen[]{frozen2}).compositeAccess();
      } else {
         registryAccess2 = registryAccess;
      }

      if (this.tagCollector != null) {
         this.tagCollector.updateTags((RegistryAccess)registryAccess2, bl);
      }

      return ((RegistryAccess)registryAccess2).freeze();
   }

   @Environment(EnvType.CLIENT)
   static class ContentsCollector {
      private final Map<ResourceKey<? extends Registry<?>>, List<PackedRegistryEntry>> elements = new HashMap();

      public void append(ResourceKey<? extends Registry<?>> resourceKey, List<PackedRegistryEntry> list) {
         ((List)this.elements.computeIfAbsent(resourceKey, (resourceKeyx) -> {
            return new ArrayList();
         })).addAll(list);
      }

      public RegistryAccess loadRegistries(ResourceProvider resourceProvider, RegistryAccess registryAccess) {
         return RegistryDataLoader.load(this.elements, resourceProvider, registryAccess, RegistryDataLoader.SYNCHRONIZED_REGISTRIES);
      }
   }
}

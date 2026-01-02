package net.minecraft.client.multiplayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization.NetworkPayload;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

@Environment(EnvType.CLIENT)
public class TagCollector {
   private final Map<ResourceKey<? extends Registry<?>>, NetworkPayload> tags = new HashMap();

   public void append(ResourceKey<? extends Registry<?>> resourceKey, NetworkPayload networkPayload) {
      this.tags.put(resourceKey, networkPayload);
   }

   private static void refreshBuiltInTagDependentData() {
      AbstractFurnaceBlockEntity.invalidateCache();
      Blocks.rebuildCache();
   }

   private void applyTags(RegistryAccess registryAccess, Predicate<ResourceKey<? extends Registry<?>>> predicate) {
      this.tags.forEach((resourceKey, networkPayload) -> {
         if (predicate.test(resourceKey)) {
            networkPayload.applyToRegistry(registryAccess.registryOrThrow(resourceKey));
         }

      });
   }

   public void updateTags(RegistryAccess registryAccess, boolean bl) {
      if (bl) {
         Set var10002 = RegistrySynchronization.NETWORKABLE_REGISTRIES;
         Objects.requireNonNull(var10002);
         this.applyTags(registryAccess, var10002::contains);
      } else {
         registryAccess.registries().filter((registryEntry) -> {
            return !RegistrySynchronization.NETWORKABLE_REGISTRIES.contains(registryEntry.key());
         }).forEach((registryEntry) -> {
            registryEntry.value().resetTags();
         });
         this.applyTags(registryAccess, (resourceKey) -> {
            return true;
         });
         refreshBuiltInTagDependentData();
      }

   }
}

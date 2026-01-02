package net.minecraft.client.gui.screens.worldselection;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;

@Environment(EnvType.CLIENT)
public record WorldCreationContext(WorldOptions options, Registry<LevelStem> datapackDimensions, WorldDimensions selectedDimensions, LayeredRegistryAccess<RegistryLayer> worldgenRegistries, ReloadableServerResources dataPackResources, WorldDataConfiguration dataConfiguration) {
   public WorldCreationContext(WorldGenSettings worldGenSettings, LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess, ReloadableServerResources reloadableServerResources, WorldDataConfiguration worldDataConfiguration) {
      this(worldGenSettings.options(), worldGenSettings.dimensions(), layeredRegistryAccess, reloadableServerResources, worldDataConfiguration);
   }

   public WorldCreationContext(WorldOptions worldOptions, WorldDimensions worldDimensions, LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess, ReloadableServerResources reloadableServerResources, WorldDataConfiguration worldDataConfiguration) {
      this(worldOptions, layeredRegistryAccess.getLayer(RegistryLayer.DIMENSIONS).registryOrThrow(Registries.LEVEL_STEM), worldDimensions, layeredRegistryAccess.replaceFrom(RegistryLayer.DIMENSIONS, new Frozen[0]), reloadableServerResources, worldDataConfiguration);
   }

   public WorldCreationContext(WorldOptions worldOptions, Registry<LevelStem> registry, WorldDimensions worldDimensions, LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess, ReloadableServerResources reloadableServerResources, WorldDataConfiguration worldDataConfiguration) {
      this.options = worldOptions;
      this.datapackDimensions = registry;
      this.selectedDimensions = worldDimensions;
      this.worldgenRegistries = layeredRegistryAccess;
      this.dataPackResources = reloadableServerResources;
      this.dataConfiguration = worldDataConfiguration;
   }

   public WorldCreationContext withSettings(WorldOptions worldOptions, WorldDimensions worldDimensions) {
      return new WorldCreationContext(worldOptions, this.datapackDimensions, worldDimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration);
   }

   public WorldCreationContext withOptions(WorldCreationContext.OptionsModifier optionsModifier) {
      return new WorldCreationContext((WorldOptions)optionsModifier.apply(this.options), this.datapackDimensions, this.selectedDimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration);
   }

   public WorldCreationContext withDimensions(WorldCreationContext.DimensionsUpdater dimensionsUpdater) {
      return new WorldCreationContext(this.options, this.datapackDimensions, (WorldDimensions)dimensionsUpdater.apply(this.worldgenLoadContext(), this.selectedDimensions), this.worldgenRegistries, this.dataPackResources, this.dataConfiguration);
   }

   public Frozen worldgenLoadContext() {
      return this.worldgenRegistries.compositeAccess();
   }

   public void validate() {
      Iterator var1 = this.datapackDimensions().iterator();

      while(var1.hasNext()) {
         LevelStem levelStem = (LevelStem)var1.next();
         levelStem.generator().validate();
      }

   }

   public WorldOptions options() {
      return this.options;
   }

   public Registry<LevelStem> datapackDimensions() {
      return this.datapackDimensions;
   }

   public WorldDimensions selectedDimensions() {
      return this.selectedDimensions;
   }

   public LayeredRegistryAccess<RegistryLayer> worldgenRegistries() {
      return this.worldgenRegistries;
   }

   public ReloadableServerResources dataPackResources() {
      return this.dataPackResources;
   }

   public WorldDataConfiguration dataConfiguration() {
      return this.dataConfiguration;
   }

   @Environment(EnvType.CLIENT)
   public interface OptionsModifier extends UnaryOperator<WorldOptions> {
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface DimensionsUpdater extends BiFunction<Frozen, WorldDimensions, WorldDimensions> {
   }
}

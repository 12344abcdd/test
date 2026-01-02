package net.minecraft.client.gui.screens.worldselection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.FileUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WorldCreationUiState {
   private static final Component DEFAULT_WORLD_NAME = Component.translatable("selectWorld.newWorld");
   private final List<Consumer<WorldCreationUiState>> listeners = new ArrayList();
   private String name;
   private WorldCreationUiState.SelectedGameMode gameMode;
   private Difficulty difficulty;
   @Nullable
   private Boolean allowCommands;
   private String seed;
   private boolean generateStructures;
   private boolean bonusChest;
   private final Path savesFolder;
   private String targetFolder;
   private WorldCreationContext settings;
   private WorldCreationUiState.WorldTypeEntry worldType;
   private final List<WorldCreationUiState.WorldTypeEntry> normalPresetList;
   private final List<WorldCreationUiState.WorldTypeEntry> altPresetList;
   private GameRules gameRules;

   public WorldCreationUiState(Path path, WorldCreationContext worldCreationContext, Optional<ResourceKey<WorldPreset>> optional, OptionalLong optionalLong) {
      this.name = DEFAULT_WORLD_NAME.getString();
      this.gameMode = WorldCreationUiState.SelectedGameMode.SURVIVAL;
      this.difficulty = Difficulty.NORMAL;
      this.normalPresetList = new ArrayList();
      this.altPresetList = new ArrayList();
      this.gameRules = new GameRules();
      this.savesFolder = path;
      this.settings = worldCreationContext;
      this.worldType = new WorldCreationUiState.WorldTypeEntry((Holder)findPreset(worldCreationContext, optional).orElse((Object)null));
      this.updatePresetLists();
      this.seed = optionalLong.isPresent() ? Long.toString(optionalLong.getAsLong()) : "";
      this.generateStructures = worldCreationContext.options().generateStructures();
      this.bonusChest = worldCreationContext.options().generateBonusChest();
      this.targetFolder = this.findResultFolder(this.name);
   }

   public void addListener(Consumer<WorldCreationUiState> consumer) {
      this.listeners.add(consumer);
   }

   public void onChanged() {
      boolean bl = this.isBonusChest();
      if (bl != this.settings.options().generateBonusChest()) {
         this.settings = this.settings.withOptions((worldOptions) -> {
            return worldOptions.withBonusChest(bl);
         });
      }

      boolean bl2 = this.isGenerateStructures();
      if (bl2 != this.settings.options().generateStructures()) {
         this.settings = this.settings.withOptions((worldOptions) -> {
            return worldOptions.withStructures(bl2);
         });
      }

      Iterator var3 = this.listeners.iterator();

      while(var3.hasNext()) {
         Consumer<WorldCreationUiState> consumer = (Consumer)var3.next();
         consumer.accept(this);
      }

   }

   public void setName(String string) {
      this.name = string;
      this.targetFolder = this.findResultFolder(string);
      this.onChanged();
   }

   private String findResultFolder(String string) {
      String string2 = string.trim();

      try {
         return FileUtil.findAvailableName(this.savesFolder, !string2.isEmpty() ? string2 : DEFAULT_WORLD_NAME.getString(), "");
      } catch (Exception var5) {
         try {
            return FileUtil.findAvailableName(this.savesFolder, "World", "");
         } catch (IOException var4) {
            throw new RuntimeException("Could not create save folder", var4);
         }
      }
   }

   public String getName() {
      return this.name;
   }

   public String getTargetFolder() {
      return this.targetFolder;
   }

   public void setGameMode(WorldCreationUiState.SelectedGameMode selectedGameMode) {
      this.gameMode = selectedGameMode;
      this.onChanged();
   }

   public WorldCreationUiState.SelectedGameMode getGameMode() {
      return this.isDebug() ? WorldCreationUiState.SelectedGameMode.DEBUG : this.gameMode;
   }

   public void setDifficulty(Difficulty difficulty) {
      this.difficulty = difficulty;
      this.onChanged();
   }

   public Difficulty getDifficulty() {
      return this.isHardcore() ? Difficulty.HARD : this.difficulty;
   }

   public boolean isHardcore() {
      return this.getGameMode() == WorldCreationUiState.SelectedGameMode.HARDCORE;
   }

   public void setAllowCommands(boolean bl) {
      this.allowCommands = bl;
      this.onChanged();
   }

   public boolean isAllowCommands() {
      if (this.isDebug()) {
         return true;
      } else if (this.isHardcore()) {
         return false;
      } else if (this.allowCommands == null) {
         return this.getGameMode() == WorldCreationUiState.SelectedGameMode.CREATIVE;
      } else {
         return this.allowCommands;
      }
   }

   public void setSeed(String string) {
      this.seed = string;
      this.settings = this.settings.withOptions((worldOptions) -> {
         return worldOptions.withSeed(WorldOptions.parseSeed(this.getSeed()));
      });
      this.onChanged();
   }

   public String getSeed() {
      return this.seed;
   }

   public void setGenerateStructures(boolean bl) {
      this.generateStructures = bl;
      this.onChanged();
   }

   public boolean isGenerateStructures() {
      return this.isDebug() ? false : this.generateStructures;
   }

   public void setBonusChest(boolean bl) {
      this.bonusChest = bl;
      this.onChanged();
   }

   public boolean isBonusChest() {
      return !this.isDebug() && !this.isHardcore() ? this.bonusChest : false;
   }

   public void setSettings(WorldCreationContext worldCreationContext) {
      this.settings = worldCreationContext;
      this.updatePresetLists();
      this.onChanged();
   }

   public WorldCreationContext getSettings() {
      return this.settings;
   }

   public void updateDimensions(WorldCreationContext.DimensionsUpdater dimensionsUpdater) {
      this.settings = this.settings.withDimensions(dimensionsUpdater);
      this.onChanged();
   }

   protected boolean tryUpdateDataConfiguration(WorldDataConfiguration worldDataConfiguration) {
      WorldDataConfiguration worldDataConfiguration2 = this.settings.dataConfiguration();
      if (worldDataConfiguration2.dataPacks().getEnabled().equals(worldDataConfiguration.dataPacks().getEnabled()) && worldDataConfiguration2.enabledFeatures().equals(worldDataConfiguration.enabledFeatures())) {
         this.settings = new WorldCreationContext(this.settings.options(), this.settings.datapackDimensions(), this.settings.selectedDimensions(), this.settings.worldgenRegistries(), this.settings.dataPackResources(), worldDataConfiguration);
         return true;
      } else {
         return false;
      }
   }

   public boolean isDebug() {
      return this.settings.selectedDimensions().isDebug();
   }

   public void setWorldType(WorldCreationUiState.WorldTypeEntry worldTypeEntry) {
      this.worldType = worldTypeEntry;
      Holder<WorldPreset> holder = worldTypeEntry.preset();
      if (holder != null) {
         this.updateDimensions((frozen, worldDimensions) -> {
            return ((WorldPreset)holder.value()).createWorldDimensions();
         });
      }

   }

   public WorldCreationUiState.WorldTypeEntry getWorldType() {
      return this.worldType;
   }

   @Nullable
   public PresetEditor getPresetEditor() {
      Holder<WorldPreset> holder = this.getWorldType().preset();
      return holder != null ? (PresetEditor)PresetEditor.EDITORS.get(holder.unwrapKey()) : null;
   }

   public List<WorldCreationUiState.WorldTypeEntry> getNormalPresetList() {
      return this.normalPresetList;
   }

   public List<WorldCreationUiState.WorldTypeEntry> getAltPresetList() {
      return this.altPresetList;
   }

   private void updatePresetLists() {
      Registry<WorldPreset> registry = this.getSettings().worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET);
      this.normalPresetList.clear();
      this.normalPresetList.addAll((Collection)getNonEmptyList(registry, WorldPresetTags.NORMAL).orElseGet(() -> {
         return registry.holders().map(WorldCreationUiState.WorldTypeEntry::new).toList();
      }));
      this.altPresetList.clear();
      this.altPresetList.addAll((Collection)getNonEmptyList(registry, WorldPresetTags.EXTENDED).orElse(this.normalPresetList));
      Holder<WorldPreset> holder = this.worldType.preset();
      if (holder != null) {
         this.worldType = (WorldCreationUiState.WorldTypeEntry)findPreset(this.getSettings(), holder.unwrapKey()).map(WorldCreationUiState.WorldTypeEntry::new).orElse((WorldCreationUiState.WorldTypeEntry)this.normalPresetList.get(0));
      }

   }

   private static Optional<Holder<WorldPreset>> findPreset(WorldCreationContext worldCreationContext, Optional<ResourceKey<WorldPreset>> optional) {
      return optional.flatMap((resourceKey) -> {
         return worldCreationContext.worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).getHolder(resourceKey);
      });
   }

   private static Optional<List<WorldCreationUiState.WorldTypeEntry>> getNonEmptyList(Registry<WorldPreset> registry, TagKey<WorldPreset> tagKey) {
      return registry.getTag(tagKey).map((named) -> {
         return named.stream().map(WorldCreationUiState.WorldTypeEntry::new).toList();
      }).filter((list) -> {
         return !list.isEmpty();
      });
   }

   public void setGameRules(GameRules gameRules) {
      this.gameRules = gameRules;
      this.onChanged();
   }

   public GameRules getGameRules() {
      return this.gameRules;
   }

   @Environment(EnvType.CLIENT)
   public static enum SelectedGameMode {
      SURVIVAL("survival", GameType.SURVIVAL),
      HARDCORE("hardcore", GameType.SURVIVAL),
      CREATIVE("creative", GameType.CREATIVE),
      DEBUG("spectator", GameType.SPECTATOR);

      public final GameType gameType;
      public final Component displayName;
      private final Component info;

      private SelectedGameMode(final String string2, final GameType gameType) {
         this.gameType = gameType;
         this.displayName = Component.translatable("selectWorld.gameMode." + string2);
         this.info = Component.translatable("selectWorld.gameMode." + string2 + ".info");
      }

      public Component getInfo() {
         return this.info;
      }

      // $FF: synthetic method
      private static WorldCreationUiState.SelectedGameMode[] $values() {
         return new WorldCreationUiState.SelectedGameMode[]{SURVIVAL, HARDCORE, CREATIVE, DEBUG};
      }
   }

   @Environment(EnvType.CLIENT)
   public static record WorldTypeEntry(@Nullable Holder<WorldPreset> preset) {
      private static final Component CUSTOM_WORLD_DESCRIPTION = Component.translatable("generator.custom");

      public WorldTypeEntry(@Nullable Holder<WorldPreset> holder) {
         this.preset = holder;
      }

      public Component describePreset() {
         return (Component)Optional.ofNullable(this.preset).flatMap(Holder::unwrapKey).map((resourceKey) -> {
            return Component.translatable(resourceKey.location().toLanguageKey("generator"));
         }).orElse(CUSTOM_WORLD_DESCRIPTION);
      }

      public boolean isAmplified() {
         return Optional.ofNullable(this.preset).flatMap(Holder::unwrapKey).filter((resourceKey) -> {
            return resourceKey.equals(WorldPresets.AMPLIFIED);
         }).isPresent();
      }

      @Nullable
      public Holder<WorldPreset> preset() {
         return this.preset;
      }
   }
}

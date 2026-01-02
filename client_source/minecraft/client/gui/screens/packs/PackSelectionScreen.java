package net.minecraft.client.gui.screens.packs;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class PackSelectionScreen extends Screen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Component AVAILABLE_TITLE = Component.translatable("pack.available.title");
   private static final Component SELECTED_TITLE = Component.translatable("pack.selected.title");
   private static final Component OPEN_PACK_FOLDER_TITLE = Component.translatable("pack.openFolder");
   private static final int LIST_WIDTH = 200;
   private static final Component DRAG_AND_DROP;
   private static final Component DIRECTORY_BUTTON_TOOLTIP;
   private static final int RELOAD_COOLDOWN = 20;
   private static final ResourceLocation DEFAULT_ICON;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final PackSelectionModel model;
   @Nullable
   private PackSelectionScreen.Watcher watcher;
   private long ticksToReload;
   private TransferableSelectionList availablePackList;
   private TransferableSelectionList selectedPackList;
   private final Path packDir;
   private Button doneButton;
   private final Map<String, ResourceLocation> packIcons = Maps.newHashMap();

   public PackSelectionScreen(PackRepository packRepository, Consumer<PackRepository> consumer, Path path, Component component) {
      super(component);
      this.model = new PackSelectionModel(this::populateLists, this::getPackIcon, packRepository, consumer);
      this.packDir = path;
      this.watcher = PackSelectionScreen.Watcher.create(path);
   }

   public void onClose() {
      this.model.commit();
      this.closeWatcher();
   }

   private void closeWatcher() {
      if (this.watcher != null) {
         try {
            this.watcher.close();
            this.watcher = null;
         } catch (Exception var2) {
         }
      }

   }

   protected void init() {
      LinearLayout linearLayout = (LinearLayout)this.layout.addToHeader(LinearLayout.vertical().spacing(5));
      linearLayout.defaultCellSetting().alignHorizontallyCenter();
      linearLayout.addChild(new StringWidget(this.getTitle(), this.font));
      linearLayout.addChild(new StringWidget(DRAG_AND_DROP, this.font));
      this.availablePackList = (TransferableSelectionList)this.addRenderableWidget(new TransferableSelectionList(this.minecraft, this, 200, this.height - 66, AVAILABLE_TITLE));
      this.selectedPackList = (TransferableSelectionList)this.addRenderableWidget(new TransferableSelectionList(this.minecraft, this, 200, this.height - 66, SELECTED_TITLE));
      LinearLayout linearLayout2 = (LinearLayout)this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      linearLayout2.addChild(Button.builder(OPEN_PACK_FOLDER_TITLE, (button) -> {
         Util.getPlatform().openPath(this.packDir);
      }).tooltip(Tooltip.create(DIRECTORY_BUTTON_TOOLTIP)).build());
      this.doneButton = (Button)linearLayout2.addChild(Button.builder(CommonComponents.GUI_DONE, (button) -> {
         this.onClose();
      }).build());
      this.reload();
      this.layout.visitWidgets((guiEventListener) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
      });
      this.repositionElements();
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
      this.availablePackList.updateSize(200, this.layout);
      this.availablePackList.setX(this.width / 2 - 15 - 200);
      this.selectedPackList.updateSize(200, this.layout);
      this.selectedPackList.setX(this.width / 2 + 15);
   }

   public void tick() {
      if (this.watcher != null) {
         try {
            if (this.watcher.pollForChanges()) {
               this.ticksToReload = 20L;
            }
         } catch (IOException var2) {
            LOGGER.warn("Failed to poll for directory {} changes, stopping", this.packDir);
            this.closeWatcher();
         }
      }

      if (this.ticksToReload > 0L && --this.ticksToReload == 0L) {
         this.reload();
      }

   }

   private void populateLists() {
      this.updateList(this.selectedPackList, this.model.getSelected());
      this.updateList(this.availablePackList, this.model.getUnselected());
      this.doneButton.active = !this.selectedPackList.children().isEmpty();
   }

   private void updateList(TransferableSelectionList transferableSelectionList, Stream<PackSelectionModel.Entry> stream) {
      transferableSelectionList.children().clear();
      TransferableSelectionList.PackEntry packEntry = (TransferableSelectionList.PackEntry)transferableSelectionList.getSelected();
      String string = packEntry == null ? "" : packEntry.getPackId();
      transferableSelectionList.setSelected((AbstractSelectionList.Entry)null);
      stream.forEach((entry) -> {
         TransferableSelectionList.PackEntry packEntry = new TransferableSelectionList.PackEntry(this.minecraft, transferableSelectionList, entry);
         transferableSelectionList.children().add(packEntry);
         if (entry.getId().equals(string)) {
            transferableSelectionList.setSelected(packEntry);
         }

      });
   }

   public void updateFocus(TransferableSelectionList transferableSelectionList) {
      TransferableSelectionList transferableSelectionList2 = this.selectedPackList == transferableSelectionList ? this.availablePackList : this.selectedPackList;
      this.changeFocus(ComponentPath.path((GuiEventListener)transferableSelectionList2.getFirstElement(), (ContainerEventHandler[])(transferableSelectionList2, this)));
   }

   public void clearSelected() {
      this.selectedPackList.setSelected((AbstractSelectionList.Entry)null);
      this.availablePackList.setSelected((AbstractSelectionList.Entry)null);
   }

   private void reload() {
      this.model.findNewPacks();
      this.populateLists();
      this.ticksToReload = 0L;
      this.packIcons.clear();
   }

   protected static void copyPacks(Minecraft minecraft, List<Path> list, Path path) {
      MutableBoolean mutableBoolean = new MutableBoolean();
      list.forEach((path2) -> {
         try {
            Stream stream = Files.walk(path2);

            try {
               stream.forEach((path3) -> {
                  try {
                     Util.copyBetweenDirs(path2.getParent(), path, path3);
                  } catch (IOException var5) {
                     LOGGER.warn("Failed to copy datapack file  from {} to {}", new Object[]{path3, path, var5});
                     mutableBoolean.setTrue();
                  }

               });
            } catch (Throwable var7) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (IOException var8) {
            LOGGER.warn("Failed to copy datapack file from {} to {}", path2, path);
            mutableBoolean.setTrue();
         }

      });
      if (mutableBoolean.isTrue()) {
         SystemToast.onPackCopyFailure(minecraft, path.toString());
      }

   }

   public void onFilesDrop(List<Path> list) {
      String string = (String)extractPackNames(list).collect(Collectors.joining(", "));
      this.minecraft.setScreen(new ConfirmScreen((bl) -> {
         if (bl) {
            List<Path> list2 = new ArrayList(list.size());
            Set<Path> set = new HashSet(list);
            PackDetector<Path> packDetector = new PackDetector<Path>(this, this.minecraft.directoryValidator()) {
               protected Path createZipPack(Path path) {
                  return path;
               }

               protected Path createDirectoryPack(Path path) {
                  return path;
               }

               // $FF: synthetic method
               protected Object createDirectoryPack(final Path path) throws IOException {
                  return this.createDirectoryPack(path);
               }

               // $FF: synthetic method
               protected Object createZipPack(final Path path) throws IOException {
                  return this.createZipPack(path);
               }
            };
            List<ForbiddenSymlinkInfo> list3 = new ArrayList();
            Iterator var7 = list.iterator();

            while(var7.hasNext()) {
               Path path = (Path)var7.next();

               try {
                  Path path2 = (Path)packDetector.detectPackResources(path, list3);
                  if (path2 == null) {
                     LOGGER.warn("Path {} does not seem like pack", path);
                  } else {
                     list2.add(path2);
                     set.remove(path2);
                  }
               } catch (IOException var10) {
                  LOGGER.warn("Failed to check {} for packs", path, var10);
               }
            }

            if (!list3.isEmpty()) {
               this.minecraft.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> {
                  this.minecraft.setScreen(this);
               }));
               return;
            }

            if (!list2.isEmpty()) {
               copyPacks(this.minecraft, list2, this.packDir);
               this.reload();
            }

            if (!set.isEmpty()) {
               String string = (String)extractPackNames(set).collect(Collectors.joining(", "));
               this.minecraft.setScreen(new AlertScreen(() -> {
                  this.minecraft.setScreen(this);
               }, Component.translatable("pack.dropRejected.title"), Component.translatable("pack.dropRejected.message", new Object[]{string})));
               return;
            }
         }

         this.minecraft.setScreen(this);
      }, Component.translatable("pack.dropConfirm"), Component.literal(string)));
   }

   private static Stream<String> extractPackNames(Collection<Path> collection) {
      return collection.stream().map(Path::getFileName).map(Path::toString);
   }

   private ResourceLocation loadPackIcon(TextureManager textureManager, Pack pack) {
      try {
         PackResources packResources = pack.open();

         ResourceLocation var15;
         label70: {
            ResourceLocation var9;
            try {
               IoSupplier<InputStream> ioSupplier = packResources.getRootResource(new String[]{"pack.png"});
               if (ioSupplier == null) {
                  var15 = DEFAULT_ICON;
                  break label70;
               }

               String string = pack.getId();
               String var10000 = Util.sanitizeName(string, ResourceLocation::validPathChar);
               ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace("pack/" + var10000 + "/" + String.valueOf(Hashing.sha1().hashUnencodedChars(string)) + "/icon");
               InputStream inputStream = (InputStream)ioSupplier.get();

               try {
                  NativeImage nativeImage = NativeImage.read(inputStream);
                  textureManager.register((ResourceLocation)resourceLocation, (AbstractTexture)(new DynamicTexture(nativeImage)));
                  var9 = resourceLocation;
               } catch (Throwable var12) {
                  if (inputStream != null) {
                     try {
                        inputStream.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }
                  }

                  throw var12;
               }

               if (inputStream != null) {
                  inputStream.close();
               }
            } catch (Throwable var13) {
               if (packResources != null) {
                  try {
                     packResources.close();
                  } catch (Throwable var10) {
                     var13.addSuppressed(var10);
                  }
               }

               throw var13;
            }

            if (packResources != null) {
               packResources.close();
            }

            return var9;
         }

         if (packResources != null) {
            packResources.close();
         }

         return var15;
      } catch (Exception var14) {
         LOGGER.warn("Failed to load icon from pack {}", pack.getId(), var14);
         return DEFAULT_ICON;
      }
   }

   private ResourceLocation getPackIcon(Pack pack) {
      return (ResourceLocation)this.packIcons.computeIfAbsent(pack.getId(), (string) -> {
         return this.loadPackIcon(this.minecraft.getTextureManager(), pack);
      });
   }

   static {
      DRAG_AND_DROP = Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY);
      DIRECTORY_BUTTON_TOOLTIP = Component.translatable("pack.folderInfo");
      DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
   }

   @Environment(EnvType.CLIENT)
   static class Watcher implements AutoCloseable {
      private final WatchService watcher;
      private final Path packPath;

      public Watcher(Path path) throws IOException {
         this.packPath = path;
         this.watcher = path.getFileSystem().newWatchService();

         try {
            this.watchDir(path);
            DirectoryStream directoryStream = Files.newDirectoryStream(path);

            try {
               Iterator var3 = directoryStream.iterator();

               while(var3.hasNext()) {
                  Path path2 = (Path)var3.next();
                  if (Files.isDirectory(path2, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
                     this.watchDir(path2);
                  }
               }
            } catch (Throwable var6) {
               if (directoryStream != null) {
                  try {
                     directoryStream.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (directoryStream != null) {
               directoryStream.close();
            }

         } catch (Exception var7) {
            this.watcher.close();
            throw var7;
         }
      }

      @Nullable
      public static PackSelectionScreen.Watcher create(Path path) {
         try {
            return new PackSelectionScreen.Watcher(path);
         } catch (IOException var2) {
            PackSelectionScreen.LOGGER.warn("Failed to initialize pack directory {} monitoring", path, var2);
            return null;
         }
      }

      private void watchDir(Path path) throws IOException {
         path.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
      }

      public boolean pollForChanges() throws IOException {
         boolean bl = false;

         WatchKey watchKey;
         while((watchKey = this.watcher.poll()) != null) {
            List<WatchEvent<?>> list = watchKey.pollEvents();
            Iterator var4 = list.iterator();

            while(var4.hasNext()) {
               WatchEvent<?> watchEvent = (WatchEvent)var4.next();
               bl = true;
               if (watchKey.watchable() == this.packPath && watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                  Path path = this.packPath.resolve((Path)watchEvent.context());
                  if (Files.isDirectory(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
                     this.watchDir(path);
                  }
               }
            }

            watchKey.reset();
         }

         return bl;
      }

      public void close() throws IOException {
         this.watcher.close();
      }
   }
}

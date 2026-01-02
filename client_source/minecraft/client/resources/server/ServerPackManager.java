package net.minecraft.client.resources.server;

import com.google.common.hash.HashCode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.DownloadQueue.BatchResult;
import net.minecraft.server.packs.DownloadQueue.DownloadRequest;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ServerPackManager {
   private final PackDownloader downloader;
   final PackLoadFeedback packLoadFeedback;
   private final PackReloadConfig reloadConfig;
   private final Runnable updateRequest;
   private ServerPackManager.PackPromptStatus packPromptStatus;
   final List<ServerPackManager.ServerPackData> packs = new ArrayList();

   public ServerPackManager(PackDownloader packDownloader, PackLoadFeedback packLoadFeedback, PackReloadConfig packReloadConfig, Runnable runnable, ServerPackManager.PackPromptStatus packPromptStatus) {
      this.downloader = packDownloader;
      this.packLoadFeedback = packLoadFeedback;
      this.reloadConfig = packReloadConfig;
      this.updateRequest = runnable;
      this.packPromptStatus = packPromptStatus;
   }

   void registerForUpdate() {
      this.updateRequest.run();
   }

   private void markExistingPacksAsRemoved(UUID uUID) {
      Iterator var2 = this.packs.iterator();

      while(var2.hasNext()) {
         ServerPackManager.ServerPackData serverPackData = (ServerPackManager.ServerPackData)var2.next();
         if (serverPackData.id.equals(uUID)) {
            serverPackData.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REPLACED);
         }
      }

   }

   public void pushPack(UUID uUID, URL uRL, @Nullable HashCode hashCode) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(uUID, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         this.pushNewPack(uUID, new ServerPackManager.ServerPackData(uUID, uRL, hashCode));
      }
   }

   public void pushLocalPack(UUID uUID, Path path) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(uUID, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         URL uRL;
         try {
            uRL = path.toUri().toURL();
         } catch (MalformedURLException var5) {
            throw new IllegalStateException("Can't convert path to URL " + String.valueOf(path), var5);
         }

         ServerPackManager.ServerPackData serverPackData = new ServerPackManager.ServerPackData(uUID, uRL, (HashCode)null);
         serverPackData.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
         serverPackData.path = path;
         this.pushNewPack(uUID, serverPackData);
      }
   }

   private void pushNewPack(UUID uUID, ServerPackManager.ServerPackData serverPackData) {
      this.markExistingPacksAsRemoved(uUID);
      this.packs.add(serverPackData);
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.ALLOWED) {
         this.acceptPack(serverPackData);
      }

      this.registerForUpdate();
   }

   private void acceptPack(ServerPackManager.ServerPackData serverPackData) {
      this.packLoadFeedback.reportUpdate(serverPackData.id, PackLoadFeedback.Update.ACCEPTED);
      serverPackData.promptAccepted = true;
   }

   @Nullable
   private ServerPackManager.ServerPackData findPackInfo(UUID uUID) {
      Iterator var2 = this.packs.iterator();

      ServerPackManager.ServerPackData serverPackData;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         serverPackData = (ServerPackManager.ServerPackData)var2.next();
      } while(serverPackData.isRemoved() || !serverPackData.id.equals(uUID));

      return serverPackData;
   }

   public void popPack(UUID uUID) {
      ServerPackManager.ServerPackData serverPackData = this.findPackInfo(uUID);
      if (serverPackData != null) {
         serverPackData.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
         this.registerForUpdate();
      }

   }

   public void popAll() {
      Iterator var1 = this.packs.iterator();

      while(var1.hasNext()) {
         ServerPackManager.ServerPackData serverPackData = (ServerPackManager.ServerPackData)var1.next();
         serverPackData.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
      }

      this.registerForUpdate();
   }

   public void allowServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.ALLOWED;
      Iterator var1 = this.packs.iterator();

      while(var1.hasNext()) {
         ServerPackManager.ServerPackData serverPackData = (ServerPackManager.ServerPackData)var1.next();
         if (!serverPackData.promptAccepted && !serverPackData.isRemoved()) {
            this.acceptPack(serverPackData);
         }
      }

      this.registerForUpdate();
   }

   public void rejectServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.DECLINED;
      Iterator var1 = this.packs.iterator();

      while(var1.hasNext()) {
         ServerPackManager.ServerPackData serverPackData = (ServerPackManager.ServerPackData)var1.next();
         if (!serverPackData.promptAccepted) {
            serverPackData.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DECLINED);
         }
      }

      this.registerForUpdate();
   }

   public void resetPromptStatus() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.PENDING;
   }

   public void tick() {
      boolean bl = this.updateDownloads();
      if (!bl) {
         this.triggerReloadIfNeeded();
      }

      this.cleanupRemovedPacks();
   }

   private void cleanupRemovedPacks() {
      this.packs.removeIf((serverPackData) -> {
         if (serverPackData.activationStatus != ServerPackManager.ActivationStatus.INACTIVE) {
            return false;
         } else if (serverPackData.removalReason != null) {
            PackLoadFeedback.FinalResult finalResult = serverPackData.removalReason.serverResponse;
            if (finalResult != null) {
               this.packLoadFeedback.reportFinalResult(serverPackData.id, finalResult);
            }

            return true;
         } else {
            return false;
         }
      });
   }

   private void onDownload(Collection<ServerPackManager.ServerPackData> collection, BatchResult batchResult) {
      Iterator var3;
      ServerPackManager.ServerPackData serverPackData;
      if (!batchResult.failed().isEmpty()) {
         var3 = this.packs.iterator();

         while(var3.hasNext()) {
            serverPackData = (ServerPackManager.ServerPackData)var3.next();
            if (serverPackData.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               if (batchResult.failed().contains(serverPackData.id)) {
                  serverPackData.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DOWNLOAD_FAILED);
               } else {
                  serverPackData.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
               }
            }
         }
      }

      var3 = collection.iterator();

      while(var3.hasNext()) {
         serverPackData = (ServerPackManager.ServerPackData)var3.next();
         Path path = (Path)batchResult.downloaded().get(serverPackData.id);
         if (path != null) {
            serverPackData.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
            serverPackData.path = path;
            if (!serverPackData.isRemoved()) {
               this.packLoadFeedback.reportUpdate(serverPackData.id, PackLoadFeedback.Update.DOWNLOADED);
            }
         }
      }

      this.registerForUpdate();
   }

   private boolean updateDownloads() {
      List<ServerPackManager.ServerPackData> list = new ArrayList();
      boolean bl = false;
      Iterator var3 = this.packs.iterator();

      while(var3.hasNext()) {
         ServerPackManager.ServerPackData serverPackData = (ServerPackManager.ServerPackData)var3.next();
         if (!serverPackData.isRemoved() && serverPackData.promptAccepted) {
            if (serverPackData.downloadStatus != ServerPackManager.PackDownloadStatus.DONE) {
               bl = true;
            }

            if (serverPackData.downloadStatus == ServerPackManager.PackDownloadStatus.REQUESTED) {
               serverPackData.downloadStatus = ServerPackManager.PackDownloadStatus.PENDING;
               list.add(serverPackData);
            }
         }
      }

      if (!list.isEmpty()) {
         Map<UUID, DownloadRequest> map = new HashMap();
         Iterator var7 = list.iterator();

         while(var7.hasNext()) {
            ServerPackManager.ServerPackData serverPackData2 = (ServerPackManager.ServerPackData)var7.next();
            map.put(serverPackData2.id, new DownloadRequest(serverPackData2.url, serverPackData2.hash));
         }

         this.downloader.download(map, (batchResult) -> {
            this.onDownload(list, batchResult);
         });
      }

      return bl;
   }

   private void triggerReloadIfNeeded() {
      boolean bl = false;
      final List<ServerPackManager.ServerPackData> list = new ArrayList();
      final List<ServerPackManager.ServerPackData> list2 = new ArrayList();
      Iterator var4 = this.packs.iterator();

      ServerPackManager.ServerPackData serverPackData;
      while(var4.hasNext()) {
         serverPackData = (ServerPackManager.ServerPackData)var4.next();
         if (serverPackData.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
            return;
         }

         boolean bl2 = serverPackData.promptAccepted && serverPackData.downloadStatus == ServerPackManager.PackDownloadStatus.DONE && !serverPackData.isRemoved();
         if (bl2 && serverPackData.activationStatus == ServerPackManager.ActivationStatus.INACTIVE) {
            list.add(serverPackData);
            bl = true;
         }

         if (serverPackData.activationStatus == ServerPackManager.ActivationStatus.ACTIVE) {
            if (!bl2) {
               bl = true;
               list2.add(serverPackData);
            } else {
               list.add(serverPackData);
            }
         }
      }

      if (bl) {
         var4 = list.iterator();

         while(var4.hasNext()) {
            serverPackData = (ServerPackManager.ServerPackData)var4.next();
            if (serverPackData.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               serverPackData.activationStatus = ServerPackManager.ActivationStatus.PENDING;
            }
         }

         for(var4 = list2.iterator(); var4.hasNext(); serverPackData.activationStatus = ServerPackManager.ActivationStatus.PENDING) {
            serverPackData = (ServerPackManager.ServerPackData)var4.next();
         }

         this.reloadConfig.scheduleReload(new PackReloadConfig.Callbacks() {
            public void onSuccess() {
               Iterator var1 = list.iterator();

               ServerPackManager.ServerPackData serverPackData;
               while(var1.hasNext()) {
                  serverPackData = (ServerPackManager.ServerPackData)var1.next();
                  serverPackData.activationStatus = ServerPackManager.ActivationStatus.ACTIVE;
                  if (serverPackData.removalReason == null) {
                     ServerPackManager.this.packLoadFeedback.reportFinalResult(serverPackData.id, PackLoadFeedback.FinalResult.APPLIED);
                  }
               }

               for(var1 = list2.iterator(); var1.hasNext(); serverPackData.activationStatus = ServerPackManager.ActivationStatus.INACTIVE) {
                  serverPackData = (ServerPackManager.ServerPackData)var1.next();
               }

               ServerPackManager.this.registerForUpdate();
            }

            public void onFailure(boolean bl) {
               Iterator var2;
               ServerPackManager.ServerPackData serverPackData;
               if (!bl) {
                  list.clear();
                  var2 = ServerPackManager.this.packs.iterator();

                  while(var2.hasNext()) {
                     serverPackData = (ServerPackManager.ServerPackData)var2.next();
                     switch(serverPackData.activationStatus.ordinal()) {
                     case 0:
                        serverPackData.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
                        break;
                     case 1:
                        serverPackData.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                        serverPackData.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.ACTIVATION_FAILED);
                        break;
                     case 2:
                        list.add(serverPackData);
                     }
                  }

                  ServerPackManager.this.registerForUpdate();
               } else {
                  var2 = ServerPackManager.this.packs.iterator();

                  while(var2.hasNext()) {
                     serverPackData = (ServerPackManager.ServerPackData)var2.next();
                     if (serverPackData.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
                        serverPackData.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                     }
                  }
               }

            }

            public List<PackReloadConfig.IdAndPath> packsToLoad() {
               return list.stream().map((serverPackData) -> {
                  return new PackReloadConfig.IdAndPath(serverPackData.id, serverPackData.path);
               }).toList();
            }
         });
      }

   }

   @Environment(EnvType.CLIENT)
   public static enum PackPromptStatus {
      PENDING,
      ALLOWED,
      DECLINED;

      // $FF: synthetic method
      private static ServerPackManager.PackPromptStatus[] $values() {
         return new ServerPackManager.PackPromptStatus[]{PENDING, ALLOWED, DECLINED};
      }
   }

   @Environment(EnvType.CLIENT)
   private static class ServerPackData {
      final UUID id;
      final URL url;
      @Nullable
      final HashCode hash;
      @Nullable
      Path path;
      @Nullable
      ServerPackManager.RemovalReason removalReason;
      ServerPackManager.PackDownloadStatus downloadStatus;
      ServerPackManager.ActivationStatus activationStatus;
      boolean promptAccepted;

      ServerPackData(UUID uUID, URL uRL, @Nullable HashCode hashCode) {
         this.downloadStatus = ServerPackManager.PackDownloadStatus.REQUESTED;
         this.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
         this.id = uUID;
         this.url = uRL;
         this.hash = hashCode;
      }

      public void setRemovalReasonIfNotSet(ServerPackManager.RemovalReason removalReason) {
         if (this.removalReason == null) {
            this.removalReason = removalReason;
         }

      }

      public boolean isRemoved() {
         return this.removalReason != null;
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum RemovalReason {
      DOWNLOAD_FAILED(PackLoadFeedback.FinalResult.DOWNLOAD_FAILED),
      ACTIVATION_FAILED(PackLoadFeedback.FinalResult.ACTIVATION_FAILED),
      DECLINED(PackLoadFeedback.FinalResult.DECLINED),
      DISCARDED(PackLoadFeedback.FinalResult.DISCARDED),
      SERVER_REMOVED((PackLoadFeedback.FinalResult)null),
      SERVER_REPLACED((PackLoadFeedback.FinalResult)null);

      @Nullable
      final PackLoadFeedback.FinalResult serverResponse;

      private RemovalReason(@Nullable final PackLoadFeedback.FinalResult finalResult) {
         this.serverResponse = finalResult;
      }

      // $FF: synthetic method
      private static ServerPackManager.RemovalReason[] $values() {
         return new ServerPackManager.RemovalReason[]{DOWNLOAD_FAILED, ACTIVATION_FAILED, DECLINED, DISCARDED, SERVER_REMOVED, SERVER_REPLACED};
      }
   }

   @Environment(EnvType.CLIENT)
   static enum PackDownloadStatus {
      REQUESTED,
      PENDING,
      DONE;

      // $FF: synthetic method
      private static ServerPackManager.PackDownloadStatus[] $values() {
         return new ServerPackManager.PackDownloadStatus[]{REQUESTED, PENDING, DONE};
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum ActivationStatus {
      INACTIVE,
      PENDING,
      ACTIVE;

      // $FF: synthetic method
      private static ServerPackManager.ActivationStatus[] $values() {
         return new ServerPackManager.ActivationStatus[]{INACTIVE, PENDING, ACTIVE};
      }
   }
}

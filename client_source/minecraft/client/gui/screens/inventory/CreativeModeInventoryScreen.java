package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.CreativeModeTab.Row;
import net.minecraft.world.item.CreativeModeTab.Type;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag.Default;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CreativeModeInventoryScreen extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> implements FabricCreativeInventoryScreen {
   private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
   private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
   private static final ResourceLocation[] UNSELECTED_TOP_TABS = new ResourceLocation[]{ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_3"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_4"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_5"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_6"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_7")};
   private static final ResourceLocation[] SELECTED_TOP_TABS = new ResourceLocation[]{ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_1"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_2"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_3"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_4"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_5"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_6"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_7")};
   private static final ResourceLocation[] UNSELECTED_BOTTOM_TABS = new ResourceLocation[]{ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_1"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_2"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_3"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_4"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_5"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_6"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_7")};
   private static final ResourceLocation[] SELECTED_BOTTOM_TABS = new ResourceLocation[]{ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_1"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_2"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_3"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_4"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_5"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_6"), ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_7")};
   private static final int NUM_ROWS = 5;
   private static final int NUM_COLS = 9;
   private static final int TAB_WIDTH = 26;
   private static final int TAB_HEIGHT = 32;
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   static final SimpleContainer CONTAINER = new SimpleContainer(45);
   private static final Component TRASH_SLOT_TOOLTIP = Component.translatable("inventory.binSlot");
   private static final int TEXT_COLOR = 16777215;
   private static CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();
   private float scrollOffs;
   private boolean scrolling;
   private EditBox searchBox;
   @Nullable
   private List<Slot> originalSlots;
   @Nullable
   private Slot destroyItemSlot;
   private CreativeInventoryListener listener;
   private boolean ignoreTextInput;
   private boolean hasClickedOutside;
   private final Set<TagKey<Item>> visibleTags = new HashSet();
   private final boolean displayOperatorCreativeTab;

   public CreativeModeInventoryScreen(LocalPlayer localPlayer, FeatureFlagSet featureFlagSet, boolean bl) {
      super(new CreativeModeInventoryScreen.ItemPickerMenu(localPlayer), localPlayer.getInventory(), CommonComponents.EMPTY);
      localPlayer.containerMenu = this.menu;
      this.imageHeight = 136;
      this.imageWidth = 195;
      this.displayOperatorCreativeTab = bl;
      this.tryRebuildTabContents(localPlayer.connection.searchTrees(), featureFlagSet, this.hasPermissions(localPlayer), localPlayer.level().registryAccess());
   }

   private boolean hasPermissions(Player player) {
      return player.canUseGameMasterBlocks() && this.displayOperatorCreativeTab;
   }

   private void tryRefreshInvalidatedTabs(FeatureFlagSet featureFlagSet, boolean bl, Provider provider) {
      ClientPacketListener clientPacketListener = this.minecraft.getConnection();
      if (this.tryRebuildTabContents(clientPacketListener != null ? clientPacketListener.searchTrees() : null, featureFlagSet, bl, provider)) {
         Iterator var5 = CreativeModeTabs.allTabs().iterator();

         while(true) {
            while(true) {
               CreativeModeTab creativeModeTab;
               Collection collection;
               do {
                  if (!var5.hasNext()) {
                     return;
                  }

                  creativeModeTab = (CreativeModeTab)var5.next();
                  collection = creativeModeTab.getDisplayItems();
               } while(creativeModeTab != selectedTab);

               if (creativeModeTab.getType() == Type.CATEGORY && collection.isEmpty()) {
                  this.selectTab(CreativeModeTabs.getDefaultTab());
               } else {
                  this.refreshCurrentTabContents(collection);
               }
            }
         }
      }
   }

   private boolean tryRebuildTabContents(@Nullable SessionSearchTrees sessionSearchTrees, FeatureFlagSet featureFlagSet, boolean bl, Provider provider) {
      if (!CreativeModeTabs.tryRebuildTabContents(featureFlagSet, bl, provider)) {
         return false;
      } else {
         if (sessionSearchTrees != null) {
            List<ItemStack> list = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
            sessionSearchTrees.updateCreativeTooltips(provider, list);
            sessionSearchTrees.updateCreativeTags(list);
         }

         return true;
      }
   }

   private void refreshCurrentTabContents(Collection<ItemStack> collection) {
      int i = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getRowIndexForScroll(this.scrollOffs);
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.clear();
      if (selectedTab.getType() == Type.SEARCH) {
         this.refreshSearchResults();
      } else {
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(collection);
      }

      this.scrollOffs = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getScrollForRowIndex(i);
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
   }

   public void containerTick() {
      super.containerTick();
      if (this.minecraft != null) {
         if (this.minecraft.player != null) {
            this.tryRefreshInvalidatedTabs(this.minecraft.player.connection.enabledFeatures(), this.hasPermissions(this.minecraft.player), this.minecraft.player.level().registryAccess());
         }

         if (!this.minecraft.gameMode.hasInfiniteItems()) {
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
         }

      }
   }

   protected void slotClicked(@Nullable Slot slot, int i, int j, ClickType clickType) {
      if (this.isCreativeSlot(slot)) {
         this.searchBox.moveCursorToEnd(false);
         this.searchBox.setHighlightPos(0);
      }

      boolean bl = clickType == ClickType.QUICK_MOVE;
      clickType = i == -999 && clickType == ClickType.PICKUP ? ClickType.THROW : clickType;
      ItemStack itemStack;
      if (slot == null && selectedTab.getType() != Type.INVENTORY && clickType != ClickType.QUICK_CRAFT) {
         if (!((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().isEmpty() && this.hasClickedOutside) {
            if (j == 0) {
               this.minecraft.player.drop(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried(), true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried());
               ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
            }

            if (j == 1) {
               itemStack = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().split(1);
               this.minecraft.player.drop(itemStack, true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack);
            }
         }
      } else {
         if (slot != null && !slot.mayPickup(this.minecraft.player)) {
            return;
         }

         if (slot == this.destroyItemSlot && bl) {
            for(int k = 0; k < this.minecraft.player.inventoryMenu.getItems().size(); ++k) {
               this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, k);
            }
         } else {
            ItemStack itemStack2;
            if (selectedTab.getType() == Type.INVENTORY) {
               if (slot == this.destroyItemSlot) {
                  ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
               } else if (clickType == ClickType.THROW && slot != null && slot.hasItem()) {
                  itemStack = slot.remove(j == 0 ? 1 : slot.getItem().getMaxStackSize());
                  itemStack2 = slot.getItem();
                  this.minecraft.player.drop(itemStack, true);
                  this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack);
                  this.minecraft.gameMode.handleCreativeModeItemAdd(itemStack2, ((CreativeModeInventoryScreen.SlotWrapper)slot).target.index);
               } else if (clickType == ClickType.THROW && !((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().isEmpty()) {
                  this.minecraft.player.drop(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried(), true);
                  this.minecraft.gameMode.handleCreativeModeItemDrop(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried());
                  ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
               } else {
                  this.minecraft.player.inventoryMenu.clicked(slot == null ? i : ((CreativeModeInventoryScreen.SlotWrapper)slot).target.index, j, clickType, this.minecraft.player);
                  this.minecraft.player.inventoryMenu.broadcastChanges();
               }
            } else {
               int l;
               if (clickType != ClickType.QUICK_CRAFT && slot.container == CONTAINER) {
                  itemStack = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried();
                  itemStack2 = slot.getItem();
                  if (clickType == ClickType.SWAP) {
                     if (!itemStack2.isEmpty()) {
                        this.minecraft.player.getInventory().setItem(j, itemStack2.copyWithCount(itemStack2.getMaxStackSize()));
                        this.minecraft.player.inventoryMenu.broadcastChanges();
                     }

                     return;
                  }

                  ItemStack itemStack3;
                  if (clickType == ClickType.CLONE) {
                     if (((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().isEmpty() && slot.hasItem()) {
                        itemStack3 = slot.getItem();
                        ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(itemStack3.copyWithCount(itemStack3.getMaxStackSize()));
                     }

                     return;
                  }

                  if (clickType == ClickType.THROW) {
                     if (!itemStack2.isEmpty()) {
                        itemStack3 = itemStack2.copyWithCount(j == 0 ? 1 : itemStack2.getMaxStackSize());
                        this.minecraft.player.drop(itemStack3, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack3);
                     }

                     return;
                  }

                  if (!itemStack.isEmpty() && !itemStack2.isEmpty() && ItemStack.isSameItemSameComponents(itemStack, itemStack2)) {
                     if (j == 0) {
                        if (bl) {
                           itemStack.setCount(itemStack.getMaxStackSize());
                        } else if (itemStack.getCount() < itemStack.getMaxStackSize()) {
                           itemStack.grow(1);
                        }
                     } else {
                        itemStack.shrink(1);
                     }
                  } else if (!itemStack2.isEmpty() && itemStack.isEmpty()) {
                     l = bl ? itemStack2.getMaxStackSize() : itemStack2.getCount();
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(itemStack2.copyWithCount(l));
                  } else if (j == 0) {
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
                  } else if (!((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().isEmpty()) {
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().shrink(1);
                  }
               } else if (this.menu != null) {
                  itemStack = slot == null ? ItemStack.EMPTY : ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getSlot(slot.index).getItem();
                  ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).clicked(slot == null ? i : slot.index, j, clickType, this.minecraft.player);
                  if (AbstractContainerMenu.getQuickcraftHeader(j) == 2) {
                     for(int m = 0; m < 9; ++m) {
                        this.minecraft.gameMode.handleCreativeModeItemAdd(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getSlot(45 + m).getItem(), 36 + m);
                     }
                  } else if (slot != null) {
                     itemStack2 = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getSlot(slot.index).getItem();
                     this.minecraft.gameMode.handleCreativeModeItemAdd(itemStack2, slot.index - ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.size() + 9 + 36);
                     l = 45 + j;
                     if (clickType == ClickType.SWAP) {
                        this.minecraft.gameMode.handleCreativeModeItemAdd(itemStack, l - ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.size() + 9 + 36);
                     } else if (clickType == ClickType.THROW && !itemStack.isEmpty()) {
                        ItemStack itemStack4 = itemStack.copyWithCount(j == 0 ? 1 : itemStack.getMaxStackSize());
                        this.minecraft.player.drop(itemStack4, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(itemStack4);
                     }

                     this.minecraft.player.inventoryMenu.broadcastChanges();
                  }
               }
            }
         }
      }

   }

   private boolean isCreativeSlot(@Nullable Slot slot) {
      return slot != null && slot.container == CONTAINER;
   }

   protected void init() {
      if (this.minecraft.gameMode.hasInfiniteItems()) {
         super.init();
         Font var10003 = this.font;
         int var10004 = this.leftPos + 82;
         int var10005 = this.topPos + 6;
         Objects.requireNonNull(this.font);
         this.searchBox = new EditBox(var10003, var10004, var10005, 80, 9, Component.translatable("itemGroup.search"));
         this.searchBox.setMaxLength(50);
         this.searchBox.setBordered(false);
         this.searchBox.setVisible(false);
         this.searchBox.setTextColor(16777215);
         this.addWidget(this.searchBox);
         CreativeModeTab creativeModeTab = selectedTab;
         selectedTab = CreativeModeTabs.getDefaultTab();
         this.selectTab(creativeModeTab);
         this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
         this.listener = new CreativeInventoryListener(this.minecraft);
         this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
         if (!selectedTab.shouldDisplay()) {
            this.selectTab(CreativeModeTabs.getDefaultTab());
         }
      } else {
         this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
      }

   }

   public void resize(Minecraft minecraft, int i, int j) {
      int k = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getRowIndexForScroll(this.scrollOffs);
      String string = this.searchBox.getValue();
      this.init(minecraft, i, j);
      this.searchBox.setValue(string);
      if (!this.searchBox.getValue().isEmpty()) {
         this.refreshSearchResults();
      }

      this.scrollOffs = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getScrollForRowIndex(k);
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
   }

   public void removed() {
      super.removed();
      if (this.minecraft.player != null && this.minecraft.player.getInventory() != null) {
         this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
      }

   }

   public boolean charTyped(char c, int i) {
      if (this.ignoreTextInput) {
         return false;
      } else if (selectedTab.getType() != Type.SEARCH) {
         return false;
      } else {
         String string = this.searchBox.getValue();
         if (this.searchBox.charTyped(c, i)) {
            if (!Objects.equals(string, this.searchBox.getValue())) {
               this.refreshSearchResults();
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public boolean keyPressed(int i, int j, int k) {
      this.ignoreTextInput = false;
      if (selectedTab.getType() != Type.SEARCH) {
         if (this.minecraft.options.keyChat.matches(i, j)) {
            this.ignoreTextInput = true;
            this.selectTab(CreativeModeTabs.searchTab());
            return true;
         } else {
            return super.keyPressed(i, j, k);
         }
      } else {
         boolean bl = !this.isCreativeSlot(this.hoveredSlot) || this.hoveredSlot.hasItem();
         boolean bl2 = InputConstants.getKey(i, j).getNumericKeyValue().isPresent();
         if (bl && bl2 && this.checkHotbarKeyPressed(i, j)) {
            this.ignoreTextInput = true;
            return true;
         } else {
            String string = this.searchBox.getValue();
            if (this.searchBox.keyPressed(i, j, k)) {
               if (!Objects.equals(string, this.searchBox.getValue())) {
                  this.refreshSearchResults();
               }

               return true;
            } else {
               return this.searchBox.isFocused() && this.searchBox.isVisible() && i != 256 ? true : super.keyPressed(i, j, k);
            }
         }
      }
   }

   public boolean keyReleased(int i, int j, int k) {
      this.ignoreTextInput = false;
      return super.keyReleased(i, j, k);
   }

   private void refreshSearchResults() {
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.clear();
      this.visibleTags.clear();
      String string = this.searchBox.getValue();
      if (string.isEmpty()) {
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(selectedTab.getDisplayItems());
      } else {
         ClientPacketListener clientPacketListener = this.minecraft.getConnection();
         if (clientPacketListener != null) {
            SessionSearchTrees sessionSearchTrees = clientPacketListener.searchTrees();
            SearchTree searchTree;
            if (string.startsWith("#")) {
               string = string.substring(1);
               searchTree = sessionSearchTrees.creativeTagSearch();
               this.updateVisibleTags(string);
            } else {
               searchTree = sessionSearchTrees.creativeNameSearch();
            }

            ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(searchTree.search(string.toLowerCase(Locale.ROOT)));
         }
      }

      this.scrollOffs = 0.0F;
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(0.0F);
   }

   private void updateVisibleTags(String string) {
      int i = string.indexOf(58);
      Predicate predicate;
      if (i == -1) {
         predicate = (resourceLocation) -> {
            return resourceLocation.getPath().contains(string);
         };
      } else {
         String string2 = string.substring(0, i).trim();
         String string3 = string.substring(i + 1).trim();
         predicate = (resourceLocation) -> {
            return resourceLocation.getNamespace().contains(string2) && resourceLocation.getPath().contains(string3);
         };
      }

      Stream var10000 = BuiltInRegistries.ITEM.getTagNames().filter((tagKey) -> {
         return predicate.test(tagKey.location());
      });
      Set var10001 = this.visibleTags;
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::add);
   }

   protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
      if (selectedTab.showTitle()) {
         guiGraphics.drawString(this.font, (Component)selectedTab.getDisplayName(), 8, 6, 4210752, false);
      }

   }

   public boolean mouseClicked(double d, double e, int i) {
      if (i == 0) {
         double f = d - (double)this.leftPos;
         double g = e - (double)this.topPos;
         Iterator var10 = CreativeModeTabs.tabs().iterator();

         while(var10.hasNext()) {
            CreativeModeTab creativeModeTab = (CreativeModeTab)var10.next();
            if (this.checkTabClicked(creativeModeTab, f, g)) {
               return true;
            }
         }

         if (selectedTab.getType() != Type.INVENTORY && this.insideScrollbar(d, e)) {
            this.scrolling = this.canScroll();
            return true;
         }
      }

      return super.mouseClicked(d, e, i);
   }

   public boolean mouseReleased(double d, double e, int i) {
      if (i == 0) {
         double f = d - (double)this.leftPos;
         double g = e - (double)this.topPos;
         this.scrolling = false;
         Iterator var10 = CreativeModeTabs.tabs().iterator();

         while(var10.hasNext()) {
            CreativeModeTab creativeModeTab = (CreativeModeTab)var10.next();
            if (this.checkTabClicked(creativeModeTab, f, g)) {
               this.selectTab(creativeModeTab);
               return true;
            }
         }
      }

      return super.mouseReleased(d, e, i);
   }

   private boolean canScroll() {
      return selectedTab.canScroll() && ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).canScroll();
   }

   private void selectTab(CreativeModeTab creativeModeTab) {
      CreativeModeTab creativeModeTab2 = selectedTab;
      selectedTab = creativeModeTab;
      this.quickCraftSlots.clear();
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.clear();
      this.clearDraggingState();
      int i;
      int j;
      if (selectedTab.getType() == Type.HOTBAR) {
         HotbarManager hotbarManager = this.minecraft.getHotbarManager();

         for(i = 0; i < 9; ++i) {
            Hotbar hotbar = hotbarManager.get(i);
            if (hotbar.isEmpty()) {
               for(j = 0; j < 9; ++j) {
                  if (j == i) {
                     ItemStack itemStack = new ItemStack(Items.PAPER);
                     itemStack.set(DataComponents.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
                     Component component = this.minecraft.options.keyHotbarSlots[i].getTranslatedKeyMessage();
                     Component component2 = this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage();
                     itemStack.set(DataComponents.ITEM_NAME, Component.translatable("inventory.hotbarInfo", new Object[]{component2, component}));
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.add(itemStack);
                  } else {
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.add(ItemStack.EMPTY);
                  }
               }
            } else {
               ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(hotbar.load(this.minecraft.level.registryAccess()));
            }
         }
      } else if (selectedTab.getType() == Type.CATEGORY) {
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(selectedTab.getDisplayItems());
      }

      if (selectedTab.getType() == Type.INVENTORY) {
         AbstractContainerMenu abstractContainerMenu = this.minecraft.player.inventoryMenu;
         if (this.originalSlots == null) {
            this.originalSlots = ImmutableList.copyOf(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots);
         }

         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.clear();

         for(i = 0; i < abstractContainerMenu.slots.size(); ++i) {
            int n;
            int k;
            int l;
            int m;
            if (i >= 5 && i < 9) {
               k = i - 5;
               l = k / 2;
               m = k % 2;
               n = 54 + l * 54;
               j = 6 + m * 27;
            } else if (i >= 0 && i < 5) {
               n = -2000;
               j = -2000;
            } else if (i == 45) {
               n = 35;
               j = 20;
            } else {
               k = i - 9;
               l = k % 9;
               m = k / 9;
               n = 9 + l * 18;
               if (i >= 36) {
                  j = 112;
               } else {
                  j = 54 + m * 18;
               }
            }

            Slot slot = new CreativeModeInventoryScreen.SlotWrapper((Slot)abstractContainerMenu.slots.get(i), i, n, j);
            ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.add(slot);
         }

         this.destroyItemSlot = new Slot(CONTAINER, 0, 173, 112);
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.add(this.destroyItemSlot);
      } else if (creativeModeTab2.getType() == Type.INVENTORY) {
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.clear();
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.addAll(this.originalSlots);
         this.originalSlots = null;
      }

      if (selectedTab.getType() == Type.SEARCH) {
         this.searchBox.setVisible(true);
         this.searchBox.setCanLoseFocus(false);
         this.searchBox.setFocused(true);
         if (creativeModeTab2 != creativeModeTab) {
            this.searchBox.setValue("");
         }

         this.refreshSearchResults();
      } else {
         this.searchBox.setVisible(false);
         this.searchBox.setCanLoseFocus(true);
         this.searchBox.setFocused(false);
         this.searchBox.setValue("");
      }

      this.scrollOffs = 0.0F;
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(0.0F);
   }

   public boolean mouseScrolled(double d, double e, double f, double g) {
      if (!this.canScroll()) {
         return false;
      } else {
         this.scrollOffs = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).subtractInputFromScroll(this.scrollOffs, g);
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
         return true;
      }
   }

   protected boolean hasClickedOutside(double d, double e, int i, int j, int k) {
      boolean bl = d < (double)i || e < (double)j || d >= (double)(i + this.imageWidth) || e >= (double)(j + this.imageHeight);
      this.hasClickedOutside = bl && !this.checkTabClicked(selectedTab, d, e);
      return this.hasClickedOutside;
   }

   protected boolean insideScrollbar(double d, double e) {
      int i = this.leftPos;
      int j = this.topPos;
      int k = i + 175;
      int l = j + 18;
      int m = k + 14;
      int n = l + 112;
      return d >= (double)k && e >= (double)l && d < (double)m && e < (double)n;
   }

   public boolean mouseDragged(double d, double e, int i, double f, double g) {
      if (this.scrolling) {
         int j = this.topPos + 18;
         int k = j + 112;
         this.scrollOffs = ((float)e - (float)j - 7.5F) / ((float)(k - j) - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
         return true;
      } else {
         return super.mouseDragged(d, e, i, f, g);
      }
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);
      Iterator var5 = CreativeModeTabs.tabs().iterator();

      while(var5.hasNext()) {
         CreativeModeTab creativeModeTab = (CreativeModeTab)var5.next();
         if (this.checkTabHovering(guiGraphics, creativeModeTab, i, j)) {
            break;
         }
      }

      if (this.destroyItemSlot != null && selectedTab.getType() == Type.INVENTORY && this.isHovering(this.destroyItemSlot.x, this.destroyItemSlot.y, 16, 16, (double)i, (double)j)) {
         guiGraphics.renderTooltip(this.font, TRASH_SLOT_TOOLTIP, i, j);
      }

      this.renderTooltip(guiGraphics, i, j);
   }

   public List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
      boolean bl = this.hoveredSlot != null && this.hoveredSlot instanceof CreativeModeInventoryScreen.CustomCreativeSlot;
      boolean bl2 = selectedTab.getType() == Type.CATEGORY;
      boolean bl3 = selectedTab.getType() == Type.SEARCH;
      Default default_ = this.minecraft.options.advancedItemTooltips ? Default.ADVANCED : Default.NORMAL;
      TooltipFlag tooltipFlag = bl ? default_.asCreative() : default_;
      List<Component> list = itemStack.getTooltipLines(TooltipContext.of(this.minecraft.level), this.minecraft.player, tooltipFlag);
      if (bl2 && bl) {
         return list;
      } else {
         List<Component> list2 = Lists.newArrayList(list);
         if (bl3 && bl) {
            this.visibleTags.forEach((tagKey) -> {
               if (itemStack.is(tagKey)) {
                  list2.add(1, Component.literal("#" + String.valueOf(tagKey.location())).withStyle(ChatFormatting.DARK_PURPLE));
               }

            });
         }

         int i = 1;
         Iterator var10 = CreativeModeTabs.tabs().iterator();

         while(var10.hasNext()) {
            CreativeModeTab creativeModeTab = (CreativeModeTab)var10.next();
            if (creativeModeTab.getType() != Type.SEARCH && creativeModeTab.contains(itemStack)) {
               list2.add(i++, creativeModeTab.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
            }
         }

         return list2;
      }
   }

   protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
      Iterator var5 = CreativeModeTabs.tabs().iterator();

      while(var5.hasNext()) {
         CreativeModeTab creativeModeTab = (CreativeModeTab)var5.next();
         if (creativeModeTab != selectedTab) {
            this.renderTabButton(guiGraphics, creativeModeTab);
         }
      }

      guiGraphics.blit(selectedTab.getBackgroundTexture(), this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      this.searchBox.render(guiGraphics, i, j, f);
      int k = this.leftPos + 175;
      int l = this.topPos + 18;
      int m = l + 112;
      if (selectedTab.canScroll()) {
         ResourceLocation resourceLocation = this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
         guiGraphics.blitSprite(resourceLocation, k, l + (int)((float)(m - l - 17) * this.scrollOffs), 12, 15);
      }

      this.renderTabButton(guiGraphics, selectedTab);
      if (selectedTab.getType() == Type.INVENTORY) {
         InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, this.leftPos + 73, this.topPos + 6, this.leftPos + 105, this.topPos + 49, 20, 0.0625F, (float)i, (float)j, this.minecraft.player);
      }

   }

   private int getTabX(CreativeModeTab creativeModeTab) {
      int i = creativeModeTab.column();
      int j = true;
      int k = 27 * i;
      if (creativeModeTab.isAlignedRight()) {
         k = this.imageWidth - 27 * (7 - i) + 1;
      }

      return k;
   }

   private int getTabY(CreativeModeTab creativeModeTab) {
      int i = 0;
      int i;
      if (creativeModeTab.row() == Row.TOP) {
         i = i - 32;
      } else {
         i = i + this.imageHeight;
      }

      return i;
   }

   protected boolean checkTabClicked(CreativeModeTab creativeModeTab, double d, double e) {
      int i = this.getTabX(creativeModeTab);
      int j = this.getTabY(creativeModeTab);
      return d >= (double)i && d <= (double)(i + 26) && e >= (double)j && e <= (double)(j + 32);
   }

   protected boolean checkTabHovering(GuiGraphics guiGraphics, CreativeModeTab creativeModeTab, int i, int j) {
      int k = this.getTabX(creativeModeTab);
      int l = this.getTabY(creativeModeTab);
      if (this.isHovering(k + 3, l + 3, 21, 27, (double)i, (double)j)) {
         guiGraphics.renderTooltip(this.font, creativeModeTab.getDisplayName(), i, j);
         return true;
      } else {
         return false;
      }
   }

   protected void renderTabButton(GuiGraphics guiGraphics, CreativeModeTab creativeModeTab) {
      boolean bl = creativeModeTab == selectedTab;
      boolean bl2 = creativeModeTab.row() == Row.TOP;
      int i = creativeModeTab.column();
      int j = this.leftPos + this.getTabX(creativeModeTab);
      int k = this.topPos - (bl2 ? 28 : -(this.imageHeight - 4));
      ResourceLocation[] resourceLocations;
      if (bl2) {
         resourceLocations = bl ? SELECTED_TOP_TABS : UNSELECTED_TOP_TABS;
      } else {
         resourceLocations = bl ? SELECTED_BOTTOM_TABS : UNSELECTED_BOTTOM_TABS;
      }

      guiGraphics.blitSprite(resourceLocations[Mth.clamp(i, 0, resourceLocations.length)], j, k, 26, 32);
      guiGraphics.pose().pushPose();
      guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
      j += 5;
      k += 8 + (bl2 ? 1 : -1);
      ItemStack itemStack = creativeModeTab.getIconItem();
      guiGraphics.renderItem(itemStack, j, k);
      guiGraphics.renderItemDecorations(this.font, itemStack, j, k);
      guiGraphics.pose().popPose();
   }

   public boolean isInventoryOpen() {
      return selectedTab.getType() == Type.INVENTORY;
   }

   public static void handleHotbarLoadOrSave(Minecraft minecraft, int i, boolean bl, boolean bl2) {
      LocalPlayer localPlayer = minecraft.player;
      RegistryAccess registryAccess = localPlayer.level().registryAccess();
      HotbarManager hotbarManager = minecraft.getHotbarManager();
      Hotbar hotbar = hotbarManager.get(i);
      if (bl) {
         List<ItemStack> list = hotbar.load(registryAccess);

         for(int j = 0; j < Inventory.getSelectionSize(); ++j) {
            ItemStack itemStack = (ItemStack)list.get(j);
            localPlayer.getInventory().setItem(j, itemStack);
            minecraft.gameMode.handleCreativeModeItemAdd(itemStack, 36 + j);
         }

         localPlayer.inventoryMenu.broadcastChanges();
      } else if (bl2) {
         hotbar.storeFrom(localPlayer.getInventory(), registryAccess);
         Component component = minecraft.options.keyHotbarSlots[i].getTranslatedKeyMessage();
         Component component2 = minecraft.options.keyLoadHotbarActivator.getTranslatedKeyMessage();
         Component component3 = Component.translatable("inventory.hotbarSaved", new Object[]{component2, component});
         minecraft.gui.setOverlayMessage(component3, false);
         minecraft.getNarrator().sayNow((Component)component3);
         hotbarManager.save();
      }

   }

   @Environment(EnvType.CLIENT)
   public static class ItemPickerMenu extends AbstractContainerMenu {
      public final NonNullList<ItemStack> items = NonNullList.create();
      private final AbstractContainerMenu inventoryMenu;

      public ItemPickerMenu(Player player) {
         super((MenuType)null, 0);
         this.inventoryMenu = player.inventoryMenu;
         Inventory inventory = player.getInventory();

         int i;
         for(i = 0; i < 5; ++i) {
            for(int j = 0; j < 9; ++j) {
               this.addSlot(new CreativeModeInventoryScreen.CustomCreativeSlot(CreativeModeInventoryScreen.CONTAINER, i * 9 + j, 9 + j * 18, 18 + i * 18));
            }
         }

         for(i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 9 + i * 18, 112));
         }

         this.scrollTo(0.0F);
      }

      public boolean stillValid(Player player) {
         return true;
      }

      protected int calculateRowCount() {
         return Mth.positiveCeilDiv(this.items.size(), 9) - 5;
      }

      protected int getRowIndexForScroll(float f) {
         return Math.max((int)((double)(f * (float)this.calculateRowCount()) + 0.5D), 0);
      }

      protected float getScrollForRowIndex(int i) {
         return Mth.clamp((float)i / (float)this.calculateRowCount(), 0.0F, 1.0F);
      }

      protected float subtractInputFromScroll(float f, double d) {
         return Mth.clamp(f - (float)(d / (double)this.calculateRowCount()), 0.0F, 1.0F);
      }

      public void scrollTo(float f) {
         int i = this.getRowIndexForScroll(f);

         for(int j = 0; j < 5; ++j) {
            for(int k = 0; k < 9; ++k) {
               int l = k + (j + i) * 9;
               if (l >= 0 && l < this.items.size()) {
                  CreativeModeInventoryScreen.CONTAINER.setItem(k + j * 9, (ItemStack)this.items.get(l));
               } else {
                  CreativeModeInventoryScreen.CONTAINER.setItem(k + j * 9, ItemStack.EMPTY);
               }
            }
         }

      }

      public boolean canScroll() {
         return this.items.size() > 45;
      }

      public ItemStack quickMoveStack(Player player, int i) {
         if (i >= this.slots.size() - 9 && i < this.slots.size()) {
            Slot slot = (Slot)this.slots.get(i);
            if (slot != null && slot.hasItem()) {
               slot.setByPlayer(ItemStack.EMPTY);
            }
         }

         return ItemStack.EMPTY;
      }

      public boolean canTakeItemForPickAll(ItemStack itemStack, Slot slot) {
         return slot.container != CreativeModeInventoryScreen.CONTAINER;
      }

      public boolean canDragTo(Slot slot) {
         return slot.container != CreativeModeInventoryScreen.CONTAINER;
      }

      public ItemStack getCarried() {
         return this.inventoryMenu.getCarried();
      }

      public void setCarried(ItemStack itemStack) {
         this.inventoryMenu.setCarried(itemStack);
      }
   }

   @Environment(EnvType.CLIENT)
   static class SlotWrapper extends Slot {
      final Slot target;

      public SlotWrapper(Slot slot, int i, int j, int k) {
         super(slot.container, i, j, k);
         this.target = slot;
      }

      public void onTake(Player player, ItemStack itemStack) {
         this.target.onTake(player, itemStack);
      }

      public boolean mayPlace(ItemStack itemStack) {
         return this.target.mayPlace(itemStack);
      }

      public ItemStack getItem() {
         return this.target.getItem();
      }

      public boolean hasItem() {
         return this.target.hasItem();
      }

      public void setByPlayer(ItemStack itemStack, ItemStack itemStack2) {
         this.target.setByPlayer(itemStack, itemStack2);
      }

      public void set(ItemStack itemStack) {
         this.target.set(itemStack);
      }

      public void setChanged() {
         this.target.setChanged();
      }

      public int getMaxStackSize() {
         return this.target.getMaxStackSize();
      }

      public int getMaxStackSize(ItemStack itemStack) {
         return this.target.getMaxStackSize(itemStack);
      }

      @Nullable
      public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
         return this.target.getNoItemIcon();
      }

      public ItemStack remove(int i) {
         return this.target.remove(i);
      }

      public boolean isActive() {
         return this.target.isActive();
      }

      public boolean mayPickup(Player player) {
         return this.target.mayPickup(player);
      }
   }

   @Environment(EnvType.CLIENT)
   static class CustomCreativeSlot extends Slot {
      public CustomCreativeSlot(Container container, int i, int j, int k) {
         super(container, i, j, k);
      }

      public boolean mayPickup(Player player) {
         ItemStack itemStack = this.getItem();
         if (super.mayPickup(player) && !itemStack.isEmpty()) {
            return itemStack.isItemEnabled(player.level().enabledFeatures()) && !itemStack.has(DataComponents.CREATIVE_SLOT_LOCK);
         } else {
            return itemStack.isEmpty();
         }
      }
   }
}

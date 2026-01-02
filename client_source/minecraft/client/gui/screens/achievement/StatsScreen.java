package net.minecraft.client.gui.screens.achievement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class StatsScreen extends Screen {
   private static final Component TITLE = Component.translatable("gui.stats");
   static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
   static final ResourceLocation HEADER_SPRITE = ResourceLocation.withDefaultNamespace("statistics/header");
   static final ResourceLocation SORT_UP_SPRITE = ResourceLocation.withDefaultNamespace("statistics/sort_up");
   static final ResourceLocation SORT_DOWN_SPRITE = ResourceLocation.withDefaultNamespace("statistics/sort_down");
   private static final Component PENDING_TEXT = Component.translatable("multiplayer.downloadingStats");
   static final Component NO_VALUE_DISPLAY = Component.translatable("stats.none");
   private static final Component GENERAL_BUTTON = Component.translatable("stat.generalButton");
   private static final Component ITEMS_BUTTON = Component.translatable("stat.itemsButton");
   private static final Component MOBS_BUTTON = Component.translatable("stat.mobsButton");
   protected final Screen lastScreen;
   private static final int LIST_WIDTH = 280;
   private static final int PADDING = 5;
   private static final int FOOTER_HEIGHT = 58;
   private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 58);
   @Nullable
   private StatsScreen.GeneralStatisticsList statsList;
   @Nullable
   StatsScreen.ItemStatisticsList itemStatsList;
   @Nullable
   private StatsScreen.MobsStatisticsList mobsStatsList;
   final StatsCounter stats;
   @Nullable
   private ObjectSelectionList<?> activeList;
   private boolean isLoading = true;

   public StatsScreen(Screen screen, StatsCounter statsCounter) {
      super(TITLE);
      this.lastScreen = screen;
      this.stats = statsCounter;
   }

   protected void init() {
      this.layout.addToContents(new LoadingDotsWidget(this.font, PENDING_TEXT));
      this.minecraft.getConnection().send(new ServerboundClientCommandPacket(Action.REQUEST_STATS));
   }

   public void initLists() {
      this.statsList = new StatsScreen.GeneralStatisticsList(this.minecraft);
      this.itemStatsList = new StatsScreen.ItemStatisticsList(this.minecraft);
      this.mobsStatsList = new StatsScreen.MobsStatisticsList(this.minecraft);
   }

   public void initButtons() {
      HeaderAndFooterLayout headerAndFooterLayout = new HeaderAndFooterLayout(this, 33, 58);
      headerAndFooterLayout.addTitleHeader(TITLE, this.font);
      LinearLayout linearLayout = ((LinearLayout)headerAndFooterLayout.addToFooter(LinearLayout.vertical())).spacing(5);
      linearLayout.defaultCellSetting().alignHorizontallyCenter();
      LinearLayout linearLayout2 = ((LinearLayout)linearLayout.addChild(LinearLayout.horizontal())).spacing(5);
      linearLayout2.addChild(Button.builder(GENERAL_BUTTON, (buttonx) -> {
         this.setActiveList(this.statsList);
      }).width(120).build());
      Button button = (Button)linearLayout2.addChild(Button.builder(ITEMS_BUTTON, (buttonx) -> {
         this.setActiveList(this.itemStatsList);
      }).width(120).build());
      Button button2 = (Button)linearLayout2.addChild(Button.builder(MOBS_BUTTON, (buttonx) -> {
         this.setActiveList(this.mobsStatsList);
      }).width(120).build());
      linearLayout.addChild(Button.builder(CommonComponents.GUI_DONE, (buttonx) -> {
         this.onClose();
      }).width(200).build());
      if (this.itemStatsList != null && this.itemStatsList.children().isEmpty()) {
         button.active = false;
      }

      if (this.mobsStatsList != null && this.mobsStatsList.children().isEmpty()) {
         button2.active = false;
      }

      this.layout = headerAndFooterLayout;
      this.layout.visitWidgets((guiEventListener) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
      });
      this.repositionElements();
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.activeList != null) {
         this.activeList.updateSize(this.width, this.layout);
      }

   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public void onStatsUpdated() {
      if (this.isLoading) {
         this.initLists();
         this.setActiveList(this.statsList);
         this.initButtons();
         this.setInitialFocus();
         this.isLoading = false;
      }

   }

   public boolean isPauseScreen() {
      return !this.isLoading;
   }

   public void setActiveList(@Nullable ObjectSelectionList<?> objectSelectionList) {
      if (this.activeList != null) {
         this.removeWidget(this.activeList);
      }

      if (objectSelectionList != null) {
         this.addRenderableWidget(objectSelectionList);
         this.activeList = objectSelectionList;
         this.repositionElements();
      }

   }

   static String getTranslationKey(Stat<ResourceLocation> stat) {
      String var10000 = ((ResourceLocation)stat.getValue()).toString();
      return "stat." + var10000.replace(':', '.');
   }

   @Environment(EnvType.CLIENT)
   class GeneralStatisticsList extends ObjectSelectionList<StatsScreen.GeneralStatisticsList.Entry> {
      public GeneralStatisticsList(final Minecraft minecraft) {
         super(minecraft, StatsScreen.this.width, StatsScreen.this.height - 33 - 58, 33, 14);
         ObjectArrayList<Stat<ResourceLocation>> objectArrayList = new ObjectArrayList(Stats.CUSTOM.iterator());
         objectArrayList.sort(Comparator.comparing((statx) -> {
            return I18n.get(StatsScreen.getTranslationKey(statx));
         }));
         ObjectListIterator var4 = objectArrayList.iterator();

         while(var4.hasNext()) {
            Stat<ResourceLocation> stat = (Stat)var4.next();
            this.addEntry(new StatsScreen.GeneralStatisticsList.Entry(stat));
         }

      }

      public int getRowWidth() {
         return 280;
      }

      @Environment(EnvType.CLIENT)
      private class Entry extends ObjectSelectionList.Entry<StatsScreen.GeneralStatisticsList.Entry> {
         private final Stat<ResourceLocation> stat;
         private final Component statDisplay;

         Entry(final Stat<ResourceLocation> stat) {
            this.stat = stat;
            this.statDisplay = Component.translatable(StatsScreen.getTranslationKey(stat));
         }

         private String getValueText() {
            return this.stat.format(StatsScreen.this.stats.getValue(this.stat));
         }

         public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            int var10000 = j + m / 2;
            Objects.requireNonNull(StatsScreen.this.font);
            int p = var10000 - 9 / 2;
            int q = i % 2 == 0 ? -1 : -4539718;
            guiGraphics.drawString(StatsScreen.this.font, this.statDisplay, k + 2, p, q);
            String string = this.getValueText();
            guiGraphics.drawString(StatsScreen.this.font, string, k + l - StatsScreen.this.font.width(string) - 4, p, q);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", new Object[]{Component.empty().append(this.statDisplay).append(CommonComponents.SPACE).append(this.getValueText())});
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private class ItemStatisticsList extends ObjectSelectionList<StatsScreen.ItemStatisticsList.ItemRow> {
      private static final int SLOT_BG_SIZE = 18;
      private static final int SLOT_STAT_HEIGHT = 22;
      private static final int SLOT_BG_Y = 1;
      private static final int SORT_NONE = 0;
      private static final int SORT_DOWN = -1;
      private static final int SORT_UP = 1;
      private final ResourceLocation[] iconSprites = new ResourceLocation[]{ResourceLocation.withDefaultNamespace("statistics/block_mined"), ResourceLocation.withDefaultNamespace("statistics/item_broken"), ResourceLocation.withDefaultNamespace("statistics/item_crafted"), ResourceLocation.withDefaultNamespace("statistics/item_used"), ResourceLocation.withDefaultNamespace("statistics/item_picked_up"), ResourceLocation.withDefaultNamespace("statistics/item_dropped")};
      protected final List<StatType<Block>> blockColumns = Lists.newArrayList();
      protected final List<StatType<Item>> itemColumns;
      protected final Comparator<StatsScreen.ItemStatisticsList.ItemRow> itemStatSorter = new StatsScreen.ItemStatisticsList.ItemRowComparator();
      @Nullable
      protected StatType<?> sortColumn;
      protected int headerPressed = -1;
      protected int sortOrder;

      public ItemStatisticsList(final Minecraft minecraft) {
         super(minecraft, StatsScreen.this.width, StatsScreen.this.height - 33 - 58, 33, 22);
         this.blockColumns.add(Stats.BLOCK_MINED);
         this.itemColumns = Lists.newArrayList(new StatType[]{Stats.ITEM_BROKEN, Stats.ITEM_CRAFTED, Stats.ITEM_USED, Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED});
         this.setRenderHeader(true, 22);
         Set<Item> set = Sets.newIdentityHashSet();
         Iterator var4 = BuiltInRegistries.ITEM.iterator();

         Item item;
         boolean bl;
         Iterator var7;
         StatType statType;
         while(var4.hasNext()) {
            item = (Item)var4.next();
            bl = false;
            var7 = this.itemColumns.iterator();

            while(var7.hasNext()) {
               statType = (StatType)var7.next();
               if (statType.contains(item) && StatsScreen.this.stats.getValue(statType.get(item)) > 0) {
                  bl = true;
               }
            }

            if (bl) {
               set.add(item);
            }
         }

         var4 = BuiltInRegistries.BLOCK.iterator();

         while(var4.hasNext()) {
            Block block = (Block)var4.next();
            bl = false;
            var7 = this.blockColumns.iterator();

            while(var7.hasNext()) {
               statType = (StatType)var7.next();
               if (statType.contains(block) && StatsScreen.this.stats.getValue(statType.get(block)) > 0) {
                  bl = true;
               }
            }

            if (bl) {
               set.add(block.asItem());
            }
         }

         set.remove(Items.AIR);
         var4 = set.iterator();

         while(var4.hasNext()) {
            item = (Item)var4.next();
            this.addEntry(new StatsScreen.ItemStatisticsList.ItemRow(item));
         }

      }

      int getColumnX(int i) {
         return 75 + 40 * i;
      }

      protected void renderHeader(GuiGraphics guiGraphics, int i, int j) {
         if (!this.minecraft.mouseHandler.isLeftPressed()) {
            this.headerPressed = -1;
         }

         int k;
         ResourceLocation resourceLocation;
         for(k = 0; k < this.iconSprites.length; ++k) {
            resourceLocation = this.headerPressed == k ? StatsScreen.SLOT_SPRITE : StatsScreen.HEADER_SPRITE;
            guiGraphics.blitSprite((ResourceLocation)resourceLocation, i + this.getColumnX(k) - 18, j + 1, 0, 18, 18);
         }

         if (this.sortColumn != null) {
            k = this.getColumnX(this.getColumnIndex(this.sortColumn)) - 36;
            resourceLocation = this.sortOrder == 1 ? StatsScreen.SORT_UP_SPRITE : StatsScreen.SORT_DOWN_SPRITE;
            guiGraphics.blitSprite((ResourceLocation)resourceLocation, i + k, j + 1, 0, 18, 18);
         }

         for(k = 0; k < this.iconSprites.length; ++k) {
            int l = this.headerPressed == k ? 1 : 0;
            guiGraphics.blitSprite((ResourceLocation)this.iconSprites[k], i + this.getColumnX(k) - 18 + l, j + 1 + l, 0, 18, 18);
         }

      }

      public int getRowWidth() {
         return 280;
      }

      protected boolean clickedHeader(int i, int j) {
         this.headerPressed = -1;

         for(int k = 0; k < this.iconSprites.length; ++k) {
            int l = i - this.getColumnX(k);
            if (l >= -36 && l <= 0) {
               this.headerPressed = k;
               break;
            }
         }

         if (this.headerPressed >= 0) {
            this.sortByColumn(this.getColumn(this.headerPressed));
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI((Holder)SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
         } else {
            return super.clickedHeader(i, j);
         }
      }

      private StatType<?> getColumn(int i) {
         return i < this.blockColumns.size() ? (StatType)this.blockColumns.get(i) : (StatType)this.itemColumns.get(i - this.blockColumns.size());
      }

      private int getColumnIndex(StatType<?> statType) {
         int i = this.blockColumns.indexOf(statType);
         if (i >= 0) {
            return i;
         } else {
            int j = this.itemColumns.indexOf(statType);
            return j >= 0 ? j + this.blockColumns.size() : -1;
         }
      }

      protected void renderDecorations(GuiGraphics guiGraphics, int i, int j) {
         if (j >= this.getY() && j <= this.getBottom()) {
            StatsScreen.ItemStatisticsList.ItemRow itemRow = (StatsScreen.ItemStatisticsList.ItemRow)this.getHovered();
            int k = this.getRowLeft();
            if (itemRow != null) {
               if (i < k || i > k + 18) {
                  return;
               }

               Item item = itemRow.getItem();
               guiGraphics.renderTooltip(StatsScreen.this.font, item.getDescription(), i, j);
            } else {
               Component component = null;
               int l = i - k;

               for(int m = 0; m < this.iconSprites.length; ++m) {
                  int n = this.getColumnX(m);
                  if (l >= n - 18 && l <= n) {
                     component = this.getColumn(m).getDisplayName();
                     break;
                  }
               }

               if (component != null) {
                  guiGraphics.renderTooltip(StatsScreen.this.font, component, i, j);
               }
            }

         }
      }

      protected void sortByColumn(StatType<?> statType) {
         if (statType != this.sortColumn) {
            this.sortColumn = statType;
            this.sortOrder = -1;
         } else if (this.sortOrder == -1) {
            this.sortOrder = 1;
         } else {
            this.sortColumn = null;
            this.sortOrder = 0;
         }

         this.children().sort(this.itemStatSorter);
      }

      @Environment(EnvType.CLIENT)
      class ItemRowComparator implements Comparator<StatsScreen.ItemStatisticsList.ItemRow> {
         public int compare(StatsScreen.ItemStatisticsList.ItemRow itemRow, StatsScreen.ItemStatisticsList.ItemRow itemRow2) {
            Item item = itemRow.getItem();
            Item item2 = itemRow2.getItem();
            int i;
            int j;
            if (ItemStatisticsList.this.sortColumn == null) {
               i = 0;
               j = 0;
            } else {
               StatType statType;
               if (ItemStatisticsList.this.blockColumns.contains(ItemStatisticsList.this.sortColumn)) {
                  statType = ItemStatisticsList.this.sortColumn;
                  i = item instanceof BlockItem ? StatsScreen.this.stats.getValue(statType, ((BlockItem)item).getBlock()) : -1;
                  j = item2 instanceof BlockItem ? StatsScreen.this.stats.getValue(statType, ((BlockItem)item2).getBlock()) : -1;
               } else {
                  statType = ItemStatisticsList.this.sortColumn;
                  i = StatsScreen.this.stats.getValue(statType, item);
                  j = StatsScreen.this.stats.getValue(statType, item2);
               }
            }

            return i == j ? ItemStatisticsList.this.sortOrder * Integer.compare(Item.getId(item), Item.getId(item2)) : ItemStatisticsList.this.sortOrder * Integer.compare(i, j);
         }

         // $FF: synthetic method
         public int compare(final Object object, final Object object2) {
            return this.compare((StatsScreen.ItemStatisticsList.ItemRow)object, (StatsScreen.ItemStatisticsList.ItemRow)object2);
         }
      }

      @Environment(EnvType.CLIENT)
      private class ItemRow extends ObjectSelectionList.Entry<StatsScreen.ItemStatisticsList.ItemRow> {
         private final Item item;

         ItemRow(final Item item) {
            this.item = item;
         }

         public Item getItem() {
            return this.item;
         }

         public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            guiGraphics.blitSprite((ResourceLocation)StatsScreen.SLOT_SPRITE, k, j, 0, 18, 18);
            guiGraphics.renderFakeItem(this.item.getDefaultInstance(), k + 1, j + 1);
            if (StatsScreen.this.itemStatsList != null) {
               int p;
               int var10003;
               int var10004;
               for(p = 0; p < StatsScreen.this.itemStatsList.blockColumns.size(); ++p) {
                  Item var14 = this.item;
                  Stat stat;
                  if (var14 instanceof BlockItem) {
                     BlockItem blockItem = (BlockItem)var14;
                     stat = ((StatType)StatsScreen.this.itemStatsList.blockColumns.get(p)).get(blockItem.getBlock());
                  } else {
                     stat = null;
                  }

                  var10003 = k + ItemStatisticsList.this.getColumnX(p);
                  var10004 = j + m / 2;
                  Objects.requireNonNull(StatsScreen.this.font);
                  this.renderStat(guiGraphics, stat, var10003, var10004 - 9 / 2, i % 2 == 0);
               }

               for(p = 0; p < StatsScreen.this.itemStatsList.itemColumns.size(); ++p) {
                  Stat var10002 = ((StatType)StatsScreen.this.itemStatsList.itemColumns.get(p)).get(this.item);
                  var10003 = k + ItemStatisticsList.this.getColumnX(p + StatsScreen.this.itemStatsList.blockColumns.size());
                  var10004 = j + m / 2;
                  Objects.requireNonNull(StatsScreen.this.font);
                  this.renderStat(guiGraphics, var10002, var10003, var10004 - 9 / 2, i % 2 == 0);
               }
            }

         }

         protected void renderStat(GuiGraphics guiGraphics, @Nullable Stat<?> stat, int i, int j, boolean bl) {
            Component component = stat == null ? StatsScreen.NO_VALUE_DISPLAY : Component.literal(stat.format(StatsScreen.this.stats.getValue(stat)));
            guiGraphics.drawString(StatsScreen.this.font, (Component)component, i - StatsScreen.this.font.width((FormattedText)component), j, bl ? -1 : -4539718);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", new Object[]{this.item.getDescription()});
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private class MobsStatisticsList extends ObjectSelectionList<StatsScreen.MobsStatisticsList.MobRow> {
      public MobsStatisticsList(final Minecraft minecraft) {
         int var10002 = StatsScreen.this.width;
         int var10003 = StatsScreen.this.height - 33 - 58;
         Objects.requireNonNull(StatsScreen.this.font);
         super(minecraft, var10002, var10003, 33, 9 * 4);
         Iterator var3 = BuiltInRegistries.ENTITY_TYPE.iterator();

         while(true) {
            EntityType entityType;
            do {
               if (!var3.hasNext()) {
                  return;
               }

               entityType = (EntityType)var3.next();
            } while(StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(entityType)) <= 0 && StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(entityType)) <= 0);

            this.addEntry(new StatsScreen.MobsStatisticsList.MobRow(entityType));
         }
      }

      public int getRowWidth() {
         return 280;
      }

      @Environment(EnvType.CLIENT)
      class MobRow extends ObjectSelectionList.Entry<StatsScreen.MobsStatisticsList.MobRow> {
         private final Component mobName;
         private final Component kills;
         private final Component killedBy;
         private final boolean hasKills;
         private final boolean wasKilledBy;

         public MobRow(final EntityType<?> entityType) {
            this.mobName = entityType.getDescription();
            int i = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(entityType));
            if (i == 0) {
               this.kills = Component.translatable("stat_type.minecraft.killed.none", new Object[]{this.mobName});
               this.hasKills = false;
            } else {
               this.kills = Component.translatable("stat_type.minecraft.killed", new Object[]{i, this.mobName});
               this.hasKills = true;
            }

            int j = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(entityType));
            if (j == 0) {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by.none", new Object[]{this.mobName});
               this.wasKilledBy = false;
            } else {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by", new Object[]{this.mobName, j});
               this.wasKilledBy = true;
            }

         }

         public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            guiGraphics.drawString(StatsScreen.this.font, (Component)this.mobName, k + 2, j + 1, -1);
            Font var10001 = StatsScreen.this.font;
            Component var10002 = this.kills;
            int var10003 = k + 2 + 10;
            int var10004 = j + 1;
            Objects.requireNonNull(StatsScreen.this.font);
            guiGraphics.drawString(var10001, var10002, var10003, var10004 + 9, this.hasKills ? -4539718 : -8355712);
            var10001 = StatsScreen.this.font;
            var10002 = this.killedBy;
            var10003 = k + 2 + 10;
            var10004 = j + 1;
            Objects.requireNonNull(StatsScreen.this.font);
            guiGraphics.drawString(var10001, var10002, var10003, var10004 + 9 * 2, this.wasKilledBy ? -4539718 : -8355712);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", new Object[]{CommonComponents.joinForNarration(new Component[]{this.kills, this.killedBy})});
         }
      }
   }
}

package net.minecraft.client.resources.metadata.gui;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

@Environment(EnvType.CLIENT)
public interface GuiSpriteScaling {
   Codec<GuiSpriteScaling> CODEC = GuiSpriteScaling.Type.CODEC.dispatch(GuiSpriteScaling::type, GuiSpriteScaling.Type::codec);
   GuiSpriteScaling DEFAULT = new GuiSpriteScaling.Stretch();

   GuiSpriteScaling.Type type();

   @Environment(EnvType.CLIENT)
   public static enum Type implements StringRepresentable {
      STRETCH("stretch", GuiSpriteScaling.Stretch.CODEC),
      TILE("tile", GuiSpriteScaling.Tile.CODEC),
      NINE_SLICE("nine_slice", GuiSpriteScaling.NineSlice.CODEC);

      public static final Codec<GuiSpriteScaling.Type> CODEC = StringRepresentable.fromEnum(GuiSpriteScaling.Type::values);
      private final String key;
      private final MapCodec<? extends GuiSpriteScaling> codec;

      private Type(final String string2, final MapCodec<? extends GuiSpriteScaling> mapCodec) {
         this.key = string2;
         this.codec = mapCodec;
      }

      public String getSerializedName() {
         return this.key;
      }

      public MapCodec<? extends GuiSpriteScaling> codec() {
         return this.codec;
      }

      // $FF: synthetic method
      private static GuiSpriteScaling.Type[] $values() {
         return new GuiSpriteScaling.Type[]{STRETCH, TILE, NINE_SLICE};
      }
   }

   @Environment(EnvType.CLIENT)
   public static record Stretch() implements GuiSpriteScaling {
      public static final MapCodec<GuiSpriteScaling.Stretch> CODEC = MapCodec.unit(GuiSpriteScaling.Stretch::new);

      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.STRETCH;
      }
   }

   @Environment(EnvType.CLIENT)
   public static record NineSlice(int width, int height, GuiSpriteScaling.NineSlice.Border border) implements GuiSpriteScaling {
      public static final MapCodec<GuiSpriteScaling.NineSlice> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
         return instance.group(ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GuiSpriteScaling.NineSlice::width), ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GuiSpriteScaling.NineSlice::height), GuiSpriteScaling.NineSlice.Border.CODEC.fieldOf("border").forGetter(GuiSpriteScaling.NineSlice::border)).apply(instance, GuiSpriteScaling.NineSlice::new);
      }).validate(GuiSpriteScaling.NineSlice::validate);

      public NineSlice(int i, int j, GuiSpriteScaling.NineSlice.Border border) {
         this.width = i;
         this.height = j;
         this.border = border;
      }

      private static DataResult<GuiSpriteScaling.NineSlice> validate(GuiSpriteScaling.NineSlice nineSlice) {
         GuiSpriteScaling.NineSlice.Border border = nineSlice.border();
         if (border.left() + border.right() >= nineSlice.width()) {
            return DataResult.error(() -> {
               int var10000 = border.left();
               return "Nine-sliced texture has no horizontal center slice: " + var10000 + " + " + border.right() + " >= " + nineSlice.width();
            });
         } else {
            return border.top() + border.bottom() >= nineSlice.height() ? DataResult.error(() -> {
               int var10000 = border.top();
               return "Nine-sliced texture has no vertical center slice: " + var10000 + " + " + border.bottom() + " >= " + nineSlice.height();
            }) : DataResult.success(nineSlice);
         }
      }

      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.NINE_SLICE;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }

      public GuiSpriteScaling.NineSlice.Border border() {
         return this.border;
      }

      @Environment(EnvType.CLIENT)
      public static record Border(int left, int top, int right, int bottom) {
         private static final Codec<GuiSpriteScaling.NineSlice.Border> VALUE_CODEC;
         private static final Codec<GuiSpriteScaling.NineSlice.Border> RECORD_CODEC;
         static final Codec<GuiSpriteScaling.NineSlice.Border> CODEC;

         public Border(int i, int j, int k, int l) {
            this.left = i;
            this.top = j;
            this.right = k;
            this.bottom = l;
         }

         private OptionalInt unpackValue() {
            return this.left() == this.top() && this.top() == this.right() && this.right() == this.bottom() ? OptionalInt.of(this.left()) : OptionalInt.empty();
         }

         public int left() {
            return this.left;
         }

         public int top() {
            return this.top;
         }

         public int right() {
            return this.right;
         }

         public int bottom() {
            return this.bottom;
         }

         static {
            VALUE_CODEC = ExtraCodecs.POSITIVE_INT.flatComapMap((integer) -> {
               return new GuiSpriteScaling.NineSlice.Border(integer, integer, integer, integer);
            }, (border) -> {
               OptionalInt optionalInt = border.unpackValue();
               return optionalInt.isPresent() ? DataResult.success(optionalInt.getAsInt()) : DataResult.error(() -> {
                  return "Border has different side sizes";
               });
            });
            RECORD_CODEC = RecordCodecBuilder.create((instance) -> {
               return instance.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("left").forGetter(GuiSpriteScaling.NineSlice.Border::left), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("top").forGetter(GuiSpriteScaling.NineSlice.Border::top), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("right").forGetter(GuiSpriteScaling.NineSlice.Border::right), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("bottom").forGetter(GuiSpriteScaling.NineSlice.Border::bottom)).apply(instance, GuiSpriteScaling.NineSlice.Border::new);
            });
            CODEC = Codec.either(VALUE_CODEC, RECORD_CODEC).xmap(Either::unwrap, (border) -> {
               return border.unpackValue().isPresent() ? Either.left(border) : Either.right(border);
            });
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static record Tile(int width, int height) implements GuiSpriteScaling {
      public static final MapCodec<GuiSpriteScaling.Tile> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
         return instance.group(ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GuiSpriteScaling.Tile::width), ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GuiSpriteScaling.Tile::height)).apply(instance, GuiSpriteScaling.Tile::new);
      });

      public Tile(int i, int j) {
         this.width = i;
         this.height = j;
      }

      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.TILE;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }
   }
}

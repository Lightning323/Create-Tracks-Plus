package com.Gr00ze.stickywheels.mixin;

import com.Gr00ze.stickywheels.StickyItems;
import com.Gr00ze.stickywheels.utils.Utils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.content.items.tire.TireItem;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.offroad.index.OffroadItems;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({OffroadItems.class})
public class OffroadItemsMixin {
   @Inject(
      method = {"<clinit>"},
      at = {@At("TAIL")}
   )
   private static void addTire(CallbackInfo ci) {
      SimulatedRegistrate REGISTRATE = Offroad.getRegistrate();
      StickyItems.SMALL_STICKY_TIRE = REGISTRATE.item("sticky_small_tire", TireItem::new).properties((x) -> x.component(OffroadDataComponents.TIRE, TireLike.SMALL_TIRE)).recipe((c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)c.get(), 1).requires(Utils.offroadItem("small_tire")).requires(Blocks.HONEY_BLOCK.asItem()).unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllBlocks.SHAFT.get())).save(p)).model(AssetLookup.itemModelWithPartials()).register();
      StickyItems.STICKY_TIRE = REGISTRATE.item("sticky_tire", TireItem::new).properties((x) -> x.component(OffroadDataComponents.TIRE, TireLike.TIRE)).recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 1).pattern(" H ").pattern("HTH").pattern(" H ").define('T', Utils.offroadItem("tire")).define('H', Items.HONEY_BOTTLE).unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllBlocks.SHAFT.get())).save(p)).model(AssetLookup.itemModelWithPartials()).register();
      StickyItems.LARGE_STICKY_TIRE = REGISTRATE.item("sticky_large_tire", TireItem::new).properties((x) -> x.component(OffroadDataComponents.TIRE, TireLike.LARGE_TIRE)).recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 1).pattern("HHH").pattern("HTH").pattern("HHH").define('T', Utils.offroadItem("large_tire")).define('H', Items.HONEY_BOTTLE).unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllBlocks.SHAFT.get())).save(p)).model(AssetLookup.itemModelWithPartials()).register();
      StickyItems.MONSTROUS_STICKY_TIRE = REGISTRATE.item("sticky_monstrous_tire", TireItem::new).properties((x) -> x.component(OffroadDataComponents.TIRE, TireLike.MONSTROUS_TIRE)).recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 1).pattern(" H ").pattern("HTH").pattern(" H ").define('T', Utils.offroadItem("monstrous_tire")).define('H', Blocks.HONEY_BLOCK.asItem()).unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllBlocks.SHAFT.get())).save(p)).model(AssetLookup.itemModelWithPartials()).register();
   }
}

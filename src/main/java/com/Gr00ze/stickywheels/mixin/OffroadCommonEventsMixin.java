package com.Gr00ze.stickywheels.mixin;

import dev.ryanhcode.offroad.events.OffroadCommonEvents;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({OffroadCommonEvents.class})
public class OffroadCommonEventsMixin {
   @Inject(
      method = {"modifyDefaultComponents"},
      at = {@At("TAIL")}
   )
   private static void addWheel(BiConsumer<ItemLike, Consumer<DataComponentPatch.Builder>> modify, CallbackInfo ci) {
      System.out.println("HOOK RUOTE ATTIVO");
   }
}

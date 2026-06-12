package com.Gr00ze.stickywheels.mixin;

import dev.ryanhcode.offroad.Offroad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Offroad.class})
public class OffroadMixin {
   @Inject(
      method = {"init"},
      at = {@At("HEAD")}
   )
   private static void onInitStart(CallbackInfo ci) {
      System.out.println("[StickyWheels] Offroad INIT MIXIN");
   }
}

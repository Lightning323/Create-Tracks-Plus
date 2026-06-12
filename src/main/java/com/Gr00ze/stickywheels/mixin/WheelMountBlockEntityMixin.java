package com.Gr00ze.stickywheels.mixin;

import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WheelMountBlockEntity.class})
public abstract class WheelMountBlockEntityMixin {
   @Shadow
   @Final
   private Vector3d queuedForce;
   @Unique
   private static final ResourceLocation STICKY_SMALL_TIRE_ID = ResourceLocation.fromNamespaceAndPath("offroad", "sticky_small_tire");
   @Unique
   private static final ResourceLocation STICKY_TIRE_ID = ResourceLocation.fromNamespaceAndPath("offroad", "sticky_tire");
   @Unique
   private static final ResourceLocation STICKY_LARGE_TIRE_ID = ResourceLocation.fromNamespaceAndPath("offroad", "sticky_large_tire");
   @Unique
   private static final ResourceLocation STICKY_MONSTROUS_TIRE_ID = ResourceLocation.fromNamespaceAndPath("offroad", "sticky_monstrous_tire");
   @Unique
   private static final double STICKY_GRAVITY_ACCELERATION = 9.81;
   @Unique
   private static final double STICKY_WALL_PRELOAD = (double)1.0F;
   @Unique
   private Vector3d stickywheels$cachedGroundNormalLocal;

   @Shadow
   public abstract ItemStack getHeldItem();

   @Unique
   private boolean stickywheels$isStickyWheel() {
      ItemStack stack = this.getHeldItem();
      return !stack.isEmpty() && (STICKY_SMALL_TIRE_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())) || STICKY_TIRE_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())) || STICKY_LARGE_TIRE_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())) || STICKY_MONSTROUS_TIRE_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())));
   }

   @Inject(
      method = {"computeMaxExtensionToTerrain"},
      at = {@At("HEAD")}
   )
   private void stickywheels$clearCachedNormal(Vector3dc normalD, Pose3dc pose, CallbackInfoReturnable<?> cir) {
      this.stickywheels$cachedGroundNormalLocal = null;
   }

   @ModifyVariable(
      method = {"computeMaxExtensionToTerrain"},
      at = @At(
   value = "INVOKE",
   target = "Ldev/ryanhcode/sable/companion/math/Pose3dc;transformNormalInverse(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;",
   shift = Shift.AFTER
)
   )
   private Vector3d stickywheels$cacheAcceptedSurfaceNormal(Vector3d hitNormal) {
      if (this.stickywheels$isStickyWheel() && hitNormal.dot((double)0.0F, (double)1.0F, (double)0.0F) >= (double)0.5F) {
         this.stickywheels$cachedGroundNormalLocal = (new Vector3d(hitNormal)).normalize();
      }

      return hitNormal;
   }

   @Inject(
      method = {"sable$physicsTick"},
      at = {@At(
   value = "INVOKE",
   target = "Ldev/ryanhcode/sable/api/physics/force/ForceTotal;applyImpulseAtPoint(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Lorg/joml/Vector3dc;Lorg/joml/Vector3dc;)V",
   shift = Shift.BEFORE
)}
   )
   private void stickywheels$addAdhesionImpulse(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep, CallbackInfo ci) {
      if (this.stickywheels$isStickyWheel() && this.stickywheels$cachedGroundNormalLocal != null) {
         Vector3d normalLocal = (new Vector3d(this.stickywheels$cachedGroundNormalLocal)).normalize();
         Vector3d gravityLocal = subLevel.logicalPose().transformNormalInverse(new Vector3d((double)0.0F, -9.81, (double)0.0F));
         double mass = subLevel.getMassTracker().getMass();
         double gravityPullingAway = Math.max((double)0.0F, gravityLocal.dot(normalLocal));
         Vector3d tangentGravity = (new Vector3d(gravityLocal)).sub((new Vector3d(normalLocal)).mul(gravityLocal.dot(normalLocal)));
         double wallPreload = tangentGravity.length() * (double)1.0F;
         double adhesionImpulse = mass * (gravityPullingAway + wallPreload) * timeStep;
         this.queuedForce.fma(-adhesionImpulse, normalLocal);
      }
   }
}

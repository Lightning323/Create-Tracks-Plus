package com.Gr00ze.stickywheels.test;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlock;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.data.OffroadLang;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.multiloader.inventory.SingleSlotContainer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class WheelMountBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor, Clearable, ClipboardCloneable, SpecialBlockEntityItemRequirement {
   private static final MutableComponent SCROLL_OPTION_TITLE = OffroadLang.translate("scroll_option.suspension_strength", new Object[0]).component();
   private static final double MAX_ALLOWED_EXTENSION = 0.65;
   private static final double NO_WHEEL_EXTENSION = (double)0.5F;
   private static final Collection<WheelMountBlockEntity> queuedWheelMounts = new ObjectOpenHashSet();
   private SuspensionStrengthValueBehaviour strength;
   private int clientSteeringSignal;
   protected int clientSteeringSignalLeft;
   protected int clientSteeringSignalRight;
   private double extension = (double)0.5F;
   private double lastExtension;
   private double chasingYaw;
   private double lastChasingYaw;
   private double lastAngle;
   private double angle;
   private double angularVelocity;
   private double touchingFriction;
   private int lastServerSteeringSignal;
   private int lastServerSteeringSignalLeft;
   private int lastServerSteeringSignalRight;
   private boolean liftedUp;
   private final Vector3d queuedForcePos;
   private final Vector3d queuedForce;
   private final ForceTotal forceTotal;

   public WheelMountBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.lastExtension = this.extension;
      this.angularVelocity = (double)0.0F;
      this.touchingFriction = (double)1.0F;
      this.liftedUp = false;
      this.queuedForcePos = new Vector3d();
      this.queuedForce = new Vector3d();
      this.forceTotal = new ForceTotal();
   }

   public static void applyAllBatchedForces(ServerLevel level, double timeStep) {
      for(WheelMountBlockEntity blockEntity : queuedWheelMounts) {
         if (!blockEntity.isRemoved()) {
            blockEntity.applyBatchedForces();
         }
      }

      queuedWheelMounts.clear();
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.strength = new SuspensionStrengthValueBehaviour(SCROLL_OPTION_TITLE, this, new SuspensionStrengthValueBox(0)));
      this.strength.value = 10;
   }

   public ItemRequirement getRequiredItems(BlockState state) {
      ItemStack stack = null;
      return stack.isEmpty() ? super.getRequiredItems(state) : new ItemRequirement(ItemUseType.CONSUME, stack);
   }

   public static double fudgeFriction(double realValue) {
      return realValue < (double)1.0F ? 0.1 + 0.9 * realValue : realValue;
   }

   public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      ItemStack item = this.getHeldItem();
      TireLike tire = (TireLike)item.get(OffroadDataComponents.TIRE);
      BlockPos blockPos = this.getBlockPos();
      if (tire != null) {
         float radius = tire.radius();
         double suspensionRestDistance = 0.65;
         MassData massData = subLevel.getMassTracker();
         Direction facing = (Direction)this.getBlockState().getValue(WheelMountBlock.HORIZONTAL_FACING);
         Vec3 localPos = blockPos.relative(facing).getCenter();
         this.queuedForcePos.set(localPos.x, localPos.y, localPos.z);
         double normalMass = (double)1.0F / massData.getInverseNormalMass(this.queuedForcePos, OrientedBoundingBox3d.UP);
         double effectiveStrength = (double)this.strength.getValue();
         double normalMassScaling = Math.min(normalMass / effectiveStrength, (double)1.0F) * (double)10.0F;
         double strengthMul = effectiveStrength * normalMassScaling * (double)2.0F;
         double springStrength = effectiveStrength * normalMassScaling * (double)40.0F;
         double dampingStrength = effectiveStrength * normalMassScaling;
         Pose3d pose = subLevel.logicalPose();
         Direction.Axis axis = facing.getAxis();
         Vec3i normal = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
         Vector3dc sideD = this.getRotatedWheelAxis(normal);
         normal = new Vec3i(normal.getZ(), 0, normal.getX());
         Vector3dc normalD = this.getRotatedWheelAxis(normal);
         TerrainCastResult extensionToTerrain = this.computeMaxExtensionToTerrain(normalD, pose);
         double maxExtension = extensionToTerrain.maxExtension();
         this.extension = Mth.lerp((double)1.0F, this.extension, maxExtension);
         if (maxExtension > 0.65 + (double)radius + (double)0.25F) {
            this.extension = 0.65;
         } else {
            double distance = 0.10833333333333334 + this.extension;
            double springLength = Mth.clamp(distance - (double)radius, (double)0.0F, 0.65);
            Vector3d velocity = Sable.HELPER.getVelocity(this.level, JOMLConversion.toJOML(localPos));
            Vector3d localVelocity = pose.transformNormalInverse(velocity);
            double dampingForce = -localVelocity.y * dampingStrength;
            double springForce = ((0.65 - springLength) * springStrength + dampingForce) * timeStep;
            Vec3i rayHitNormal = extensionToTerrain.normal().getNormal();
            Vec3 localForce = new Vec3(springForce * (double)rayHitNormal.getX(), springForce * (double)rayHitNormal.getY(), springForce * (double)rayHitNormal.getZ());
            if (extensionToTerrain.subLevel() != null) {
               localForce = extensionToTerrain.subLevel().logicalPose().transformNormal(localForce);
            }

            localForce = pose.transformNormalInverse(localForce);
            this.queuedForce.set(localForce.x, localForce.y, localForce.z);
            if (extensionToTerrain.minInteractingBlock() != null) {
               this.touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(this.level.getBlockState(extensionToTerrain.minInteractingBlock())));
            } else {
               this.touchingFriction = (double)1.0F;
            }

            double brakeStrength = (double)this.level.getSignal(blockPos.above(), Direction.DOWN) / (double)15.0F;
            double surfaceBraking = Math.min(this.touchingFriction, (double)1.0F);
            double brakingFrictionStrength = (0.075 + brakeStrength * 0.3) * surfaceBraking;
            float kineticSpeed = facing.getAxis() == Axis.X ? this.getSpeed() : -this.getSpeed();
            this.queuedForce.fma(localVelocity.dot(normalD) * -brakingFrictionStrength * strengthMul * timeStep + (double)kineticSpeed * ((double)1.0F - brakeStrength) * surfaceBraking * (double)1.75F * timeStep, normalD);
            this.queuedForce.fma(localVelocity.dot(sideD) * -0.6 * this.touchingFriction * strengthMul * timeStep, sideD);
            this.forceTotal.applyImpulseAtPoint(subLevel, this.queuedForcePos, this.queuedForce);
            queuedWheelMounts.add(this);
         }
      }

   }

   private void applyBatchedForces() {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      if (subLevel != null) {
         RigidBodyHandle handle = RigidBodyHandle.of((ServerSubLevel)subLevel);
         handle.applyForcesAndReset(this.forceTotal);
      }

   }

   public void tick() {
      super.tick();
      ItemStack item = this.getHeldItem();
      TireLike tire = (TireLike)item.get(OffroadDataComponents.TIRE);
      this.lastChasingYaw = this.chasingYaw;
      this.chasingYaw = Mth.lerp(0.4, this.chasingYaw, this.computeYaw());
      if (this.level.isClientSide) {
         if (tire == null) {
            this.angle = (double)0.0F;
            this.lastAngle = (double)0.0F;
            this.lastExtension = this.extension;
            this.extension = Mth.lerp(0.6, this.extension, (double)0.5F);
         } else {
            float radius = tire.radius();
            SubLevel subLevel = Sable.HELPER.getContaining(this);
            this.lastExtension = this.extension;
            this.extension = Mth.lerp(0.7, this.extension, this.computeMaxExtension(radius));
            Direction facing = (Direction)this.getBlockState().getValue(WheelMountBlock.HORIZONTAL_FACING);
            float speed = facing.getAxis() == Axis.X ? -this.getSpeed() : this.getSpeed();
            double rpt = (double)speed * Math.PI * (double)2.0F / (double)60.0F / (double)20.0F * (double)(15 - this.level.getSignal(this.getBlockPos().above(), Direction.DOWN)) / (double)15.0F;
            double attemptedAngularVelocity = Mth.lerp(0.2, this.angularVelocity, rpt);
            if (subLevel != null && !this.liftedUp) {
               Vector3d velocity = Sable.HELPER.getVelocity(this.level, JOMLConversion.atCenterOf(this.getBlockPos().relative(facing)));
               Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(velocity).div((double)20.0F);
               Direction.Axis axis = facing.getAxis();
               Vec3i normal = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
               normal = new Vec3i(normal.getZ(), 0, normal.getX());
               Vector3dc normalD = this.getRotatedWheelAxis(normal);
               double translation = localVelocity.dot(normalD);
               double circumference = Math.PI * (double)radius * (double)2.0F;
               double angularDelta = -translation / circumference * Math.PI * (double)2.0F;
               if (this.touchingFriction < (double)1.0F) {
                  angularDelta = Mth.lerp(this.touchingFriction, attemptedAngularVelocity, angularDelta);
               }

               this.lastAngle = this.angle;
               this.angle += angularDelta;
               this.angularVelocity = angularDelta;
            } else {
               this.angularVelocity = attemptedAngularVelocity;
               this.lastAngle = this.angle;
               this.angle += this.angularVelocity;
            }
         }
      }

   }

   private double computeMaxExtension(float radius) {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      if (subLevel == null) {
         return 0.65;
      } else {
         Direction facing = (Direction)this.getBlockState().getValue(WheelMountBlock.HORIZONTAL_FACING);
         Pose3dc pose = subLevel.logicalPose();
         Direction.Axis axis = facing.getAxis();
         Vec3i normal = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
         normal = new Vec3i(normal.getZ(), 0, normal.getX());
         Vector3dc rotatedAxis = this.getRotatedWheelAxis(normal);
         TerrainCastResult extensionToTerrain = this.computeMaxExtensionToTerrain(rotatedAxis, pose);
         double unclampedExtension = extensionToTerrain.maxExtension - (double)radius;
         this.liftedUp = unclampedExtension > 0.65;
         if (extensionToTerrain.minInteractingBlock() == null) {
            this.touchingFriction = (double)1.0F;
         } else {
            this.touchingFriction = fudgeFriction(PhysicsBlockPropertyHelper.getFriction(this.level.getBlockState(extensionToTerrain.minInteractingBlock())));
         }

         return Mth.clamp(unclampedExtension, -0.45, 0.65);
      }
   }

   public String getClipboardKey() {
      return "Wheel Mount";
   }

   public boolean writeToClipboard(HolderLookup.@NotNull Provider registries, CompoundTag tag, Direction side) {
      return false;
   }

   public boolean readFromClipboard(HolderLookup.@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
      return false;
   }

   private TerrainCastResult computeMaxExtensionToTerrain(Vector3dc normalD, Pose3dc pose) {
      Direction facing = (Direction)this.getBlockState().getValue(WheelMountBlock.HORIZONTAL_FACING);
      Vec3 wheelPosCenter = this.getBlockPos().relative(facing).getCenter();
      double minExtension = (double)5.0F;
      Direction minNormal = Direction.UP;
      SubLevel minHitSubLevel = null;
      BlockPos minInteractingBlock = null;

      for(int i = -1; i <= 1; ++i) {
         Vec3 localPosO = wheelPosCenter.add(JOMLConversion.toMojang(normalD).scale((double)i));
         ClipContext clipContext = new ClipContext(localPosO, localPosO.subtract((double)0.0F, (double)5.0F, (double)0.0F), Block.COLLIDER, Fluid.NONE, CollisionContext.empty());
         ((ClipContextExtension)clipContext).sable$setIgnoredSubLevel(Sable.HELPER.getContaining(this));
         BlockHitResult clipResult = this.level.clip(clipContext);
         if (clipResult.getType() != Type.MISS) {
            SubLevel hitSubLevel = Sable.HELPER.getContaining(this.level, clipResult.getLocation());
            Vec3 localHitPos = pose.transformPositionInverse(hitSubLevel == null ? clipResult.getLocation() : hitSubLevel.logicalPose().transformPosition(clipResult.getLocation()));
            if (!(localHitPos.y > wheelPosCenter.y) && !(localPosO.distanceTo(localHitPos) < 0.05)) {
               double dist = wheelPosCenter.y - localHitPos.y;
               if (!(dist <= 1.0E-5)) {
                  Direction dir = clipResult.getDirection();
                  Vector3d hitNormal = new Vector3d((double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ());
                  if (hitSubLevel != null) {
                     hitSubLevel.logicalPose().transformNormal(hitNormal);
                  }

                  pose.transformNormalInverse(hitNormal);
                  if (!(hitNormal.dot((double)0.0F, (double)1.0F, (double)0.0F) < (double)0.5F)) {
                     minExtension = Math.min(minExtension, dist);
                     minNormal = clipResult.getDirection();
                     minHitSubLevel = hitSubLevel;
                     minInteractingBlock = clipResult.getBlockPos();
                  }
               }
            }
         }
      }

      return new TerrainCastResult(minExtension, minNormal, minHitSubLevel, minInteractingBlock);
   }

   private @NotNull Vector3dc getRotatedWheelAxis(Vec3i normal) {
      Vector3d normalD = new Vector3d((double)normal.getX(), (double)normal.getY(), (double)normal.getZ());
      normalD.rotateY(this.getChasingYaw());
      return normalD;
   }

   protected double getChasingYaw() {
      return this.chasingYaw;
   }

   protected double getLerpedYaw(double partialTick) {
      return Mth.lerp(partialTick, this.lastChasingYaw, this.chasingYaw);
   }

   public float getLerpedAngle(float partialTicks) {
      return (float)Mth.lerp((double)partialTicks, this.lastAngle, this.angle);
   }

   public double getLerpedExtension(float partialTick) {
      return Mth.lerp((double)partialTick, this.lastExtension, this.extension);
   }

   protected double computeYaw() {
      int signal = this.getSteeringSignal();
      return signal == 0 ? (double)0.0F : (double)(-signal) / (double)15.0F * Math.PI / (double)4.0F * 0.6666666666666666;
   }

   protected int getSteeringSignal() {
      if (this.level.isClientSide) {
         return this.clientSteeringSignal;
      } else {
         BlockState state = this.getBlockState();
         Direction facing = (Direction)state.getValue(WheelMountBlock.HORIZONTAL_FACING);
         Direction d1 = facing.getClockWise();
         Direction d2 = facing.getCounterClockWise();
         BlockPos pos = this.getBlockPos();
         int signalLeft = this.level.getSignal(pos.relative(d1), d2);
         int signalRight = this.level.getSignal(pos.relative(d2), d1);
         int signal = signalLeft - signalRight;
         boolean sendData = signal != this.lastServerSteeringSignal || signalLeft != this.lastServerSteeringSignalLeft || signalRight != this.lastServerSteeringSignalRight;
         this.lastServerSteeringSignal = signal;
         this.lastServerSteeringSignalLeft = signalLeft;
         this.lastServerSteeringSignalRight = signalRight;
         if (sendData) {
            this.sendData();
         }

         return signal;
      }
   }

   public ItemStack getHeldItem() {
      return null;
   }

   protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
      tag.put("CurrentStack", this.getHeldItem().saveOptional(registries));
      if (clientPacket) {
         tag.putInt("SteeringSignalStrength", this.lastServerSteeringSignal);
         tag.putInt("SteeringSignalStrengthLeft", this.lastServerSteeringSignalLeft);
         tag.putInt("SteeringSignalStrengthRight", this.lastServerSteeringSignalRight);
      }

      super.write(tag, registries, clientPacket);
   }

   protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
      ItemStack.parseOptional(registries, tag.getCompound("CurrentStack"));
      if (clientPacket) {
         if (tag.contains("SteeringSignalStrength")) {
            this.clientSteeringSignal = tag.getInt("SteeringSignalStrength");
            this.clientSteeringSignalLeft = tag.getInt("SteeringSignalStrengthLeft");
            this.clientSteeringSignalRight = tag.getInt("SteeringSignalStrengthRight");
         }

         this.onStackChanged();
      }

      super.read(tag, registries, clientPacket);
   }

   public void clearContent() {
   }

   public SingleSlotContainer getInventory() {
      return null;
   }

   public void onStackChanged() {
      this.invalidateRenderBoundingBox();
   }

   protected AABB createRenderBoundingBox() {
      AABB aabb = new AABB(this.getBlockPos());
      if (this.getHeldItem() != null && this.getHeldItem().has(OffroadDataComponents.TIRE)) {
         TireLike tire = (TireLike)this.getHeldItem().getComponents().get(OffroadDataComponents.TIRE);
         aabb = aabb.inflate((double)(tire.radius() + 1.0F));
      }

      return aabb;
   }

   private static record TerrainCastResult(double maxExtension, @NotNull Direction normal, @Nullable SubLevel subLevel, @Nullable BlockPos minInteractingBlock) {
   }

   private static class SuspensionStrengthValueBehaviour extends ScrollValueBehaviour {
      private static final int MAX_SUSPENSION_STRENGTH = 180;

      public SuspensionStrengthValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
         super(label, be, slot);
         this.between(5, 180);
      }

      public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
         return new ValueSettingsBoard(this.label, 180, 20, ImmutableList.of(OffroadLang.translate("scroll_option.suspension_strength_label", new Object[0]).component()), new ValueSettingsFormatter(ValueSettingsBehaviour.ValueSettings::format));
      }
   }

   private static final class SuspensionStrengthValueBox extends ValueBoxTransform {
      private final int hOffset;

      public SuspensionStrengthValueBox(int hOffset) {
         this.hOffset = hOffset;
      }

      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         Direction facing = (Direction)state.getValue(RollerBlock.FACING);
         float yRot = AngleHelper.horizontalAngle(facing) + 180.0F;
      }

      public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
         Vec3 offset = this.getLocalOffset(level, pos, state);
         if (offset == null) {
            return false;
         } else {
            return localHit.distanceTo(offset) < (double)(this.scale / 3.0F);
         }
      }

      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         Direction facing = (Direction)state.getValue(RollerBlock.FACING);
         float stateAngle = AngleHelper.horizontalAngle(facing) + 180.0F;
         return VecHelper.rotateCentered(VecHelper.voxelSpace((double)(8 + this.hOffset), (double)15.5F, (double)11.0F), (double)stateAngle, Axis.Y);
      }
   }
}

/*
 * Decompiled with CFR 0.152.
 */
package org.lightning323.createkinetic;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lightning323.createkinetic.client.TrackRenderTuning;
import org.lightning323.createkinetic.client.TrackTuningScreen;
import org.lightning323.createkinetic.content.blocks.sable_track.SableTrackRenderer;
import org.lightning323.createkinetic.content.blocks.wheel_mount.AdjustableWheelMountRenderer;
import org.lightning323.createkinetic.content.items.SuspensionKeyItem;
import org.lightning323.createkinetic.content.joystick.JoystickControlClient;
import org.lightning323.createkinetic.registry.*;
import org.lightning323.createkinetic.network.RequestOpenTuningPayload;
import org.lightning323.createkinetic.registry.KineticBlockEntityTypes;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.lightning323.createkinetic.CreateKinetic.MOD_ID;

@Mod(value = CreateKinetic.MOD_ID, dist = {Dist.CLIENT})
public class TracksClient {
    private static final KeyMapping OPEN_TUNING = new KeyMapping("key." + CreateKinetic.MOD_ID + ".open_tuning", InputConstants.Type.KEYSYM, 74, "key.categories." + CreateKinetic.MOD_ID);

    public static boolean holdingSuspensionKey = false;
    public static boolean holdingSuspensionKeyInPositionMode = false;
    public static boolean holdingSuspensionKeyInAllPositionMode = false;
    public static boolean holdingSuspensionKeyInResetMode = false;


    private static void registerClientHandlers(IEventBus modEventBus) {
        modEventBus.register(KineticClient.class);
        modEventBus.register(KineticKeys.class);
        NeoForge.EVENT_BUS.register(JoystickControlClient.class);
    }


    public static void init(IEventBus modBus) {
        registerClientHandlers(modBus);
        KineticPartialModels.init();
        modBus.addListener(TracksClient::clientSetup);
        modBus.addListener(TracksClient::registerKeys);
        modBus.addListener(TracksClient::buildContents);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, TracksClient::clientTick);
        TracksPartialModels.init();
        TracksSpriteShifts.init();
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            TrackRenderTuning.load(Minecraft.getInstance().gameDirectory.toPath().resolve("config/" + CreateKinetic.MOD_ID + "-render-tuning.txt"));
            BlockEntityRenderers.register((BlockEntityType) ((BlockEntityType) OffroadBlockEntityTypes.WHEEL_MOUNT.get()), AdjustableWheelMountRenderer::new);
            BlockEntityRenderers.register((BlockEntityType) ((BlockEntityType) KineticBlockEntityTypes.SABLE_TRACK.get()), SableTrackRenderer::new);
        });
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TUNING);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean bl = TracksClient.holdingSuspensionKey = minecraft.player != null && (minecraft.player.getMainHandItem().is(KineticItems.SUSPENSION_KEY.asItem()) || minecraft.player.getOffhandItem().is(KineticItems.SUSPENSION_KEY.asItem()));
        if (minecraft.player != null && TracksClient.holdingSuspensionKey) {
            ItemStack key = minecraft.player.getMainHandItem().is(KineticItems.SUSPENSION_KEY.asItem()) ? minecraft.player.getMainHandItem() : minecraft.player.getOffhandItem();
            SuspensionKeyItem.TuningMode mode = SuspensionKeyItem.getMode(key);
            TracksClient.holdingSuspensionKeyInPositionMode = mode == SuspensionKeyItem.TuningMode.POSITION;
            TracksClient.holdingSuspensionKeyInAllPositionMode = mode == SuspensionKeyItem.TuningMode.ALL_POSITION;
            TracksClient.holdingSuspensionKeyInResetMode = mode == SuspensionKeyItem.TuningMode.RESET;
        } else {
            TracksClient.holdingSuspensionKeyInPositionMode = false;
            TracksClient.holdingSuspensionKeyInAllPositionMode = false;
            TracksClient.holdingSuspensionKeyInResetMode = false;
        }
        while (OPEN_TUNING.consumeClick()) {
            if (minecraft.player == null) continue;
            PacketDistributor.sendToServer((CustomPacketPayload) new RequestOpenTuningPayload(), (CustomPacketPayload[]) new CustomPacketPayload[0]);
        }
    }

    public static void openTuningScreen() {
        Minecraft.getInstance().setScreen((Screen) new TrackTuningScreen());
    }


    /**
     * Register items in the existing tabs
     */
    private static AtomicBoolean built = new AtomicBoolean(false);

    public static final ResourceLocation SIMULATED_CREATIVE_SECTION = ResourceLocation.fromNamespaceAndPath("simulated", "simulated");
    public static final ResourceLocation AERONAUTICS_CREATIVE_SECTION = ResourceLocation.fromNamespaceAndPath("aeronautics", "aeronautics");
    public static final ResourceLocation OFFROAD_CREATIVE_SECTION = ResourceLocation.fromNamespaceAndPath("offroad", "offroad");

    private static void registerSectionItem(ResourceLocation sectionId, String itemPath, Supplier<Item> itemSupplier) {
        SimulatedRegistrate.TAB_ITEMS.add(itemSupplier);
        SimulatedRegistrate.ITEM_TO_SECTION.put(ResourceLocation.fromNamespaceAndPath(MOD_ID, itemPath), sectionId);
    }

    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (!built.get()) {
            registerSectionItem(OFFROAD_CREATIVE_SECTION, "track_mount", KineticBlocks.TRACK_MOUNT::asItem);
            registerSectionItem(OFFROAD_CREATIVE_SECTION, "small_suspension_track", KineticItems.SMALL_SUSPENSION_TRACK::get);
            registerSectionItem(OFFROAD_CREATIVE_SECTION, "small_track_drive_wheel", KineticItems.SMALL_TRACK_DRIVE_WHEEL::get);
            registerSectionItem(OFFROAD_CREATIVE_SECTION, "suspension_key", KineticItems.SUSPENSION_KEY::get);
            built.set(true);
        }
    }

}


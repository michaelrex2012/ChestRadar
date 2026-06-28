package quark19.chestradar;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestRadar implements ModInitializer {
	public static final String MOD_ID = "chestradar";
	private static final Map<UUID, Integer> COOLDOWN_MAP = new HashMap<>();

	@Override
	public void onInitialize() {
		ModConfig.load();

		PayloadTypeRegistry.playC2S().register(SearchRequestPayload.TYPE, SearchRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SearchResponsePayload.TYPE, SearchResponsePayload.CODEC);

		// FIX 1: The timer MUST rely on the actual server clock (20 ticks a second), not packet arrivals!
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			COOLDOWN_MAP.entrySet().removeIf(entry -> {
				int remaining = entry.getValue() - 1;
				if (remaining <= 0) return true; // Clear out expired entries
				entry.setValue(remaining);
				return false;
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(SearchRequestPayload.TYPE, (payload, context) -> {
			ItemStack targetStack = payload.stack();
			if (targetStack.isEmpty()) return;

			// FIX 2: Move EVERYTHING into the main thread execution block.
			// Reading player position on the network thread causes massive desyncs!
			context.server().execute(() -> {
				ServerPlayer player = context.player();

				// Run the firewall check securely on the main thread
				if (!ChestRadar.tryScan(player)) {
					return;
				}

				ServerLevel world = player.level();
				Map<BlockPos, Integer> chestData = new HashMap<>();
				int SCAN_RADIUS = ModConfig.INSTANCE.scanRadius;

				BlockPos playerPos = player.blockPosition();
				BlockPos minPos = playerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS);
				BlockPos maxPos = playerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS);

				// 1. Convert block coordinates to chunk coordinates
				int minChunkX = minPos.getX() >> 4;
				int maxChunkX = maxPos.getX() >> 4;
				int minChunkZ = minPos.getZ() >> 4;
				int maxChunkZ = maxPos.getZ() >> 4;

				// 2. Iterate only over the chunks within our radius
				for (int cX = minChunkX; cX <= maxChunkX; cX++) {
					for (int cZ = minChunkZ; cZ <= maxChunkZ; cZ++) {

						// Safety: Only check chunks that are currently loaded to prevent lag spikes
						if (world.hasChunk(cX, cZ)) {
							LevelChunk chunk = world.getChunk(cX, cZ);

							// 3. Directly access the chunk's pre-made list of BlockEntities
							for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
								BlockPos pos = entry.getKey();

								// 4. Verify the entity is actually inside our exact 3D radius box
								if (pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX() &&
										pos.getY() >= minPos.getY() && pos.getY() <= maxPos.getY() &&
										pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ()) {

									BlockEntity blockEntity = entry.getValue();
									if (blockEntity instanceof Container container) {
										int totalCount = 0;

										for (int i = 0; i < container.getContainerSize(); i++) {
											ItemStack slotStack = container.getItem(i);
											if (!slotStack.isEmpty() && ItemStack.isSameItem(targetStack, slotStack)) {
												totalCount += slotStack.getCount();
											}
										}

										if (totalCount > 0) {
											chestData.put(pos.immutable(), totalCount);
										}
									}
								}
							}
						}
					}
				}

				context.responseSender().sendPacket(new SearchResponsePayload(chestData));
			});
		});
	}

	public static boolean tryScan(ServerPlayer player) {
		UUID uuid = player.getUUID();
		if (COOLDOWN_MAP.containsKey(uuid)) {
			return false;
		}
		COOLDOWN_MAP.put(uuid, 10);
		return true;
	}
}
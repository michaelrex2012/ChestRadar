package quark19.chestradar;

import net.fabricmc.api.ModInitializer;
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

public class ChestRadar implements ModInitializer {
	public static final String MOD_ID = "chestradar";

	@Override
	public void onInitialize() {
		ModConfig.load();

		PayloadTypeRegistry.playC2S().register(SearchRequestPayload.TYPE, SearchRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SearchResponsePayload.TYPE, SearchResponsePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SearchRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ServerLevel world = player.level();
			ItemStack targetStack = payload.stack();

			if (targetStack.isEmpty()) return;

			Map<BlockPos, Integer> chestData = new HashMap<>();
			int SCAN_RADIUS = ModConfig.INSTANCE.scanRadius;

			BlockPos playerPos = player.blockPosition();
			BlockPos minPos = playerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS);
			BlockPos maxPos = playerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS);

			context.server().execute(() -> {
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

				if (!chestData.isEmpty()) {
					context.responseSender().sendPacket(new SearchResponsePayload(chestData));
				}
			});
		});
	}
}
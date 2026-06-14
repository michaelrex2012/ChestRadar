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

import java.util.HashMap;
import java.util.Map;

public class ChestRadar implements ModInitializer {
	public static final String MOD_ID = "chestradar";

	@Override
	public void onInitialize() {
		ModConfig.load();

		// 1. Register Payloads (From Phase 1)
		PayloadTypeRegistry.playC2S().register(SearchRequestPayload.TYPE, SearchRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SearchResponsePayload.TYPE, SearchResponsePayload.CODEC);

		// 2. Register Server Network Receiver
		ServerPlayNetworking.registerGlobalReceiver(SearchRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ServerLevel world = player.level();
			ItemStack targetStack = payload.stack();

			if (targetStack.isEmpty()) return;

			// Prepare our results map
			Map<BlockPos, Integer> chestData = new HashMap<>();

			// Calculate the bounding box around the player
			int SCAN_RADIUS = ModConfig.INSTANCE.scanRadius;

			BlockPos playerPos = player.blockPosition();
			BlockPos minPos = playerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS);
			BlockPos maxPos = playerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS);

			// Execute the scan on the server thread safely using context.server()
			context.server().execute(() -> {
				// Iterate through every block coordinate within the radius
				for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
					BlockEntity blockEntity = world.getBlockEntity(pos);

					// Check if the block entity implements Container (Chests, Barrels, Shulkers, etc.)
					if (blockEntity instanceof Container container) {
						int totalCount = 0;

						// Loop through all slots in the inventory
						for (int i = 0; i < container.getContainerSize(); i++) {
							ItemStack slotStack = container.getItem(i);

							// Check if the item matches what the player is holding
							if (!slotStack.isEmpty() && ItemStack.isSameItem(targetStack, slotStack)) {
								totalCount += slotStack.getCount();
							}
						}

						// If we found any matching items, save the block position and the count
						if (totalCount > 0) {
							// We use an immutable copy of the position because BlockPos.betweenClosed reuses mutable objects
							chestData.put(pos.immutable(), totalCount);
						}
					}
				}

				// Send the compiled map back to the player who requested it
				if (!chestData.isEmpty()) {
					context.responseSender().sendPacket(new SearchResponsePayload(chestData));
				}
			});
		});
	}
}
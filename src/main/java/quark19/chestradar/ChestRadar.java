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
				for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
					BlockEntity blockEntity = world.getBlockEntity(pos);

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

						if (totalCount > 0) {
							chestData.put(pos.immutable(), totalCount);
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
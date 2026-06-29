package quark19.chestradar.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import quark19.chestradar.ModConfig;
import quark19.chestradar.SearchRequestPayload;
import quark19.chestradar.SearchResponsePayload;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class ChestRadarClient implements ClientModInitializer {
	public static KeyMapping highlightKeyMapping;

	public static final Map<BlockPos, ChestHistory> CHEST_CACHE = new HashMap<>();

	private static float ratio = 0.4f;
	private static int extra = 0;

	private int scanCooldown = 0;
	private boolean wasPressedLastTick = false;
	private boolean isToggleActive = false;
	private long currentClientTick = 0;
	private static int seconds = Math.toIntExact(Math.round((ModConfig.INSTANCE.scanCooldown + extra) * ratio));

	private static final ByteBufferBuilder TEXT_BYTE_BUFFER = new ByteBufferBuilder(1024);

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		ClientPlayNetworking.registerGlobalReceiver(SearchResponsePayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				CHEST_CACHE.keySet().retainAll(payload.chestData().keySet());

				payload.chestData().forEach((pos, count) -> {
					ChestHistory history = CHEST_CACHE.computeIfAbsent(pos, k -> new ChestHistory(seconds));
					history.addSnapshot(count, currentClientTick);
				});
			});
		});

		KeyMapping.Category ChestRadarKeyMappingCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath("chestradar", "storage"));

		highlightKeyMapping = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.chestradar.highlight",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_Y,
				ChestRadarKeyMappingCategory
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!client.isPaused()) {
				currentClientTick++;
			}

			if (ModConfig.INSTANCE.scanCooldown <= 10 && ModConfig.INSTANCE.scanCooldown > 5) {
				ratio = 0.4f;
				extra = 0;
			} else if (ModConfig.INSTANCE.scanCooldown <= 5 && ModConfig.INSTANCE.scanCooldown >= 3) {
				ratio = 0.6f;
				extra = 0;
			} else if (ModConfig.INSTANCE.scanCooldown < 3 && ModConfig.INSTANCE.scanCooldown > 0) {
				ratio = 1.5f;
				extra = 0;
			} else if (ModConfig.INSTANCE.scanCooldown == 0) {
				ratio = 1f;
				extra = 3;
			} else {
				ratio = 0.2f;
				extra = 0;
			}

			if (client.player == null) return;

			if (!ModConfig.INSTANCE.enableMod) {CHEST_CACHE.clear(); return;}
			boolean useToggle = ModConfig.INSTANCE.toggleMode;
			seconds = Math.round((ModConfig.INSTANCE.scanCooldown + extra) * ratio);

			if (useToggle) {
				wasPressedLastTick = false;

				if (highlightKeyMapping.consumeClick()) {
					isToggleActive = !isToggleActive;

					if (!isToggleActive) {
						CHEST_CACHE.clear();
						scanCooldown = 0;
					}
				}

				if (isToggleActive) {
					if (scanCooldown <= 0) {
						ItemStack heldItem = client.player.getMainHandItem();
						if (!heldItem.isEmpty()) {
							ClientPlayNetworking.send(new SearchRequestPayload(heldItem));
						} else {
							CHEST_CACHE.clear();
						}
						scanCooldown = 0;
					} else {
						scanCooldown--;
					}
				}

			} else {
				isToggleActive = false;

				if (highlightKeyMapping.isDown()) {
					wasPressedLastTick = true;

					if (scanCooldown <= 0) {
						ItemStack heldItem = client.player.getMainHandItem();
						if (!heldItem.isEmpty()) {
							ClientPlayNetworking.send(new SearchRequestPayload(heldItem));
						} else {
							CHEST_CACHE.clear();
						}
						scanCooldown = 10;
					} else {
						scanCooldown--;
					}
				} else {
					if (wasPressedLastTick) {
						CHEST_CACHE.clear();
						scanCooldown = 0;
						wasPressedLastTick = false;
					}
				}
			}
		});



		WorldRenderEvents.END_MAIN.register(context -> {
			if (CHEST_CACHE.isEmpty()) return;

			Minecraft client = Minecraft.getInstance();
			if (client.player == null) return;

			Vec3 cameraPos = context.gameRenderer().getMainCamera().position();
			PoseStack poseStack = context.matrices();

			var coreBufferSource = client.renderBuffers().bufferSource();

			MultiBufferSource.BufferSource textBufferSource = MultiBufferSource.immediate(TEXT_BYTE_BUFFER);

			CHEST_CACHE.forEach((pos, chestHistory) -> {
				int currentCount = chestHistory.getCurrentCount();
				float deltaPerSecond = chestHistory.getDeltaPerSecond();


				float r, g, b;

				float ri = ModConfig.INSTANCE.redAmount;
				float yi = ModConfig.INSTANCE.yellowAmount;
				float gi = ModConfig.INSTANCE.greenAmount;


				if (currentCount <= ri) {
					r = 1.0f; g = 0.0f; b = 0.0f;
				} else if (currentCount <= 32) {
					float t = (currentCount - ri) / yi - 1f;

					r = 1.0f;
					g = t;
					b = 0.0f;
				} else if (currentCount <= gi) {
					float t = (currentCount - yi) / yi;

					r = 1.0f - t;
					g = 1.0f;
					b = 0.0f;
				} else {
					r = 0.0f; g = 1.0f; b = 0.0f;
				}

				int ir = (int)(r * 255);
				int ig = (int)(g * 255);
				int ib = (int)(b * 255);
				int hexColor = 0xFF000000 | (ir << 16) | (ig << 8) | ib;

				poseStack.pushPose();
				poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

				VertexConsumer lineBuffer = coreBufferSource.getBuffer(RenderTypes.lines());
				PoseStack.Pose pose = poseStack.last();

				if (ModConfig.INSTANCE.renderOutlines) {
					if (!ModConfig.INSTANCE.slimOutlines) {
						drawSafeBox(pose, lineBuffer, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, r, g, b, 1.0f);
					} else {
						drawSafeBox(pose, lineBuffer, 0.05f, 0.0f, 0.05f, 0.95f, 0.9f, 0.95f, r, g, b, 1.0f);
					}
				}

				poseStack.popPose();

				poseStack.pushPose();

				if (ModConfig.INSTANCE.textLocation) {
					poseStack.translate(pos.getX() - cameraPos.x + 0.5, pos.getY() - cameraPos.y + 0.5625, pos.getZ() - cameraPos.z + 0.5);
				} else {
					poseStack.translate(pos.getX() - cameraPos.x + 0.5, pos.getY() - cameraPos.y + 1.5, pos.getZ() - cameraPos.z + 0.5);
				}

				poseStack.mulPose(context.gameRenderer().getMainCamera().rotation());
				poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
				poseStack.scale(-0.025f, -0.025f, 0.025f);

				Matrix4f textMatrix = poseStack.last().pose();
				Font font = client.font;
				String text = String.valueOf(currentCount);
				float textWidth = font.width(text);

				font.drawInBatch(
						text,
						-textWidth / 2f,
						0,
						hexColor,
						false,
						textMatrix,
						textBufferSource,
						Font.DisplayMode.SEE_THROUGH,
						0x40000000,
						0xF000F0
				);

				if (deltaPerSecond != 0.0f && ModConfig.INSTANCE.doItemDelta) {
					String deltaText = (deltaPerSecond > 0 ? "+" : "") + String.format("%.1f/s", deltaPerSecond);
					int deltaColor = deltaPerSecond > 0 ? 0xFF00FF00 : 0xFFFF0000;

					font.drawInBatch(
							deltaText,
							-font.width(deltaText) / 2f,
							font.lineHeight,
							deltaColor,
							false,
							textMatrix,
							textBufferSource,
							Font.DisplayMode.SEE_THROUGH,
							0x40000000,
							0xF000F0
					);
				}

				poseStack.popPose();
			});

			coreBufferSource.endBatch();
			textBufferSource.endBatch();
		});
	}

	private static void drawSafeBox(PoseStack.Pose pose, VertexConsumer buffer,
	                                float minX, float minY, float minZ,
	                                float maxX, float maxY, float maxZ,
	                                float r, float g, float b, float a) {
		Matrix4f matrix = pose.pose();
		float thickness = ModConfig.INSTANCE.outlineThickness;

		vertex(buffer, matrix, minX, minY, minZ, r, g, b, a, thickness);
		vertex(buffer, matrix, maxX, minY, minZ, r, g, b, a, thickness);

		vertex(buffer, matrix, maxX, minY, minZ, r, g, b, a, thickness);
		vertex(buffer, matrix, maxX, minY, maxZ, r, g, b, a, thickness);

		vertex(buffer, matrix, maxX, minY, maxZ, r, g, b, a, thickness);
		vertex(buffer, matrix, minX, minY, maxZ, r, g, b, a, thickness);

		vertex(buffer, matrix, minX, minY, maxZ, r, g, b, a, thickness);
		vertex(buffer, matrix, minX, minY, minZ, r, g, b, a, thickness);

		vertex(buffer, matrix, minX, maxY, minZ, r, g, b, a, thickness);
		vertex(buffer, matrix, maxX, maxY, minZ, r, g, b, a, thickness);

		vertex(buffer, matrix, maxX, maxY, minZ, r, g, b, a, thickness);
		vertex(buffer, matrix, maxX, maxY, maxZ, r, g, b, a, thickness);

		vertex(buffer, matrix, maxX, maxY, maxZ, r, g, b, a, thickness);
		vertex(buffer, matrix, minX, maxY, maxZ, r, g, b, a, thickness);

		vertex(buffer, matrix, minX, maxY, maxZ, r, g, b, a, thickness);
		vertex(buffer, matrix, minX, maxY, minZ, r, g, b, a, thickness);

		vertex(buffer, matrix, minX, minY, minZ, r, g, b, a, thickness);
		vertex(buffer, matrix, minX, maxY, minZ, r, g, b, a, thickness);

		vertex(buffer, matrix, maxX, minY, minZ, r, g, b, a, thickness);
		vertex(buffer, matrix, maxX, maxY, minZ, r, g, b, a, thickness);

		vertex(buffer, matrix, maxX, minY, maxZ, r, g, b, a, thickness);
		vertex(buffer, matrix, maxX, maxY, maxZ, r, g, b, a, thickness);

		vertex(buffer, matrix, minX, minY, maxZ, r, g, b, a, thickness);
		vertex(buffer, matrix, minX, maxY, maxZ, r, g, b, a, thickness);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix,
	                           float x, float y, float z,
	                           float r, float g, float b, float a, float thickness) {
		buffer.addVertex(matrix, x, y, z)
				.setColor(r, g, b, a)
				.setNormal(1.0f, 0.0f, 0.0f)
				.setLineWidth(thickness);
	}

	public static class ChestHistory {
		private final ArrayDeque<Snapshot> window = new ArrayDeque<>();

		private float smoothedRate = 0.0f;

		public ChestHistory(int initialSeconds) {}

		public void addSnapshot(int count, long currentTick) {
			window.addLast(new Snapshot(count, currentTick));

			long currentMaxTicks = ChestRadarClient.seconds * 20L;

			while (!window.isEmpty() && (currentTick - window.peekFirst().tick() > currentMaxTicks)) {
				window.removeFirst();
			}

			updateSmoothedRate();
		}

		private void updateSmoothedRate() {
			if (window.size() < 2) return;

			Snapshot oldest = window.peekFirst();
			Snapshot newest = window.peekLast();

			long tickDelta = newest.tick() - oldest.tick();
			if (tickDelta <= 0) return;

			int countDelta = newest.count() - oldest.count();
			float rawRate = ((float) countDelta / tickDelta) * 20.0f;

			if (smoothedRate == 0.0f) {
				smoothedRate = rawRate;
			} else {
				float alpha = Math.max(0.02f, 1.0f / window.size());

				smoothedRate = smoothedRate + alpha * (rawRate - smoothedRate);
			}

			if (Math.abs(smoothedRate) < 0.01f) {
				smoothedRate = 0.0f;
			}
		}

		public boolean isWindowFull() {
			if (window.size() < 2) return false;

			long oldestTick = window.peekFirst().tick();
			long newestTick = window.peekLast().tick();
			long actualSpan = newestTick - oldestTick;

			long currentMaxTicks = ChestRadarClient.seconds * 20L;

			return actualSpan >= (currentMaxTicks - 10);
		}

		public void clear() {
			window.clear();
			smoothedRate = 0.0f;
		}

		public int getCurrentCount() {
			if (window.isEmpty()) return 0;
			return window.peekLast().count();
		}

		public float getDeltaPerSecond() {
			return smoothedRate;
		}

		private record Snapshot(int count, long tick) {}
	}
}
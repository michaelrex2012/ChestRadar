package quark19.chestradar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SearchResponsePayload(Map<BlockPos, Integer> chestData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SearchResponsePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("chestradar", "search_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SearchResponsePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.chestData().size());
                payload.chestData().forEach((pos, count) -> {
                    buf.writeBlockPos(pos);
                    buf.writeInt(count);
                });
            },
            buf -> {
                int size = buf.readInt();
                Map<BlockPos, Integer> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    map.put(buf.readBlockPos(), buf.readInt());
                }
                return new SearchResponsePayload(map);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
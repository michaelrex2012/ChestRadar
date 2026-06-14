package quark19.chestradar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record SearchRequestPayload(ItemStack stack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SearchRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("chestradar", "search_request"));

    //.of used instead .ofStatic
    public static final StreamCodec<RegistryFriendlyByteBuf, SearchRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> ItemStack.STREAM_CODEC.encode(buf, payload.stack()),
            buf -> new SearchRequestPayload(ItemStack.STREAM_CODEC.decode(buf))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
package com.easygame.network;

import com.easygame.EasyGame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record CycleTradesPayload(List<Integer> lockedIndices, boolean loadTrims) implements CustomPacketPayload {
    public static final Type<CycleTradesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(EasyGame.MOD_ID, "cycle_trades"));

    public static final StreamCodec<FriendlyByteBuf, CycleTradesPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.lockedIndices().size());
            for (int idx : payload.lockedIndices()) {
                buf.writeVarInt(idx);
            }
            buf.writeBoolean(payload.loadTrims());
        },
        buf -> {
            int size = buf.readVarInt();
            List<Integer> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(buf.readVarInt());
            }
            boolean trims = buf.readBoolean();
            return new CycleTradesPayload(list, trims);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

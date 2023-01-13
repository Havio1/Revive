package net.revive.packet;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.particle.ParticleTypes;
import net.revive.ReviveMain;
import net.revive.accessor.PlayerEntityAccessor;

public class ReviveClientPacket {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ReviveServerPacket.REVIVE_SYNC_PACKET, (client, handler, buf, sender) -> {
            int entityId = buf.readInt();
            int healthPoints = buf.readInt();
            client.execute(() -> {
                if (client.player.getId() == entityId) {
                    if (ReviveMain.CONFIG.thirdPersonOnDeath)
                        client.options.setPerspective(Perspective.FIRST_PERSON);
                    ((PlayerEntityAccessor) client.player).setCanRevive(false);
                    client.player.setHealth(healthPoints);
                    client.currentScreen.close();
                    client.player.deathTime = 0;
                    client.player.hurtTime = 0;
                    client.player.extinguish();
                } else {
                    PlayerEntity playerEntity = (PlayerEntity) client.world.getEntityById(entityId);
                    playerEntity.setHealth(healthPoints);
                    playerEntity.deathTime = 0;
                    playerEntity.hurtTime = 0;
                    playerEntity.extinguish();
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ReviveServerPacket.DEATH_REASON_PACKET, (client, handler, buf, sender) -> {
            boolean isOutOfWorld = buf.readBoolean();
            client.execute(() -> {
                ((PlayerEntityAccessor) client.player).setDeathReason(isOutOfWorld);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ReviveServerPacket.REVIVABLE_PACKET, (client, handler, buf, sender) -> {
            boolean canRevive = buf.readBoolean();
            boolean isSupportiveRevival = buf.readBoolean();
            client.execute(() -> {
                for (int u = 0; u < 30; u++)
                    client.world.addParticle(ParticleTypes.END_ROD, (double) client.player.getX() - 1.0D + client.world.random.nextFloat() * 2F, client.player.getRandomBodyY(),
                            (double) client.player.getZ() - 1.0D + client.world.random.nextFloat() * 2F, 0.0D, 0.2D, 0.0D);
                ((PlayerEntityAccessor) client.player).setCanRevive(canRevive);
                ((PlayerEntityAccessor) client.player).setSupportiveRevival(isSupportiveRevival);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ReviveServerPacket.FIRST_PERSON_PACKET, (client, handler, buf, sender) -> {
            client.execute(() -> {
                client.options.setPerspective(Perspective.FIRST_PERSON);
            });
        });
    }

    public static void writeC2SRevivePacket(boolean supportiveRevival) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBoolean(supportiveRevival);
        CustomPayloadC2SPacket packet = new CustomPayloadC2SPacket(ReviveServerPacket.REVIVE_PACKET, buf);
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
    }

}

/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.entity.type;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.AddPaintingPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.level.PaintingType;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.PaintingVariant;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;

import java.util.UUID;

public class PaintingEntity extends HangingEntity {
    private static final double OFFSET = -0.46875;
    private int paintingId = -1; // Ideally this would be the default painting Java uses in their metadata, but seems to depend on the current paintings loaded in the registry
    private Direction direction = Direction.SOUTH; // Default to SOUTH direction, like on Java - entity metadata should correct this when necessary

    public PaintingEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    @Override
    public void spawnEntity() {
        // Wait until we get the metadata needed
    }

    @Override
    public void setDirection(Direction direction) {
        this.direction = direction;
        updatePainting();
    }

    public void setPaintingType(ObjectEntityMetadata<Holder<PaintingVariant>> entityMetadata) {
        if (!entityMetadata.getValue().isId()) {
            return;
        }
        paintingId = entityMetadata.getValue().id();
        updatePainting();
    }

    private void updatePainting() {
        if (paintingId == -1) {
            return;
        } else if (valid) {
            despawnEntity();
        }

        PaintingType type = session.getRegistryCache().registry(JavaRegistries.PAINTING_VARIANT).byId(paintingId);
        if (type == null) {
            return;
        }

        if (type == PaintingType.DENNIS && !GameProtocol.is1_21_90orHigher(session)) {
            type = PaintingType.TIDES;
        }

        AddPaintingPacket addPaintingPacket = new AddPaintingPacket();
        addPaintingPacket.setUniqueEntityId(geyserId);
        addPaintingPacket.setRuntimeEntityId(geyserId);
        addPaintingPacket.setMotive(type.getBedrockName());
        addPaintingPacket.setPosition(fixOffset(type));
        addPaintingPacket.setDirection(switch (direction) {
            //TODO this doesn't seem right. Why did it work fine before?
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        });
        session.sendUpstreamPacket(addPaintingPacket);

        valid = true;

        session.getGeyser().getLogger().debug("Spawned painting on " + position);
    }

    @Override
    public void updateHeadLookRotation(float headYaw) {
        // Do nothing, as head look messes up paintings
    }

    private Vector3f fixOffset(PaintingType paintingName) {
        Vector3f position = super.position;
        // ViaVersion already adds the offset for us on older versions,
        // so no need to do it then otherwise it will be spaced
        if (session.isEmulatePost1_18Logic()) {
            position = position.add(0.5, 0.5, 0.5);
        }
        double widthOffset = paintingName.getWidth() > 1 && paintingName.getWidth() != 3 ? 0.5 : 0;
        double heightOffset = paintingName.getHeight() > 1 && paintingName.getHeight() != 3 ? 0.5 : 0;

        return switch (direction) {
            case SOUTH -> position.add(widthOffset, heightOffset, OFFSET);
            case WEST -> position.add(-OFFSET, heightOffset, widthOffset);
            case NORTH -> position.add(-widthOffset, heightOffset, -OFFSET);
            case EAST -> position.add(OFFSET, heightOffset, -widthOffset);
            default -> position;
        };
    }
}

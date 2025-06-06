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

package org.geysermc.geyser.translator.protocol.bedrock;

import org.cloudburstmc.protocol.bedrock.packet.FilterTextPacket;
import org.geysermc.geyser.inventory.AnvilContainer;
import org.geysermc.geyser.inventory.CartographyContainer;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

/**
 * Used to send strings to the server and filter out unwanted words.
 * Java doesn't care, so we don't care, and we approve all strings.
 */
@Translator(packet = FilterTextPacket.class)
public class BedrockFilterTextTranslator extends PacketTranslator<FilterTextPacket> {

    @Override
    public void translate(GeyserSession session, FilterTextPacket packet) {
        InventoryHolder<?> holder = session.getInventoryHolder();

        if (holder != null) {
            if (holder.inventory() instanceof CartographyContainer) {
                // We don't want to be able to rename in the cartography table
                return;
            }
            if (holder.inventory() instanceof AnvilContainer anvilContainer) {
                packet.setText(anvilContainer.checkForRename(session, packet.getText()));
                holder.updateSlot(1);
            }
        }

        packet.setFromServer(true);
        session.sendUpstreamPacket(packet);
    }
}

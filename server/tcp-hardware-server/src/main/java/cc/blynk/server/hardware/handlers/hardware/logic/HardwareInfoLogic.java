package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.HardwareInfo;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.ByteBufUtil.*;

/**
 *
 * Simple handler that accepts info command from hardware.
 * At the moment only 1 param is used "h-beat".
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareInfoLogic {

    private static final Logger log = LogManager.getLogger(HardwareInfoLogic.class);

    private final int hardwareIdleTimeout;

    public HardwareInfoLogic(int hardwareIdleTimeout) {
        this.hardwareIdleTimeout = hardwareIdleTimeout;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        String[] messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        HardwareInfo hardwareInfo = new HardwareInfo(messageParts);
        int newHardwareInterval = hardwareInfo.heartbeatInterval;

        log.trace("Info command. heartbeat interval {}", newHardwareInterval);

        if (hardwareIdleTimeout != 0 && newHardwareInterval > 0) {
            final int newReadTimeout = (int) Math.ceil(newHardwareInterval * 2.3D);
            log.trace("Changing read timeout interval to {}", newReadTimeout);
            ctx.pipeline().remove(ReadTimeoutHandler.class);
            ctx.pipeline().addFirst(new ReadTimeoutHandler(newReadTimeout));
        }

        DashBoard dashBoard = state.user.profile.getDashById(state.dashId);
        if (dashBoard != null && !hardwareInfo.equals(dashBoard.hardwareInfo)) {
            dashBoard.hardwareInfo = hardwareInfo;
            dashBoard.updatedAt = System.currentTimeMillis();
        }

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}

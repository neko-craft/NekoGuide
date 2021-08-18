package cn.apisium.nekoguide;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterOutputStream;

final class LivePacket {
    private final short version;
    int type;
    private final byte[] body;

    private LivePacket(short version, int type, byte[] body) {
        this.version = version;
        this.type = type;
        this.body = body;
    }

    LivePacket(int type, String body) {
        this((short) 0, type, body.getBytes());
    }

    static LivePacket parse(ByteBuf buf) {
        final int len = buf.readInt();
        final int bodyLen = len - buf.readShort();
        final short version = buf.readShort();
        final int type = buf.readInt();
        buf.readInt();
        byte[] bytes = new byte[bodyLen];
        buf.readBytes(bytes);
        return new LivePacket(version, type, bytes);
    }

    Buffer serialize() {
        return Buffer.buffer(Unpooled.buffer()
            .writeInt(16 + body.length)
            .writeShort(16)
            .writeShort(version)
            .writeInt(type)
            .writeInt(1)
            .writeBytes(body)
        );
    }

    String getStringBody() {
        if (version == 2) {
            try (final ByteArrayOutputStream buf = new ByteArrayOutputStream();
                final InflaterOutputStream stream = new InflaterOutputStream(buf)) {
                stream.write(body);
                return "{" + buf.toString(StandardCharsets.UTF_8).split("\\{", 2)[1];
            } catch (final Exception e) { e.printStackTrace(); }
            return "";
        }
        return new String(body);
    }
}

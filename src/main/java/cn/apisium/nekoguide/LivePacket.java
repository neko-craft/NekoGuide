package cn.apisium.nekoguide;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.vertx.core.buffer.Buffer;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

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
        int len = buf.readInt();
        int bodyLen = len - buf.readShort();
        short version = buf.readShort();
        int type = buf.readInt();
        buf.readInt();
        byte[] bytes = new byte[bodyLen];
        buf.readBytes(bytes);
        return new LivePacket(version, type, bytes);
    }

    Buffer serialize() {
        return Buffer.buffer(UnpooledByteBufAllocator.DEFAULT.buffer()
            .writeInt(16 + body.length)
            .writeShort(16)
            .writeShort(version)
            .writeInt(type)
            .writeInt(1)
            .writeBytes(body)
        );
    }

    String getStringBody() throws DataFormatException {
        if (type == 2) {
            Inflater inf = new Inflater();
            inf.setInput(body);
            byte[] bytes = new byte[inf.getTotalOut()];
            inf.inflate(bytes);
            inf.end();
            return new String(bytes);
        }
        return new String(body);
    }
}

package cn.apisium.nekoguide;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.function.Consumer;

final class LiveRoom {
    private final int rootId;
    private long time;
    private boolean stopped;
    private String beforeMsg = "";
    private WebSocket ws;
    private Consumer<String> messageCallback;
    private Runnable connectedCallback;
    private Runnable disconnectedCallback;
    private final static Vertx vertx = Vertx.vertx();
    private final static Buffer heartBeat = new LivePacket(2, "{}").serialize();

    LiveRoom(int rootId) {
        this.rootId = rootId;
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setKeepAlive(false));
        webClient
            .getAbs("https://api.live.bilibili.com/room/v1/Danmu/getConf?platform=pc&player=web&room_id=" + rootId)
            .send(ar -> {
                if (ar.succeeded()) {
                    JsonObject data = ar.result().bodyAsJsonObject().getJsonObject("data");
                    JsonObject server = data.getJsonArray("host_server_list").getJsonObject(0);
                    connect(server.getInteger("ws_port"), server.getString("host"), data.getString("token"));
                } else {
                    ar.cause().printStackTrace();
                }
            });
    }

    private void connect(int port, String host, String token) {
        time = System.currentTimeMillis();
        vertx.createHttpClient().webSocket(
            port,
            host,
            "/sub",
            webSocket -> {
                if (webSocket.succeeded()) {
                    ws = webSocket.result();
                    ws.exceptionHandler(Throwable::printStackTrace);
                    ws.writeBinaryMessage(new LivePacket(7, "{\"roomid\":" + rootId + ",\"key\":\"" +
                        token + "\"}").serialize());
                    ws.handler(it -> {
                        LivePacket packet = LivePacket.parse(it.getByteBuf());
                        try {
                            switch (packet.type) {
                                case 3:
                                    long cTime = System.currentTimeMillis();
                                    if (time - cTime > 50000) ws.end();
                                    time = cTime;
                                    break;
                                case 5:
                                    if (messageCallback == null) break;
                                    String msg = packet.getStringBody();
                                    if (!beforeMsg.equals(msg)) {
                                        beforeMsg = msg;
                                        messageCallback.accept(msg);
                                    }
                                    break;
                                case 8:
                                    time = System.currentTimeMillis();
                                    ws.write(heartBeat);
                                    if (connectedCallback != null) connectedCallback.run();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    Thread thread = new Thread(() -> {
                        try {
                            Thread.sleep(20000);
                            ws.write(heartBeat);
                        } catch (InterruptedException ignored) { }
                    });
                    ws.endHandler($ -> {
                        if (disconnectedCallback != null) disconnectedCallback.run();
                        if (stopped) return;
                        connect(port, host, token);
                        thread.interrupt();
                    });
                } else {
                    webSocket.cause().printStackTrace();
                }
            });
    }

    void stop() {
        stopped = true;
        if (ws != null) {
            ws.end();
            ws = null;
        }
    }

    LiveRoom onConnected(Runnable fn) {
        connectedCallback = fn;
        return this;
    }

    LiveRoom onDisconnected(Runnable fn) {
        disconnectedCallback = fn;
        return this;
    }

    LiveRoom onMessage(Consumer<String> fn) {
        messageCallback = fn;
        return this;
    }
}

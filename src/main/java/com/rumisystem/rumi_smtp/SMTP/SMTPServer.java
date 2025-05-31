package com.rumisystem.rumi_smtp.SMTP;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;
import static su.rumishistem.rumi_java_lib.LOG_PRINT.Main.LOG;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.net.ssl.SSLException;

import com.rumisystem.rumi_smtp.TYPE.SERVER_MODE;

import su.rumishistem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import su.rumishistem.rumi_java_lib.Socket.Server.SocketServer;
import su.rumishistem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT;
import su.rumishistem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT_LISTENER;

public class SMTPServer {
	private boolean TLS = false;

	public void Main(int PORT, SERVER_MODE MODE) throws SSLException, InterruptedException {
		SocketServer SS = new SocketServer();

		//鍵が有ればTLS
		if (Files.exists(Path.of(CONFIG_DATA.get("SSL").getData("CERT").asString()))) {
			if (Files.exists(Path.of(CONFIG_DATA.get("SSL").getData("PRIV").asString()))) {
				TLS = true;
				SS.setSSLSetting(
					CONFIG_DATA.get("SSL").getData("CERT").asString(),
					CONFIG_DATA.get("SSL").getData("PRIV").asString(),
					new String[] {"TLSv1.3"}
				);
				LOG(LOG_TYPE.OK, "STARTTLSが使用可能です");
			}
		}

		SS.setEventListener(new CONNECT_EVENT_LISTENER() {
			@Override
			public void CONNECT(CONNECT_EVENT SESSION) throws IOException {
				if (MODE == SERVER_MODE.TRANSFER) {
					SESSION.setEventListener(new Transfer(SESSION));
				} else {
					SESSION.setEventListener(new Submission(SESSION));
				}
			}
		});

		SS.START(PORT);
	}
}

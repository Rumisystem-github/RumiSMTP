package com.rumisystem.rumi_smtp.SMTP;

import static su.rumishistem.rumi_java_lib.LOG_PRINT.Main.LOG;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.rumisystem.rumi_smtp.TYPE.MAIL;

import su.rumishistem.rumi_java_lib.LOG_PRINT.LOG_TYPE;

public class MailDataReceive {
	public static MAIL Receive(byte[] Body, ByteArrayOutputStream BAOS, int MAX_SIZE) throws IOException {
		//メールデータ受信
		if (BAOS.size() <= MAX_SIZE) {
			BAOS.write(Body);
			BAOS.flush();
		}

		//メールデータの受信が終わったらここでEND
		byte[] All = BAOS.toByteArray();
		if (
				All.length >= 5
				&& All[All.length - 5] == 0x0D
				&& All[All.length - 4] == 0x0A
				&& All[All.length - 3] == '.'
				&& All[All.length - 2] == 0x0D
				&& All[All.length - 1] == 0x0A
			) {
			//最後の「\r\n.\r\n」を除外しようね
			byte[] MailBody = Arrays.copyOfRange(All, 0, All.length - 5);
			String Content = new String(MailBody, StandardCharsets.UTF_8);
			BAOS.reset();

			//メールデータ
			MAIL MD = new MAIL(Content);
			return MD;
		} else {
			return null;
		}
	}
}

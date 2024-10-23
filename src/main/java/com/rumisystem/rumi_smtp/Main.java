package com.rumisystem.rumi_smtp;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import static com.rumisystem.rumi_smtp.LOG_SYSTEM.LOG_SYSTEM.LOG_PRINT;

import java.io.File;

import com.rumisystem.rumi_java_lib.ArrayNode;
import com.rumisystem.rumi_java_lib.CONFIG;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_smtp.LOG_SYSTEM.LOG_LEVEL;
import com.rumisystem.rumi_smtp.MODULE.ACCOUNT;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX;
import com.rumisystem.rumi_smtp.SMTP.SMTP_MODE;
import com.rumisystem.rumi_smtp.SMTP.SMTP_SERVER;

public class Main {
	public static ArrayNode CONFIG_DATA = null;

	public static void main(String[] args) {
		try {
			LOG(LOG_TYPE.OK, "Staat RumiSMTP!");
			LOG(LOG_TYPE.PROCESS, "Loading Config.ini ....");

			//設定ファイルを読み込む
			if (new File("Config.ini").exists()) {
				CONFIG_DATA = new CONFIG().DATA;
				LOG(LOG_TYPE.PROCESS_END_OK, "");
			} else {
				LOG(LOG_TYPE.PROCESS_END_FAILED, "ERR! Config.ini ga NAI!!!!!!!!!!!!!!");
				System.exit(1);
			}

			//配送受付鯖起動
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						LOG_PRINT("TRANSFER_SERVER PORT[" + CONFIG_DATA.get("TRANSFER").asString("PORT") + "] kidou", LOG_TYPE.PROCESS, LOG_LEVEL.INFO);
						SMTP_SERVER TRANSFER_SERVER = new SMTP_SERVER(SMTP_MODE.TRANSFER, CONFIG_DATA.get("TRANSFER").asInt("PORT"));
						TRANSFER_SERVER.Main();
						LOG_PRINT("", LOG_TYPE.PROCESS_END_OK, LOG_LEVEL.INFO);
					} catch (Exception EX) {
						EX.printStackTrace();
						System.exit(1);
					}
				}
			}).start();

			//提出受付鯖起動
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						LOG_PRINT("SUBMISSION_SERVER PORT[" + CONFIG_DATA.get("SUBMISSION").asString("PORT") + "] kidou", LOG_TYPE.PROCESS, LOG_LEVEL.INFO);
						SMTP_SERVER SUBMISSTION_SERVER = new SMTP_SERVER(SMTP_MODE.SUBMISSION, CONFIG_DATA.get("SUBMISSION").asInt("PORT"));
						SUBMISSTION_SERVER.Main();
						LOG_PRINT("", LOG_TYPE.PROCESS_END_OK, LOG_LEVEL.INFO);
					} catch (Exception EX) {
						EX.printStackTrace();
						System.exit(1);
					}
				}
			}).start();

			/*
			TRANSFER TF = new TRANSFER("noreply@rumiserver.com", "rumisan_@outlook.com");
			TF.SEND_MAIL("From: RumiSaabaa <noreply@rumiserver.com>\n"
						+ "To: <rumisan@rumiserver.com>\n"
						+ "Date: " + new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Date()) + "\n"
						+ "Subject: aaa\n"
						+ "\n"
						+ "aaaaa\n");
			*/
		} catch (Exception EX) {
			EX.printStackTrace();
		}
	}
}

package com.rumisystem.rumi_smtp;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rumisystem.rumi_java_lib.ArrayNode;
import com.rumisystem.rumi_java_lib.CONFIG;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_smtp.MODULE.ACCOUNT_Manager;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX_Manager;
import com.rumisystem.rumi_smtp.TYPE.MAIL;
import com.rumisystem.rumi_smtp.TYPE.SERVER_MODE;

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

			ACCOUNT_Manager.INIT();

			//配送受付側
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						new SMTP_SERVER().Main(CONFIG_DATA.get("TRANSFER").asInt("PORT"), SERVER_MODE.TRANSFER);
					} catch (Exception EX) {
						EX.printStackTrace();
						LOG(LOG_TYPE.FAILED, "TRANSFER SERVER START ERR!");
						System.exit(1);
					}
				}
			}).start();

			//提出受付側
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						new SMTP_SERVER().Main(CONFIG_DATA.get("SUBMISSION").asInt("PORT"), SERVER_MODE.SUBMISSION);
					} catch (Exception EX) {
						EX.printStackTrace();
						LOG(LOG_TYPE.FAILED, "SUBMISSION SERVER START ERR!");
						System.exit(1);
					}
				}
			}).start();
		} catch (Exception EX) {
			EX.printStackTrace();
		}
	}
}
package com.rumisystem.rumi_smtp.MODULE;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;
import com.rumisystem.rumi_java_lib.FILER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MAILBOX {
	private String MAILBOX_DIR = null;

	public MAILBOX(String MAIL_ADDRESS) {
		//メアドが正規かチェック
		if (MAIL_ADDRESS.contains("@")) {
			//メールボックスのモードによって処理を変える
			if (CONFIG_DATA.get("MAILBOX").asString("MODE").equals("FILESYSTEM")) {
				//メールボックスのフォルダが有るか？
				if (VRFY(MAIL_ADDRESS)) {
					MAILBOX_DIR = ADDRES_TO_MAILBOXPATH(MAIL_ADDRESS);
				} else {
					//throw new Error("メアドが存在しない");
				}
			} else {
				//System.out.println("未実装");
			}
		} else {
			//throw new Error("メアドが不正");
		}
	}
	
	private static String ADDRES_TO_MAILBOXPATH(String MAIL_ADDRESS) {
		String MAIL_ADDRESS_R = SANITIZE(MAIL_ADDRESS);
		String UID = MAIL_ADDRESS_R.split("@")[0];
		String DOMAIN = MAIL_ADDRESS_R.split("@")[1];

		return CONFIG_DATA.get("MAILBOX").asString("PATH") + DOMAIN.replace(".", "_") + "/" + UID.replace(".", "_") + "/";
	}
	
	public static boolean VRFY(String MAIL_ADDRESS) {
		if (new File(ADDRES_TO_MAILBOXPATH(MAIL_ADDRESS)).exists()) {
			return true;
		} else {
			return false;
		}
	}
	
	public void MAIL_SAVE(String ID, String DATA) throws FileNotFoundException, IOException {
		new FILER(new File(MAILBOX_DIR + ID)).WRITE_STRING(DATA);
	}
	
	private static String SANITIZE(String ADDRESS) {
		return ADDRESS.replaceAll("[^A-Za-z._@-]", "");
	}
}

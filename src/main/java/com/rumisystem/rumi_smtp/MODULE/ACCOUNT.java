package com.rumisystem.rumi_smtp.MODULE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import com.rumisystem.rumi_java_lib.FILER;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;

public class ACCOUNT {
	private HashMap<String, String> DATA = new HashMap<String, String>();
	
	public ACCOUNT(String ADDRESS) throws FileNotFoundException, IOException {
		//ファイルモード時の処理
		if (CONFIG_DATA.get("ACCOUNT").asString("MODE").equals("FILE")) {
			String LIST = new FILER(new File(CONFIG_DATA.get("ACCOUNT").asString("PATH"))).OPEN_STRING();

			//アカウントリストから探す
			for(String INFO:LIST.split("\n")) {
				String ADD = INFO.split(":")[0];
				String PASS = INFO.split(":")[1];

				//アドレスが一致したなら変数に書き込んで終了
				if (ADD.equals(ADDRESS)) {
					DATA.put("ADDRESS", ADD);
					DATA.put("PASS", PASS);
					break;
				}
			}
		}
	}
	
	public boolean EXISTS() {
		if (DATA.get("ADDRESS") != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean LOGIN(String PASS) {
		try {
			//アカウントが存在しない
			if (DATA.get("ADDRESS") == null) {
				return false;
			}

			if (CONFIG_DATA.get("ACCOUNT").asString("MODE").equals("FILE")) {
				String IN_PASS = SHA3_256(PASS);
				if (DATA.get("PASS").equals(IN_PASS)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch (Exception EX) {
			EX.printStackTrace();
			return false;
		}
	}
	
	private String SHA3_256(String INPUT) throws NoSuchAlgorithmException {
		byte[] HASH_BYTE = MessageDigest.getInstance("SHA3-256").digest(INPUT.getBytes());
		StringBuilder SB = new StringBuilder();

		for (byte B:HASH_BYTE) {
			SB.append(String.format("%02x", B));
		}

		return SB.toString();
	}
}

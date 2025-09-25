package com.rumisystem.rumi_smtp.MODULE;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import su.rumishistem.rumi_java_lib.ArrayNode;
import su.rumishistem.rumi_java_lib.HASH;
import su.rumishistem.rumi_java_lib.HASH.HASH_TYPE;
import su.rumishistem.rumi_java_lib.SQL;

public class ACCOUNT_Manager {
	private static HashMap<String, String> ACCOUNT_LIST = new HashMap<String, String>();

	public static void INIT() throws IOException {
		if (CONFIG_DATA.get("ACCOUNT").getData("MODE").asString().equals("FILE")) {
			//ファイルモード
			List<String> ACCOUNT_TXT = Files.readAllLines(Paths.get(CONFIG_DATA.get("ACCOUNT").getData("PATH").asString()));
			for (String LINE:ACCOUNT_TXT) {
				if (LINE.split(":").length == 2) {
					String ADDRESS = LINE.split(":")[0];
					String PASS = LINE.split(":")[1];
					ACCOUNT_LIST.put(ADDRESS, PASS);
				}
			}
		}
	}

	public static boolean Exists(String ADDRESS) {
		if (CONFIG_DATA.get("ACCOUNT").getData("MODE").asString().equals("RSV")) {
			//るみ鯖モード
			try {
				ArrayNode result = SQL.RUN("SELECT `ID` FROM `MAIL_USER` WHERE CONCAT(`ADDRESS`, '@', `HOST`) = ?;", new Object[] {
					ADDRESS
				});

				if (result.length() == 1) {
					return true;
				} else {
					return false;
				}
			} catch (SQLException EX) {
				EX.printStackTrace();
				return false;
			}
		} else {
			//ファイルモード
			if (ACCOUNT_LIST.get(ADDRESS) != null) {
				return true;
			} else {
				return false;
			}
		}
	}

	public static boolean Auth(String Address, String Password) {
		if (!Exists(Address)) return false;

		if (CONFIG_DATA.get("ACCOUNT").getData("MODE").asString().equals("RSV")) {
			//るみ鯖モード
			try {
				ArrayNode result = SQL.RUN("SELECT `PASSWORD` FROM `MAIL_USER` WHERE CONCAT(`ADDRESS`, '@', `HOST`) = ? AND `LOGIN` = 1;", new Object[] {Address});
				if (result.length() == 1) {
					String hashed_password = HASH.Gen(HASH_TYPE.SHA3_256, Password.getBytes());
					if (result.get(0).getData("PASSWORD").asString().equals(hashed_password)) {
						return true;
					}
				}

				return false;
			} catch (SQLException EX) {
				return false;
			} catch (NoSuchAlgorithmException e) {
				return false;
			}
		} else {
			//ファイルモード
			return ACCOUNT_LIST.get(Address).equals(Password);
		}
	}
}

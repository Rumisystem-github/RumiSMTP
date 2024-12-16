package com.rumisystem.rumi_smtp.MODULE;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.rumisystem.rumi_java_lib.FILER;

public class MAILBOX_Manager {
	private Path MAILBOX_PATH;

	public MAILBOX_Manager(String ADDRESS) throws IOException {
		if (ACCOUNT_Manager.Exists(ADDRESS)) {
			if (CONFIG_DATA.get("MAILBOX").asString("MODE").equals("FILE")) {
				//ファイルモード
				MAILBOX_PATH = Paths.get(CONFIG_DATA.get("MAILBOX").asString("PATH") + ADDRESS);

				//ボックスがないなら作る
				if (!Files.exists(MAILBOX_PATH)) {
					Files.createDirectory(MAILBOX_PATH);
					Files.createDirectory(MAILBOX_PATH.resolve("cur"));
					Files.createDirectory(MAILBOX_PATH.resolve("new"));
					Files.createDirectory(MAILBOX_PATH.resolve("tmp"));
				}
			}
		} else {
			throw new Error("存在しないメアドのメールボックスを開こうとした");
		}
	}

	public void SaveMail(String ID, String DATA) throws FileNotFoundException, IOException {
		new FILER(new File(MAILBOX_PATH.resolve("new").resolve(ID).toUri())).WRITE_STRING(DATA);
	}
}

package com.rumisystem.rumi_smtp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.rumisystem.rumi_smtp.Config.MailBox.MBMode;

public class MailBox {
	private final long id;
	private final String userid;
	private final String host;

	public MailBox(long id, String userid, String host) throws IOException {
		this.id = id;
		this.userid = userid;
		this.host = host;

		if (Config.MailBox.Mode == MBMode.File) {
			Path path = Path.of(Config.MailBox.DirectoryPath + userid + "@" + host);
			if (!Files.exists(path)) Files.createDirectory(path);
		}
	}

	public void save_mail(String message_id, String body) throws IOException{
		if (Config.MailBox.Mode == MBMode.File) {
			File file = new File(Config.MailBox.DirectoryPath + userid + "@" + host + "/" + message_id);
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(body.getBytes(StandardCharsets.UTF_8));
			fos.close();
		}
	}
}

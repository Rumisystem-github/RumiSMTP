package com.rumisystem.rumi_smtp.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.rumisystem.rumi_smtp.Config;
import com.rumisystem.rumi_smtp.Config.MailBox.MBMode;

public class MailBox {
	private final long id;
	private final String userid;
	private final String host;

	public final String mailbox_base_path;
	public final String mail_toc_path;

	public MailBox(long id, String userid, String host) throws IOException {
		this.id = id;
		this.userid = userid;
		this.host = host;

		if (Config.MailBox.Mode == MBMode.File) {
			this.mailbox_base_path = Config.MailBox.DirectoryPath + userid + "@" + host;
			this.mail_toc_path = this.mailbox_base_path + "/TOC";
			Path path = Path.of(mailbox_base_path);
			if (!Files.exists(path)) Files.createDirectory(path);
		} else {
			mailbox_base_path = "/var/mail/" + id;
			this.mail_toc_path = this.mailbox_base_path + "/TOC";
		}

		create_directory("INBOX");
		create_directory("TRASH");
	}

	public void save_mail(String message_id, String body) throws IOException{
		if (Config.MailBox.Mode == MBMode.File) {
			File file = new File(mailbox_base_path + "/INBOX/" + message_id);
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(body.getBytes(StandardCharsets.UTF_8));
			fos.close();

			TOC toc = new TOC(mail_toc_path);
			toc.add(message_id, "INBOX");
			toc.close();
		}
	}

	public String[] get_directory() {
		List<String> list = new ArrayList<>();

		File directory = new File(mailbox_base_path);
		for (File f:directory.listFiles()) {
			if (f.isDirectory()) {
				list.add(f.getName().toUpperCase());
			}
		}

		String[] array = new String[list.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	public boolean is_exists_directory(String name) {
		File directory = new File(mailbox_base_path + "/" + name);
		return directory.exists();
	}

	public void create_directory(String name) throws IOException {
		if (Config.MailBox.Mode == MBMode.File) {
			Path path = Path.of(mailbox_base_path + "/" + name);
			if (!Files.exists(path)) Files.createDirectory(path);
		}
	}

	public int[] get_directory_status(String directory) throws IOException {
		TOC toc = new TOC(mail_toc_path);
		int[] status = new int[] {
			toc.get_exists(directory.toUpperCase()),
			toc.get_ack_count(directory.toUpperCase())
		};
		toc.close();

		return status;
	}

	public TOCEntry[] get_toc_entry_list() throws IOException {
		TOC toc = new TOC(mail_toc_path);

		TOCEntry[] list = new TOCEntry[toc.get_list().size()];
		for (int i = 0; i < list.length; i++) {
			list[i] = toc.get_list().get(i);
		}
		toc.close();

		return list;
	}

	public long get_mail_size(String directory, String message_id) {
		return new File(mailbox_base_path + "/" + directory + "/" + message_id).length();
	}

	public FileInputStream get_mail_fis(String directory, String message_id) throws FileNotFoundException {
		return new FileInputStream(new File(mailbox_base_path + "/" + directory + "/" + message_id));
	}
}

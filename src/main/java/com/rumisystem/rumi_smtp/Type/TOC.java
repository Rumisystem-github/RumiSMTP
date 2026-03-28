package com.rumisystem.rumi_smtp.Type;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class TOC implements Closeable{
	private final File f;
	private final RandomAccessFile raf;
	private final FileChannel ch;
	private final FileLock lock;

	private List<TOCEntry> list = new ArrayList<>();
	private boolean is_edited = false;

	public TOC(String path) throws IOException {
		this.f = new File(path);
		if (!Files.exists(Path.of(path))) {
			Files.createFile(Path.of(path));
			FileOutputStream fos = new FileOutputStream(f);
			fos.write("0\n".getBytes(StandardCharsets.UTF_8));
			fos.close();
		}

		this.raf = new RandomAccessFile(f, "rw");
		this.ch = this.raf.getChannel();
		this.lock = this.ch.lock();

		int length = Integer.parseInt(read_line());
		for (int i = 0; i < length; i++) {
			String line = read_line();
			String[] parts = line.split("\t");
			String message_id = parts[0];
			int imap_uid = Integer.parseInt(parts[1]);
			String directory = new String(Base64.getDecoder().decode(parts[2])).toUpperCase();
			boolean ack = parts[3].equals("1");

			list.add(new TOCEntry(i + 1, message_id, imap_uid, directory, ack));
		}
	}

	public List<TOCEntry> get_list() {
		return list;
	}

	public int get_exists(String directory) {
		int count = 0;
		for (TOCEntry e:list) {
			if (e.get_directory_name().equals(directory)) count += 1;
		}
		return count;
	}

	public int get_ack_count(String directory) {
		int count = 0;
		for (TOCEntry e:list) {
			if (e.get_directory_name().equals(directory) && e.is_ack()) count += 1;
		}
		return count;
	}

	public void add(String message_id, String directory) {
		is_edited = true;
		list.add(new TOCEntry(list.size() + 1, message_id, list.size() + 1, directory.toUpperCase(), false));
	}

	public void move(String message_id, String directory) {
		for (TOCEntry e:list) {
			if (e.get_message_id().equals(message_id)) {
				is_edited = true;
				e.set_directory(directory);
				return;
			}
		}
	}

	public void ack(String message_id) {
		for (TOCEntry e:list) {
			if (e.get_message_id().equals(message_id)) {
				is_edited = true;
				e.set_ack();
				return;
			}
		}
	}

	@Override
	public void close() throws IOException {
		//保存処理
		if (is_edited) {
			FileOutputStream fos = new FileOutputStream(f);
			fos.write((list.size() + "\n").getBytes(StandardCharsets.UTF_8));
			for (TOCEntry e:list) {
				fos.write(e.to_binary());
			}
			fos.close();
		}

		lock.release();
		ch.close();
		raf.close();
	}

	private String read_line() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte b;
		while ((b = raf.readByte()) != -1) {
			if (b == '\n') break;
			baos.write(b);
		}

		return baos.toString(StandardCharsets.UTF_8);
	}
}

/*
先頭4バイト			メール数

区切りは\nにするつもり
[メッセージID]\t[ディレクトリ名(base64)]\t[既読か(0 or 1)]
*/
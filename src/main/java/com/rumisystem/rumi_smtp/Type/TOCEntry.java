package com.rumisystem.rumi_smtp.Type;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TOCEntry {
	private final int sequence_number;
	private final String message_id;
	private final int imap_uid;
	private String directory;
	private boolean ack;

	public TOCEntry(int sequence_number, String message_id, int imap_uid, String directory, boolean ack) {
		this.sequence_number = sequence_number;
		this.message_id = message_id;
		this.imap_uid = imap_uid;
		this.directory = directory;
		this.ack = ack;
	}

	public int get_sequence_number() {
		return sequence_number;
	}

	public String get_message_id() {
		return message_id;
	}

	public int get_imap_uid() {
		return imap_uid;
	}

	public String get_directory_name() {
		return directory;
	}

	public boolean is_ack() {
		return ack;
	}

	public void set_directory(String in) {
		this.directory = in;
	}

	public void set_ack() {
		this.ack = true;
	}

	public byte[] to_binary() {
		String ack_string = "0";
		if (ack) ack_string = "1";

		return (message_id + "\t" + imap_uid + "\t" + Base64.getEncoder().encodeToString(directory.getBytes(StandardCharsets.UTF_8)) + "\t" + ack_string + "\n").getBytes(StandardCharsets.UTF_8);
	}
}

package com.rumisystem.rumi_smtp.POP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rumisystem.rumi_smtp.Hash;
import com.rumisystem.rumi_smtp.Main;
import com.rumisystem.rumi_smtp.Type.Account;
import com.rumisystem.rumi_smtp.Type.TOC;
import com.rumisystem.rumi_smtp.Type.TOCEntry;

public class POP {
	private static final String CRLF = "\r\n";

	private ExecutorService pool = Executors.newFixedThreadPool(4);

	public POP(int port) throws IOException {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				pool.shutdown();
			}
		}));

		ServerSocket tcp = new ServerSocket(port);
		while (true) {
			final Socket socket = tcp.accept();

			pool.submit(new Runnable() {
				private Account account;
				private String user_address;

				@Override
				public void run() {
					log("接続");

					try {
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						OutputStream out = socket.getOutputStream();
						println(out, "+OK POP3 {NAME} Joukoso, by RumiSMTP.");

						try {
							String line;
							while ((line = in.readLine()) != null) {
								String[] command = line.split(" ");
								log("->" + line);

								switch (command[0]) {
									case "USER": {
										if (command.length < 2) {
											println(out, "-ERR Insuu ga okashii.");
											break;
										}

										user_address = command[1];
										println(out, "+OK");
										break;
									}

									case "PASS": {
										if (command.length < 2) {
											println(out, "-ERR Insuu ga okashii.");
											break;
										}

										try {
											account = new Account(user_address.split("@")[0], user_address.split("@")[1]);
											if (!account.login(Hash.sha3_256(command[1].getBytes(StandardCharsets.UTF_8)))) {
												throw new RuntimeException("パスワード違がががいますね");
											}
										} catch (Exception ex) {
											println(out, "-ERR Shippai!");
										}

										println(out, "+OK Loggin shita.");
										break;
									}

									case "STAT": {
										int total_count = 0;
										long total_size = 0;

										for (TOCEntry entry:account.get_mailbox().get_toc_entry_list()) {
											if (!entry.get_directory_name().equals("INBOX")) continue;

											long size = account.get_mailbox().get_mail_size(entry.get_directory_name(), entry.get_message_id());

											total_count += 1;
											total_size += size;
										}

										println(out, "+OK " + total_count + " " + total_size);
										break;
									}

									case "LIST": {
										println(out, "+OK");

										for (TOCEntry entry:account.get_mailbox().get_toc_entry_list()) {
											if (!entry.get_directory_name().equals("INBOX")) continue;

											long size = account.get_mailbox().get_mail_size(entry.get_directory_name(), entry.get_message_id());
											println(out, entry.get_sequence_number() + " " + size);
										}

										println(out, ".");
										break;
									}

									case "RETR": {
										if (command.length < 2) {
											println(out, "-ERR Insuu ga okashii.");
											break;
										}

										int request_mail_number = Integer.parseInt(command[1]);
										for (TOCEntry entry:account.get_mailbox().get_toc_entry_list()) {
											if (entry.get_sequence_number() == request_mail_number) {
												println(out, "+OK");
												BufferedReader br = new BufferedReader(new InputStreamReader(account.get_mailbox().get_mail_fis(entry.get_directory_name(), entry.get_message_id())));
												String read_line;
												while ((read_line = br.readLine()) != null) {
													if (read_line.equals(".")) read_line = "..";
													out.write((read_line + CRLF).getBytes(StandardCharsets.UTF_8));
												}
												println(out, ".");
												break;
											}
										}
										break;
									}

									case "UIDL": {
										println(out, "+OK");

										for (TOCEntry entry:account.get_mailbox().get_toc_entry_list()) {
											if (!entry.get_directory_name().equals("INBOX")) continue;

											long size = account.get_mailbox().get_mail_size(entry.get_directory_name(), entry.get_message_id());
											println(out, entry.get_sequence_number() + " " + entry.get_message_id());
										}

										println(out, ".");
										break;
									}

									case "DELE": {
										if (command.length < 2) {
											println(out, "-ERR Insuu ga okashii.");
											break;
										}

										for (TOCEntry entry:account.get_mailbox().get_toc_entry_list()) {
											if (entry.get_sequence_number() == Integer.parseInt(command[1])) {
												entry.set_directory("TRASH");
												Files.move(Path.of(account.get_mailbox().mailbox_base_path + "/INBOX/" + entry.get_message_id()), Path.of(account.get_mailbox().mailbox_base_path + "/TRASH/" + entry.get_message_id()));
												break;
											}
										}
										println(out, "+OK");
										break;
									}

									case "QUIT": {
										println(out, "+OK");
										return;
									}

									default:
										if (account == null) {
											println(out, "-ERR 死なすぞ");
											return;
										} else {
											println(out, "-ERR Command ga naidesu!");
										}
										break;
								}
							}
						} finally {
							out.flush();
							socket.close();

							log("切断");
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						try {
							socket.close();
						} catch (Exception e) {
						}
					}
				}
			});
		}
	}

	private void println(OutputStream out, String message) throws IOException {
		out.write((message + CRLF).getBytes(StandardCharsets.UTF_8));
		out.flush();
		log("<- [" + message + "]");
	}

	private void log(String message) {
		if (!Main.develop) return;
		System.out.println(message);
	}
}

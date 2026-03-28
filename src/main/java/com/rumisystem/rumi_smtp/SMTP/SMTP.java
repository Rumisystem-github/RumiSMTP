package com.rumisystem.rumi_smtp.SMTP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rumisystem.rumi_smtp.Config;
import com.rumisystem.rumi_smtp.Hash;
import com.rumisystem.rumi_smtp.Main;
import com.rumisystem.rumi_smtp.Type.Account;

public class SMTP {
	private static final Pattern address_regex = Pattern.compile("([a-zA-Z0-9.!#$%&'*+\\/=?^_`{|}~-]+)@([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)");
	private static final int MAX_SIZE = 25 * 1024 * 1024; //25MB
	private static final String CRLF = "\r\n";

	private ExecutorService pool = Executors.newFixedThreadPool(4);
	private boolean is_transfer;

	public SMTP(int port, boolean transfer) throws IOException {
		is_transfer = transfer;

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
				private BufferedReader in;
				private BufferedWriter out;
				private String client_domain = null;

				private String mail_from_userid = null;
				private String mail_from_host = null;

				private String mail_to_userid = null;
				private String mail_to_host = null;

				private boolean client_auth_ok = false;

				@Override
				public void run() {
					try {
						in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
						println(out, "220 {NAME} ESMTP RumiSMTP");

						for (String ip:Config.SMTP.whitelist_host) {
							if (socket.getInetAddress().getHostAddress().equals(ip)) {
								client_auth_ok = true;
								break;
							}
						}

						try {
							String line;
							while ((line = in.readLine()) != null) {
								String[] command = line.split(" ");
								log("-> [" + line + "]");
								switch (command[0].toUpperCase()) {
									case "HELO":
									case "EHLO": {
										if (command.length != 2) {
											println(out, "501 Command no Argument ga okashii");
											break;
										}

										client_domain = command[1];
										println(out, "250-Joukoso <"+client_domain+">!");
										//println(out, "250-STARTTLS");
										if (is_transfer == false) println(out, "250-AUTH LOGIN PLAIN");
										println(out, "250 OK");
										break;
									}

									case "STARTTLS": {
										//TODO: 実装
										break;
									}

									case "AUTH": {
										//提出側のみ認証を許可する
										if (is_transfer) {
											println(out, "501 Command wa nai.");
											break;
										}

										if (command.length < 3) {
											println(out, "501 Command no Argument ga okashii");
											break;
										}

										if (!command[1].equalsIgnoreCase("PLAIN")) {
											println(out, "501 AUTH wa PLAIN nomi.");
											break;
										}

										String login_data = new String(Base64.getDecoder().decode(command[2]), StandardCharsets.UTF_8);
										String[] parts = login_data.split("\u0000", -1);
										String userid = parts[1].split("@")[0];
										String host = parts[1].split("@")[1];
										String password = parts[2];
										String password_hash = Hash.sha3_256(password.getBytes(StandardCharsets.UTF_8));
										if (new Account(userid, host).login(password_hash)) {
											client_auth_ok = true;
											println(out, "235 Ok!");
										} else {
											println(out, "501 Auth Error");
											return;
										}
										break;
									}

									case "MAIL": {
										if (command.length >= 2) {
											Matcher mtc = address_regex.matcher(line);
											if (mtc.find()) {
												mail_from_userid = mtc.group(1);
												mail_from_host = mtc.group(2);
												println(out, "250 Oke");
												break;
											}
										}
										println(out, "501 Command no Argument ga okashii");
										break;
									}

									case "RCPT": {
										if (command.length >= 2) {
											Matcher mtc = address_regex.matcher(line);
											if (mtc.find()) {
												mail_to_userid = mtc.group(1);
												mail_to_host = mtc.group(2);

												//ローカルユーザーか？
												try {
													new Account(mail_to_userid, mail_to_host);
												} catch (NoSuchElementException ex) {
													//外部ユーザーでも、提出側で認証済みなら通す
													if (!(is_transfer == false && client_auth_ok)) {
														println(out, "550 Error");
														return;
													}
												}

												println(out, "250 Oke");
												break;
											}
										}
										println(out, "501 Command no Argument ga okashii");
										break;
									}

									case "DATA": {
										//MAIL FROMやらRCPT TOをちゃんとしたかチェック
										if (mail_from_userid == null || mail_from_host == null || mail_to_userid == null || mail_to_host == null) {
											println(out, "501 MAIL FROM to RCPT TO wo shiro.");
											break;
										}

										println(out, "354 Mail Data wo okutte! Shuuljou wa <CR><LF>.<CR><LF> daze.");

										String message_id = UUID.randomUUID().toString();
										String mail_data_original_id = null;
										String mail_data_from_userid = null;
										String mail_data_from_host = null;

										ByteArrayOutputStream baos = new ByteArrayOutputStream();
										baos.write(("Received: RumiSMTP test" + CRLF).getBytes(StandardCharsets.UTF_8));
										baos.write(("Message-ID: <"+message_id+">" + CRLF).getBytes(StandardCharsets.UTF_8));

										String data_line;
										int current_size = 0;
										while ((data_line = in.readLine()) != null) {
											if (data_line.equals(".")) break;
											if (data_line.startsWith("..")) data_line = data_line.substring(1);

											byte[] line_byte = (data_line + "\r\n").getBytes(StandardCharsets.UTF_8);
											current_size += line_byte.length;

											if (current_size > MAX_SIZE) {
												out.write("552 dekasugimasu!" + CRLF);
												return;
											}

											baos.write(line_byte);

											if (data_line.toUpperCase().startsWith("FROM:")) {
												Matcher mtc = address_regex.matcher(data_line);
												if (mtc.find()) {
													mail_data_from_userid = mtc.group(1);
													mail_data_from_host = mtc.group(2);
												}
												continue;
											}
											if (data_line.toUpperCase().startsWith("MESSAGE-ID")) {
												int index = data_line.indexOf('<');
												mail_data_original_id = data_line.substring(index + 1);
												baos.write(("References: <"+mail_data_original_id+">\r\n").getBytes(StandardCharsets.UTF_8));
												continue;
											}
										}

										String mail_data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
										baos.close();

										// if (!(mail_from_userid.equals(mail_data_from_userid) && mail_from_host.equals(mail_data_from_host))) {
										// 	println(out, "550 Error");
										// 	break;
										// }

										//保存
										try {
											//ローカルユーザー
											Account account = new Account(mail_to_userid, mail_to_host);
											account.get_mailbox().save_mail(message_id, mail_data);
										} catch (NoSuchElementException ex) {
											//TODO: 外部ユーザーなので配送する
											break;
										}

										println(out, "250 Oke");
										break;
									}

									case "RSET": {
										mail_from_userid = null;
										mail_from_host = null;
										mail_to_userid = null;
										mail_to_host = null;
										println(out, "250 Oke");
										break;
									}

									case "QUIT": {
										println(out, "221 bajbaj");
										return;
									}

									default:
										if (client_domain == null) {
											println(out, "666 死ね");
											return;
										}
										println(out, "502 Command ga nai");
										break;
								}
							}
						} finally {
							out.flush();
							socket.close();
						}
					} catch (SocketException ex) {
						return;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		}
	}

	private void println(BufferedWriter bw, String message) throws IOException {
		bw.write(message + CRLF);
		bw.flush();
		log("<- [" + message + "]");
	}

	private void log(String message) {
		if (!Main.develop) return;
		System.out.println(message);
	}
}

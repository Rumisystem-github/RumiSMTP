package com.rumisystem.rumi_smtp;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rumisystem.rumi_java_lib.FILER;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_java_lib.Socket.Server.SocketServer;
import com.rumisystem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT;
import com.rumisystem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT_LISTENER;
import com.rumisystem.rumi_java_lib.Socket.Server.EVENT.CloseEvent;
import com.rumisystem.rumi_java_lib.Socket.Server.EVENT.EVENT_LISTENER;
import com.rumisystem.rumi_java_lib.Socket.Server.EVENT.MessageEvent;
import com.rumisystem.rumi_java_lib.Socket.Server.EVENT.ReceiveEvent;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX_Manager;
import com.rumisystem.rumi_smtp.MODULE.MAIL_ADDRESS_FIND;
import com.rumisystem.rumi_smtp.TYPE.MAIL;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;

public class TRANSFER_SERVER {
	private static int MAX_SIZE = 35882577;

	public static void Main(int PORT) throws IOException {
		SocketServer SS = new SocketServer();

		SS.setEventListener(new CONNECT_EVENT_LISTENER() {
			@Override
			public void CONNECT(CONNECT_EVENT SESSION) {
				try {
					MAIL MAIL_DATA = new MAIL();
					boolean[] DATA_SEND_NOW = {false}; //←Javaくんは頭が悪いので、こうしないといけません。
					StringBuilder MAIL_TEXT_SB = new StringBuilder();
					String[] HELO_DOMAIN = {""};
					boolean[] DATA_SKIP = {false};

					LOG(LOG_TYPE.INFO, "TRANSFER CONNECTED!");
					SEND("220 rumiserver.com ESMTP RumiSMTP joukoso!", SESSION);

					SESSION.setEventListener(new EVENT_LISTENER() {
						@Override
						public void Message(MessageEvent E) {
							try {
								if (!DATA_SEND_NOW[0]) {
									if (DATA_SKIP[0]) {
										if (E.getString().equals(".")) {
											DATA_SKIP[0] = false;
										}
										return;
									}

									String[] CMD = E.getString().replace("\r\n", "").split(" ");

									LOG(LOG_TYPE.INFO, "T<-[" + E.getString() + "]");

									//コマンド処理
									switch (CMD[0]) {
										//へろー
										case "HELO": {
											if (CMD[1] != null) {
												SEND("250 OK", SESSION);

												HELO_DOMAIN[0] = CMD[1];
											} else {
												SEND("500 Domain ga naizo!", SESSION);
											}
											break;
										}

										//拡張へろー
										case "EHLO": {
											if (CMD[1] != null) {
												SEND("250-OK", SESSION);
												SEND("250 SIZE-" + MAX_SIZE, SESSION);

												HELO_DOMAIN[0] = CMD[1];
											} else {
												SEND("500 Domain ga naizo!", SESSION);
											}
											break;
										}

										//送信元指定
										case "MAIL": {
											if (CMD[1] != null) {
												String FROM = MAIL_ADDRESS_FIND.FIND(E.getString());
		
												//ヌルチェック
												if (FROM != null) {
													MAIL_DATA.setFROM(FROM);
		
													SEND("250 OK", SESSION);
												} else {
													//メアドを見つけれんかった
													SEND("500 Mail ADDRESS ga naizo!", SESSION);
												}
											} else {
												SEND("500 FROM ga naizo!", SESSION);
											}
											break;
										}
		
										//送信先指定
										case "RCPT": {
											if (CMD[1] != null) {
												if (MAIL_DATA.getTO_Length() <= CONFIG_DATA.get("SMTP").asInt("MAX_TO_SIZE")) {
													String TO = MAIL_ADDRESS_FIND.FIND(E.getString());
		
													//ヌルチェック
													if (TO != null) {
														MAIL_DATA.addTO(TO);
														SEND("250 OK", SESSION);
													} else {
														//メアドを見つけれんかった
														SEND("500 Mail ADDRESS ga naizo!", SESSION);
													}
												} else {
													SEND("500 TO ga ooi", SESSION);
												}
											} else {
												SEND("500 TO ga naizo!", SESSION);
											}
											break;
										}
		
										case "DATA": {
											if (MAIL_DATA.getTO(0) != null && MAIL_DATA.getFROM() != null) {
												SEND("354 OK! meeru deeta wo okutte! owari wa <CRLF>.<CRLF> dajo!", SESSION);
												
												DATA_SEND_NOW[0] = true;
											} else {
												SEND("500 FUCk YOU", SESSION);
											}
											break;
										}

										//トランザクションをリセット
										case "RSET": {
											MAIL_DATA.RESET();

											SEND("250 OK! Zenkeshi", SESSION);
											break;
										}
		
										//メアドの存在チェック(セキュリティ的に無効化するべきらしい)
										case "VRFY": {
											SEND("502 Nha!", SESSION);
											break;
										}
		
										//NOOP：何もしない
										case "NOOP": {
											SEND("250 NOOOOOOOOOOP", SESSION);
											break;
										}
		
										//切断
										case "QUIT": {
											SEND("221 Pakapaka", SESSION);
											SESSION.close();
											break;
										}
		
										default: {
											SEND("502 COMMAND GA NAI", SESSION);
										}
									}
								}
							} catch (Exception EX) {
								EX.printStackTrace();
								SESSION.close();
							}
						}

						@Override
						public void Receive(ReceiveEvent E) {
							try {
								if (DATA_SEND_NOW[0]) {
									LOG(LOG_TYPE.INFO, "T<=" + E.getString());

									String END_TEXT = "\r\n.\r\n";
									//受信したデータをメールデータに挿入
									if (MAIL_TEXT_SB.length() <= MAX_SIZE) {
										MAIL_TEXT_SB.append(E.getString());
									}

									//メールデータ入力モード
									if (MAIL_TEXT_SB.toString().contains(END_TEXT)) {
										try {
											//終了
											DATA_SEND_NOW[0] = false;

											System.out.println("============================================");
											System.out.println(MAIL_TEXT_SB.toString());
											System.out.println("============================================");

											//取得できた本文を整形
											String MAIL_TEXT = MAIL_TEXT_SB.toString();
											MAIL_TEXT = MAIL_TEXT.replaceAll(END_TEXT + ".*", "");

											//Receivedヘッダーを設定する
											StringBuilder RECEIVED_HEADER_SB = new StringBuilder();
											RECEIVED_HEADER_SB.append("FROM " + HELO_DOMAIN[0] + "\r\n");
											RECEIVED_HEADER_SB.append("\tWITH ESMTP\r\n");
											RECEIVED_HEADER_SB.append("\tID <aaaa>\r\n");
											RECEIVED_HEADER_SB.append("\tFOR <" + MAIL_DATA.getTO(0) + ">;");
											MAIL_DATA.addHEADER("Received", RECEIVED_HEADER_SB.toString());

											boolean HEADER_PARSE = true;
											String MAE_KEY = "";
											for (String TEXT_LINE:MAIL_TEXT.split("\r\n")) {
												if (HEADER_PARSE) {
													if (TEXT_LINE.equals("")) {
														//ヘッダー解析離脱
														HEADER_PARSE = false;
														continue;
													}

													//ヘッダー解析モード
													//先頭がタブorスペース(インデント)なら前に処理したヘッダーに追記する
													if (!(TEXT_LINE.startsWith("\t") || TEXT_LINE.startsWith(" "))) {
														Pattern PTN = Pattern.compile("^([^:]+):\\s?(.*)$");
														Matcher MAT = PTN.matcher(TEXT_LINE);
														if (MAT.matches()) {
															String KEY = MAT.group(1).trim();
															String VAL = MAT.group(2).trim();
															if (KEY != null && VAL != null) {
																MAIL_DATA.addHEADER(KEY, VAL);
																MAE_KEY = KEY;
																continue;
															}
														}
													} else {
														//はいそれは先頭にインデントあり
														MAIL_DATA.appendHEADER(MAE_KEY, "\r\n" + TEXT_LINE);
														continue;
													}

													//此処に来た＝エラー
													throw new Error("ヘッダー解析失敗：正規表現がマッチせず");
												} else {
													//本文解析モード
													MAIL_DATA.addTEXT(TEXT_LINE + "\r\n");
													continue;
												}
											}

											//メッセージIDを設定(送信側で既に設定されてる可能性があるので、こっちで書き換える)
											String MESSAGE_ID = UUID.randomUUID().toString() + "@" + HELO_DOMAIN[0];
											MAIL_DATA.addHEADER("MESSAGE-ID", "<" + MESSAGE_ID + ">");

											String KANSEI = MAIL_DATA.BUILD();

											MAILBOX_Manager MAILBOX = new MAILBOX_Manager(MAIL_DATA.getTO(0));
											MAILBOX.SaveMail(MESSAGE_ID, KANSEI);

											MAIL_DATA.RESET();

											DATA_SKIP[0] = true;

											//成功
											SEND("250 OK! Okuttajo!", SESSION);
										} catch (Error EX) {
											//Errorということは9割メールデータの解析ミスなので、とりあえずRFCの所為にする
											SEND("554 Blja! MAIL DATA ga RFC ni zhunkjo shite nai!", SESSION);
											return;
										} catch (Exception EX) {
											EX.printStackTrace();
											SEND("554 Blja! Server ERR!", SESSION);
										}
									}
								}
							} catch (Exception EX) {
								EX.printStackTrace();
							}
						};

						@Override
						public void Close(CloseEvent E) {
							LOG(LOG_TYPE.INFO, "TRANSFER CLOSE!");
						}
					});
				} catch (Exception EX) {
					EX.printStackTrace();
					SESSION.close();
				}
			}
		});

		SS.START(PORT);
	}

	public static void SEND(String TEXT, CONNECT_EVENT SESSION) throws IOException {
		LOG(LOG_TYPE.INFO, "T->" + TEXT);
		SESSION.sendMessage(TEXT + "\r\n");
	}
}

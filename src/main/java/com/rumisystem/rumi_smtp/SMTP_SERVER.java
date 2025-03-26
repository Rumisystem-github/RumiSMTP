package com.rumisystem.rumi_smtp;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import su.rumishistem.rumi_java_lib.SANITIZE;
import su.rumishistem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import su.rumishistem.rumi_java_lib.Socket.Server.SocketServer;
import su.rumishistem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT;
import su.rumishistem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT_LISTENER;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.CloseEvent;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.EVENT_LISTENER;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.MessageEvent;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.ReceiveEvent;
import com.rumisystem.rumi_smtp.MODULE.ACCOUNT_Manager;
import com.rumisystem.rumi_smtp.MODULE.DMARCChecker;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX_Manager;
import com.rumisystem.rumi_smtp.MODULE.MAIL_ADDRESS_FIND;
import com.rumisystem.rumi_smtp.MODULE.SMTP_TRANSFER;
import com.rumisystem.rumi_smtp.TYPE.MAIL;
import com.rumisystem.rumi_smtp.TYPE.SERVER_MODE;
import static su.rumishistem.rumi_java_lib.LOG_PRINT.Main.LOG;
import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;

public class SMTP_SERVER {
	private int MAX_SIZE = 35882577;

	public void Main(int PORT, SERVER_MODE MODE) throws InterruptedException {
		SocketServer SS = new SocketServer();

		SS.setEventListener(new CONNECT_EVENT_LISTENER() {
			@Override
			public void CONNECT(CONNECT_EVENT SESSION) {
				try {
					MAIL MAIL_DATA = new MAIL();
					boolean[] DATA_SEND_NOW = {false}; //←Javaくんは頭が悪いので、こうしないといけません。
					boolean[] AUTH = {false};
					StringBuilder MAIL_TEXT_SB = new StringBuilder();
					String[] HELO_DOMAIN = {""};
					boolean[] DATA_SKIP = {false};

					//ようこそメッセージ
					LOG(LOG_TYPE.INFO, "TRANSFER CONNECTED!");
					SEND("220 rumiserver.com ESMTP RumiSMTP joukoso!", SESSION);

					//提出側＆ホワイトリストのIPならAUTHをtrueにする
					if (MODE == SERVER_MODE.SUBMISSION) {
						for (String WHITE_IP:CONFIG_DATA.get("SUBMISSION").getData("WHITE_LIST").asString().split(",")) {
							if (SESSION.getIP().contains(WHITE_IP)) {
								AUTH[0] = true;
								break;
							}
						}
					}

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

									LOG(LOG_TYPE.INFO, "T<-[" + SANITIZE.CONSOLE_SANITIZE(E.getString()) + "]");

									//コマンド処理
									switch (CMD[0].toUpperCase()) {
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

										//TODO:AUTHを搭載する

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
												if (MAIL_DATA.getTO_Length() <= CONFIG_DATA.get("SMTP").getData("MAX_TO_SIZE").asInt()) {
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
											//提出側なら認証チェック
											if (MODE == SERVER_MODE.SUBMISSION) {
												if (!AUTH[0]) {
													SEND("530 AUTH shiro!", SESSION);
													return;
												}
											}

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
											if (!HELO_DOMAIN[0].equals("")) {
												SEND("502 COMMAND GA NAI", SESSION);
											} else {
												//HELO/EHLOをしていない＆コマンドが違う=SMTPにHTTP送りつけてる馬鹿野郎の亜種
												SEND("221 Omae ga ruuru wo mamoranai nara ore mo yaburuwa, zhaana.", SESSION);
												SESSION.close();
											}
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
									LOG(LOG_TYPE.INFO, "T<=" + E.getString().length() + "Byte");

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
											//仕様により、受信後にMessageイベントにデータが流れてくるので、「.」が来るまでスキップするように命令する
											DATA_SKIP[0] = true;

											//取得できた本文を整形
											String MAIL_TEXT = MAIL_TEXT_SB.toString();
											MAIL_TEXT = MAIL_TEXT.replaceAll(END_TEXT + ".*", "");
											System.out.println("-----------------------------------------------------");
											System.out.println(MAIL_TEXT);
											System.out.println("-----------------------------------------------------");

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
													//Errorということは9割メールデータの解析ミスなので、とりあえずRFCの所為にする
													SEND("554 Blja! MAIL DATA ga RFC ni zhunkjo shite nai!", SESSION);
													return;
												} else {
													//本文解析モード
													MAIL_DATA.addTEXT(TEXT_LINE + "\r\n");
													continue;
												}
											}

											//送信元のSMTPが設定したメッセージIDを保存する
											MAIL_DATA.addHEADER("REFERENCES", MAIL_DATA.getHeader("MESSAGE-ID"));

											//メッセージIDを設定(送信側で既に設定されてる可能性があるので、こっちで書き換える)
											String MESSAGE_ID = UUID.randomUUID().toString() + "@" + HELO_DOMAIN[0];
											MAIL_DATA.addHEADER("MESSAGE-ID", "<" + MESSAGE_ID + ">");

											//メールデータをビルド
											String KANSEI = MAIL_DATA.BUILD();

											//ローカルユーザーか？
											if (ACCOUNT_Manager.Exists(MAIL_DATA.getTO(0))) {
												//提出側なら、認証してるかチェック
												if (MODE == SERVER_MODE.SUBMISSION) {
													if (!AUTH[0]) {
														SEND("530 AUTH shiro!", SESSION);
														return;
													}
												}

												System.out.println(MAIL_DATA.getFROM().split("@")[1]);
												System.out.println(SESSION.getIP());

												//DMARCチェック
												if (DMARCChecker.Check(MAIL_DATA.getFROM().split("@")[1], SESSION.getIP())) {
													//ローカルユーザーのメールボックスを開く
													MAILBOX_Manager MAILBOX = new MAILBOX_Manager(MAIL_DATA.getTO(0));
													MAILBOX.SaveMail(MESSAGE_ID, KANSEI);
												} else {
													SEND("530 DMARC Error", SESSION);
												}
											} else {
												//外部のユーザー宛だが、スパム鯖に使われたら問題なので、提出側からの受信であることをチェック&認証してるか
												if (MODE == SERVER_MODE.SUBMISSION && AUTH[0]) {
													try {
														//外部のSMTP鯖に提出する
														SMTP_TRANSFER TRANSFER_SYSTEM = new SMTP_TRANSFER(MAIL_DATA.getFROM(), MAIL_DATA.getTO(0), MESSAGE_ID);
														TRANSFER_SYSTEM.SEND_MAIL(KANSEI);
													} catch (Exception EX) {
														EX.printStackTrace();
														SEND("451 Blja! SMTP Server ERR:", SESSION);
														return;
													}
												} else {
													SEND("530 AUTH shiro!", SESSION);
													return;
												}
											}

											//初期化
											MAIL_DATA.RESET();

											//成功を通知
											SEND("250 OK! Okuttajo!", SESSION);
										} catch (Error EX) {
											//エラー
											SEND("451 Errr<" + new String(Base64.getEncoder().encode(EX.getMessage().getBytes())) + ">", SESSION);
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
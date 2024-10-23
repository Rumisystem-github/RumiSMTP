package com.rumisystem.rumi_smtp.SMTP;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import static com.rumisystem.rumi_smtp.LOG_SYSTEM.LOG_SYSTEM.LOG_PRINT;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rumisystem.rumi_java_lib.SANITIZE;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_java_lib.Socket.Server.SocketServer;
import com.rumisystem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT;
import com.rumisystem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT_LISTENER;
import com.rumisystem.rumi_java_lib.Socket.Server.EVENT.CloseEvent;
import com.rumisystem.rumi_java_lib.Socket.Server.EVENT.EVENT_LISTENER;
import com.rumisystem.rumi_java_lib.Socket.Server.EVENT.MessageEvent;
import com.rumisystem.rumi_smtp.TRANSFER;
import com.rumisystem.rumi_smtp.LOG_SYSTEM.LOG_LEVEL;
import com.rumisystem.rumi_smtp.MODULE.ACCOUNT;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX;
import com.rumisystem.rumi_smtp.TYPE.MAIL;

public class SMTP_SERVER {
	private String MODE_NAME = "";
	private String MODE_SNAME = "";
	private SMTP_MODE MODE = null;
	private int PORT = 0;
	private int MAX_SIZE = 35882577;
	
	public SMTP_SERVER(SMTP_MODE MODE, int PORT) {
		this.PORT = PORT;
		this.MODE = MODE;
		this.MAX_SIZE = CONFIG_DATA.get("SMTP").asInt("MAX_SIZE");
		
		//サーバー名を決定する
		if (MODE == SMTP_MODE.TRANSFER) {
			MODE_NAME = "TRANSFER";
			MODE_SNAME = "T";
		} else {
			MODE_NAME = "SUBMISSION";
			MODE_SNAME = "S";
		}
	}

	public void Main() throws IOException {
		SocketServer SS = new SocketServer();
		SS.setEventListener(new CONNECT_EVENT_LISTENER() {
			@Override
			public void CONNECT(CONNECT_EVENT SESSION) {
				try {
					String IP = SESSION.getIP();
					boolean[] AUTH = {false};			//Javaのクソ仕様でこうなった
					boolean[] DATA_NOW = {false};

					//メールデータ
					MAIL MAIL_DATA = new MAIL();

					//モードがTRANSFER(配送)ならAUTHをtrueにする(認証不要)
					if (MODE == SMTP_MODE.TRANSFER) {
						AUTH[0] = true;
					} else {
						//提出モードなのでAUTH必須、だが設定されたIPなら除外する
						for (String WHITE_IP:CONFIG_DATA.get("SUBMISSION").asString("WHITE_LIST").split(",")) {
							if (IP.contains(WHITE_IP)) {
								AUTH[0] = true;
								break;
							}
						}
					}

					//ログ
					LOG_PRINT(MODE_NAME + " New SESSION! IP:" + IP, LOG_TYPE.INFO, LOG_LEVEL.INFO);

					//ようこそメッセージ
					SESSION.sendMessage(ababa("220 rumiserver.com ESMTP RumiSMTP joukoso!"));

					SESSION.setEventListener(new EVENT_LISTENER() {
						@Override
						public void Message(MessageEvent E) {
							try {
								String[] CMD = E.getString().split(" ");

								//ログ
								LOG_PRINT(MODE_SNAME + "<-" + IP + "|" + SANITIZE.CONSOLE_SANITIZE(E.getString()), LOG_TYPE.INFO, LOG_LEVEL.DEBUG);

								//データ受信モードなら受信する
								if (DATA_NOW[0]) {
									//ドット単体なら終了
									if (E.getString().equals(".")) {
										//受信モード終了
										DATA_NOW[0] = false;

										//指定された送信先に保存なりなんなりする
										for (String TO:MAIL_DATA.TO) {
											//モードがTRANSFER(配送)ならメールボックスに保存(アカウントがないやつは無視する)
											if (MODE == SMTP_MODE.TRANSFER) {
												String ID = UUID.randomUUID().toString();
												String TREES_DATA = "Received: "
														+ "from " + MAIL_DATA.DOMAIN + "(" + SESSION.getIP() + ") by "
														+ "with ESMTP id " + ID + " for <" + TO + ">;";

												//メアドは自分のドメインか？
												if (CONFIG_DATA.get("SMTP").asString("DOMAIN").contains(TO.split("@")[1])) {
													//アカウントが有るか？
													if (new ACCOUNT(TO).EXISTS()) {
														//メールボックスを開く
														MAILBOX MB = new MAILBOX(TO);
														MB.MAIL_SAVE(ID, TREES_DATA
																+"\r\n"
																+"Message-ID: <" + ID + ">\r\n"
																+MAIL_DATA.TEXT.toString());

														LOG_PRINT("MAIL[" + ID + "] SAVE!", LOG_TYPE.OK, LOG_LEVEL.INFO);
													}
												} else {
													//ということは、別のSMTPサーバーに配送するものだが、
													//配送側(TRANSFER)でされるとスパムされかねないので、提出側(SUBMISSION)で有ることを確認する
													if (MODE == SMTP_MODE.SUBMISSION) {
														try {
															//念の為認証チェック
															if (AUTH[0]) {
																TRANSFER SENDER = new TRANSFER(MAIL_DATA.FROM, TO, ID);
																SENDER.SEND_MAIL(TREES_DATA + "\r\n" + MAIL_DATA.TEXT);
															}
														} catch (Exception EX) {
															//エラーはなかったことにする
														}
													}
												}
											} else {
												//TODO:提出側を実装
											}
										}

										//完了を知らせる
										SESSION.sendMessage(ababa("250 OK! Okuttajo!"));
									} else {
										//サイズをオーバーしていないなら記録
										if (MAIL_DATA.TEXT.length() <= MAX_SIZE) {
											MAIL_DATA.TEXT.append(E.getString());
											MAIL_DATA.TEXT.append("\r\n");
										}
									}
									return;
								}

								switch(CMD[0]) {
									case "HELO":{
										if (CMD[1] == null) {
											SESSION.sendMessage(ababa("500 Domain ga naizo!"));
											break;
										}

										MAIL_DATA.DOMAIN = CMD[1];

										SESSION.sendMessage(ababa("250 rumiserver.com kon nichi wa"));
										break;
									}

									case "EHLO": {
										if (CMD[1] == null) {
											SESSION.sendMessage(ababa("500 Domain ga naizo!"));
											break;
										}

										MAIL_DATA.DOMAIN = CMD[1].replace("\r", "").replace("\n", "");

										if (MODE == SMTP_MODE.TRANSFER) {
											SESSION.sendMessage(ababa("250-rumiserver.com Hello " + MAIL_DATA.DOMAIN));
											//SESSION.sendMessage("250-STARTTLS");
											SESSION.sendMessage(ababa("250 SIZE " + MAX_SIZE));
										} else {
											SESSION.sendMessage(ababa("250-rumiserver.com Hello " + MAIL_DATA.DOMAIN));
											//SESSION.sendMessage(ababa("250-STARTTLS"));
											SESSION.sendMessage(ababa("250-AUTH LOGIN PLAIN"));
											SESSION.sendMessage(ababa("250 SIZE " + MAX_SIZE));
										}
										break;
									}

									case "MAIL": {
										if (AUTH[0]) {
											//ヌルチェック(FROMがあるか)
											if (CMD[1] == null) {
												SESSION.sendMessage(ababa("500 Komand ga okashii"));
												break;
											}

											String FROM = CMD[1].split(":")[1];

											//<>で囲われていない場合が有るらしいので
											if (FROM.startsWith("<") && FROM.endsWith(">")) {
												FROM = FROM.replace("<", "");
												FROM = FROM.replace(">", "");
												
												MAIL_DATA.FROM = FROM;
											} else {
												MAIL_DATA.FROM = FROM;
											}

											SESSION.sendMessage(ababa("250 OK!"));
										} else {
											SESSION.sendMessage(ababa("535 AUTH SHIRO"));
										}
										break;
									}

									case "RCPT": {
										if (AUTH[0]) {
											//ヌルチェック(FROMがあるか)
											if (CMD[1] == null) {
												SESSION.sendMessage(ababa("500 Komand ga okashii"));
												break;
											}

											//Toの最大値を超えていないことをチェック
											if (MAIL_DATA.TO.size() <= CONFIG_DATA.get("SMTP").asInt("MAX_TO_SIZE")) {
												String TO = CMD[1].split(":")[1];

												//<>で囲われていない場合が有るらしいので
												if (TO.startsWith("<") && TO.endsWith(">")) {
													Matcher MATCH = Pattern.compile("<[^>]+>(.*?)</[^>]+>").matcher(TO);
													TO = TO.replace("<", "");
													TO = TO.replace(">", "");

													MAIL_DATA.TO.add(TO);
												} else {
													MAIL_DATA.TO.add(TO);
												}
											} else {
												SESSION.sendMessage(ababa("500 TO ga ooi"));
											}

											SESSION.sendMessage(ababa("250 OK!"));
										} else {
											SESSION.sendMessage(ababa("535 AUTH SHIRO"));
										}
										break;
									}

									case "DATA": {
										if (MAIL_DATA.TO.size() >= 0 && MAIL_DATA.FROM != null) {
											SESSION.sendMessage(ababa("354 OK! meeru deeta wo okutte! owari wa <CRLF>.<CRLF> dajo!"));

											DATA_NOW[0] = true;
										} else {
											SESSION.sendMessage(ababa("500 MAIL ka RCPT wo tobashitana?"));
										}
										break;
									}

									case "RSET": {
										SESSION.sendMessage(ababa("250 OK! Zenkeshi"));
										break;
									}

									//メアドの存在チェック(セキュリティ的に無効化するべきらしい)
									case "VRFY": {
										SESSION.sendMessage(ababa("502 Nha!"));
										break;
									}

									//NOOP：何もしない
									case "NOOP": {
										SESSION.sendMessage(ababa("250 NOOOOOOOOOOP"));
										break;
									}

									//切断
									case "QUIT": {
										SESSION.close();
										break;
									}

									//コマンドが実装されていない
									default: {
										SESSION.sendMessage(ababa("502 sono komando nai!"));
									}
								}
							} catch (Exception EX) {
								EX.printStackTrace();
								try {
									SESSION.sendMessage(ababa("451 SYETEM_ERR\r\n"));
								} catch (IOException EX2) {
									EX2.printStackTrace();
									//TODO:切断処理を書く
								}
							}
						}

						@Override
						public void Close(CloseEvent E) {
							LOG_PRINT(MODE_NAME + " Secudan IP:" + IP, LOG_TYPE.INFO, LOG_LEVEL.INFO);
						}
					});
				} catch (Exception EX) {
					EX.printStackTrace();
					//TODO:切断処理を書く
				}
			}
		});

		SS.START(PORT);
	}

	//関数名思いつかんかったわ
	private static String ababa(String TEXT) {
		return TEXT + "\r\n";
	}
}

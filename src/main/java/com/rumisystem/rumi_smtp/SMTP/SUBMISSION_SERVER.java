package com.rumisystem.rumi_smtp.SMTP;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.*;

import com.rumisystem.rumi_java_lib.SANITIZE;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_smtp.TRANSFER;
import com.rumisystem.rumi_smtp.MODULE.BW_WRITEER;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX;
import com.rumisystem.rumi_smtp.MODULE.MAIL_CHECK;
import com.rumisystem.rumi_smtp.SMTP.COMMAND.EHLO;
import com.rumisystem.rumi_smtp.SMTP.COMMAND.NOOP;
import com.rumisystem.rumi_smtp.SMTP.COMMAND.VRFY;

public class SUBMISSION_SERVER {
	private static int PORT = 25;
	private static int MAX_SIZE = 0;

	public static void Main() throws Exception {
		PORT = CONFIG_DATA.get("SUBMISSION").asInt("PORT");
		MAX_SIZE = CONFIG_DATA.get("SMTP").asInt("MAX_SIZE");
		
		ServerSocket SS = new ServerSocket(PORT);

		List<String> CONNECTERE_IP = new ArrayList<String>();
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					while(true) {
						Socket SOCKET = SS.accept();
						String IP = SOCKET.getInetAddress().getHostAddress();
						LOG(LOG_TYPE.INFO, "SUBMISSION New SESSION! IP:" + IP);
						
						//既にそのIPで接続しているか？
						if (!CONNECTERE_IP.contains(IP)) {
							//接続していないので、そのまま追加
							CONNECTERE_IP.add(IP);

							//そのまま通信開始
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										Socket SESSION = SOCKET;
										SSLSocket SSL_SESSION = null;
										BufferedReader BR = new BufferedReader(new InputStreamReader(SESSION.getInputStream()));
										PrintWriter BW = new PrintWriter(SESSION.getOutputStream(), true);
										BW_WRITEER BWW = new BW_WRITEER(BW, IP, "S");
										
										//最初のメッセージ
										BWW.SEND("220 rumiserver.com ESMTP RumiSMTP joukoso!");

										MAILBOX MB = null;
										String REMOTE_DOMAIN = null;
										String MAIL_FROM = null;
										List<String> MAIL_TO = new ArrayList<String>();
										String MAIL_TEXT = null;
										Boolean AUTH_OK = false;

										//ホワイトリストに有るなら認証済みにする
										if (CONFIG_DATA.get("SUBMISSION").asString("WHITE_LIST").contains(IP)) {
											AUTH_OK = true;
										}

										String LINE = "";
										while((LINE = BR.readLine()) != null) {
											String[] CMD = LINE.split(" ");

											LOG(LOG_TYPE.INFO, "S<-" + IP + "|" + SANITIZE.CONSOLE_SANITIZE(LINE));

											switch(CMD[0]) {
												case "HELO":{
													if (CMD[1] != null) {
														REMOTE_DOMAIN = CMD[1];

														BWW.SEND("250 rumiserver.com kon nichi wa");
													} else {
														BWW.SEND("500 Domain ga naizo!");
													}
													break;
												}

												case "EHLO":{
													if (CMD[1] != null) {
														REMOTE_DOMAIN = CMD[1];

														EHLO.Main(BWW, REMOTE_DOMAIN, MAX_SIZE, 1);
													} else {
														BWW.SEND("500 Domain ga naizo!");
													}
													break;
												}

												case "AUTH":{
													if (CMD[1].equals("PLAIN")) {
														//TODO:認証実装
														AUTH_OK = true;
														BWW.SEND("235 Ninshou OK!");
													} else {
														BWW.SEND("502 PLAIN nomi");
													}
												}

												case "MAIL":{
													if (AUTH_OK) {
														if (CMD[1].split(":")[1] != null) {
															String FROM = CMD[1].split(":")[1];
															
															//<>で囲われていない場合が有るらしいので
															if (FROM.startsWith("<") && FROM.endsWith(">")) {
																FROM = FROM.replace("<", "");
																FROM = FROM.replace(">", "");
																
																MAIL_FROM = FROM;
															} else {
																MAIL_FROM = FROM;
															}

															//OK
															BWW.SEND("250 OK!");
															break;
														} else {
															BWW.SEND("800 meeru adoresu ga okashii");
														}
													} else {
														BWW.SEND("535 AUTH SHIRO");
													}
													break;
												}

												case "RCPT":{
													if (AUTH_OK) {
														//Toの最大値を超えていないことをチェック
														if (MAIL_TO.size() <= CONFIG_DATA.get("SMTP").asInt("MAX_TO_SIZE")) {
															String TO = CMD[1].split(":")[1];

															//<>で囲われていない場合が有るらしいので
															if (TO.startsWith("<") && TO.endsWith(">")) {
																Matcher MATCH = Pattern.compile("<[^>]+>(.*?)</[^>]+>").matcher(TO);
																TO = TO.replace("<", "");
																TO = TO.replace(">", "");

																MAIL_TO.add(TO);
															} else {
																MAIL_TO.add(TO);
															}

															//自分のドメインならメアドが有るかチェックしない
															if (CONFIG_DATA.get("SMTP").asString("DOMAIN").contains(TO.split("@")[1])) {
																//メアドがあるか？
																if (MAILBOX.VRFY(TO)) {
																	//OK
																	BWW.SEND("250 OK!");
																} else {
																	BWW.SEND("550 meeru adoresu ga cukaenai");
																}
															} else {
																BWW.SEND("250 OK!");
															}
														} else {
															BWW.SEND("500 TO ga ooi");
														}
													} else {
														BWW.SEND("535 AUTH SHIRO");
													}
													break;
												} 

												case "DATA":{
													if (AUTH_OK) {
														if (MAIL_FROM != null && MAIL_TO.size() >= 0) {
															BWW.SEND("354 OK! meeru deeta wo okutte! owari wa <CRLF>.<CRLF> dajo!");
															StringBuilder SB = new StringBuilder();
															String LT = "";
															while((LT = BR.readLine()) != null) {
																//.だけなら終了
																if (!LT.equals(".")) {
																	if (SB.length() >= MAX_SIZE) {
																		BWW.SEND("552 MAIL SIZE GA DEKAI! MAX HA" + MAX_SIZE + "DAJO!");
																		break;
																	}

																	SB.append(LT + "\n");
																} else {
																	//データ内容を全てMAIL_TEXTに入れる
																	MAIL_TEXT = SB.toString();
																	break;
																}
															}

															//メールヘッダーを解析する
															MAIL_CHECK MC = new MAIL_CHECK(MAIL_TEXT);

															//FROMヘッダーがあるか、そしてMAIL FROMと同じかをチェック
															String MC_FROM = MC.FROM();
															if (MC_FROM != null) {
																if (!MC_FROM.equals(MAIL_FROM)) {
																	//一致していない
																	BWW.SEND("550-Mail format ga okashii");
																	BWW.SEND("550 From header ga husei");
																	break;
																}
															} else {
																//存在しない
																BWW.SEND("550-Mail format ga okashii");
																BWW.SEND("550 From header ga nai");
																break;
															}

															//メールのID
															String ID = UUID.randomUUID().toString();

															for(String TO:MAIL_TO) {
																try {
																	//トレース情報
																	String TREES_DATA = "Received: "
																			+ "from " + REMOTE_DOMAIN + "(" + SESSION.getInetAddress().getHostAddress() + ") by "
																			+ "with ESMTP id " + ID + " for <" + TO + ">;";

																	//自分のドメインならメールボックスを開いてメールを保存する
																	if (CONFIG_DATA.get("SMTP").asString("DOMAIN").contains(TO.split("@")[1])) {
																		//メールボックスを開く
																		MB = new MAILBOX(TO);
																		MB.MAIL_SAVE(ID, TREES_DATA + "\n" + MAIL_TEXT);

																		LOG(LOG_TYPE.OK, "MAIL[" + ID + "] SAVE!");
																	} else {
																		//外部のメアド
																		TRANSFER SENDER = new TRANSFER(MAIL_FROM, TO, ID);
																		SENDER.SEND_MAIL(TREES_DATA + "\r\n" + MAIL_TEXT);
																	}
																} catch (Exception EX) {
																	EX.printStackTrace();
																}
															}

															//OK
															BWW.SEND("250 OK! Okuttajo!");
														} else {
															BWW.SEND("500 MAIL ka RCPT wo tobashitana? ato AUTH");
															break;
														}
													} else {
														BWW.SEND("535 AUTH SHIRO");
													}
													break;
												}
												
												case "RSET":{
													MAIL_FROM = null;
													MAIL_TO.clear();
													MAIL_TEXT = null;
													
													BWW.SEND("250 OK! Zenkeshi");
													break;
												}

												case "VRFY":{
													VRFY.Main(BWW);
													break;
												}

												case "NOOP":{
													NOOP.Main(BWW);
													break;
												}

												case "QUIT":{
													BWW.SEND("221 Sajonara!");
													
													CONNECTERE_IP.remove(IP);

													if(SSL_SESSION == null) {
														SESSION.close();
													} else {
														SSL_SESSION.close();
													}
													return;
												}

												default:{
													BWW.SEND("502 sono komando nai!");
													break;
												}
											}
										}
									}catch (Exception EX) {
										EX.printStackTrace();
									}
								}
							}).start();
						} else {
							SOCKET.close();
						}
					}
				} catch (Exception EX) {
					EX.printStackTrace();
				}
			}
		}).start();
	}
}

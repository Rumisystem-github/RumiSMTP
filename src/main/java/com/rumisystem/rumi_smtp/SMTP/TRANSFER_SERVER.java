package com.rumisystem.rumi_smtp.SMTP;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;

public class TRANSFER_SERVER {
	private static int PORT = 25;
	
	public static void Main() throws IOException {
		ServerSocket SS = new ServerSocket(PORT);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					while(true) {
						Socket SOCKET = SS.accept();
						LOG(LOG_TYPE.INFO, "New SESSION! IP:" + SOCKET.getInetAddress().getHostAddress());
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									Socket SESSION = SOCKET;
									BufferedReader BR = new BufferedReader(new InputStreamReader(SESSION.getInputStream()));
									PrintWriter BW = new PrintWriter(SESSION.getOutputStream(), true);
									
									//最初のメッセージ
									WRITE(BW, "220 rumiserver.com ESMTP RumiSMTP joukoso!");
									
									String REMOTE_DOMAIN = null;
									String MAIL_FROM = null;
									List<String> MAIL_TO = new ArrayList<String>();
									String MAIL_TEXT = null;
									
									String LINE = "";
									while((LINE = BR.readLine()) != null) {
										String[] CMD = LINE.split(" ");

										LOG(LOG_TYPE.INFO, "<-" + LINE);

										switch(CMD[0]) {
											case "HELO":{
												REMOTE_DOMAIN = CMD[1];

												WRITE(BW, "250 rumiserver.com kon nichi wa");
												break;
											}

											case "EHLO":{
												REMOTE_DOMAIN = CMD[1];

												WRITE(BW, "250-rumiserver.com Hello " + CMD[1]);
												//WRITE(BW, "250-STARTTLS");
												WRITE(BW, "250 SIZE 35882577");
												//WRITE(BW, "250 8BITMIME");
												break;
											}

											case "MAIL":{
												String FROM = CMD[1].split(":")[1];
												
												System.out.println(CMD[1].split(":")[1]);

												//<>で囲われていない場合が有るらしいので
												if (FROM.startsWith("<") && FROM.endsWith(">")) {
													FROM = FROM.replace("<", "");
													FROM = FROM.replace(">", "");
													
													MAIL_FROM = FROM;
												} else {
													MAIL_FROM = FROM;
												}

												//OK
												WRITE(BW, "250 OK!");
												break;
											}

											case "RCPT":{
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

												//OK
												WRITE(BW, "250 OK!");
												break;
											}

											case "DATA":{
												System.out.println(MAIL_FROM);
												System.out.println(MAIL_TO.size());

												if (MAIL_FROM != null && MAIL_TO.size() >= 0) {
													WRITE(BW, "354 OK! meeru deeta wo okutte! owari wa <CRLF>.<CRLF> dajo!");
													StringBuilder SB = new StringBuilder();
													String LT = "";
													while((LT = BR.readLine()) != null) {
														//.だけなら終了
														if (!LT.equals(".")) {
															SB.append(LT + "\n");
															System.out.println(SB.length());
														} else {
															//データ内容を全てMAIL_TEXTに入れる
															MAIL_TEXT = SB.toString();
															WRITE(BW, "250 OK! Okuttajo!");
															break;
														}
													}

													//トレース情報
													for(String TO:MAIL_TO) {
														String ID = UUID.randomUUID().toString();
														String TREES_DATA = "Received:\n"
																+ "FROM <" + REMOTE_DOMAIN + "> (<" + SESSION.getInetAddress().getHostAddress() + ">)\n"
																+ "VIA TCP\n"
																+ "WITH ESMTP\n"
																+ "ID <" + ID + ">\n"
																+ "FOR <" + TO + ">";

														System.out.println("新しいメール：\n" + MAIL_FROM + "から" + TO + "へ\n" + TREES_DATA + "\n" + MAIL_TEXT);
													}
												} else {
													WRITE(BW, "500 MAIL ka RCPT wo tobashitana?");
													break;
												}
												break;
											}
											
											case "RSET":{
												MAIL_FROM = null;
												MAIL_TO.clear();
												MAIL_TEXT = null;
												
												WRITE(BW, "250 OK! Zenkeshi");
												break;
											}
											
											case "QUIT":{
												WRITE(BW, "221 Sajonara!");
												SESSION.close();
												return;
											}

											default:{
												WRITE(BW, "502 sono komando nai!");
												break;
											}
										}
									}
								} catch (Exception EX) {
									EX.printStackTrace();
								}
							}
						}).start();
					}
				} catch (Exception EX) {
					EX.printStackTrace();
				}
			}
		}).start();
	}
	
	private static void WRITE(PrintWriter BW, String TEXT) {
		BW.print(TEXT + "\r\n");
		BW.flush();
		
		LOG(LOG_TYPE.INFO, "->" + TEXT);
	}
}

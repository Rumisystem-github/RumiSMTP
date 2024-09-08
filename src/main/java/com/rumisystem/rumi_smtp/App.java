package com.rumisystem.rumi_smtp;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.util.Base64;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class App {
	private static String FROM = "noreply@rumiserver.com";
	private static String TO = "reimu@hakurei.win";
	private static String HOST = "route2.mx.cloudflare.net";
	private static int PORT = 25;
	
	public static void main(String[] args) {
		try {
			Socket SOCKET = new Socket(HOST, PORT);

			BufferedReader BR = new BufferedReader(new InputStreamReader(SOCKET.getInputStream()));
			BufferedWriter BW = new BufferedWriter(new OutputStreamWriter(SOCKET.getOutputStream()));

			String FIRST_MSG = WAIT_MSG(BR, BW);
			
			System.out.println("最初のメッセージ：" + FIRST_MSG);
			if (FIRST_MSG.startsWith("220")) {
				System.out.println("220が送られました、通信を開始します！");
				String EHLO_RESULT = RUNCMD("EHLO rumiserver.com", BR, BW);

				//EHLOの返答の中にSTARTTLSがあるか
				if (EHLO_RESULT.contains("STARTTLS")) {
					System.out.println("STARTTLSの対応を確認、TLS化します");
					
					if (RUNCMD("STARTTLS", BR, BW).startsWith("220")) {
						SSLSocketFactory SSLFACTORY = (SSLSocketFactory) SSLSocketFactory.getDefault();
						SSLSocket SSLS = (SSLSocket) SSLFACTORY.createSocket(SOCKET, HOST, PORT, true);

						//SSL化する
						SSLS.startHandshake();
						System.out.println("SSLのハンドシェイクが完了した！");

						BR = new BufferedReader(new InputStreamReader(SSLS.getInputStream()));
						BW = new BufferedWriter(new OutputStreamWriter(SSLS.getOutputStream()));
						
						RUNCMD("EHLO rumiserver.com", BR, BW);
					} else {
						System.out.println("STARTTLSできませんでした；；");
						System.exit(1);
					}
				} else {
					System.out.println("悲報：TLS非対応");
				}
				
				if (RUNCMD("MAIL FROM:<" + FROM + ">", BR, BW).startsWith("250")) {
					if (RUNCMD("RCPT TO:<" + TO + ">", BR, BW).startsWith("250")) {
						if (RUNCMD("DATA", BR, BW).startsWith("354")) {
							//Base64の用意
							Base64.Encoder BE = Base64.getEncoder();

							//メール本体
							SEND_MSG("From: RumiSaba <" + FROM + ">", BR, BW);
							SEND_MSG("To: rumisan_ <" + TO + ">", BR, BW);
							SEND_MSG("Date: " + new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Date()) + "", BR, BW);
							SEND_MSG("Subject: =?UTF-8?B?" + BE.encodeToString("テストメッセージ".getBytes()) + "?=", BR, BW);
							SEND_MSG("Content-Transfer-Encoding: base64", BR, BW);
							SEND_MSG("Content-Type: text/plain; charset=UTF-8", BR, BW);
							SEND_MSG("", BR, BW);
							SEND_MSG(BE.encodeToString("RumiSMTPのテストなのだ！！".getBytes()), BR, BW);
							SEND_MSG("", BR, BW);

							if (RUNCMD(".", BR, BW).startsWith("250")) {
								System.out.println("メール送信成功！切断します！");
								RUNCMD("QUIT", BR, BW);
								System.exit(0);
							}
						}
					}
				}
				System.out.println("メールを送れなかった；；");
			} else {
				System.out.print("相手が220を返しませんでした、切断します");
				SOCKET.close();
			}
		} catch (Exception EX) {
			EX.printStackTrace();
		}
	}
	
	public static String RUNCMD(String TEXT, BufferedReader BR, BufferedWriter BW) throws IOException {
		SEND_MSG(TEXT, BR, BW);
		return WAIT_MSG(BR, BW);
	}
	
	public static void SEND_MSG(String TEXT, BufferedReader BR, BufferedWriter BW) throws IOException {
		BW.write(TEXT + "\r\n");
		BW.flush();
	}
	
	public static String WAIT_MSG(BufferedReader BR, BufferedWriter BW) throws IOException {
		StringBuilder RESULT = new StringBuilder();
		char[] BUFFER = new char[1024];
		//相手が送信し終わっていないか、何も読み込んでいない場合は続ける
		while (BR.ready() || RESULT.length() == 0) {
			int CHARS_READ = BR.read(BUFFER);
			if (CHARS_READ > 0) {
				RESULT.append(BUFFER, 0, CHARS_READ);
			} else {
				//ストリームの終わりに達した
				break;
			}
		}

		System.out.println("-----------------[START]---------------------");
		System.out.println(RESULT.toString());
		System.out.println("-----------------[ END ]---------------------");
		
		return RESULT.toString();
	}
}

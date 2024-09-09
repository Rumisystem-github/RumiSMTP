package com.rumisystem.rumi_smtp;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

public class TRANSFER {
	private BufferedReader BR = null;
	private BufferedWriter BW = null;
	private String SMTP_HOST = null;
	private int PORT = 25;
	private String FROM = "";
	private String TO = "";
	private String FROM_NAME = "";
	
	public TRANSFER(String FROM, String FNAME, String TO) throws UnknownHostException, IOException {
		this.FROM = FROM;
		this.FROM_NAME = FNAME;
		this.TO = TO;
		
		//MXレコードから探し出す
		SMTP_HOST = DNSMX(TO.split("@")[1]).replaceAll("^\\d+\\s+", "");;
		
		if (SMTP_HOST != null) {
			CONNECT_SMTP();
		} else {
			throw new Error("MXレコードが見つからない");
		}
	}
	
	private void CONNECT_SMTP() throws UnknownHostException, IOException {
		Socket SOCKET = new Socket(SMTP_HOST, PORT);
		
		BR = new BufferedReader(new InputStreamReader(SOCKET.getInputStream()));
		BW = new BufferedWriter(new OutputStreamWriter(SOCKET.getOutputStream()));
		
		String FIRST_MSG = WAIT_MSG(BR, BW);
		
		System.out.println("最初のメッセージ：" + FIRST_MSG);
		if (FIRST_MSG.startsWith("220")) {
			System.out.println("220が送られました、通信を開始します！");
			String EHLO_RESULT = RUNCMD("EHLO rumiserver.com", BR, BW);

			//EHLOの返答の中にSTARTTLSがあるか
			if (EHLO_RESULT.contains("STARTTLS")) {
				//STARTTLS対応なのでSTARTTLSする
				System.out.println("STARTTLSの対応を確認、TLS化します");
				
				if (RUNCMD("STARTTLS", BR, BW).startsWith("220")) {
					SSLSocketFactory SSLFACTORY = (SSLSocketFactory) SSLSocketFactory.getDefault();
					SSLSocket SSLS = (SSLSocket) SSLFACTORY.createSocket(SOCKET, SMTP_HOST, PORT, true);

					//SSL化する
					SSLS.startHandshake();
					System.out.println("SSLのハンドシェイクが完了した！");

					BR = new BufferedReader(new InputStreamReader(SSLS.getInputStream()));
					BW = new BufferedWriter(new OutputStreamWriter(SSLS.getOutputStream()));
					
					RUNCMD("EHLO rumiserver.com", BR, BW);
				} else {
					System.out.println("STARTTLSできませんでした；；");
					throw new Error("STARTTLSエラー");
				}
			} else {
				//STARTTLS非対応なのでそのままにする
			}
		} else {
			System.out.print("相手が220を返しませんでした、切断します");
			SOCKET.close();
		}
	}
	
	public void SEND_MAIL(String SUBJECT, String TEXT) throws IOException {
		if (RUNCMD("MAIL FROM:<" + FROM + ">", BR, BW).startsWith("250")) {
			if (RUNCMD("RCPT TO:<" + TO + ">", BR, BW).startsWith("250")) {
				if (RUNCMD("DATA", BR, BW).startsWith("354")) {
					//Base64の用意
					Base64.Encoder BE = Base64.getEncoder();

					//メール本体
					SEND_MSG("From: " + FROM_NAME + " <" + FROM + ">", BR, BW);
					SEND_MSG("To: to <" + TO + ">", BR, BW);
					SEND_MSG("Date: " + new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Date()) + "", BR, BW);
					SEND_MSG("Subject: =?UTF-8?B?" + BE.encodeToString(SUBJECT.getBytes()) + "?=", BR, BW);
					SEND_MSG("Content-Transfer-Encoding: base64", BR, BW);
					SEND_MSG("Content-Type: text/plain; charset=UTF-8", BR, BW);
					SEND_MSG("", BR, BW);
					SEND_MSG(BE.encodeToString(TEXT.getBytes()), BR, BW);
					SEND_MSG("", BR, BW);

					if (RUNCMD(".", BR, BW).startsWith("250")) {
						System.out.println("メール送信成功！切断します！");
						RUNCMD("QUIT", BR, BW);
						return;
					}
				}
			}
		}
		
		throw new Error("メールを送信できませんでした");
	}

	//DNSのMXレコードを検索する
	private String DNSMX(String DOMAIN) {
		// 環境設定
		Hashtable<String, String> ENV = new Hashtable<String, String>();
		//ENV.put(DirContext.PROVIDER_URL, "dns://8.8.8.8");
		ENV.put(DirContext.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

		try {
			// InitialDirContextの作成
			DirContext ICTX = new InitialDirContext(ENV);

			// MXレコードの取得
			Attributes ATTRS = ICTX.getAttributes(DOMAIN, new String[]{"MX"});
			Attribute MX = ATTRS.get("MX");

			if (MX != null) {
				return MX.get(0).toString();
			} else {
				return null;
			}
		} catch (Exception EX) {
			EX.printStackTrace();
			return null;
		}
	}
	
	private String RUNCMD(String TEXT, BufferedReader BR, BufferedWriter BW) throws IOException {
		SEND_MSG(TEXT, BR, BW);
		return WAIT_MSG(BR, BW);
	}
	
	private void SEND_MSG(String TEXT, BufferedReader BR, BufferedWriter BW) throws IOException {
		BW.write(TEXT + "\r\n");
		BW.flush();
	}
	
	private String WAIT_MSG(BufferedReader BR, BufferedWriter BW) throws IOException {
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

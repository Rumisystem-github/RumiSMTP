package com.rumisystem.rumi_smtp.MODULE;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.naming.directory.Attribute;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import static com.rumisystem.rumi_smtp.MODULE.LOG_SYSTEM.LOG_PRINT;
import su.rumishistem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_smtp.TYPE.LOG_LEVEL;

public class SMTP_TRANSFER {
	private BufferedReader BR = null;
	private BufferedWriter BW = null;
	private String SMTP_HOST = null;
	private int PORT = 25;
	private String FROM = "";
	private String TO = "";
	private String ID = "";
	private Socket SOCKET;

	public SMTP_TRANSFER(String FROM, String TO, String ID) throws UnknownHostException, IOException {
		this.FROM = FROM;
		this.TO = TO;
		this.ID = ID;

		//MXレコードから探し出す (replaceAllしてるのは、MXレコードに「5 」みたいな頭悪い文字入れてる場合があるから)
		SMTP_HOST = DNSMX(MAIL_ADDRESS_FIND.FIND_DOMAIN(TO)).replaceAll("^\\d+\\s+", "");

		if (SMTP_HOST != null) {
			LOG_PRINT("SEND MAIL[" + ID + "]MX REKOODO GET!", LOG_TYPE.OK, LOG_LEVEL.DEBUG);

			CONNECT_SMTP();
		} else {
			throw new Error("MXレコードが見つからない");
		}
	}
	
	private void CONNECT_SMTP() throws UnknownHostException, IOException {
		SOCKET = new Socket(SMTP_HOST, PORT);
		
		BR = new BufferedReader(new InputStreamReader(SOCKET.getInputStream()));
		BW = new BufferedWriter(new OutputStreamWriter(SOCKET.getOutputStream()));
		
		String FIRST_MSG = WAIT_MSG(BR, BW);
		
		if (FIRST_MSG.startsWith("220")) {
			LOG_PRINT("SEND MAIL[" + ID + "]220 OK!", LOG_TYPE.OK, LOG_LEVEL.DEBUG);
			String EHLO_RESULT = RUNCMD("EHLO rumiserver.com", BR, BW);

			//EHLOの返答の中にSTARTTLSがあるか
			if (EHLO_RESULT.contains("STARTTLS")) {
				//STARTTLS対応なのでSTARTTLSする
				LOG_PRINT("SEND MAIL[" + ID + "]STARTTLS START", LOG_TYPE.OK, LOG_LEVEL.DEBUG);

				if (RUNCMD("STARTTLS", BR, BW).startsWith("220")) {
					SSLSocketFactory SSLFACTORY = (SSLSocketFactory) SSLSocketFactory.getDefault();
					SSLSocket SSLS = (SSLSocket) SSLFACTORY.createSocket(SOCKET, SMTP_HOST, PORT, true);

					//SSL化する
					SSLS.startHandshake();
					LOG_PRINT("SEND MAIL[" + ID + "]SSL HANDSHAKE OK!", LOG_TYPE.OK, LOG_LEVEL.DEBUG);

					BR = new BufferedReader(new InputStreamReader(SSLS.getInputStream()));
					BW = new BufferedWriter(new OutputStreamWriter(SSLS.getOutputStream()));

					RUNCMD("EHLO rumiserver.com", BR, BW);
					LOG_PRINT("SEND MAIL[" + ID + "]Send EHLO!", LOG_TYPE.OK, LOG_LEVEL.DEBUG);
					return;
				} else {
					LOG_PRINT("SEND MAIL[" + ID + "]STARTTLS ERR!!!", LOG_TYPE.FAILED, LOG_LEVEL.DEBUG);
					throw new Error("STARTTLSエラー");
				}
			} else {
				//STARTTLS非対応なのでそのままにする
				return;
			}
		} else {
			LOG_PRINT("SEND MAIL[" + ID + "]CONNECT ERR! 220 zhanai!", LOG_TYPE.FAILED, LOG_LEVEL.DEBUG);
			SOCKET.close();
		}
	}
	
	public void SEND_MAIL(String MAILDATA) throws IOException {
		LOG_PRINT("SEND MAIL[" + ID + "]Start SEND...", LOG_TYPE.OK, LOG_LEVEL.DEBUG);
		if (RUNCMD("MAIL FROM:<" + FROM + ">", BR, BW).startsWith("250")) {
			LOG_PRINT("SEND MAIL[" + ID + "]MAIL FROM", LOG_TYPE.OK, LOG_LEVEL.DEBUG);
			if (RUNCMD("RCPT TO:<" + TO + ">", BR, BW).startsWith("250")) {
				LOG_PRINT("SEND MAIL[" + ID + "]RCPT TO", LOG_TYPE.OK, LOG_LEVEL.DEBUG);
				if (RUNCMD("DATA", BR, BW).startsWith("354")) {
					LOG_PRINT("SEND MAIL[" + ID + "]MAIL DATA SEND...", LOG_TYPE.INFO, LOG_LEVEL.DEBUG);
					//メール本体
					BW.write(MAILDATA + "\r\n");
					BW.flush();

					String RESULT = RUNCMD(".", BR, BW);
					if (RESULT.startsWith("250")) {
						LOG_PRINT("SEND MAIL[" + ID + "]MAIL SEND!", LOG_TYPE.OK, LOG_LEVEL.DEBUG);
						RUNCMD("QUIT", BR, BW);
						return;
					} else {
						RUNCMD("QUIT", BR, BW);

						LOG_PRINT("SEND MAIL[" + ID + "]SEND ERR!", LOG_TYPE.FAILED, LOG_LEVEL.DEBUG);
						LOG_PRINT("SEND MAIL[" + ID + "]" + RESULT, LOG_TYPE.FAILED, LOG_LEVEL.DEBUG);

						throw new Error("メールを送信を確定できず");
					}
				} else {
					RUNCMD("QUIT", BR, BW);
					throw new Error("DATAでエラー");
				}
			} else {
				RUNCMD("QUIT", BR, BW);
				throw new Error("RCPT TOでエラー");
			}
		} else {
			RUNCMD("QUIT", BR, BW);
			throw new Error("MAIL FROMでエラー");
		}
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

		return RESULT.toString();
	}
}

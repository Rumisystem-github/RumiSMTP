package com.rumisystem.rumi_smtp.SMTP;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;
import static su.rumishistem.rumi_java_lib.LOG_PRINT.Main.LOG;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.rumisystem.rumi_smtp.MODULE.ACCOUNT_Manager;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX_Manager;
import com.rumisystem.rumi_smtp.MODULE.MAIL_ADDRESS_FIND;
import com.rumisystem.rumi_smtp.MODULE.SMTP_TRANSFER;
import com.rumisystem.rumi_smtp.MODULE.TelnetParse;
import com.rumisystem.rumi_smtp.TYPE.MAIL;
import su.rumishistem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import su.rumishistem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.CloseEvent;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.EVENT_LISTENER;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.MessageEvent;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.ReceiveEvent;

public class Submission implements EVENT_LISTENER{
	private static final String HeloMessage = "220 rumiserver.com ESMTP RumiSMTP Sub joukoso!";

	private CONNECT_EVENT SESSION;
	private boolean WhiteListIn = false;
	private int MAX_SIZE = 35882577;
	private ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
	private String HeloDomain = null;
	private String MailFrom = null;
	private List<String> MailTo = new ArrayList<String>();
	private boolean SendingData = false;
	private boolean AuthOK = false;
	private ByteArrayOutputStream DataBAOS = new ByteArrayOutputStream();

	public Submission(CONNECT_EVENT SESSION) {
		try {
			LOG(LOG_TYPE.INFO, "SUBMISSION：Open Session["+SESSION.getIP()+"]");

			this.SESSION = SESSION;

			for (String WHITE_IP:CONFIG_DATA.get("SUBMISSION").getData("WHITE_LIST").asString().split(",")) {
				if (SESSION.getIP().contains(WHITE_IP)) {
					WhiteListIn = true;
					AuthOK = true;
					break;
				}
			}

			//挨拶
			Send(HeloMessage);
		} catch (Exception EX) {
			EX.printStackTrace();
			SESSION.close();
		}
	}

	private void Send(String MSG) {
		try {
			SESSION.sendMessage(MSG + "\r\n");
			LOG(LOG_TYPE.INFO, "SUBMISSION->[" + MSG + "]");
		} catch (Exception EX) {
			//EX.printStackTrace();
		}
	}

	private void Run(String[] CMD) {
		LOG(LOG_TYPE.INFO, "SUBMISSION<-[" + String.join(" ", CMD) + "]");

		switch (CMD[0]) {
			case "HELO": {
				if (CMD[1] != null) {
					Send("250 OK");
					HeloDomain = CMD[1];
				} else {
					Send("500 Domain ga naizo!");
				}
				return;
			}

			case "EHLO": {
				if (CMD[1] != null) {
					Send("250-OK");
					Send("250-STARTTLS");
					Send("250-AUTH-PLAIN");
					Send("250 SIZE-"+MAX_SIZE);
					HeloDomain = CMD[1];
				} else {
					Send("500 Domain ga naizo!");
				}
				return;
			}

			case "STARTTLS": {
				Send("220 TLS Handshake douzo!");

				SESSION.StartTLS().thenAccept(Suc->{
					try {
						if (Suc) {
							Send(HeloMessage);
						} else {
							SESSION.close();
						}
					} catch (Exception EX) {
						SESSION.close();
					}
				});
				return;
			}

			case "MAIL": {
				if (!AuthOK) {
					Send("530 AUTH SHIRO!");
					return;
				}

				if (CMD[1] != null) {
					String FROM = MAIL_ADDRESS_FIND.FIND(String.join(" ", CMD));
					
					//ヌルチェック
					if (FROM != null) {
						MailFrom = FROM;

						//ローカルユーザーか？
						if (!ACCOUNT_Manager.Exists(FROM)) {
							Send("530 Fuck supamaa");
							return;
						}

						Send("250 OK");
					} else {
						//メアドを見つけれんかった
						Send("500 Mail ADDRESS ga naizo!");
					}
				} else {
					Send("500 FROM ga naizo!");
				}
				return;
			}

			case "RCPT": {
				if (!AuthOK) {
					Send("530 AUTH SHIRO!");
					return;
				}

				if (CMD[1] != null) {
					if (MailTo.size() <= CONFIG_DATA.get("SMTP").getData("MAX_TO_SIZE").asInt()) {
						String TO = MAIL_ADDRESS_FIND.FIND(String.join(" ", CMD));

						//ヌルチェック
						if (TO != null) {
							MailTo.add(TO);
							Send("250 OK");
						} else {
							//メアドを見つけれんかった
							Send("500 Mail ADDRESS ga naizo!");
						}
					} else {
						Send("500 TO ga ooi");
					}
				} else {
					Send("500 FROM ga naizo!");
				}
				return;
			}

			case "DATA": {
				if (MailFrom != null && MailTo.size() != 0) {
					SendingData = true;

					Send("354 OK! meeru deeta wo okutte! owari wa <CRLF>.<CRLF> dajo!");
				} else {
					Send("500 FUCk YOU");
				}
				return;
			}

			case "RSET": {
				MailFrom = null;
				MailTo = null;
				DataBAOS.reset();

				Send("250 OK! Zenkeshi");
				return;
			}

			case "NOOP": {
				Send("250 Niin");
				return;
			}

			case "QUIT": {
				Send("221 Sainara");
				SESSION.close();
				return;
			}

			default: {
				if (HeloDomain != null) {
					//コマンドがない
					Send("502 COMMAND GA NAI");
				} else {
					//そもそもSMTPではないのでは？
					Send("221 Fuck you");
				}
				return;
			}
		}
	}

	@Override
	public void Close(CloseEvent e) {
		try {
			LOG(LOG_TYPE.INFO, "SUBMISSION：Close Session["+SESSION.getIP()+"]");
		} catch (Exception EX) {
			//EX.printStackTrace();
		}
	}

	@Override
	public void Message(MessageEvent e) {}

	@Override
	public void Receive(ReceiveEvent e) {
		try {
			if (!SendingData) {
				for (String CommandLine:TelnetParse.Parse(e.getByte(), BAOS)) {
					Run(CommandLine.split(" "));
				}
			} else {
				LOG(LOG_TYPE.INFO, "SUBMISSION<=" + e.getByte().length + "Byte");
				MAIL MD = MailDataReceive.Receive(e.getByte(), DataBAOS, MAX_SIZE);
				if (MD != null) {
					SendingData = false;

					//メッセージID
					String ID = UUID.randomUUID().toString();
					MD.setHeader("REFERENCES", MD.getHeader("MESSAGE-ID"));
					MD.setHeader("MESSAGE-ID", "<"+ID+">");

					//Receivedヘッダー
					StringBuilder ReceivedHeaderSB = new StringBuilder();
					ReceivedHeaderSB.append("FROM ").append(HeloDomain).append("\r\n");
					ReceivedHeaderSB.append("\t").append("WITH ESMTP").append("\r\n");
					ReceivedHeaderSB.append("\t").append("ID ").append("<").append(ID).append(">").append("\r\n");
					MD.setHeader("RECEIVE", ReceivedHeaderSB.toString());

					for (String To:MailTo) {
						MD.setHeader("TO", To);
						String Kansei = MD.BUILD();

						//ローカルユーザー？
						if (ACCOUNT_Manager.Exists(To)) {
							//ローカルユーザーのメールボックスを開く
							MAILBOX_Manager MAILBOX = new MAILBOX_Manager(To);
							MAILBOX.SaveMail(ID, Kansei);
						} else {
							//別鯖
							try {
								//外部のSMTP鯖に提出する
								SMTP_TRANSFER TRANSFER_SYSTEM = new SMTP_TRANSFER(MailFrom, To, ID);
								TRANSFER_SYSTEM.SEND_MAIL(Kansei);
							} catch (Exception EX) {
								EX.printStackTrace();
								Send("451 Blja! SMTP Server ERR:");
								return;
							}
						}
					}

					Send("250 OK! Okuttajo!");
				}
			}
		} catch (Exception EX) {
			EX.printStackTrace();
			SESSION.close();
		}
	}
}

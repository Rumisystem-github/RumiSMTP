package com.rumisystem.rumi_smtp.SMTP;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;
import static su.rumishistem.rumi_java_lib.LOG_PRINT.Main.LOG;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.rumisystem.rumi_smtp.MODULE.ACCOUNT_Manager;
import com.rumisystem.rumi_smtp.MODULE.DMARCChecker;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX_Manager;
import com.rumisystem.rumi_smtp.MODULE.MAIL_ADDRESS_FIND;
import com.rumisystem.rumi_smtp.TYPE.MAIL;

import su.rumishistem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import su.rumishistem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.CloseEvent;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.EVENT_LISTENER;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.MessageEvent;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.ReceiveEvent;

public class Transfer implements EVENT_LISTENER {
	private static final String HeloMessage = "220 rumiserver.com ESMTP RumiSMTP joukoso!";

	private CONNECT_EVENT SESSION;
	private boolean WhiteListIn = false;
	private int MAX_SIZE = 35882577;
	private ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
	private String HeloDomain = null;
	private String MailFrom = null;
	private List<String> MailTo = new ArrayList<String>();
	private boolean SendingData = false;
	private ByteArrayOutputStream DataBAOS = new ByteArrayOutputStream();

	public Transfer(CONNECT_EVENT SESSION) {
		try {
			LOG(LOG_TYPE.INFO, "TRANSFER：Open Session["+SESSION.getIP()+"]");

			this.SESSION = SESSION;

			for (String WHITE_IP:CONFIG_DATA.get("SUBMISSION").getData("WHITE_LIST").asString().split(",")) {
				if (SESSION.getIP().contains(WHITE_IP)) {
					WhiteListIn = true;
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
			LOG(LOG_TYPE.INFO, "TRANSFER->[" + MSG + "]");
		} catch (Exception EX) {
			//EX.printStackTrace();
		}
	}

	private void Run(String[] CMD) {
		LOG(LOG_TYPE.INFO, "TRANSFER<-[" + String.join(" ", CMD) + "]");

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
				if (CMD[1] != null) {
					String FROM = MAIL_ADDRESS_FIND.FIND(String.join(" ", CMD));
					
					//ヌルチェック
					if (FROM != null) {
						MailFrom = FROM;

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
				if (CMD[1] != null) {
					if (MailTo.size() <= CONFIG_DATA.get("SMTP").getData("MAX_TO_SIZE").asInt()) {
						String TO = MAIL_ADDRESS_FIND.FIND(String.join(" ", CMD));

						//ヌルチェック
						if (TO != null) {
							//宛先がローカルユーザーか？
							if (!ACCOUNT_Manager.Exists(TO)) {
								Send("530 Fuck supamaa");
								return;
							}

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
			LOG(LOG_TYPE.INFO, "TRANSFER：Close Session["+SESSION.getIP()+"]");
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
				//コマンド受信
				BAOS.write(e.getByte());
				BAOS.flush();

				byte[] All = BAOS.toByteArray();
				int Last = 0;

				for (int I = 0; I < All.length - 1; I++) {
					//↓\r\nだよ
					if (All[I] == 0x0D && All[I+1] == 0x0A) {
						byte[] CommandBytes = Arrays.copyOfRange(All, Last, I);
						String CommandLine = new String(CommandBytes, StandardCharsets.UTF_8);

						Run(CommandLine.split(" "));

						//次の行の先頭へ
						I++;//\n
						Last = I + 1;
					}
				}

				//未処理の残りを保持(全部処理したならresetする)
				if (Last > 0) {
					byte[] ReMain = Arrays.copyOfRange(All, Last, All.length);
					BAOS.reset();
					BAOS.write(ReMain);
				}
			} else {
				//メールデータ受信
				if (DataBAOS.size() <= MAX_SIZE) {
					DataBAOS.write(e.getByte());
					DataBAOS.flush();
					LOG(LOG_TYPE.INFO, "TRANSFER<=" + e.getByte().length + "Byte");
				}

				//メールデータの受信が終わったらここでEND
				byte[] All = DataBAOS.toByteArray();
				if (
						All.length >= 5
						&& All[All.length - 5] == 0x0D
						&& All[All.length - 4] == 0x0A
						&& All[All.length - 3] == '.'
						&& All[All.length - 2] == 0x0D
						&& All[All.length - 1] == 0x0A
					) {
					//最後の「\r\n.\r\n」を除外しようね
					byte[] MailBody = Arrays.copyOfRange(All, 0, All.length - 5);
					String Content = new String(MailBody, StandardCharsets.UTF_8);
					DataBAOS.reset();
					SendingData = false;

					//メールデータ
					MAIL MD = new MAIL(Content);

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

						//DMARCチェック
						boolean DMARC_STATUS = DMARCChecker.Check(MailFrom.split("@")[1], SESSION.getIP());

						if (WhiteListIn) {
							DMARC_STATUS = true;
						}

						if (DMARC_STATUS) {
							//ローカルユーザーのメールボックスを開く
							MAILBOX_Manager MAILBOX = new MAILBOX_Manager(To);
							MAILBOX.SaveMail(ID, Kansei);
						} else {
							Send("530 DMARC Error");
							return;
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

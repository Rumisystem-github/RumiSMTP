package com.rumisystem.rumi_smtp.POP;

import static com.rumisystem.rumi_smtp.Main.CONFIG_DATA;
import static su.rumishistem.rumi_java_lib.LOG_PRINT.Main.LOG;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.rumisystem.rumi_smtp.MODULE.ACCOUNT_Manager;
import com.rumisystem.rumi_smtp.MODULE.MAILBOX_Manager;
import com.rumisystem.rumi_smtp.MODULE.TelnetParse;

import su.rumishistem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import su.rumishistem.rumi_java_lib.Socket.Server.CONNECT_EVENT.CONNECT_EVENT;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.CloseEvent;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.EVENT_LISTENER;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.MessageEvent;
import su.rumishistem.rumi_java_lib.Socket.Server.EVENT.ReceiveEvent;

public class POPHandler implements EVENT_LISTENER{
	private ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
	private CONNECT_EVENT SESSION;
	private String Address;
	private MAILBOX_Manager MBOX = null;
	private File[] MailList = null;
	private boolean AuthOK = false;

	public POPHandler(CONNECT_EVENT SESSION) {
		try {
			LOG(LOG_TYPE.INFO, "POP：Open Session["+SESSION.getIP()+"]");

			this.SESSION = SESSION;

			//挨拶
			Send("+OK RumiSMTP POP3 Ready");
		} catch (Exception EX) {
			EX.printStackTrace();
			SESSION.close();
		}
	}

	private void Send(String MSG) {
		try {
			SESSION.sendMessage(MSG + "\r\n");
			LOG(LOG_TYPE.INFO, "POP->[" + MSG + "]");
		} catch (Exception EX) {
			//EX.printStackTrace();
		}
	}

	private void Run(String[] CMD) throws IOException {
		switch (CMD[0]) {
			case "CAPA":
			case "CAPABILITY": {
				Send("+OK");
				Send("STLS");
				Send(".");
				return;
			}

			case "STLS": {
				Send("+OK TLS Go Go GO");
				SESSION.StartTLS().thenAccept(Suc->{
					if (Suc) {
						//ようこそメッセージは送らんでいいらしい
					} else {
						SESSION.close();
					}
				});
				return;
			}

			case "USER": {
				if (CMD[1] != null) {
					if (ACCOUNT_Manager.Exists(CMD[1])) {
						Address = CMD[1];
						Send("+OK");
					} else {
						Send("-ERR");
					}
				} else {
					Send("-ERR");
				}
				return;
			}

			case "PASS": {
				if (CMD[1] != null) {
					if (ACCOUNT_Manager.Auth(Address, CMD[1])) {
						MBOX = new MAILBOX_Manager(Address);
						MailList = MBOX.getNewMailList();
						AuthOK = true;

						Send("+OK");
					} else {
						Send("-ERR");
					}
				} else {
					Send("-ERR");
				}
				return;
			}

			case "STAT": {
				if (!AuthOK) {
					Send("-ERR");
					return;
				}

				Send(new StringBuilder().append("+OK ").append(MBOX.NewMailCount()).append(" 0").toString());
				return;
			}

			case "LIST": {
				if (!AuthOK) {
					Send("-ERR");
					return;
				}

				Send("+OK");

				for (int I = 0; I < MailList.length; I++) {
					Send(new StringBuilder().append(I).append(" ").append(MailList[I].length()).toString());
				}

				Send(".");
				return;
			}

			case "UIDL": {
				if (!AuthOK) {
					Send("-ERR");
					return;
				}

				Send("+OK");

				for (int I = 0; I < MailList.length; I++) {
					Send(new StringBuilder().append(I).append(" ").append(MailList[I].getName()).toString());
				}

				Send(".");
				return;
			}

			case "RETR": {
				if (!AuthOK) {
					Send("-ERR");
					return;
				}

				if (CMD[1] != null) {
					File MailFile = null;
					if (MailList.length >= Integer.parseInt(CMD[1])) {
						Send("-ERR");
						return;
					}
					MailFile = MailList[Integer.parseInt(CMD[1])];

					Send("+OK " + MailFile.length() + "octets");
					BufferedReader BR = new BufferedReader(new FileReader(MailFile));
					String Line;
					while ((Line = BR.readLine()) != null) {
						Send(Line);
					}
					BR.close();
					Send(".");
				} else {
					Send("-ERR");
				}
				return;
			}

			case "DELE": {
				if (!AuthOK) {
					Send("-ERR");
					return;
				}

				File MailFile = null;
				if (MailList.length >= Integer.parseInt(CMD[1])) {
					Send("-ERR");
					return;
				}
				MailFile = MailList[Integer.parseInt(CMD[1])];

				MailFile.delete();
				Send("+OK");
				return;
			}

			case "NOOP": {
				Send("+OK");
				return;
			}

			case "RSET": {
				Send("+OK");
				return;
			}

			case "QUIT": {
				Send("+OK");
				SESSION.close();
				return;
			}

			default: {
				Send("-ERR");
				return;
			}
		}
	}

	@Override
	public void Close(CloseEvent e) {
	}

	@Override
	public void Message(MessageEvent e) {
	}

	@Override
	public void Receive(ReceiveEvent e) {
		try {
			for (String CommandLine:TelnetParse.Parse(e.getByte(), BAOS)) {
				LOG(LOG_TYPE.INFO, "POP<-[" + CommandLine + "]");
				Run(CommandLine.split(" "));
			}
		} catch (Exception EX) {
			EX.printStackTrace();
			Send("-ERR");
		}
	}
}

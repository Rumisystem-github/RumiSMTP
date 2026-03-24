package com.rumisystem.rumi_smtp;

import java.io.File;

import com.rumisystem.rumi_smtp.Config.Account.ACMode;
import com.rumisystem.rumi_smtp.Config.MailBox.MBMode;

import su.rumishistem.rumi_java_lib.ArrayNode;
import su.rumishistem.rumi_java_lib.CONFIG;

public class Config {
	public static class SSL {
		public static String Cert = "";
		public static String Privatekey = "";
	}

	public static class SMTP {
		public static String[] whitelist_host = null;

		/**
		 * 配送
		 */
		public static int TransferPort = 0;
		/**
		 * 提出
		 */
		public static int SubmissionPort = 0;
	}

	public static class MailBox {
		public enum MBMode {
			File,
			RSV;
		};

		public static MBMode Mode = MBMode.File;
		public static String DirectoryPath = null;
	}

	public static class Account {
		public enum ACMode {
			File,
			RSV;
		};

		public static ACMode Mode = ACMode.File;
		public static String FilePath = null;

		public static String AccountServerHost = null;
		public static int AccountServerPort = 0;

		public static String SQLHost = null;
		public static int SQLPort = 0;
		public static String SQLDB = null;
		public static String SQLUser = null;
		public static String SQLPass = null;
	}

	public static void load() {
		ArrayNode data;

		File file = new File("Config.ini");
		if (!file.exists()) {
			System.out.println("Config.ini Not Found");
			System.exit(1);
			return;
		}

		try {
			data = new CONFIG().DATA;
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Config.ini Error.");
			return;
		}

		SSL.Cert = data.get("SSL").getData("CERT").asString();
		SSL.Privatekey = data.get("SSL").getData("PRIV").asString();

		SMTP.TransferPort = data.get("TRANSFER").getData("PORT").asInt();
		SMTP.SubmissionPort = data.get("SUBMISSION").getData("PORT").asInt();
		SMTP.whitelist_host = data.get("SUBMISSION").getData("WHITE_LIST").asString().split(",");

		if (data.get("MAILBOX").getData("MODE").asString().equals("FILE")) {
			MailBox.Mode = MBMode.File;
			MailBox.DirectoryPath = data.get("MAILBOX").getData("PATH").asString();
		}

		if (data.get("ACCOUNT").getData("MODE").asString().equals("FILE")) {
			Account.Mode = ACMode.File;
			Account.FilePath = data.get("ACCOUNT").getData("PATH").asString();
		} else {
			Account.Mode = ACMode.RSV;
			Account.AccountServerHost = data.get("ACCOUNT").getData("ACCOUNT_HOST").asString();
			Account.AccountServerPort = data.get("ACCOUNT").getData("ACCOUNT_PORT").asInt();
			Account.SQLHost = data.get("ACCOUNT").getData("SQL_HOST").asString();
			Account.SQLPort = data.get("ACCOUNT").getData("SQL_PORT").asInt();
			Account.SQLDB = data.get("ACCOUNT").getData("SQL_DB").asString();
			Account.SQLUser = data.get("ACCOUNT").getData("SQL_USER").asString();
			Account.SQLPass = data.get("ACCOUNT").getData("SQL_PASS").asString();
		}
	}
}

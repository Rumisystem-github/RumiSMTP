package com.rumisystem.rumi_smtp.Type;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import com.rumisystem.rumi_smtp.Config;
import com.rumisystem.rumi_smtp.Config.Account.ACMode;
import su.rumishistem.rumi_java_sql.*;

public class Account {
	private final long id;
	private final String userid;
	private final String host;
	private final boolean is_allow_login;
	private final String password;

	public Account(String userid, String host) {
		this.userid = userid;
		this.host = host;

		if (Config.Account.Mode == ACMode.File) {
			try {
				for (String line:new String(Files.readAllBytes(Path.of(Config.Account.FilePath))).split("\n")) {
					int index = line.indexOf(':');
					String address = line.substring(0, index);
					String password = line.substring(index + 1);
					if (address.equals("@")) {
						this.id = 0;
						this.is_allow_login = !password.equals("");
						this.password = password.toUpperCase();
						return;
					}
				}

				throw new NoSuchElementException("ﾕｰｻﾞｰ無し");
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new NoSuchElementException("ﾌｧｲﾙﾘｰﾄﾞｴﾗｰ");
			}
		} else {
			SQL.connect(Config.Account.SQLHost, String.valueOf(Config.Account.SQLPort), Config.Account.SQLDB, Config.Account.SQLUser, Config.Account.SQLPass);
			try {
				Map<String, SQLValue>[] select = SQL.new_auto_commit_connection().select_execute("SELECT * FROM `MAIL_USER` WHERE `ADDRESS` = ? AND `HOST` = ?;", new Object[]{
					userid, host
				});
				if (select.length == 0) {
					throw new NoSuchElementException("ﾕｰｻﾞｰ無し");
				}

				Map<String, SQLValue> row = select[0];
				this.id = row.get("ID").as_long();
				this.is_allow_login = row.get("LOGIN").as_boolean();
				if (this.is_allow_login) {
					this.password = row.get("PASSWORD").as_string().toUpperCase();
				} else {
					this.password = null;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new NoSuchElementException("SQLﾘｰﾄﾞｴﾗｰ");
			}
		}
	}

	public MailBox get_mailbox() throws IOException {
		return new MailBox(id, userid, host);
	}

	public boolean login(String input_password_hash) {
		input_password_hash = input_password_hash.toUpperCase();
		return input_password_hash.equals(password);
	}
}

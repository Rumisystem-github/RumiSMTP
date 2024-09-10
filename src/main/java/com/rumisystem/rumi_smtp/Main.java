package com.rumisystem.rumi_smtp;

import com.rumisystem.rumi_smtp.SMTP.TRANSFER_SERVER;

public class Main {
	public static void main(String[] args) {
		try {
			//TRANSFER TF = new TRANSFER("noreply@rumiserver.com", "RumiSaabaa", "irjitgd@gmail.com");
			//TF.SEND_MAIL("テスト", "メール送信テスト");
			
			TRANSFER_SERVER.Main();
		} catch (Exception EX) {
			EX.printStackTrace();
		}
	}
}

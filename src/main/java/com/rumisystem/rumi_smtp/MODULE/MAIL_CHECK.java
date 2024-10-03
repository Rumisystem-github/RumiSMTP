package com.rumisystem.rumi_smtp.MODULE;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MAIL_CHECK {
	private HashMap<String, String> MAIL_HEADER = new HashMap<>();

	public MAIL_CHECK(String MAILDATA) {
		for(String TEXT:MAILDATA.split("\n")) {
			//1以下ということはヘッダーではないので終了
			if (TEXT.split(":").length <= 1) {
				break;
			}
			
			//ヘッダーを読む
			String KEY = TEXT.split(":")[0].toUpperCase();
			String VAL = TEXT.split(":")[1];
			MAIL_HEADER.put(KEY.toUpperCase(), VAL);
		}
	}
	
	public String FROM() {
		if (MAIL_HEADER.get("FROM") != null) {
			String FROM_MAILADDRES = MAIL_HEADER.get("FROM");

			if (FROM_MAILADDRES.contains("<") && FROM_MAILADDRES.contains("<")) {
				Pattern REGEX = Pattern.compile("<(.*?)>");
				Matcher MATCHER = REGEX.matcher(FROM_MAILADDRES);
				
				if (MATCHER.find()) {
					return MATCHER.group(1);
				} else {
					return null;
				}
			} else {
				return FROM_MAILADDRES.replaceAll(" ", "");
			}
		} else {
			return null;
		}
	}
}

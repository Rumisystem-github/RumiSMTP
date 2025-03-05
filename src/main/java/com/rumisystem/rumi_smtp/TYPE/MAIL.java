package com.rumisystem.rumi_smtp.TYPE;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MAIL {
	private String FROM;
	private List<String> TO = new ArrayList<String>();
	private LinkedHashMap<String, String> HEADER_LIST = new LinkedHashMap<String, String>();
	private String TEXT = "";

	public void setFROM(String FROM) {
		this.FROM = FROM;
	}

	public String getFROM() {
		return FROM;
	}

	public void addTO(String TO) {
		this.TO.add(TO);
	}

	public int getTO_Length() {
		return TO.size();
	}

	public String getTO(int I) {
		return TO.get(I);
	}

	public void addHEADER(String KEY, String VAL) {
		HEADER_LIST.put(KEY.toUpperCase(), VAL);
	}

	public String getHeader(String KEY) {
		return HEADER_LIST.get(KEY);
	}

	public void appendHEADER(String KEY, String NEW_VAL) {
		if (HEADER_LIST.get(KEY.toUpperCase()) != null) {
			String OLD_VAL = HEADER_LIST.get(KEY.toUpperCase());

			//消し飛ばして入れる
			HEADER_LIST.remove(KEY.toUpperCase());
			HEADER_LIST.put(KEY.toUpperCase(), OLD_VAL + NEW_VAL);
		} else {
			throw new Error("指定されたキーは存在しませぬ");
		}
	}

	public void addTEXT(String TEXT) {
		this.TEXT += TEXT;
	}

	//本来は、MAIL_DATA = new MAIL();でやる予定だったが、Javaがクソなので出来ず
	public void RESET() {
		FROM = "";
		TO.clear();
		HEADER_LIST.clear();
		TEXT = "";
	}

	public String BUILD() {
		StringBuilder SB = new StringBuilder();

		//ヘッダーをセット
		for (String KEY:HEADER_LIST.keySet()) {
			SB.append(KEY + ": " + HEADER_LIST.get(KEY) + "\r\n");
		}

		SB.append("\r\n");

		//本文をセット(改行コードは既に入ってるので不要)
		SB.append(TEXT);

		return SB.toString();
	}
}

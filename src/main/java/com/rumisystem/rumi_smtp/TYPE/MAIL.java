package com.rumisystem.rumi_smtp.TYPE;

import java.util.LinkedHashMap;

public class MAIL {
	private LinkedHashMap<String, String> HeaderList = new LinkedHashMap<String, String>();
	private StringBuilder Text = new StringBuilder();

	public MAIL(String MailData) {
		String[] LineList = MailData.split("\r\n|\n");

		boolean ParseHeader = true;
		String CurrentHeaderKey = null;
		StringBuilder CurrentHeaderValue = new StringBuilder();
		for (String Line:LineList) {
			if (ParseHeader) {
				//ヘッダー終わり
				if (Line.isEmpty()) {
					ParseHeader = false;

					//ヘッダーリストに追加
					if (CurrentHeaderKey != null) {
						HeaderList.put(CurrentHeaderKey, CurrentHeaderValue.toString().trim());
					}
					continue;
				}

				if (!(Line.startsWith(" ") || Line.startsWith("\t"))) {
					//新たなヘッダー開始
					if (CurrentHeaderKey != null) {
						//ヘッダーリストに追加
						HeaderList.put(CurrentHeaderKey, CurrentHeaderValue.toString().trim());
					}

					int Colon = Line.indexOf(":");
					if (Colon > 0) {
						CurrentHeaderKey = Line.substring(0, Colon).trim();
						CurrentHeaderValue = new StringBuilder(Line.substring(Colon+1).trim());
					}
				} else {
					//折返し行(RGC5322)
					CurrentHeaderValue.append("\t").append(Line.trim());
				}
			} else {
				//本文
				Text.append(Line).append("\r\n");
			}
		}
	}

	public void setHeader(String KEY, String VAL) {
		HeaderList.put(KEY.toUpperCase(), VAL);
	}

	public String getHeader(String KEY) {
		return HeaderList.get(KEY);
	}

	public void appendHeader(String KEY, String NEW_VAL) {
		if (HeaderList.get(KEY.toUpperCase()) != null) {
			String OLD_VAL = HeaderList.get(KEY.toUpperCase());

			//消し飛ばして入れる
			HeaderList.remove(KEY.toUpperCase());
			HeaderList.put(KEY.toUpperCase(), OLD_VAL + NEW_VAL);
		} else {
			throw new Error("指定されたキーは存在しませぬ");
		}
	}

	public void addTEXT(String Text) {
		this.Text.append(Text);
	}

	public String BUILD() {
		StringBuilder SB = new StringBuilder();

		//ヘッダーをセット
		for (String KEY:HeaderList.keySet()) {
			SB.append(KEY + ": " + HeaderList.get(KEY) + "\r\n");
		}

		SB.append("\r\n");

		//本文をセット(改行コードは既に入ってるので不要)
		SB.append(Text);

		return SB.toString();
	}
}

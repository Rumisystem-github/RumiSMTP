package com.rumisystem.rumi_smtp.MODULE;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.directory.*;

import com.fasterxml.jackson.annotation.Nulls;
import com.rumisystem.rumi_java_lib.LOG_PRINT.LOG_TYPE;
import com.rumisystem.rumi_smtp.TYPE.SPFAll;
import com.rumisystem.rumi_smtp.TYPE.SPFAllowIP;
import com.rumisystem.rumi_smtp.TYPE.SPFContent;
import com.rumisystem.rumi_smtp.TYPE.SPFSetting;
import javax.naming.*;

import static com.rumisystem.rumi_java_lib.LOG_PRINT.Main.LOG;
import static com.rumisystem.rumi_smtp.MODULE.DNSResolve.GetDNS;

public class DMARCChecker {
	public static boolean Check(String Domain, String IP) {
		try {
			boolean SPF_RESULT = CheckSPF(Domain, IP);

			//TODO:DMARCの設定を読み込む

			LOG(LOG_TYPE.OK, "DMARC完了");
			return SPF_RESULT;
		} catch (Exception EX) {
			EX.printStackTrace();
			return false;
		}
	}

	private static boolean CheckSPF(String Domain, String IP) throws NamingException, UnknownHostException {
		SPFSetting Setting = GetSPF(Domain);
		List<SPFContent> ContentList = Setting.GetContent();

		for (SPFContent SPF:ContentList) {
			switch (SPF.GetAllowIP()) {
				case IP:{
					for (String ROW:SPF.GetIPList()) {
						LOG(LOG_TYPE.OK, "[CheckSPF]IPチェック:" + ROW);
						String BaseIP = ROW.split("/")[0];
						int PrefixLength = Integer.parseInt(ROW.split("/")[1]);

						long BaseIPLong = ipToLong(BaseIP);
						long Mask = -1L << (32 - PrefixLength);
						long IPLong = ipToLong(IP);

						if ((IPLong & Mask) == (BaseIPLong & Mask)) {
							return true;
						} else if (Setting.GetAll() == SPFAll.SoftFail) {
							return true;
						}
					}
					break;
				}

				case A: {
					for (String ROW:SPF.GetIPList()) {
						LOG(LOG_TYPE.OK, "[CheckSPF]Aチェック:" + ROW);

						if (ROW.equals(IP)) {
							return true;
						} else if (Setting.GetAll() == SPFAll.SoftFail) {
							return true;
						}
					}
					break;
				}

				case MX: {
					for (String ROW:SPF.GetIPList()) {
						String MX_A_Recorde = GetDNS(ROW, "A").get(0);

						if (MX_A_Recorde != null) {
							LOG(LOG_TYPE.OK, "[CheckSPF]MXレコードチェック:" + ROW);
							if (MX_A_Recorde.equals(IP)) {
								return true;
							} else if (Setting.GetAll() == SPFAll.SoftFail) {
								return true;
							}
						}
					}
					break;
				}
			}
		}

		//此処に来たならSPF失敗だ
		return false;
	}

	private static SPFSetting GetSPF(String Domain) throws NamingException {
		SPFAll all = SPFAll.Fail;
		List<SPFContent> ContentList = new ArrayList<SPFContent>();

		for (String ROW:GetDNS(Domain, "TXT")) {
			if (ROW.contains("v=spf1 ") && !ROW.contains(" redirect=")) {
				SPFAllowIP AllowIP = null;
				List<String> IPList = new ArrayList<String>();

				if (ROW.contains(" ~all")) {
					all = SPFAll.SoftFail;
				} else if (ROW.contains(" -all")) {
					all = SPFAll.Fail;
				}

				//IP
				Matcher IPv4Match = Pattern.compile("(?:ip4|ip6):(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\/\\d{1,2})").matcher(ROW);
				while (IPv4Match.find()) {
					LOG(LOG_TYPE.INFO, "[GetSPF]IP:" + IPv4Match.group(1));

					AllowIP = SPFAllowIP.IP;
					IPList.add(IPv4Match.group(1));
				}

				//Aレコード
				Matcher AMatch = Pattern.compile(" a ").matcher(ROW);
				while (AMatch.find()) {
					AllowIP = SPFAllowIP.A;
					//Aレコードを取得する
					List<String> DNSA = GetDNS(Domain, "A");
					if (DNSA.get(0) != null) {
						IPList.add(DNSA.get(0));
					} else {
						//※ダミー
						IPList.add("0.0.0.0");
					}

					LOG(LOG_TYPE.INFO, "[GetSPF]Aレコード:" + Domain);
				}

				//Aレコード(参照せよ)
				Matcher ASanshouMatch = Pattern.compile(" a:(.*) ").matcher(ROW);
				while (ASanshouMatch.find()) {
					AllowIP = SPFAllowIP.A;
					//参照しろと言われているAレコードを参照する
					List<String> DNSA = GetDNS(ASanshouMatch.group(1), "A");
					if (DNSA.get(0) != null) {
						IPList.add(DNSA.get(0));
					} else {
						//※ダミー
						IPList.add("0.0.0.0");
					}

					LOG(LOG_TYPE.INFO, "[GetSPF]Aレコード:" + ASanshouMatch.group(1));
				}

				//MXレコード
				Matcher MXMatch = Pattern.compile(" mx ").matcher(ROW);
				while (MXMatch.find()) {
					AllowIP = SPFAllowIP.MX;
					//MXレコードを取得する
					List<String> DNSA = GetDNS(Domain, "MX");
					if (DNSA.get(0) != null) {
						IPList.add(DNSA.get(0).replaceAll("^\\d+\\s+", ""));
					} else {
						//※ダミー
						IPList.add("example.com");
					}

					LOG(LOG_TYPE.INFO, "[GetSPF]MXレコード:" + Domain);
				}

				//MXレコード(参照せよ)
				Matcher MXSanshouMatch = Pattern.compile(" mx:(.*) ").matcher(ROW);
				while (MXSanshouMatch.find()) {
					AllowIP = SPFAllowIP.MX;
					//参照しろと言われているAレコードを参照する
					List<String> DNSA = GetDNS(MXSanshouMatch.group(1), "MX");
					if (DNSA.get(0) != null) {
						IPList.add(DNSA.get(0).replaceAll("^\\d+\\s+", ""));
					} else {
						//※ダミー
						IPList.add("example.com");
					}

					LOG(LOG_TYPE.INFO, "[GetSPF]MXレコード:" + MXSanshouMatch.group(1));
				}

				//インクルード
				Matcher IncludeMatch = Pattern.compile(" include:(\\S*)").matcher(ROW);
				while (IncludeMatch.find()) {
					for (SPFContent SPF:GetSPF(IncludeMatch.group(1)).GetContent()) {
						ContentList.add(SPF);
					}

					LOG(LOG_TYPE.INFO, "[GetSPF]インクルード:" + IncludeMatch.group(1));
				}

				//値が有ったなら追加しましょう
				if (AllowIP != null && IPList.size() != 0) {
					ContentList.add(new SPFContent(AllowIP, IPList));
				}
			} else if (ROW.contains(" redirect=")) {
				//リダイレクト
				Matcher RedirectDomainMatch = Pattern.compile(" redirect=(.*)").matcher(ROW);
				if (RedirectDomainMatch.find()) {
					String RedirectDomain = RedirectDomainMatch.group(1).replace("\"", "");
					LOG(LOG_TYPE.INFO, "[GetSPF]リダイレクト:" + RedirectDomain);
					return GetSPF(RedirectDomain);
				}
			}
		}

		LOG(LOG_TYPE.OK, "[GetSPF]Done");

		return new SPFSetting(ContentList, all);
	}

	public static long ipToLong(String IP) throws UnknownHostException {
		byte[] BYTES = InetAddress.getByName(IP).getAddress();
		long RESULT = 0;
		for (byte B : BYTES) {
			RESULT = (RESULT << 8) | (B & 0xFF);
		}
		return RESULT & 0xFFFFFFFFL;
	}
}

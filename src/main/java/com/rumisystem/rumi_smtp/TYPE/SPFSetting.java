package com.rumisystem.rumi_smtp.TYPE;

import java.util.List;

public class SPFSetting {
	private List<SPFContent> Content = null;
	private SPFAll AllSetting;

	public SPFSetting(List<SPFContent> Content, SPFAll AllSetting) {
		this.Content = Content;
		this.AllSetting = AllSetting;
	}

	public List<SPFContent> GetContent() {
		return Content;
	}

	public SPFAll GetAll() {
		return AllSetting;
	}
}

/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.helper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.boubei.tss.framework.persistence.pagequery.MacrocodeQueryCondition;
import com.boubei.tss.util.EasyUtils;

/**
 * 站内信查询条件对象
 */
public class MessageQueryCondition extends MacrocodeQueryCondition {

	private Long   receiverId;
	private String sender;  // 发件人
	private String title;   // 站内信标题
	private String content; // 站内信内容
	private Date searchTime1;
	private Date searchTime2;
	private String category;
	private String level;
	private String read;

	public Map<String, Object> getConditionMacrocodes() {
		Map<String, Object> map = new HashMap<String, Object>(); // 无需关心域，消息都是按接收人过滤
		
		map.put("${receiverId}", " and o.receiverId = :receiverId");
		map.put("${sender}", " and o.sender = :sender");
		map.put("${title}", " and o.title like :title");
		map.put("${content}", " and o.content like :content");
		map.put("${searchTime1}", " and o.sendTime >= :searchTime1");
		map.put("${searchTime2}", " and o.sendTime <= :searchTime2");
		map.put("${category}", " and o.category = :category");
		map.put("${level}", " and o.level = :level");
		
		if (!EasyUtils.isNullOrEmpty(read)) {
			if("no".equals(read)){
				map.put("${read}", " and 'no' = :read and o.readTime is null");
			} else {
				map.put("${read}", " and 'yes' = :read and o.readTime is not null");
			}
		}
		
		return map;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getTitle() {
		return wrapLike(title);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return wrapLike(content);
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getSearchTime1() {
		return searchTime1;
	}

	public void setSearchTime1(Date searchTime1) {
		this.searchTime1 = searchTime1;
	}

	public Date getSearchTime2() {
		return searchTime2;
	}

	public void setSearchTime2(Date searchTime2) {
		this.searchTime2 = searchTime2;
	}

	public Long getReceiverId() {
		return receiverId;
	}

	public void setReceiverId(Long receiverId) {
		this.receiverId = receiverId;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getRead() {
		return read;
	}

	public void setRead(String read) {
		this.read = read;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}
}
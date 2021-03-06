/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.display.tree;


/** 
 * <p> 单层树节点。 </p> 
 * 
 * TreeAttributesMap包含了树节点的属性信息，如name、path等
 */
public interface ITreeNode {

	/**
	 * 获去节点属性Map
	 * 
	 * @return TreeAttributesMap
	 */
	TreeAttributesMap getAttributes();
}

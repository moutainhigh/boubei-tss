/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.mvc;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.web.display.SuccessMessageEncoder;
import com.boubei.tss.framework.web.display.XmlPrintWriter;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.xmlhttp.XmlHttpEncoder;
 
/** 
 *  所有的Action必须继承此Action，以统一的方式输出响应内容。
 */
public abstract class BaseActionSupport {
    
    protected Logger log = Logger.getLogger(this.getClass());
    
    /**
     * 约定新增对象时前台传过来ID统一为 -10；
     */
    public static final Long DEFAULT_NEW_ID = -10L; 

    /** 数据流方式向客户端返回数据 */
    protected void print(Object xml) {
        getWriter().append(xml);
    }

    /** 获取输出流 */
    protected XmlPrintWriter getWriter() {
        /* 初始化数据输出流  */
        HttpServletResponse response = Context.getResponse();
        response.setContentType("text/html;charset=UTF-8");
        
        PrintWriter writer = null;
        try {
        	writer = response.getWriter();
        } catch (Exception e) {
        }
        return new XmlPrintWriter(writer);
    }
    
    /**
     * 在action调用service进行保存操作后执行
     * @param isNew 
     *            是否新增节点。如果是新增，则返回新增的树形式节点。    
     * @param returnObj
     * @param treeName
     */
    protected void doAfterSave(boolean isNew, Object returnObj, String treeName){        
        doAfterSave(isNew, returnObj, treeName, EX.DEFAULT_SUCCESS_MSG);
    }
    
    protected void doAfterSave(boolean isNew, Object returnObj, String treeName, String successMsg){        
        if(isNew) {
            List<Object> list = new ArrayList<Object>();
            list.add(returnObj);       
            TreeEncoder encoder = new TreeEncoder(list);
            encoder.setNeedRootNode(false);
            print(treeName, encoder);
        } 
        else {
        	printSuccessMessage(successMsg);
        }
    }
    
    /**
     * 向客户端输出信息
     * @param returnDataName
     * @param value
     */
    protected void print(String returnDataName, Object value){
        XmlHttpEncoder xmlHttpEncoder = new XmlHttpEncoder();
        xmlHttpEncoder.put(returnDataName, value);
        xmlHttpEncoder.print(getWriter());
    }
    
    /**
     * 向客户端输出信息
     * @param returnDataNames
     * @param values
     */
    protected void print(String[] returnDataNames, Object[] values){
        XmlHttpEncoder xmlHttpEncoder = new XmlHttpEncoder();
        for(int i = 0; i < returnDataNames.length; i++){
            xmlHttpEncoder.put(returnDataNames[i], values[i]);
        }       
        xmlHttpEncoder.print(getWriter());
    }
    
    /**
     * 向客户端输出一条成功信息
     */
    protected void printSuccessMessage(){
        printSuccessMessage(EX.DEFAULT_SUCCESS_MSG);
    }
    
    /**
     * 向客户端输出一条成功信息
     */
    protected void printSuccessMessage(String str){
    	if( Context.getRequestContext().isXmlhttpRequest() ) { // tssJS.ajax
    		new SuccessMessageEncoder(str).print(getWriter());
		} else {
			getWriter().append("{\"result\": \"" +str+ "\"}"); // not tssJS
		}
    }
    
    protected void printJSON(String str) {
    	getWriter().append("{\"result\": \"" +str+ "\"}");
    }
    
    /** 生成分页信息 */
    protected String generatePageInfo(int totalRows, int page, int pagesize, int currentPageRows) {
        int totalPages = totalRows / pagesize;
        if(totalRows % pagesize != 0){
            totalPages ++;
        }       
        
        StringBuffer sb = new StringBuffer("<PageList totalpages=\"").append(totalPages).append("\" totalrecords=\"");
        sb.append(totalRows).append("\" currentpage=\"").append(page).append("\" pagesize=\"").append(pagesize);
        sb.append("\" currentpagerows=\"").append(currentPageRows).append("\" />");
        return sb.toString();
    }
 
}


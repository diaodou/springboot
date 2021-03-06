package com.reptile.service.socialsecurity;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.reptile.model.FormBean;
import com.reptile.model.PersonAccount;
import com.reptile.model.PersonInfo;
import com.reptile.springboot.Scheduler;
import com.reptile.util.ConstantInterface;
import com.reptile.util.PushState;
import com.reptile.util.Resttemplate;
import com.reptile.util.WebClientFactory;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by HotWong on 2017/5/2 0002.
 */
@Service("socialSecurityService")
public class SocialSecurityService {
    private final static String loginUrl="http://117.36.52.39/sxlssLogin.jsp";
    private final static String infoUrl="http://117.36.52.39/personInfoQuery.do";
    private final static String detailsUrl="http://117.36.52.39/personAccountQuery.do";
   // private final static String detailsUrl="http://117.36.52.39/paymentQuery.do";
    
    private Logger logger= LoggerFactory.getLogger(SocialSecurityService.class);
    public Map<String,Object> login(FormBean bean,String idCardNum){
        Map<String,Object> map=new HashMap<String,Object>();
        Map<String,Object> data=new HashMap<String,Object>();
        try {
            if(bean.getUserName()==null || bean.getUserName().equals("")){
                throw new NullPointerException("请输入姓名!");
            }
            if(bean.getUserId()==null){
                throw new NullPointerException("请输入身份证号!");
            }
            final WebClient webClient = new WebClientFactory().getWebClient();
//            final WebClient webClient = new WebClient(BrowserVersion.CHROME, Scheduler.ip,Scheduler.port);
//            webClient.getOptions().setCssEnabled(false);// 禁用css支持
//            webClient.getOptions().setThrowExceptionOnScriptError(false);// 忽略js异常
//            webClient.getOptions().setTimeout(8000); // 设置连接超时时间
            HtmlPage loginPage=null;
            try{
                loginPage = webClient.getPage(loginUrl);
            }catch (Exception e){
                Scheduler.sendGet(Scheduler.getIp);
            }
            HtmlForm form = loginPage.getForms().get(0);
            HtmlTextInput userId = form.getInputByName("uname");
            HtmlTextInput userName = form.getInputByName("aac003");
            HtmlTextInput verifyCode = (HtmlTextInput) loginPage.getElementById("PSINPUT");
            HtmlTextInput checkCode = (HtmlTextInput) loginPage.getElementById("checkCode");
            HtmlImageInput submit = form.getInputByName("Icon2");
            userId.setValueAttribute(bean.getUserId().toString().trim());
            userName.setValueAttribute(bean.getUserName().trim());
            verifyCode.setValueAttribute(checkCode.getValueAttribute());
            HtmlPage resultPage=(HtmlPage)submit.click();
            String result=resultPage.asText();
            if(result.indexOf("公民身份证号码")!=-1){
                throw new NullPointerException("身份证号码或姓名不正确，请重新输入!");
            }
            PushState.state(idCardNum, "socialSecurity", 100);
            HtmlPage infoPage = webClient.getPage(infoUrl);
            HtmlTable infoTable=(HtmlTable)infoPage.getElementsByTagName("table").get(0);
            PersonInfo person=new PersonInfo();
            person.setUserName(infoTable.getCellAt(1, 1).asText().trim());
            person.setPersonStatus(infoTable.getCellAt(1, 3).asText().trim());
            person.setPayStatus(infoTable.getCellAt(1, 5).asText().trim());
            person.setCreateTime(infoTable.getCellAt(2, 1).asText().trim());
            person.setAmount(infoTable.getCellAt(2, 3).asText().trim());
            person.setAgencyName(infoTable.getCellAt(3, 1).asText().trim());
            HtmlPage detailsPage = webClient.getPage(detailsUrl);
            List<PersonAccount> accountList=new ArrayList<PersonAccount>();
            HtmlTable accountTable=(HtmlTable)detailsPage.getElementsByTagName("table").get(0);
            List<HtmlTableRow> accountRows=accountTable.getRows();
            String payNum="";
            for (int i = 2; i < accountRows.size(); i++) {
                PersonAccount pa=new PersonAccount();
                
                pa.setYear(accountTable.getCellAt(i, 0).asText().trim());
                payNum=  accountTable.getCellAt(i, 1).asText().trim();
                pa.setPayMonthNumber(payNum);
                if(payNum.equals("0")){
                	
               	 pa.setPayBaseNumber(accountTable.getCellAt(i, 2).asText().trim());
                 pa.setPersonPayAmount(accountTable.getCellAt(i, 3).asText().trim());
                 pa.setCompanyPayAmount(accountTable.getCellAt(i, 4).asText().trim());
               }else{
            	   
            	   pa.setPayBaseNumber( Double.valueOf(accountTable.getCellAt(i, 2).asText().trim())/ Double.valueOf(payNum)+"");
            	   pa.setPersonPayAmount(Double.valueOf(accountTable.getCellAt(i, 3).asText().trim())/ Double.valueOf(payNum)+"");
            	   pa.setCompanyPayAmount(Double.valueOf(accountTable.getCellAt(i, 4).asText().trim())/ Double.valueOf(payNum)+"");
               }
              
               // pa.setPayBaseNumber(accountTable.getCellAt(i, 2).asText().trim());
               // 
                //pa.setCompanyPayAmount(accountTable.getCellAt(i, 4).asText().trim());
                pa.setLastYearPayMonthNumber(accountTable.getCellAt(i, 5).asText().trim());
                accountList.add(pa);
            }
            person.setAccountList(accountList);
            webClient.close();
            data.put("person",person);
            map.put("ResultInfo","查询成功");
            map.put("ResultCode","0000");
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
            map.put("userId",idCardNum);
            map.put("queryDate",sdf.format(new Date()).toString());
            map.put("data",data);
            map.put("city",bean.getCityCode());
//            HttpUtils.sendPost("http://192.168.3.16:8089/HSDC/person/socialSecurity", JSONObject.fromObject(map).toString());
            //ludangwei 2017-08-11
            Resttemplate resttemplate = new Resttemplate();
            map = resttemplate.SendMessageCredit(JSONObject.fromObject(map), ConstantInterface.port+"/HSDC/person/socialSecurity");


            if(map!=null&&"0000".equals(map.get("ResultCode").toString())){
            	PushState.state(idCardNum, "socialSecurity", 300);
                map.put("errorInfo","推送成功");
                map.put("errorCode","0000");
            }else{
            	PushState.state(idCardNum, "socialSecurity", 200);
                map.put("errorInfo","推送失败");
                map.put("errorCode","0001");
            }

        }catch (NullPointerException e) {
        	//PushState.state(idCardNum, "socialSecurity", 200);
            logger.warn(e.getMessage()+"     mrlu");
            map.put("ResultInfo","服务器繁忙，请稍后再试！");
            map.put("ResultCode","0001");
            map.put("errorInfo","服务器繁忙，请稍后再试！");
            map.put("errorCode","0001");
        } catch (Exception e) {
        	//PushState.state(idCardNum, "socialSecurity", 200);
            logger.warn(e.getMessage()+"     mrlu");
            map.clear();
            data.clear();
            map.put("ResultInfo","服务器繁忙，请稍后再试！");
            map.put("ResultCode","0002");
            map.put("errorInfo","服务器繁忙，请稍后再试！");
            map.put("errorCode","0002");
        }
        map.put("data",data);
        return map;
    }

}

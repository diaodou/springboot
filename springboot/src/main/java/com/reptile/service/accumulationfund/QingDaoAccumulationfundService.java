package com.reptile.service.accumulationfund;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.reptile.util.Dates;
import com.reptile.util.MyCYDMDemo;
import com.reptile.util.Resttemplate;
import com.reptile.util.WebClientFactory;

import net.sf.json.JSONObject;
/**
 * 
 * @ClassName: QingDaoAccumulationfundService  
 * @Description: TODO  
 * @author: lusiqin
 * @date 2018年1月2日  
 *
 */
@Service
public class QingDaoAccumulationfundService {

	private Logger logger = LoggerFactory.getLogger(QingDaoAccumulationfundService.class);

    public Map<String, Object> getDetailMes(HttpServletRequest request, String idCard,String passWord, String cityCode, String idCardNum) {
        Map<String, Object> map = new HashMap<>(16);
        Map<String, Object> dataMap = new HashMap<>(16);
        WebClient webClient = new WebClientFactory().getWebClient();
        try {
            HtmlPage page = webClient.getPage("http://219.147.7.52:89/grptLogin.htm");
            //身份证号
            page.getElementById("name").setAttribute("value", idCard); 
            //密码
            page.getElementByName("password").setAttribute("value", passWord);
            //验证码grptlogin_yzm
		    HtmlImage image=   (HtmlImage) page.getElementById("grptlogin_yzm");
			BufferedImage read = image.getImageReader().read(0);
			ImageIO.write(read, "png", new File("C://aa.png"));
			Map<String, Object> code = MyCYDMDemo.getCode("C://aa.png");
			String vecCode = code.get("strResult").toString();
			page.getElementByName("yzm").setAttribute("value", vecCode);
			//登录
			HtmlPage  loginpage= (HtmlPage) page.executeJavaScript("$(\".log_in\").click()").getNewPage();
			loginpage= (HtmlPage) page.executeJavaScript("$(\".log_in\").click()").getNewPage();
            Thread.sleep(5000);
            String str="退出系统";
            if(!(loginpage.asXml().contains(str))) {
            	dataMap.put("errorCode", "0002");
            	dataMap.put("errorInfo", " 登录失败");
	        	System.out.println("登录失败");
            }else {
            	System.out.println("登录成功");
           
            	//基本信息
            Map<String,Object> parseBaseInfo=parseBaseInfo(webClient);
            
            List<Map<String,Object>> flows=flows(webClient);
         
            HtmlPage loanspage= webClient.getPage("http://219.147.7.52:89/GR/dkcx/dkhtxx.htm?_=1511765035077");
            Map<String, Object> loans=loans(loanspage);
                
            //个人信息
                dataMap.put("basicInfos", parseBaseInfo);
                //流水
                dataMap.put("flows", flows);
                //贷款信息  无贷款信息测试
                dataMap.put("loans", loans);
                map.put("data", dataMap);
                map.put("userId", idCardNum);
                map.put("city", cityCode);
                map.put("cityName", "青岛");
                map.put("insertTime", Dates.currentTime());
            }
          //数据推送
          map = new Resttemplate().SendMessage(map,"http://192.168.3.16:8089/HSDC/person/accumulationFund");
        } catch (Exception e) {
            logger.warn("青岛公积金认证失败", e);
            e.printStackTrace();
            map.put("errorCode", "0002");
            map.put("errorInfo", "系统繁忙，请稍后再试");
        } finally {
            if (webClient != null) {
                webClient.close();
            }
        }
        return map;
    }
    
  	private static Map<String, Object> parseBaseInfo(WebClient webClient) {
  		Map<String,Object> baseInfo = new HashMap<String, Object>(16);	
  		UnexpectedPage basicInfos;
		try {
			basicInfos = webClient.getPage("http://219.147.7.52:89/Controller/GR/gjcx/gjjzlcx.ashx?dt=1511506147131");
    	String result=basicInfos.getWebResponse().getContentAsString();
    	JSONObject obj = JSONObject.fromObject(result);
    	//用户姓名
	    	baseInfo.put("name",obj.get("hm"));
	    	//身份证号码
			baseInfo.put("idCard",obj.get("sfz"));
			//单位公积金账号
			baseInfo.put("companyFundAccount","");
			//个人公积金账号
			baseInfo.put("personFundAccount","");
			//公司名称
			baseInfo.put("companyName","");
			//个人公积金卡号
			baseInfo.put("personFundCard","");
			//缴费基数
			baseInfo.put("baseDeposit", "");
			//公司缴费比例
			baseInfo.put("companyRatio",obj.get("dwjcbl"));
			//个人缴费比例
			baseInfo.put("personRatio",obj.get("grjcbl"));
			//个人缴费金额
			baseInfo.put("personDepositAmount",obj.get("gryhjje"));
			//公司缴费金额
			baseInfo.put("companyDepositAmount",obj.get("dwyhjje"));
			//最后缴费日期
			baseInfo.put("lastDepositDate","");
			//余额
			baseInfo.put("balance",obj.get("zhye"));
			//状态（正常）
			baseInfo.put("status",obj.get("zt"));
		} catch (Exception e) {
			e.printStackTrace();
		} 
		 
		return baseInfo;
}
	private  List<Map<String,Object>> flows(WebClient webClient) {
		List<Map<String,Object>> flows = new ArrayList<Map<String,Object>>();
		String url="http://219.147.7.52:89/Controller/GR/gjcx/gjjmx.ashx?dt=1511512508637&transDateBegin=1900-01-01&transDateEnd=2017-11-24&page=1&rows=20&sort=mxbc&order=desc";
    	WebRequest requests;
		try {
		requests = new WebRequest(new URL(url));
		requests.setHttpMethod(HttpMethod.GET);
		TextPage flowspage=webClient.getPage(requests);
		String result=flowspage.getWebResponse().getContentAsString();
		String str="连接服务器错误！";
		if(str.equals(result)) {
			System.out.println("连接服务器错误！");
		}else {
		JSONObject obj = JSONObject.fromObject(result);
		List<Map<String,Object>> lis1 = (List<Map<String, Object>>) obj.get("rows");
		for (int j = lis1.size()-1; j >1 ; j--) {
			Map<String,Object>  mapp= lis1.get(j); 
			System.out.println("mapp---"+mapp.get("fse"));
			//发生额
			mapp.get("fse");
			mapp.get("jylxzh");
			//交易日期
			mapp.get("jyrq");
			mapp.get("mxbc");
			//所属年月
			mapp.get("ssny");
			mapp.get("ye");
			//类型
			mapp.get("zymzh");
				if((mapp.get("zymzh")+"").contains("汇缴") || (mapp.get("zymzh")+"").contains("补缴")){
					Map<String,Object> item = new HashMap<String, Object>(16);
					//操作时间（2015-08-19）
					item.put("operatorDate",mapp.get("jyrq"));
					//操作金额
				item.put("amount",mapp.get("fse"));
					String type = "";
					if((mapp.get("zymzh")+"").contains("汇缴")){
						type = "汇缴";
					}else{
						type = "补缴";
					}
					//操作类型
					item.put("type",type);
					//业务描述（汇缴201508公积金）
					item.put("bizDesc",type +  (mapp.get("ssny"))+"公积金");
					//单位名称
					item.put("companyName","");
					//缴费月份
					item.put("payMonth",mapp.get("ssny"));
					flows.add(item);
				}
			}
		}} catch (Exception e) {
			e.printStackTrace();
		}
   		return flows;
   	}
   
    /**
     * 获取贷款信息
     * @param loanspage
     * @return
     * @throws FailingHttpStatusCodeException
     * @throws MalformedURLException
     * @throws IOException
     */
    public Map<String,Object> loans(HtmlPage loanspage) throws Exception{
    	
    	Map<String,Object> loans = new HashMap<String, Object>(16);
		String table = loanspage.getElementsByTagName("table").get(0).asXml();
		List<List<String>> list = table(table);
		if(list.size()>0) {
			//贷款账号
		loans.put("loanAccNo", "");
		//贷款期限
		loans.put("loanLimit", list.get(2).get(1));
		//开户日期
		loans.put("openDate",   list.get(1).get(0));
		//贷款总额
		loans.put("loanAmount",   (list.get(2).get(0)));
		//最近还款日期
		loans.put("lastPaymentDate", list.get(4).get(0));
		//还款状态
		loans.put("status",  "");
		//贷款余额
		loans.put("loanBalance",   (list.get(3).get(1)));
		//还款方式
		loans.put("paymentMethod", list.get(5).get(0));
		} 
    	return loans;
    	
    }
    /**
	 * 解析table
	 * @param xml
	 * @return
	 */
	 private static List<List<String>>  table(String xml){ 
		 
		 Document doc = Jsoup.parse(xml);
		 Elements trs = doc.select("table").select("tr");  
		 
		 List<List<String>> list = new ArrayList<List<String>>();
		 for (int i = 0; i < trs.size(); i++) {
			 Elements tds = trs.get(i).select("td");
			 List<String> item = new ArrayList<String>();
			 for (int j = 0; j < tds.size(); j++){
				 String txt = tds.get(j).text().replace(" ", "").replace(" ", "").replace("元", "");
				 item.add(txt);
			 }  
			 list.add(item);
		 }
		 
		return list;	
    } 
    /**
	   * 获取图形验证码
	   * */
	  public Map<String, Object> getImageCode(HttpServletRequest request){
		  Map<String, Object> map = new HashMap<String, Object>(16);
		  Map<String,String> mapPath=new HashMap<String, String>(16);
	        HttpSession session = request.getSession();
	        
	        WebClient webClient=new WebClientFactory().getWebClient();
	        //=============图形验证码=====================
	        try {
			HtmlPage page=webClient.getPage(new URL("http://222.172.223.90:8081/kmnbp/"));
			Thread.sleep(500);
			UnexpectedPage page7 = webClient.getPage("http://222.172.223.90:8081/kmnbp/vericode.jsp");

           String path = request.getServletContext().getRealPath("/vecImageCode");
           File file = new File(path);
           if (!file.exists()) {
               file.mkdirs();
           }
           String fileName = "XZCode" + System.currentTimeMillis() + ".png";
           BufferedImage bi = ImageIO.read(page7.getInputStream());
           ImageIO.write(bi, "png", new File(file, fileName));
           
           mapPath.put("imagePath", request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/vecImageCode/" + fileName);
           map.put("data",mapPath);
           map.put("errorCode", "0000");
           map.put("errorInfo", "验证码获取成功");
           session.setAttribute("KM-WebClient", webClient);
          /// session.setAttribute("XZ-page", page);
	        }catch (Exception e) {
	        	logger.warn("昆明住房公积金",e);
       		map.put("errorCode", "0001");
              map.put("errorInfo", "网络连接异常!");
  			e.printStackTrace();
			}
		return map; 
}
	  /**
		  * 去除千分符
		  * @param amount
		  * @return
		  * @throws ParseException
		  */
		 private static String numFormat(String amount) throws ParseException{
			 return amount.replace(",", "");
		 }	 
		 /**
		  * 2007-10-10  去- （20171010）
		  * @param amount
		  * @return
		  * @throws ParseException
		  */
		 private static String numFormatyear(String amount) throws ParseException{
			 return amount.replace("-", "");
		 }
}
 

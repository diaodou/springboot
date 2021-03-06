package com.reptile.contorller.chinatelecom;

import com.reptile.service.chinatelecom.ShanXiTelecomService;
import com.reptile.util.CustomAnnotation;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


/**
 * 陕西电信controller
 *
 * @author mrlu
 * @date 2016/10/31
 */
@Controller
@RequestMapping("ShanXiTelecomController")
public class ShanXiTelecomController {
    @Autowired
    private ShanXiTelecomService service;

    @CustomAnnotation
    @ResponseBody
    @RequestMapping(value = "TelecomLogin", method = RequestMethod.POST)
    @ApiOperation(value = "陕西电信登录", notes = "电信登录")
    public Map<String, Object> telecomLogin(HttpServletRequest req, @RequestParam("userPhone") String userPhone,
                                            @RequestParam("userPassword") String userPassword,@RequestParam("longitude") String longitude,
                                            @RequestParam("latitude") String latitude,@RequestParam("UUID")String uuid) throws Exception {
       System.out.println("--------");
    	return service.telecomLogin(req, userPhone,userPassword,longitude,latitude,uuid);
    }
}

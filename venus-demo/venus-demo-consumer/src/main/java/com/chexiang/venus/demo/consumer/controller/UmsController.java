package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.model.Echo;
import com.chexiang.venus.demo.provider.service.EchoService;
import com.meidusa.venus.Result;
import com.saic.framework.message.Mail;
import com.saic.framework.message.UniMessageService;
import com.saic.framework.service.mail.api.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HelloController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/ums")
public class UmsController {

    private static Logger logger = LoggerFactory.getLogger(UmsController.class);

    @Autowired
    UniMessageService uniMessageService;

    @RequestMapping("/sendMail/{param}")
    public Result sendMail(@PathVariable String param){
        try {
            sendMail();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new Result("OK");
    }


    public void sendMail() throws IOException {
        String appId="po";
        String schemaId="023";
        String[] to=new String[] { "weijiaqiang@chexiang.com" };

        Map<String,String> params=new HashMap<>();
        params.put("name", "nbxq");
        params.put("userName", "nbxq-1");
        params.put("password","123456");
        Mail mail=new Mail(appId,schemaId);
        mail.setParams(params);
        mail.setTo(to);
        mail.setSender("nbxq@chexiang.com");
        List<MailService.Attachment> attachments=new ArrayList<>();
        MailService.Attachment attach=new MailService.Attachment();
        attach.setFileName("appname.txt");
        File file=new File("D:\\appname.txt");
        InputStream input = new FileInputStream(file);
        byte[] byt = new byte[input.available()];
        input.read(byt);
        attach.setData(byt);
        attachments.add(attach);
        mail.setAttachments(attachments);
        //String encode=JSON.toJSONString(mail);
        //Mail mail2=JSON.toJavaObject(encode, Mail.class)
        System.err.println();
        //mail.setSubject("nbxqtet");
        uniMessageService.sendMail(mail); //使用UMS服务发送mail
    }

}

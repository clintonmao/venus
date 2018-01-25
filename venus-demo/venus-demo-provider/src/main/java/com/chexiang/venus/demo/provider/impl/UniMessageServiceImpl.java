package com.chexiang.venus.demo.provider.impl;

import com.saic.framework.message.Mail;
import com.saic.framework.message.Sms;
import com.saic.framework.message.UniMessageService;
import com.saic.framework.service.mail.exception.MailValidateException;
import com.saic.framework.service.sms.exception.SMSValidateException;
import org.springframework.stereotype.Component;

/**
 * Created by Zhangzhihua on 2018/1/25.
 */
@Component("uniMessageService")
public class UniMessageServiceImpl implements UniMessageService {

    @Override
    public void sendMail(Mail mail) throws MailValidateException {
        System.out.println(mail);
    }

    @Override
    public void sendSms(Sms sms) throws SMSValidateException {
        System.out.println(sms);
    }
}

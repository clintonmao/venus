package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.VenusContext;
import com.meidusa.venus.client.factory.ServiceFactory;
import com.meidusa.venus.monitor.athena.reporter.AthenaClientTransaction;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionId;
import com.saic.framework.athena.client.Athena;
import com.saic.framework.athena.configuration.client.entity.RemoteContext;
import com.saic.framework.athena.configuration.client.entity.impl.RemoteContextInstance;
import com.saic.framework.athena.message.Transaction;
import com.saic.framework.athena.site.helper.AthenaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Stack;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public class DefaultClientTransactionReporter extends AbstractTransactionReporter implements AthenaClientTransaction {

    private static Logger logger = LoggerFactory.getLogger(DefaultClientTransactionReporter.class);

    public AthenaTransactionId startTransaction(String itemName) {

        AthenaTransactionId  transactionId = new AthenaTransactionId();
        try{
            RemoteContext context = new RemoteContextInstance();
            getAthena().logRemoteCallClient(context);
            transactionId.setRootId(context.getProperty(RemoteContext.ROOT));
            transactionId.setParentId(context.getProperty(RemoteContext.PARENT));
            transactionId.setMessageId(context.getProperty(RemoteContext.CHILD));
            Stack<Transaction> transactionStack = TransactionThreadLocal.getInstance().get();
            transactionStack.add(AthenaUtils.getInstance().newTransaction(Constants.TRANSACTION_TYPE_RPC, itemName));
        }catch (Exception e) {
            return null;
        }
        return transactionId;
    }

    /**
     * 获取Athena实例
     * @return
     */
    Athena getAthena(){
        ApplicationContext context = VenusContext.getInstance().getApplicationContext();
        //TODO

        //设置applicationContext
        AthenaUtils athenaUtils = new AthenaUtils();
        athenaUtils.setApplicationContext(context);
        //添加AthenaImpl到上下文
        try {
            if(context.getBean(Athena.class) == null){
                VenusContext.getInstance().getBeanContext().createBean(com.saic.framework.athena.client.AthenaImpl.class);
            }
        } catch (Exception e) {
            try {
                VenusContext.getInstance().getBeanContext().createBean(com.saic.framework.athena.client.AthenaImpl.class);
            } catch (Exception ex) {
                logger.error("create athena bean failed.",ex);
                return null;
            }
        }

        Athena athena = AthenaUtils.getInstance();
        return athena;
    }

}

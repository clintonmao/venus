package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.VenusContext;
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
            AthenaUtils.getInstance().logRemoteCallClient(context);
            transactionId.setRootId(context.getProperty(RemoteContext.ROOT));
            transactionId.setParentId(context.getProperty(RemoteContext.PARENT));
            transactionId.setMessageId(context.getProperty(RemoteContext.CHILD));
            Stack<Transaction> transactionStack = TransactionThreadLocal.getInstance().get();
            transactionStack.add(AthenaUtils.getInstance().newTransaction(Constants.TRANSACTION_TYPE_RPC, itemName));
        }catch (Exception e) {
            logger.error("client startTransaction error.",e);
            return null;
        }
        return transactionId;
    }

    /**
     * 初始化athena
     */
//    void initAthena(){
//        ApplicationContext context = VenusContext.getInstance().getApplicationContext();
//        if(context != null){
//            //初始化AthenaUtils
//            AthenaUtils athenaUtils = new AthenaUtils();
//            athenaUtils.setApplicationContext(context);
//            //初始化AthenaImpl
//            try {
//                if(context.getBean(Athena.class) == null){
//                    VenusContext.getInstance().getBeanContext().createBean(com.saic.framework.athena.client.AthenaImpl.class);
//                }
//            } catch (Exception e) {
//                logger.error("init AthenaClient failed on consumer.",e);
//            }
//        }
//
//    }

}

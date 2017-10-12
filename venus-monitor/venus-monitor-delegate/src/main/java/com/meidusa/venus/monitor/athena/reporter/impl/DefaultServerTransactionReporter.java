package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.VenusContext;
import com.meidusa.venus.monitor.athena.reporter.AthenaServerTransaction;
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
public class DefaultServerTransactionReporter extends AbstractTransactionReporter implements AthenaServerTransaction {

    private static Logger logger = LoggerFactory.getLogger(DefaultServerTransactionReporter.class);

    public void startTransaction(AthenaTransactionId transactionId, String itemName) {
        try {
            RemoteContext context = new RemoteContextInstance();
            context.addProperty(RemoteContext.ROOT, transactionId.getRootId());
            context.addProperty(RemoteContext.PARENT, transactionId.getParentId());
            context.addProperty(RemoteContext.CHILD, transactionId.getMessageId());

            AthenaUtils.getInstance().logRemoteCallServer(context);

            Stack<Transaction> transactionStack = TransactionThreadLocal.getInstance().get();

            transactionStack.add(AthenaUtils.getInstance().newTransaction(Constants.TRANSACTION_TYPE_LOCAL, itemName));
        } catch (Exception e) {
            logger.error("server startTransaction error.",e);
        }
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

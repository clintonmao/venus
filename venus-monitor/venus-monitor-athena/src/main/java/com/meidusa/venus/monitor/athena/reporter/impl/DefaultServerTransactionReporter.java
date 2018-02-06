package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.monitor.athena.support.AthenaConstants;
import com.meidusa.venus.monitor.athena.support.TransactionThreadLocal;
import com.meidusa.venus.monitor.athena.reporter.ServerTransactionReporter;
import com.meidusa.venus.monitor.athena.AthenaTransactionId;
import com.saic.framework.athena.configuration.client.entity.RemoteContext;
import com.saic.framework.athena.configuration.client.entity.impl.RemoteContextInstance;
import com.saic.framework.athena.message.Transaction;
import com.saic.framework.athena.site.helper.AthenaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public class DefaultServerTransactionReporter extends AbstractTransactionReporter implements ServerTransactionReporter {

    private static Logger logger = LoggerFactory.getLogger(DefaultServerTransactionReporter.class);

    public void startTransaction(AthenaTransactionId transactionId, String itemName) {
        try {
            RemoteContext context = new RemoteContextInstance();
            context.addProperty(RemoteContext.ROOT, transactionId.getRootId());
            context.addProperty(RemoteContext.PARENT, transactionId.getParentId());
            context.addProperty(RemoteContext.CHILD, transactionId.getMessageId());

            AthenaUtils.getInstance().logRemoteCallServer(context);

            Stack<Transaction> transactionStack = TransactionThreadLocal.getInstance().get();

            Transaction transaction = AthenaUtils.getInstance().newTransaction(AthenaConstants.TRANSACTION_TYPE_LOCAL, itemName);
            if(transaction != null){
                transactionStack.add(transaction);
            }
        } catch (Exception e) {
            logger.error("server startTransaction error.",e);
        }
    }

}

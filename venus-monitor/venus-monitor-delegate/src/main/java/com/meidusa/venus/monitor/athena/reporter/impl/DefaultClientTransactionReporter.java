package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.monitor.athena.reporter.support.AthenaConstants;
import com.meidusa.venus.monitor.athena.reporter.support.TransactionThreadLocal;
import com.meidusa.venus.monitor.athena.reporter.ClientTransactionReporter;
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
public class DefaultClientTransactionReporter extends AbstractTransactionReporter implements ClientTransactionReporter {

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
            Transaction transaction = AthenaUtils.getInstance().newTransaction(AthenaConstants.TRANSACTION_TYPE_RPC, itemName);
            if(transaction != null){
                transactionStack.add(transaction);
            }
        }catch (Exception e) {
            logger.error("client startTransaction error.",e);
            return null;
        }
        return transactionId;
    }

    public AthenaTransactionId newTransaction() {
        AthenaTransactionId  transactionId = new AthenaTransactionId();
        RemoteContext context = new RemoteContextInstance();

        AthenaUtils.getInstance().logRemoteCallClient(context);

        transactionId.setRootId(context.getProperty(RemoteContext.ROOT));
        transactionId.setParentId(context.getProperty(RemoteContext.PARENT));
        transactionId.setMessageId(context.getProperty(RemoteContext.CHILD));
        return transactionId;
    }

}

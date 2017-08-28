package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.monitor.athena.reporter.AthenaClientTransaction;
import com.meidusa.venus.monitor.athena.reporter.AthenaTransactionId;
import com.saic.framework.athena.configuration.client.entity.RemoteContext;
import com.saic.framework.athena.configuration.client.entity.impl.RemoteContextInstance;
import com.saic.framework.athena.message.Transaction;
import com.saic.framework.athena.site.helper.AthenaUtils;

import java.util.Stack;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public class DefaultClientTransactionReporter extends AbstractTransactionReporter implements AthenaClientTransaction {

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
            return null;
        }
        return transactionId;
    }




}

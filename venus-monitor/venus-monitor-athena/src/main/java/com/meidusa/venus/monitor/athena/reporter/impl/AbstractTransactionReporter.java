package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.monitor.athena.support.TransactionThreadLocal;
import com.meidusa.venus.monitor.athena.TransactionCommittable;
import com.meidusa.venus.monitor.athena.TransactionStatistics;
import com.saic.framework.athena.message.Transaction;

import java.util.Stack;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public abstract class AbstractTransactionReporter implements TransactionStatistics, TransactionCommittable {

    public void commit() {
        Stack<Transaction> transactionStack = TransactionThreadLocal.getInstance().get();
        Transaction transaction = null;
        try {
            transaction = transactionStack.pop();
        } catch (Exception e) {

        }
        if (transaction != null) {
            transaction.complete();
        }
    }

    public void setInputSize(long size){
        Transaction transaction = TransactionThreadLocal.getInstance().get().peek();
        transaction.setInputSize(size);
    }

    public void setOutputSize(long size) {
        Transaction transaction = TransactionThreadLocal.getInstance().get().peek();
        transaction.setOutputSize(size);
    }
}

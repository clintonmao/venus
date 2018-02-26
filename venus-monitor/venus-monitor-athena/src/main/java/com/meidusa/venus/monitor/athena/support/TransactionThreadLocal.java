package com.meidusa.venus.monitor.athena.support;

import com.saic.framework.athena.message.Transaction;

import java.util.Stack;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public class TransactionThreadLocal extends ThreadLocal<Stack<Transaction>> {

    private static TransactionThreadLocal instance = new TransactionThreadLocal();

    public static TransactionThreadLocal getInstance() {
        return instance;
    }

    @Override
    protected Stack<Transaction> initialValue() {
        return new Stack<Transaction>();
    }

}

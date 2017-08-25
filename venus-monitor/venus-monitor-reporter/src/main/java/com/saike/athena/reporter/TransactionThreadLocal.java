package com.saike.athena.reporter;

import com.saic.framework.athena.message.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

    public static void main(String[] args) {
        long current = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        Date date = new Date(current);
        c.setTime(date);
        System.out.println(c.get(Calendar.MILLISECOND));
        System.out.println(c.getTime().getTime());
        c.set(Calendar.MILLISECOND, c.get(Calendar.MILLISECOND) + 300);
        System.out.println(c.getTime().getTime());
        System.out.println(c.get(Calendar.MILLISECOND));
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date));
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(c.getTime()));
    }


}

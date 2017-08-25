package com.saike.athena.reporter;

import com.meidusa.venus.extension.athena.AthenaServerTransaction;
import com.meidusa.venus.extension.athena.AthenaTransactionId;
import com.saic.framework.athena.configuration.client.entity.RemoteContext;
import com.saic.framework.athena.configuration.client.entity.impl.RemoteContextInstance;
import com.saic.framework.athena.message.Transaction;
import com.saic.framework.athena.site.helper.AthenaUtils;
import com.saike.athena.reporter.utils.Constants;

import java.util.Stack;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public class DefaultServerTransactionReporter extends AbstractTransactionReporter implements AthenaServerTransaction {

    public void startTransaction(AthenaTransactionId transactionId, String itemName) {
        RemoteContext context = new RemoteContextInstance();
        context.addProperty(RemoteContext.ROOT, transactionId.getRootId());
        context.addProperty(RemoteContext.PARENT, transactionId.getParentId());
        context.addProperty(RemoteContext.CHILD, transactionId.getMessageId());

        AthenaUtils.getInstance().logRemoteCallServer(context);

        Stack<Transaction> transactionStack = TransactionThreadLocal.getInstance().get();

        transactionStack.add(AthenaUtils.getInstance().newTransaction(Constants.TRANSACTION_TYPE_LOCAL, itemName));

    }

}

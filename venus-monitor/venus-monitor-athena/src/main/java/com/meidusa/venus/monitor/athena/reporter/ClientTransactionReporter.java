package com.meidusa.venus.monitor.athena.reporter;

import com.meidusa.venus.monitor.athena.AthenaTransactionId;
import com.meidusa.venus.monitor.athena.TransactionCommittable;
import com.meidusa.venus.monitor.athena.TransactionStatistics;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public interface ClientTransactionReporter extends TransactionStatistics, TransactionCommittable {

    AthenaTransactionId startTransaction(String itemName);

}

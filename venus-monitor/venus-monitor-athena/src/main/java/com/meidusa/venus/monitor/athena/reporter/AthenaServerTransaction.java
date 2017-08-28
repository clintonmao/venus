package com.meidusa.venus.monitor.athena.reporter;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public interface AthenaServerTransaction extends TransactionStatistics, TransactionCommittable {

    void startTransaction(AthenaTransactionId transactionId, String itemName);

}

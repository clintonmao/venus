package com.meidusa.venus.monitor.athena.reporter;

import com.meidusa.venus.monitor.athena.AthenaTransactionId;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;

/**
 * athena上报代理
 * Created by GodzillaHua on 7/3/16.
 */
public class AthenaReporterDelegate{

    private static Logger logger = LoggerFactory.getLogger(AthenaReporterDelegate.class);

    private static AthenaReporterDelegate athenaReporterDelegate = new AthenaReporterDelegate();

    private static ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private MetricReporter metricReporter;

    private ProblemReporter problemReporter;

    private ClientTransactionReporter clientTransactionReporter;

    private ServerTransactionReporter serverTransactionReporter;

    private AthenaReporterDelegate(){
    }

    public static AthenaReporterDelegate getInstance() {
        return athenaReporterDelegate;
    }

    public void metric(String key) {
        metric(key, 1);
    }

    public void metric(String key, int count) {
        if (metricReporter != null) {
            metricReporter.metric(key, count);
        }
    }

    public void problem(String message, Throwable cause) {
        if (problemReporter != null) {
            problemReporter.problem(message, cause);
        }
    }

    public AthenaTransactionId startClientTransaction(String itemName) {
        if (clientTransactionReporter != null) {
            return clientTransactionReporter.startTransaction(itemName);
        }
        return null;
    }

    public void completeClientTransaction() {
        if (clientTransactionReporter != null) {
            clientTransactionReporter.commit();
        }
    }

    public void startServerTransaction(AthenaTransactionId transactionId, String itemName) {
        if (serverTransactionReporter != null) {
            serverTransactionReporter.startTransaction(transactionId, itemName);
        }
    }

    public void completeServerTransaction(){
        if (serverTransactionReporter != null) {
            serverTransactionReporter.commit();
        }
    }

    public void setServerInputSize(long size) {
        if (serverTransactionReporter != null) {
            serverTransactionReporter.setInputSize(size);
        }
    }

    public void setServerOutputSize(long size) {
        if (serverTransactionReporter != null) {
            serverTransactionReporter.setOutputSize(size);
        }
    }

    public  void setClientOutputSize(long size) {
        if(clientTransactionReporter != null) {
            clientTransactionReporter.setOutputSize(size);
        }
    }

    public void setClientInputSize(long size) {
        if (clientTransactionReporter != null) {
            clientTransactionReporter.setInputSize(size);
        }
    }

    public AthenaTransactionId newTransaction(){
        if (clientTransactionReporter != null) {
            return clientTransactionReporter.newTransaction();
        }
        return null;
    }

    public void init(){
        Ini ini = new Ini();
        String athenaExtensionIniLocation = PathMatchingResourcePatternResolver.CLASSPATH_URL_PREFIX + "/META-INF/venus.extension.athena.ini";
        Resource resource = resourcePatternResolver.getResource(athenaExtensionIniLocation);
        if (resource.exists()) {
            InputStream is = null;
            try {
                is = resource.getInputStream();
                ini.load(is);
            } catch (IOException e) {
                logger.error("load athena ini file error", e);
                return;
            }finally {
                try{
                    if(is != null){
                        is.close();
                    }
                }catch (Exception e) {
                    logger.warn("resource cannot be close correctly", e);
                    //ignore
                }
            }

            Profile.Section metricSection = ini.get("metric");
            if (metricSection != null) {
                String metricReportClassName = metricSection.get("metric.reporter");
                MetricReporter metricReporter = newInstance(metricReportClassName);
                this.metricReporter = metricReporter;
            }

            Profile.Section problemSection = ini.get("problem");
            if (problemSection != null) {
                String problemReportClassName = problemSection.get("problem.reporter");
                ProblemReporter problemReporter = newInstance(problemReportClassName);
                this.problemReporter = problemReporter;
            }

            Profile.Section transactionSection = ini.get("transaction");
            if (transactionSection != null) {
                String clientTransactionClassName = transactionSection.get("client.transaction");
                ClientTransactionReporter clientTransaction = newInstance(clientTransactionClassName);
                this.clientTransactionReporter = clientTransaction;
            }

            if (transactionSection != null) {
                String serverTransactionClassName = transactionSection.get("server.transaction");
                ServerTransactionReporter serverTransaction = newInstance(serverTransactionClassName);
                this.serverTransactionReporter = serverTransaction;
            }
        }
    }

    <T> T newInstance(String className){
        if (className == null) {
            return null;
        }
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.error("load class error " + className, e);
            return null;
        }

        try {
            return (T) (clazz.newInstance());
        } catch (InstantiationException e) {
            logger.error("instantiate class error " + className, e);
            return null;
        } catch (IllegalAccessException e) {
            logger.error("class cannot be access error " + className, e);
            return null;
        }
    }


}

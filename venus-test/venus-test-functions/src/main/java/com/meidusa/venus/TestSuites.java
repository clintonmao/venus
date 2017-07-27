package com.meidusa.venus;

import com.meidusa.venus.registry.MysqlRegisterTest;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;

/**
 * Created by Zhangzhihua on 2017/7/27.
 */
public class TestSuites {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite ("All tests");
        //registry
        suite.addTest(new JUnit4TestAdapter(MysqlRegisterTest.class));

        //remoting
        //rpc
        return suite;
    }

}

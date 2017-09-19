package com.meidusa.venus.registry.test;

import com.meidusa.venus.registry.domain.VenusServiceDefinitionDO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.meidusa.venus.URL;
import com.meidusa.venus.registry.Register;

@ContextConfiguration(locations = { "classpath:registry-dao.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringTestCase extends AbstractJUnit4SpringContextTests {

	private Register mysqlRegister = null;//MysqlRegister.getInstance(true,null);

	@Before // 在每个测试用例方法之前都会执行
	public void init() {
		System.out.println("init");
	}

	@After // 在每个测试用例执行完之后执行
	public void destory() {
		System.out.println("destory");
	}

	@Test
	public void testRegiste() {
		URL u = new URL();
		u.setServiceName("orderService");
		u.setInterfaceName("com.chexiang.order.OrderService");
		u.setVersion("1.0.0");

		u.setPath("com.chexiang.order.OrderService/orderService");
		u.setApplication("test-order-domain");
		u.setHost("192.168.0.1");

		u.setPort(16800);
		u.setProtocol("venus");
		u.setLoadbanlance("random");
		u.setMethods("getOrderById[java.lang.String],selectAllOrder[java.lang.String]");

		mysqlRegister.registe(u);

	}

	@Test
	public void testSubscrible() {
		URL u = new URL();
		u.setServiceName("orderService");
		u.setInterfaceName("com.chexiang.order.OrderService");
		u.setVersion("1.0.0");

		u.setPath("com.chexiang.order.OrderService/orderService");
		u.setApplication("test-order-domain");
		u.setHost("192.168.1.100");

		u.setPort(0);
		u.setProtocol("subscrible");
		u.setLoadbanlance("random");
		u.setMethods("getOrderById[java.lang.String],selectAllOrder[java.lang.String]");

		u.setConsumerCheck(true);
		mysqlRegister.subscrible(u);

	}

	@Test
	public void testLookUp() {
		URL u = new URL();
		u.setServiceName("orderService");
		u.setInterfaceName("com.chexiang.order.OrderService");
		u.setVersion("1.0.0");

		u.setPath("com.chexiang.order.OrderService/orderService");
		u.setApplication("test-order-domain");
		u.setHost("192.168.1.100");

		u.setPort(0);
		u.setProtocol("subscrible");
		u.setLoadbanlance("random");
		u.setMethods("getOrderById[java.lang.String],selectAllOrder[java.lang.String]");

		u.setConsumerCheck(true);
		mysqlRegister.subscrible(u);

		VenusServiceDefinitionDO lookup = mysqlRegister.lookup(u);
		System.out.println(lookup);

	}

	@Test
	public void testUnregiste() {
		URL u = new URL();
		u.setServiceName("orderService");
		u.setInterfaceName("com.chexiang.order.OrderService");
		u.setVersion("1.0.0");

		u.setPath("com.chexiang.order.OrderService/orderService");
		u.setApplication("test-order-domain");
		u.setHost("192.168.0.1");

		u.setPort(16800);
		u.setProtocol("venus");
		u.setLoadbanlance("random");
		u.setMethods("getOrderById[java.lang.String],selectAllOrder[java.lang.String]");
		mysqlRegister.unregiste(u);

	}

	@Test
	public void testUnsubscrible() {
		URL u = new URL();
		u.setServiceName("orderService");
		u.setInterfaceName("com.chexiang.order.OrderService");
		u.setVersion("1.0.0");

		u.setPath("com.chexiang.order.OrderService/orderService");
		u.setApplication("test-order-domain");
		u.setHost("192.168.1.100");

		u.setPort(0);
		u.setProtocol("subscrible");
		u.setLoadbanlance("random");
		u.setMethods("getOrderById[java.lang.String],selectAllOrder[java.lang.String]");

		u.setConsumerCheck(true);
		mysqlRegister.unsubscrible(u);

	}

}
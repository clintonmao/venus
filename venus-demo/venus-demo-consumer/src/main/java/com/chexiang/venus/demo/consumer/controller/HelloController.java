package com.chexiang.venus.demo.consumer.controller;

import com.chexiang.venus.demo.provider.HelloService;
import com.chexiang.venus.demo.provider.HelloValidException;
import com.chexiang.venus.demo.provider.InvalidParamException;
import com.chexiang.venus.demo.provider.model.Hello;
import com.meidusa.fastjson.JSON;
import com.meidusa.fastmark.feature.SerializerFeature;
import com.meidusa.venus.Result;
import com.meidusa.venus.notify.InvocationListener;
import com.saic.ebiz.mdsecenter.carmall.vo.SpuVO;
import com.saic.ebiz.mdsecenter.vo.MdseCityPriceVO;
import com.saic.ebiz.order.service.api.HugePayService;
import com.saic.usedcar.shangcheng.model.UsedCarMerchandiseVO;
import com.saic.usedcar.shangcheng.model.UsedCarParamVO;
import com.saic.usedcar.shangcheng.pagination.Pagination;
import com.saic.usedcar.shangcheng.pagination.PaginationResult;
import com.saic.usedcar.shangcheng.service.api.IUsedCarMallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HelloController
 * Created by Zhangzhihua on 2017/9/25.
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    private static Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    HelloService helloService;

    @Autowired
    HugePayService hugePayService;

    @Autowired
    IUsedCarMallService iUsedCarMallService;

    @RequestMapping("/sayHello")
    public Result sayHello(){
    	helloService.sayHello("jack");
    	return new Result("ok");
    }
    
    @RequestMapping("/sayHelloWithCallback")
    public Result sayHelloWithCallback(){
        helloService.sayHello("jack", new InvocationListener<Hello>() {

            @Override
            public void callback(Hello object) {
                logger.info("Hello:" + object);
            }

            @Override
            public void onException(Exception e) {
                logger.error("e:" + e);
            }
        });
        return new Result("callback.");
    }

    @RequestMapping("/getHello/{name}")
    public Result getHello(@PathVariable String name){
        if("A".equalsIgnoreCase("B")){
            return new Result(new Hello("hi","meme"));
        }
        Hello hello = null;
        try {
            logger.info("testGetHello begin...");
            hello = helloService.getHello(name);
            logger.info("testGetHello end,result:" + hello);
        } catch (Exception e) {
            logger.error("e:{}.",e);
            return new Result(e);
        }
        return new Result(hello);
    }

    @RequestMapping("/cal/{param}")
    public Result cal(@PathVariable String param) throws HelloValidException,InvalidParamException {
        try {
            int ret = helloService.cal(Integer.parseInt(param));
        } catch (HelloValidException e) {
            logger.error("HelloValidException error",e);
        } catch (InvalidParamException e) {
            logger.error("InvalidParamException error",e);
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException error",e);
        }
        return new Result("ok");
    }

    @RequestMapping("/order/{param}")
    public Result order(@PathVariable String param) throws HelloValidException,InvalidParamException {
        //构造VO
        SpuVO spuVO = new SpuVO();
        spuVO.setBrandName("ABC");
        Map<Long, MdseCityPriceVO> mdseCityPriceMap = new HashMap<>();
        MdseCityPriceVO cityPriceVO = new MdseCityPriceVO();
        cityPriceVO.setStatus(1);
        mdseCityPriceMap.put(new Long(324),cityPriceVO);
        spuVO.setMdseCityPriceMap(mdseCityPriceMap);

        SerializerFeature[] serializerFeature = new SerializerFeature[]{SerializerFeature.WriteNonStringKeyAsString};
        String serialVo = com.meidusa.fastjson.JSON.toJSONString(spuVO,serializerFeature);
        logger.info("serialVo:{}",serialVo);

        Object deseriaVo = JSON.parse(serialVo);
        logger.info("deseriaVo:{}",serialVo);

        boolean ret = false;
        try {
            ret = hugePayService.isMallHugePay(spuVO);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("ret:",ret);
        return new Result("ok");
    }

    @RequestMapping("/usedcar/{param}")
    public Result usedcar(@PathVariable String param) throws HelloValidException,InvalidParamException {
//        String url = databaseConfigure.getUrl();
//   		String user = databaseConfigure.getUser();
//   		String password = databaseConfigure.getPassword();
//   		Connection conn = null;
//   		Statement ps = null;

        UsedCarParamVO paramVO = new UsedCarParamVO();
        paramVO.setBrandCode("");
        paramVO.setPriceSection(""); // {"0", ""}, {"1", "3万以下"}, {"2", "3-5万"},
        // {"3", "5-8万"}, {"4", "8-10万"}, {"5",
        // "10-15万"}, {"6", "15-20万"}, {"7",
        // "20-30万"}, {"8", "30-50万"}, {"9",
        // "50万以上"}
        paramVO.setCarUsedTimeSection(""); // {"0", ""}, {"1", "3年以内"}, {"2",
        // "3-5年"}, {"3", "5-8年"}, {"4",
        // "8-10年"}, {"5", "10年以上"}
        paramVO.setLevel(""); // {"0", ""}, {"1", "紧凑型"}, {"2", "中型"}, {"3",
        // "中大型"}, {"4", "MPV"}, {"5", "SUV"}, {"6",
        // "跑车"}, {"7", "小型车"},
        paramVO.setLocation(""); // {"0", ""}, {"1", "上海"}, {"2", "北京"}, {"3",
        // "成都"}, {"4", "杭州"}, {"5", "南京"}, {"6",
        // "天津"}, {"7", "石家庄"}, {"8", "广州"}, {"9",
        // "深圳"}, {"10", "郑州"}
        paramVO.setDistanceSection(""); // {"0", ""}, {"1", "1万公里以内"}, {"2",
        // "1-3万公里"}, {"3", "3-5万公里"}, {"4",
        // "5-8万公里"}, {"5", "8-10万公里"}, {"6",
        // "10万公里以上"}
        paramVO.setSource(""); // {"0", ""}, {"1", "否"},//自营 {"2", "是"} //加盟
        paramVO.setIsSale("");
        paramVO.setIsDesc(1);
        paramVO.setOrderBy(0); // 0:代表默认按照最新的时间排序
        try {
//   			Class.forName(databaseConfigure.getDriver());
//   			conn = DriverManager.getConnection(url, user, password);
//   			for (int j = 1; j <= 22; j++) {
            //Pagination page = new Pagination(9, 2);
            Pagination page = new Pagination();
            page.setPagesize(9);
            page.setCurrentPagex(2);
            page.setCurPage(2);

            PaginationResult<List<UsedCarMerchandiseVO>> carMessage = iUsedCarMallService.getUsedCarInfoList(paramVO, page);
            System.out.println(carMessage);
            /*
            List<UsedCarMerchandiseVO> carList = carMessage.getR();
            for (int i = 0; i < carList.size(); i++) {
                JSONObject a = JSONObject.fromObject(carList.get(i));
                UsedCarMerchandiseVO source = (UsedCarMerchandiseVO) JSONObject.toBean(a, UsedCarMerchandiseVO.class);
            }
            */

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result("ok");
    }




}

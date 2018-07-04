package com.meidusa.venus.backend.services.xml.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("method")
public class ExportMethod {

    @XStreamAsAttribute
    private String name;

    //tracer是否打印输入参数
    @XStreamAsAttribute
    private String printParam;

    //tracer是否打印输出参数
    @XStreamAsAttribute
    private String printResult;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrintParam() {
        return printParam;
    }

    public void setPrintParam(String printParam) {
        this.printParam = printParam;
    }

    public String getPrintResult() {
        return printResult;
    }

    public void setPrintResult(String printResult) {
        this.printResult = printResult;
    }
}

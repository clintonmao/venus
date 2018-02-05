package com.meidusa.venus.backend.interceptor;

import com.meidusa.venus.backend.services.Endpoint;
import com.meidusa.venus.backend.services.EndpointInvocation;
import com.meidusa.venus.exception.InvalidParameterException;
import com.meidusa.venus.validate.ValidatorManager;
import com.meidusa.venus.validate.VenusValidatorManager;
import com.meidusa.venus.validate.exception.ValidationException;
import com.meidusa.venus.validate.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorInterceptor extends AbstractInterceptor {

    private static Logger logger = LoggerFactory.getLogger(ValidatorInterceptor.class);

    private static ValidatorManager validatorManager = new VenusValidatorManager();

    static {
        validatorManager.init();
    }

    @Override
    public Object intercept(EndpointInvocation invocation) {
        logger.info("invoke ValidatorInterceptor...");
        Endpoint endpoint = invocation.getEndpoint();
        Validator chain = validatorManager.getValidatorChain(endpoint.getMethod());
        try {
            chain.validate(invocation.getContext().getParameters());
            return invocation.invoke();
        } catch (ValidationException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

}

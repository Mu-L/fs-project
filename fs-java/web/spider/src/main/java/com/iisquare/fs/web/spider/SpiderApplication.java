package com.iisquare.fs.web.spider;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.iisquare.fs.base.core.*",
        "com.iisquare.fs.base.jpa.*",
        "com.iisquare.fs.web.core.*",
        "com.iisquare.fs.web.spider",
})
@EnableFeignClients(basePackages = {
        "com.iisquare.fs.web.core.rpc"
}, defaultConfiguration = { FeignInterceptor.class })
public class SpiderApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpiderApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }

}

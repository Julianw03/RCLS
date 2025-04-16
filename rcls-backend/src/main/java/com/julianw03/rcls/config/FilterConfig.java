package com.julianw03.rcls.config;

import com.julianw03.rcls.filter.ServiceReadyFilter;
import com.julianw03.rcls.service.riotclient.RiotClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class FilterConfig {
    //The ordinal of the filter determines its order. Done to ensure unique Ordering
    //CHANGING THE ORDERING HERE WILL HAVE EFFECTS ON THE FILTER ORDERING
    private enum FilterOrdering {
        RIOT_CLIENT_READY
    }

    @Bean
    public FilterRegistrationBean<ServiceReadyFilter> getRiotClientReadyFilter(@Autowired RiotClientService service) {
        FilterRegistrationBean<ServiceReadyFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ServiceReadyFilter(service));
        registrationBean.addUrlPatterns("/api/riotclient/*");
        registrationBean.setOrder(FilterOrdering.RIOT_CLIENT_READY.ordinal());
        return registrationBean;
    }

}

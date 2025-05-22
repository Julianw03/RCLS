package com.julianw03.rcls;

import com.julianw03.rcls.controller.DebugController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Profile("test")
class RCLSApplicationTests {

    @Autowired
    private DebugController controller;

    @Test
    void contextLoads() {
        assertThat(controller).isNotNull();
    }

}

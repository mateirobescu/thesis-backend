package com.mateirobescu.thesis;

import com.mateirobescu.thesis.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ThesisApplicationTests {

    @Test
    void contextLoads() {
    }

}

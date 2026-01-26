package com.omnip;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class LombokTest {
    private String name;
    private int value;

    public void test() {
        log.info("Testing: {}", name);
    }
}

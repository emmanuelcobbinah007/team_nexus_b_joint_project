package edu.ug.nexusb.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void mainRunsWithoutThrowing() {
        assertDoesNotThrow(() -> App.main(new String[] {}));
    }
}

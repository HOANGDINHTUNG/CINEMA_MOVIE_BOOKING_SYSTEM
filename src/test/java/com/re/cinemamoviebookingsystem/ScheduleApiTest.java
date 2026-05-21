package com.re.cinemamoviebookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scheduleApiReturnsJson() throws Exception {
        mockMvc.perform(get("/api/public/schedule").param("lang", "vi-VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedDate").exists())
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.movies").isArray());
    }
}

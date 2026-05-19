package com.re.cinemamoviebookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CalendarPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "customer_minh", roles = "CUSTOMER")
    void calendarPageRenders() throws Exception {
        mockMvc.perform(get("/customer/calendar").with(csrf()))
                .andExpect(status().isOk());
    }
}

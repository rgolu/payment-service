package com.payments.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void initiateAndCompleteThroughHttp() throws Exception {
        String userId = mapper.readTree(mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rishav\",\"initialBalance\":5000}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String merchantId = mapper.readTree(mvc.perform(post("/api/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cafe\",\"supportedMethods\":[\"UPI\",\"CARD\"]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String initiateBody = """
                {"userId":"%s","merchantId":"%s","amount":1000,"method":"UPI"}
                """.formatted(userId, merchantId);

        String initiated = mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initiateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fee").value(20.00))
                .andExpect(jsonPath("$.amountCharged").value(1020.00))
                .andReturn().getResponse().getContentAsString();

        String paymentId = mapper.readTree(initiated).get("id").asText();

        String completed = mvc.perform(post("/api/payments/" + paymentId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = mapper.readTree(completed);
        assertThat(node.get("amountCharged").decimalValue()).isEqualByComparingTo("1020.00");
    }

    @Test
    void insufficientBalanceReturnsConflict() throws Exception {
        String userId = mapper.readTree(mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Poor\",\"initialBalance\":10}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        String merchantId = mapper.readTree(mvc.perform(post("/api/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cafe\",\"supportedMethods\":[\"UPI\"]}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","merchantId":"%s","amount":1000,"method":"UPI"}
                                """.formatted(userId, merchantId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("InsufficientBalanceException"));
    }
}

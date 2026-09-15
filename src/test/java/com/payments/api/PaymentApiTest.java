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
                        .content("{\"name\":\"Rishav\",\"initial_balance\":5000}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("user_id").asText();

        String merchantId = mapper.readTree(mvc.perform(post("/api/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cafe\",\"supported_methods\":[\"UPI\",\"CARD\"]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("merchant_id").asText();

        String initiateBody = """
                {"payment_id":"pay_demo_1","user_id":"%s","merchant_id":"%s","amount":1000.00,"method":"UPI"}
                """.formatted(userId, merchantId);

        String initiated = mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initiateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fee").value(20.00))
                .andExpect(jsonPath("$.amount_charged").value(1020.00))
                .andReturn().getResponse().getContentAsString();

        String paymentId = mapper.readTree(initiated).get("payment_id").asText();

        String completed = mvc.perform(post("/api/payments/" + paymentId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = mapper.readTree(completed);
        assertThat(node.get("amount_charged").decimalValue()).isEqualByComparingTo("1020.00");
    }

    @Test
    void insufficientBalanceReturnsConflict() throws Exception {
        String userId = mapper.readTree(mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Poor\",\"initial_balance\":10}"))
                .andReturn().getResponse().getContentAsString()).get("user_id").asText();
        String merchantId = mapper.readTree(mvc.perform(post("/api/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cafe\",\"supported_methods\":[\"UPI\"]}"))
                .andReturn().getResponse().getContentAsString()).get("merchant_id").asText();

        mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payment_id":"pay_poor","user_id":"%s","merchant_id":"%s","amount":1000.00,"method":"UPI"}
                                """.formatted(userId, merchantId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("InsufficientBalanceException"));
    }

    @Test
    void rejectsAmountWithMoreThanTwoDecimals() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rishav\",\"initial_balance\":10.001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationError"));
    }

    @Test
    void rejectsInvalidPaymentIdRegex() throws Exception {
        mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payment_id":"pay id!","user_id":"usr_1","merchant_id":"mer_1","amount":100.00,"method":"UPI"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationError"));
    }
}

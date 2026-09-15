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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PaymentApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String registerUser(String name, String balance) throws Exception {
        String body = mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"initial_balance\":" + balance + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("user_id").asText();
    }

    private String registerMerchant(String name, String methodsJson) throws Exception {
        String body = mvc.perform(post("/api/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"supported_methods\":" + methodsJson + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("merchant_id").asText();
    }

    @Test
    void initiate_UpiThousand_HttpReturnsFeeAndChargesWallet() throws Exception {
        // GIVEN
        String userId = registerUser("Rishav", "5000");
        String merchantId = registerMerchant("Cafe", "[\"UPI\",\"CARD\"]");

        // TEST
        String initiated = mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payment_id":"pay_demo_1","user_id":"%s","merchant_id":"%s","amount":1000.00,"method":"UPI"}
                                """.formatted(userId, merchantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fee").value(20.00))
                .andExpect(jsonPath("$.amount_charged").value(1020.00))
                .andExpect(jsonPath("$.executed_method").value("UPI"))
                .andExpect(jsonPath("$.fee_method").value("UPI"))
                .andReturn().getResponse().getContentAsString();

        // VERIFY
        mvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet").value(3980.00));

        String paymentId = mapper.readTree(initiated).get("payment_id").asText();
        String completed = mvc.perform(post("/api/payments/" + paymentId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(completed);
        assertEquals(0, node.get("amount_charged").decimalValue().compareTo(new java.math.BigDecimal("1020.00")));

        mvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/users/" + userId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payment_id").value(paymentId));
        mvc.perform(get("/api/merchants/" + merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlement").value(1000.00));
        mvc.perform(get("/api/merchants/" + merchantId + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void initiate_InsufficientBalance_Returns409AndWalletUnchanged() throws Exception {
        // GIVEN
        String userId = registerUser("Poor", "10");
        String merchantId = registerMerchant("Cafe", "[\"UPI\"]");

        // TEST
        mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payment_id":"pay_poor","user_id":"%s","merchant_id":"%s","amount":1000.00,"method":"UPI"}
                                """.formatted(userId, merchantId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("InsufficientBalanceException"));

        // VERIFY
        mvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet").value(10.00));
    }

    @Test
    void initiate_UpiProviderDown_ReroutesToCardAtUpiFee() throws Exception {
        // GIVEN
        String userId = registerUser("Rishav", "5000");
        String merchantId = registerMerchant("Cafe", "[\"UPI\",\"CARD\"]");
        mvc.perform(post("/api/admin/providers/UPI/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        // TEST
        mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payment_id":"pay_reroute","user_id":"%s","merchant_id":"%s","amount":1000.00,"method":"UPI"}
                                """.formatted(userId, merchantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rerouted").value(true))
                .andExpect(jsonPath("$.requested_method").value("UPI"))
                .andExpect(jsonPath("$.executed_method").value("CARD"))
                .andExpect(jsonPath("$.fee_method").value("UPI"))
                .andExpect(jsonPath("$.fee").value(20.00))
                .andExpect(jsonPath("$.amount_charged").value(1020.00));

        // VERIFY
        mvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet").value(3980.00));
    }

    @Test
    void initiate_CardThousand_HttpReturnsCardFee() throws Exception {
        // GIVEN
        String userId = registerUser("Rishav", "5000");
        String merchantId = registerMerchant("Cafe", "[\"UPI\",\"CARD\"]");

        // TEST + VERIFY
        mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payment_id":"pay_card","user_id":"%s","merchant_id":"%s","amount":1000.00,"method":"CARD"}
                                """.formatted(userId, merchantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(25.00))
                .andExpect(jsonPath("$.amount_charged").value(1025.00))
                .andExpect(jsonPath("$.fee_method").value("CARD"));
    }

    @Test
    void topUp_Refund_Coupon_Admin_HappyPaths() throws Exception {
        // GIVEN
        String userId = registerUser("Rishav", "5000");
        String merchantId = registerMerchant("Cafe", "[\"UPI\",\"CARD\"]");

        mvc.perform(post("/api/users/" + userId + "/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credit_id\":\"crd_1\",\"amount\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet").value(5100.00));

        mvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SAVE10\",\"type\":\"PERCENT\",\"value\":10.00,\"remaining_uses\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"));

        mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payment_id":"pay_c1","user_id":"%s","merchant_id":"%s","amount":1000.00,"method":"UPI","coupon_code":"SAVE10"}
                                """.formatted(userId, merchantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount_charged").value(918.00));

        mvc.perform(post("/api/payments/pay_c1/complete")).andExpect(status().isOk());
        mvc.perform(post("/api/payments/pay_c1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refund_id\":\"ref_1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        mvc.perform(post("/api/admin/routing-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CHEAPEST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("CHEAPEST"));

        mvc.perform(post("/api/admin/load/UPI")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"factor\":1.20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factor").value(1.20));

        mvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"FLAT50\",\"type\":\"FLAT\",\"value\":50.00}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/coupons/FLAT50")).andExpect(status().isNoContent());
    }

    @Test
    void initiate_AmountWithMoreThanTwoDecimals_Returns400() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rishav\",\"initial_balance\":10.001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationError"));
    }

    @Test
    void initiate_InvalidPaymentIdRegex_Returns400() throws Exception {
        mvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payment_id":"pay id!","user_id":"usr_1","merchant_id":"mer_1","amount":100.00,"method":"UPI"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationError"));
    }

    @Test
    void get_UnknownUser_Returns404() throws Exception {
        mvc.perform(get("/api/users/usr_nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NotFoundException"));
    }

    @Test
    void initiate_MalformedJson_Returns400() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MalformedRequest"));
    }

    @Test
    void get_InvalidPathId_Returns400() throws Exception {
        mvc.perform(get("/api/users/bad id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationError"));
    }
}

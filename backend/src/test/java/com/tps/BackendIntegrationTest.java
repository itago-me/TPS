package com.tps;

/**
 * 文件说明：后端集成测试入口，负责验证主要接口与配置能否协同工作。
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tps.entity.User;
import com.tps.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void authFlowSupportsRegisterLoginAndRefresh() throws Exception {
        JsonNode register = register("13800138000", "seller");
        String token = register.at("/data/token").asText();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", "13800138000", "password", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", not(emptyString())))
                .andExpect(jsonPath("$.data.refreshToken", not(emptyString())))
                .andExpect(jsonPath("$.data.role").value("USER"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", register.at("/data/refreshToken").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", not(emptyString())))
                .andExpect(jsonPath("$.data.refreshToken", not(emptyString())));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void productUploadFavoriteAndPagingUseMobileFriendlyDtos() throws Exception {
        String token = register("13800138001", "seller").at("/data/token").asText();
        String imageUrl = uploadPng(token);
        Long productId = createProduct(token, imageUrl);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.content[0].imageUrls[0]", startsWith("http://localhost/img/")));

        mockMvc.perform(post("/api/favorites/{productId}", productId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/favorites/{productId}/status", productId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(delete("/api/favorites/{productId}", productId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        MockMultipartFile file = new MockMultipartFile("file", "evil.txt", "text/plain", "not an image".getBytes());
        mockMvc.perform(multipart("/api/files/upload").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void productOwnerCanTakedownAndRelistButCannotMarkSoldDirectly() throws Exception {
        String sellerToken = register("13800138110", "seller").at("/data/token").asText();
        Long productId = createProduct(sellerToken, null);

        mockMvc.perform(patch("/api/products/{id}/status", productId)
                        .header("Authorization", "Bearer " + sellerToken)
                        .param("status", "OFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFF"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id", not(hasItem(productId.intValue()))));

        mockMvc.perform(patch("/api/products/{id}/status", productId)
                        .header("Authorization", "Bearer " + sellerToken)
                        .param("status", "ON_SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));

        mockMvc.perform(patch("/api/products/{id}/status", productId)
                        .header("Authorization", "Bearer " + sellerToken)
                        .param("status", "SOLD"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void orderFlowSupportsTradeReviewAndPreventsDuplicateActiveOrders() throws Exception {
        String sellerToken = register("13800138002", "seller").at("/data/token").asText();
        String buyerToken = register("13800138003", "buyer").at("/data/token").asText();
        String otherBuyerToken = register("13800138004", "other").at("/data/token").asText();
        Long productId = createProduct(sellerToken, null);

        Long orderId = createOrder(buyerToken, productId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + otherBuyerToken)
                        .param("productId", String.valueOf(productId))
                        .param("finalPrice", "88.00"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/orders/{id}/pay", orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/orders/{id}/ship", orderId).header("Authorization", "Bearer " + sellerToken).param("trackingNumber", "SF123"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/orders/{id}/confirm", orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/{id}/review", orderId).header("Authorization", "Bearer " + buyerToken)
                        .param("score", "5")
                        .param("content", "good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(5));
    }

    @Test
    void refundAndNotificationFlowWorks() throws Exception {
        String sellerToken = register("13800138100", "seller").at("/data/token").asText();
        String buyerToken = register("13800138101", "buyer").at("/data/token").asText();
        Long productId = createProduct(sellerToken, null);
        Long orderId = createOrder(buyerToken, productId);

        mockMvc.perform(put("/api/orders/{id}/pay", orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/{id}/refund", orderId)
                        .header("Authorization", "Bearer " + buyerToken)
                        .param("reason", "changed mind"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].type", hasItem("REFUND")));

        mockMvc.perform(put("/api/orders/{id}/refund/approve", orderId).header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/{id}", orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    @Test
    void profileUpdateAndDeactivateWork() throws Exception {
        String token = register("13800138102", "profile").at("/data/token").asText();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nickname", "updated", "bio", "bio", "location", "Shanghai"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("updated"))
                .andExpect(jsonPath("$.data.location").value("Shanghai"));

        mockMvc.perform(post("/api/users/me/deactivate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void messageHistoryRequiresConversationMembership() throws Exception {
        String sellerToken = register("13800138005", "seller").at("/data/token").asText();
        JsonNode buyer = register("13800138006", "buyer");
        String buyerToken = buyer.at("/data/token").asText();
        String outsiderToken = register("13800138007", "outsider").at("/data/token").asText();
        Long sellerId = userRepository.findByPhone("13800138005").orElseThrow().getId();
        Long productId = createProduct(sellerToken, null);

        String body = mockMvc.perform(post("/api/messages/conversation")
                        .header("Authorization", "Bearer " + buyerToken)
                        .param("targetUserId", String.valueOf(sellerId))
                        .param("productId", String.valueOf(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetUserId").value(sellerId))
                .andReturn().getResponse().getContentAsString();
        Long conversationId = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(get("/api/messages/{conversationId}", conversationId).header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminEndpointsRequireAdminRoleAndDoNotExposePasswordHash() throws Exception {
        String userToken = register("13800138008", "user").at("/data/token").asText();
        register("13800138009", "admin");
        User adminUser = userRepository.findByPhone("13800138009").orElseThrow();
        adminUser.setRole(User.Role.ADMIN);
        userRepository.save(adminUser);
        String adminToken = login("13800138009").at("/data/token").asText();

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
    }

    @Test
    void reportAndAdminHandlingFlowWorks() throws Exception {
        String sellerToken = register("13800138103", "seller").at("/data/token").asText();
        String reporterToken = register("13800138104", "reporter").at("/data/token").asText();
        String adminToken = createAdmin("13800138105");
        Long productId = createProduct(sellerToken, null);

        mockMvc.perform(post("/api/products/{id}/report", productId)
                        .header("Authorization", "Bearer " + reporterToken)
                        .param("reason", "bad product"))
                .andExpect(status().isOk());

        String reportBody = mockMvc.perform(get("/api/admin/products/reported").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].productId").value(productId))
                .andReturn().getResponse().getContentAsString();
        Long reportId = objectMapper.readTree(reportBody).at("/data/content/0/id").asLong();

        mockMvc.perform(put("/api/admin/reports/{id}/handle", reportId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("takedown", "true")
                        .param("reason", "平台判定违规"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFF"))
                .andExpect(jsonPath("$.data.takedownReason").value("平台判定违规"));
    }

    @Test
    void adminCanListOnSaleProductsAndForceTakedownWithReason() throws Exception {
        String sellerToken = register("13800138106", "seller").at("/data/token").asText();
        String adminToken = createAdmin("13800138107");
        Long productId = createProduct(sellerToken, null);

        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "ON_SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id", hasItem(productId.intValue())));

        mockMvc.perform(put("/api/admin/products/{id}/takedown", productId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("reason", "含有违规联系方式"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFF"))
                .andExpect(jsonPath("$.data.takedownReason").value("含有违规联系方式"));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].type", hasItem("PRODUCT_TAKEDOWN")));
    }

    @Test
    void unauthenticatedProtectedEndpointReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    private JsonNode register(String phone, String nickname) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "phone", phone,
                                "password", "123456",
                                "code", "1234",
                                "studentId", phone.substring(3),
                                "nickname", nickname
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode login(String phone) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", phone, "password", "123456"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String createAdmin(String phone) throws Exception {
        register(phone, "admin");
        User adminUser = userRepository.findByPhone(phone).orElseThrow();
        adminUser.setRole(User.Role.ADMIN);
        userRepository.save(adminUser);
        return login(phone).at("/data/token").asText();
    }

    private String uploadPng(String token) throws Exception {
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
                0x00, 0x00, 0x00, 0x0d
        };
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", png);
        String body = mockMvc.perform(multipart("/api/files/upload").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url", startsWith("http://localhost/img/")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/path").asText();
    }

    private Long createProduct(String token, String imageUrl) throws Exception {
        Map<String, Object> request = imageUrl == null
                ? Map.of("title", "Phone", "description", "A phone", "price", new BigDecimal("99.00"), "category", "digital", "condition", "GOOD", "location", "Shanghai")
                : Map.of("title", "Phone", "description", "A phone", "price", new BigDecimal("99.00"), "category", "digital", "condition", "GOOD", "location", "Shanghai", "imageUrls", java.util.List.of(imageUrl));
        String body = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private Long createOrder(String token, Long productId) throws Exception {
        String body = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("productId", String.valueOf(productId))
                        .param("finalPrice", "88.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productTitle").value("Phone"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}

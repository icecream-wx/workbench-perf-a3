package com.example.perfa3;

import com.example.perfa3.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void cleanUp() {
        employeeRepository.deleteAll();
    }

    private String body(String employeeNo, String name) {
        return """
                {
                  "employeeNo": "%s",
                  "name": "%s",
                  "gender": "MALE",
                  "department": "研发部",
                  "position": "工程师",
                  "phone": "13800000000",
                  "email": "zhangsan@example.com",
                  "hireDate": "2024-01-15",
                  "status": "ACTIVE"
                }
                """.formatted(employeeNo, name);
    }

    private JsonNode createEmployee(String employeeNo, String name) throws Exception {
        String response = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(employeeNo, name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data");
    }

    @Test
    void createEmployeeReturnsFullRecord() throws Exception {
        String response = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("E2024001", "张三")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.employeeNo").value("E2024001"))
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.gender").value("MALE"))
                .andExpect(jsonPath("$.data.hireDate").value("2024-01-15"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.createdAt").isString())
                .andExpect(jsonPath("$.data.updatedAt").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.path("createdAt").asText()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void createEmployeeDefaultsStatusAndRejectsDuplicateEmployeeNo() throws Exception {
        String withoutStatus = """
                { "employeeNo": "E1001", "name": "李四" }
                """;
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withoutStatus))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("E1001", "王五")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("工号已存在：E1001"));
    }

    @Test
    void createEmployeeValidatesRequiredAndFormatFields() throws Exception {
        String missingName = """
                { "employeeNo": "E2001", "name": "  " }
                """;
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingName))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));

        String invalidGender = """
                { "employeeNo": "E2002", "name": "赵六", "gender": "UNKNOWN" }
                """;
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidGender))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("gender")));

        String invalidEmail = """
                { "employeeNo": "E2003", "name": "钱七", "email": "not-an-email" }
                """;
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmail))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    void updateEmployeeKeepsCreatedAtAndRefreshesFields() throws Exception {
        JsonNode created = createEmployee("E3001", "张三");
        long id = created.path("id").asLong();
        String createdAt = created.path("createdAt").asText();

        mockMvc.perform(put("/api/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("E3001", "张三丰")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("张三丰"))
                .andExpect(jsonPath("$.data.createdAt").value(createdAt));

        String listResponse = mockMvc.perform(get("/api/employees").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("张三丰"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode item = objectMapper.readTree(listResponse).path("data").path("items").get(0);
        assertThat(item.path("createdAt").asText()).isEqualTo(createdAt);
    }

    @Test
    void updateMissingEmployeeReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/employees/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("E4001", "张三")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("员工不存在"));
    }

    @Test
    void updateToExistingEmployeeNoReturnsConflict() throws Exception {
        createEmployee("E5001", "张三");
        JsonNode second = createEmployee("E5002", "李四");

        mockMvc.perform(put("/api/employees/" + second.path("id").asLong())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("E5001", "李四")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void listReturnsNewestFirstAndClampsBoundaryParams() throws Exception {
        createEmployee("E6001", "张三");
        createEmployee("E6002", "李四");

        mockMvc.perform(get("/api/employees").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.items[0].employeeNo").value("E6002"))
                .andExpect(jsonPath("$.data.items[1].employeeNo").value("E6001"));

        mockMvc.perform(get("/api/employees").param("page", "0").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(100));
    }
}

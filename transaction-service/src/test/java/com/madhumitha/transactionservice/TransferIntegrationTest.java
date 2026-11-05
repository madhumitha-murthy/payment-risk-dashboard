package com.madhumitha.transactionservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madhumitha.transactionservice.dto.TransferRequest;
import com.madhumitha.transactionservice.model.Account;
import com.madhumitha.transactionservice.repository.AccountRepository;
import com.madhumitha.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TransferIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        accountRepository.save(Account.builder()
                .accountNumber("ALICE-001")
                .ownerName("Alice")
                .balance(new BigDecimal("10000.0000"))
                .build());

        accountRepository.save(Account.builder()
                .accountNumber("BOB-002")
                .ownerName("Bob")
                .balance(new BigDecimal("5000.0000"))
                .build());
    }

    @Test
    void successfulTransfer_debitsAndCreditsCorrectly() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("ALICE-001")
                .destinationAccountNumber("BOB-002")
                .amount(new BigDecimal("2500.00"))
                .description("Test transfer")
                .build();

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderId").value("ALICE-001"))
                .andExpect(jsonPath("$.receiverId").value("BOB-002"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Account alice = accountRepository.findByAccountNumber("ALICE-001").orElseThrow();
        Account bob = accountRepository.findByAccountNumber("BOB-002").orElseThrow();

        assertThat(alice.getBalance()).isEqualByComparingTo("7500.0000");
        assertThat(bob.getBalance()).isEqualByComparingTo("7500.0000");
    }

    @Test
    void insufficientFunds_rollsBack_balancesUnchanged() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("BOB-002")
                .destinationAccountNumber("ALICE-001")
                .amount(new BigDecimal("99999.00"))
                .description("Should fail")
                .build();

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Insufficient funds")));

        // Balances unchanged
        Account alice = accountRepository.findByAccountNumber("ALICE-001").orElseThrow();
        Account bob = accountRepository.findByAccountNumber("BOB-002").orElseThrow();

        assertThat(alice.getBalance()).isEqualByComparingTo("10000.0000");
        assertThat(bob.getBalance()).isEqualByComparingTo("5000.0000");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void concurrentTransfers_optimisticLockHandled() throws Exception {
        // Set Alice to exactly 1000 so that 5 concurrent transfers of 300 can't all succeed
        Account alice = accountRepository.findByAccountNumber("ALICE-001").orElseThrow();
        alice.setBalance(new BigDecimal("1000.0000"));
        accountRepository.save(alice);

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        AtomicInteger badRequests = new AtomicInteger(0);

        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("ALICE-001")
                .destinationAccountNumber("BOB-002")
                .amount(new BigDecimal("300.00"))
                .description("Concurrent transfer")
                .build();

        String json = objectMapper.writeValueAsString(request);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    var result = mockMvc.perform(post("/api/transfers")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                            .andReturn();
                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 201) successes.incrementAndGet();
                    else if (statusCode == 409) conflicts.incrementAndGet();
                    else if (statusCode == 400) badRequests.incrementAndGet();
                } catch (Exception e) {
                    // count as conflict
                    conflicts.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // At most 3 can succeed (1000 / 300 = 3), remaining get insufficient funds or conflict
        assertThat(successes.get()).isLessThanOrEqualTo(3);
        assertThat(successes.get() + conflicts.get() + badRequests.get()).isEqualTo(threads);

        // Alice's balance should be non-negative
        Account aliceAfter = accountRepository.findByAccountNumber("ALICE-001").orElseThrow();
        assertThat(aliceAfter.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void existingEndpoints_preserveApiContract() throws Exception {
        // POST /api/transactions should still work
        String txJson = """
                {
                    "senderId": "user_001",
                    "receiverId": "user_002",
                    "amount": 500.00,
                    "type": "TRANSFER",
                    "currency": "SGD",
                    "description": "Test payment"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderId").value("user_001"))
                .andExpect(jsonPath("$.receiverId").value("user_002"))
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.currency").value("SGD"))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.riskLevel").exists());

        // GET /api/transactions
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].senderId").value("user_001"));

        // GET /api/transactions/stats
        mockMvc.perform(get("/api/transactions/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        // GET /api/transactions/health
        mockMvc.perform(get("/api/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("transaction-service"));
    }

    @Test
    void reportEndpoint_returnsAggregatedData() throws Exception {
        // Create a transfer to have data
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("ALICE-001")
                .destinationAccountNumber("BOB-002")
                .amount(new BigDecimal("100.00"))
                .description("For report")
                .build();

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String today = java.time.LocalDate.now().toString();

        mockMvc.perform(get("/api/transfers/report")
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].riskLevel").value("LOW"))
                .andExpect(jsonPath("$[0].transactionCount").value(1))
                .andExpect(jsonPath("$[0].totalAmount").value(100.0));
    }
}

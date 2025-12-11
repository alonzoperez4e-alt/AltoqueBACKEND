package com.altoque.altoque.Service.Impl;

import com.altoque.altoque.Dto.Flow.FlowCreatePaymentDto;
import com.altoque.altoque.Dto.Flow.FlowCreateResponseDto;
import com.altoque.altoque.Service.FlowPaymentService;
import com.altoque.altoque.Utils.FlowSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class FlowPaymentServiceImpl implements FlowPaymentService {

    @Value("${flow.base-url}")
    private String flowBaseUrl;

    @Value("${flow.api-key}")
    private String flowApiKey;

    @Value("${flow.secret-key}")
    private String flowSecretKey;

    @Value("${flow.currency}")
    private String flowCurrency;

    @Value("${server.api-url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // Para convertir metadata a JSON string

    public FlowPaymentServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> getPaymentStatus(String token) throws Exception {
        return callFlowGetEndpoint("/payment/getStatus", token);
    }

    @Override
    public Map<String, Object> getPaymentStatusExtended(String token) throws Exception {
        return callFlowGetEndpoint("/payment/getStatusExtended", token);
    }

    @Override
    public FlowCreateResponseDto createPayment(FlowCreatePaymentDto request) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("apiKey", flowApiKey);
        params.put("commerceOrder", request.getCommerceOrder());
        params.put("subject", request.getSubject());
        params.put("currency", flowCurrency);
        params.put("amount", request.getAmount());
        params.put("email", request.getEmail());
        params.put("urlConfirmation", apiBaseUrl + "/api/flow/confirm");
        params.put("urlReturn", request.getUrlReturn());
        params.put("paymentMethod", 9);

        // --- LÓGICA DE METADATA (Caja ID) ---
        // Si el request trae metadata (ej: cajaId, userId), lo convertimos a JSON y lo metemos en 'optional'
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            String optionalJson = objectMapper.writeValueAsString(request.getMetadata());
            params.put("optional", optionalJson);
        }
        // ------------------------------------

        String signature = FlowSignatureUtil.calculateSignature(params, flowSecretKey);
        params.put("s", signature);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        params.forEach((key, value) -> formData.add(key, value.toString()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);

        String url = flowBaseUrl + "/payment/create";
        try {
            return restTemplate.postForObject(url, entity, FlowCreateResponseDto.class);
        } catch (Exception e) {
            throw new Exception("Error creando pago en Flow: " + e.getMessage());
        }
    }

    private Map<String, Object> callFlowGetEndpoint(String endpoint, String token) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("apiKey", flowApiKey);
        params.put("token", token);

        String signature = FlowSignatureUtil.calculateSignature(params, flowSecretKey);
        params.put("s", signature);

        String url = flowBaseUrl + endpoint;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(builder::queryParam);

        try {
            return restTemplate.getForObject(builder.toUriString(), Map.class);
        } catch (Exception e) {
            throw new Exception("Error comunicando con Flow " + endpoint + ": " + e.getMessage());
        }
    }
}
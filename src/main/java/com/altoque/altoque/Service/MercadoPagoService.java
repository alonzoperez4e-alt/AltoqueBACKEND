package com.altoque.altoque.Service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access_token}")
    private String accessToken;

    @Value("${mercadopago.back_urls.success}")
    private String successUrl;

    @Value("${mercadopago.back_urls.failure}")
    private String failureUrl;

    @Value("${mercadopago.back_urls.pending}")
    private String pendingUrl;

    @PostConstruct
    public void init() {
        if (accessToken != null && !accessToken.isEmpty()) {
            MercadoPagoConfig.setAccessToken(accessToken);
            System.out.println("✅ Mercado Pago SDK inicializado correctamente");
            System.out.println("🔗 Success URL: " + successUrl);
            System.out.println("🔗 Failure URL: " + failureUrl);
            System.out.println("🔗 Pending URL: " + pendingUrl);
        } else {
            System.err.println("❌ ERROR: Access Token de Mercado Pago no configurado");
        }
    }

    public String createPreference(String titulo, Integer quantity, BigDecimal precio, String idCuota) {
        try {
            System.out.println("📝 Creando preferencia MP:");
            System.out.println("   Título: " + titulo);
            System.out.println("   Cantidad: " + quantity);
            System.out.println("   Precio: " + precio);
            System.out.println("   ID Cuota: " + idCuota);

            if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio debe ser mayor a 0");
            }

            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title(titulo)
                    .quantity(quantity)
                    .unitPrice(precio)
                    .currencyId("PEN")
                    .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(itemRequest);

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(backUrls)
                    .autoReturn("approved")  // ✅ HABILITADO para producción
                    .externalReference(idCuota)
                    .statementDescriptor("ALTOQUE")
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            System.out.println("✅ Preferencia creada exitosamente: " + preference.getId());
            return preference.getId();

        } catch (MPApiException e) {
            System.err.println("❌ Error API Mercado Pago:");
            System.err.println("   Status Code: " + e.getStatusCode());
            System.err.println("   Message: " + e.getMessage());
            if (e.getApiResponse() != null) {
                System.err.println("   API Response: " + e.getApiResponse().getContent());
            }
            throw new RuntimeException("Error en API de Mercado Pago: " + e.getMessage(), e);

        } catch (MPException e) {
            System.err.println("❌ Error SDK Mercado Pago: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error en SDK de Mercado Pago: " + e.getMessage(), e);

        } catch (Exception e) {
            System.err.println("❌ Error inesperado creando preferencia: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error creando preferencia: " + e.getMessage(), e);
        }
    }
}
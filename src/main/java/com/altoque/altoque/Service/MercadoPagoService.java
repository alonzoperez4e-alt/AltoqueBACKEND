package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.Payment.PagoRequestDto;
import com.altoque.altoque.Dto.Payment.PreferenceResponseDto;
import com.altoque.altoque.Entity.Cliente;
import com.altoque.altoque.Entity.Prestamo;
import com.altoque.altoque.Exception.ApiServerException;
import com.altoque.altoque.Repository.ClienteRepository;
import com.altoque.altoque.Repository.CuotaRepository;
import com.altoque.altoque.Repository.PrestamoRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${mercadopago.webhook.url}")
    private String notificationUrl;

    // URLs de retorno
    @Value("${mercadopago.back.urls.success}")
    private String backUrlSuccess;

    @Value("${mercadopago.back.urls.failure}")
    private String backUrlFailure;

    @Value("${mercadopago.back.urls.pending}")
    private String backUrlPending;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @PostConstruct
    public void init() {
        // Validación básica de credenciales al iniciar
        if (accessToken == null || accessToken.isEmpty() || accessToken.contains("TEST")) {
            System.out.println("ADVERTENCIA: El Access Token parece ser de PRUEBA o no existe.");
            System.out.println("Token actual inicia con: " + (accessToken != null && accessToken.length() > 10 ? accessToken.substring(0, 10) : "NULL"));
        }
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    public PreferenceResponseDto createPreference(PagoRequestDto pagoRequest) {
        try {
            // 1. Obtener datos
            Prestamo prestamo = prestamoRepository.findById(pagoRequest.getPrestamoId())
                    .orElseThrow(() -> new RuntimeException("Préstamo no encontrado ID: " + pagoRequest.getPrestamoId()));

            Cliente cliente = clienteRepository.findById(prestamo.getCliente().getIdCliente())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado para el préstamo"));

            // 2. Construir items de la preferencia
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Pago Préstamo #" + prestamo.getIdPrestamo())
                    .quantity(1)
                    .unitPrice(pagoRequest.getMonto())
                    .currencyId("PEN")
                    .description("Pago de cuota - Cliente: " + (cliente.getRazonSocial() != null ? cliente.getRazonSocial() : cliente.getNombreCliente()))
                    .id("PRESTAMO-" + prestamo.getIdPrestamo())
                    .build();
            items.add(item);

            // 3. URLs de Retorno
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(backUrlSuccess)
                    .failure(backUrlFailure)
                    .pending(backUrlPending)
                    .build();

            // 4. Datos del Pagador (LÓGICA JURÍDICO vs NATURAL)
            String nombrePayer;
            String apellidoPayer;
            String docType = "DNI";
            String docNumber = "";

            // Verificamos si tiene Razón Social (Prioridad para Jurídicos)
            // Asumimos que tu entidad Cliente tiene getRazonSocial() y getRuc()
            if (cliente.getRazonSocial() != null && !cliente.getRazonSocial().trim().isEmpty()) {
                // --- ES UNA EMPRESA ---
                nombrePayer = cliente.getRazonSocial();
                // Mercado Pago exige un apellido. Para empresas, usamos un placeholder válido.
                apellidoPayer = "Empresa";

                // Usamos RUC si está disponible
                if (cliente.getRuc() != null && !cliente.getRuc().trim().isEmpty()) {
                    docType = "RUC";
                    docNumber = cliente.getRuc();
                }
            } else {
                // --- ES PERSONA NATURAL ---
                nombrePayer = (cliente.getNombreCliente() != null && !cliente.getNombreCliente().trim().isEmpty())
                        ? cliente.getNombreCliente() : "Cliente";
                apellidoPayer = (cliente.getApellidoCliente() != null && !cliente.getApellidoCliente().trim().isEmpty())
                        ? cliente.getApellidoCliente() : "General";

                // Usamos DNI si está disponible
                if (cliente.getDniCliente() != null && !cliente.getDniCliente().trim().isEmpty()) {
                    docNumber = cliente.getDniCliente();
                }
            }

            // Email es obligatorio siempre
            String emailPayer = (cliente.getCorreoCliente() != null && !cliente.getCorreoCliente().trim().isEmpty())
                    ? cliente.getCorreoCliente() : "cliente@sinemail.com";

            // Construcción del objeto Payer
            PreferencePayerRequest.PreferencePayerRequestBuilder payerBuilder = PreferencePayerRequest.builder()
                    .email(emailPayer)
                    .name(nombrePayer)
                    .surname(apellidoPayer);

            // Agregar identificación solo si tenemos datos válidos
            if (!docNumber.isEmpty()) {
                payerBuilder.identification(
                        IdentificationRequest.builder()
                                .type(docType)
                                .number(docNumber)
                                .build()
                );
            }

            PreferencePayerRequest payer = payerBuilder.build();

            // 5. Validar HTTPS en Webhook (Requisito estricto de Producción)
            String finalNotificationUrl = notificationUrl;
            if (notificationUrl != null && !notificationUrl.startsWith("https://")) {
                System.out.println("ADVERTENCIA: La notificationUrl no es HTTPS. Mercado Pago podría no enviar notificaciones en Producción.");
            }

            // 6. Configurar Preferencia
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .notificationUrl(finalNotificationUrl)
                    .statementDescriptor("ALTOQUE") // Nombre en el estado de cuenta
                    .externalReference(String.valueOf(prestamo.getIdPrestamo()))
                    .expires(false)
                    .build();

            // 7. Crear en Mercado Pago
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            System.out.println("Preferencia creada con éxito. ID: " + preference.getId());
            return new PreferenceResponseDto(preference.getId(), preference.getInitPoint());

        } catch (MPApiException e) {
            // ERROR ESPECÍFICO DE MERCADO PAGO
            System.err.println("============== ERROR MERCADO PAGO API ==============");
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Respuesta Completa: " + e.getApiResponse().getContent());
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("====================================================");

            String errorMsg = "Error en pasarela de pago: " + e.getMessage();
            if (e.getApiResponse() != null && e.getApiResponse().getContent() != null) {
                errorMsg += " Detalle: " + e.getApiResponse().getContent();
            }
            throw new ApiServerException(errorMsg);

        } catch (MPException e) {
            System.err.println("Error General MP: " + e.getMessage());
            throw new ApiServerException("Error interno de conexión con Mercado Pago.");
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error inesperado al procesar pago.");
        }
    }

    public String handleWebhook(String topic, Long id) {
        // ... (Tu lógica existente para webhooks)
        return "OK";
    }
}
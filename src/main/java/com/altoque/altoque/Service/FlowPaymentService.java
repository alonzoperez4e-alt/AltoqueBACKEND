package com.altoque.altoque.Service;

import com.altoque.altoque.Dto.Flow.FlowCreatePaymentDto;
import com.altoque.altoque.Dto.Flow.FlowCreateResponseDto;
import java.util.Map;

public interface FlowPaymentService {
    /**
     * Consulta el estado de un pago (Básico).
     * @param token El token recibido.
     * @return Datos básicos del pago.
     */
    Map<String, Object> getPaymentStatus(String token) throws Exception;

    /**
     * Consulta el estado extendido de un pago.
     * Útil para obtener códigos de error detallados en caso de rechazo.
     * Corresponde al servicio: payment/getStatusExtended
     * @param token El token recibido.
     * @return Mapa con datos detallados, incluyendo códigos de error si aplica.
     */
    Map<String, Object> getPaymentStatusExtended(String token) throws Exception;

    /**
     * Crea una nueva orden de pago en Flow.
     * Corresponde al servicio: POST /payment/create
     * @param request Datos del pago.
     * @return Objeto con URL y Token para redirección.
     */
    FlowCreateResponseDto createPayment(FlowCreatePaymentDto request) throws Exception;
}
package com.altoque.altoque.Entity;

import jakarta.persistence.*;
import lombok.Data; // Asumo que usas Lombok por el estilo del proyecto
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne
    @JoinColumn(name = "id_prestamo", nullable = false)
    private Prestamo prestamo;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "monto_total", nullable = false)
    private Double monto;

    @Column(name = "ajuste_redondeo")
    private Double ajusteRedondeo = 0.0;

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    // Aquí guardaremos el 'flowOrder' (Número corto de Flow)
    @Column(name = "nro_operacion")
    private String nroOperacion;

    // NUEVO: Aquí guardaremos el 'commerceOrder' (Tu ID único LOAN-X-TIMESTAMP)
    @Column(name = "orden_externa")
    private String ordenExterna;

    @Column(name = "tipo_comprobante", nullable = false)
    private String tipoComprobante = "BOLETA";

    // Modificado: nullable = true para pagos online
    @ManyToOne
    @JoinColumn(name = "id_caja", nullable = true)
    private Caja caja;

    @PrePersist
    public void prePersist() {
        if (this.fechaPago == null) this.fechaPago = LocalDateTime.now();
        if (this.hora == null) this.hora = LocalTime.now();
        if (this.tipoComprobante == null) this.tipoComprobante = "BOLETA";
    }
}
package com.altoque.altoque.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

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

    @ManyToOne
    @JoinColumn(name = "id_caja")
    private Caja caja;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "monto_total", nullable = false)
    private Double montoTotal;

    @Column(name = "ajuste_redondeo")
    private Double ajusteRedondeo = 0.0;

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    @Column(name = "nro_operacion")
    private String nroOperacion;

    @Column(name = "tipo_comprobante", nullable = false)
    private String tipoComprobante;

    @PrePersist
    public void prePersist() {
        if (this.fechaPago == null) {
            this.fechaPago = LocalDateTime.now();
        }
    }
}
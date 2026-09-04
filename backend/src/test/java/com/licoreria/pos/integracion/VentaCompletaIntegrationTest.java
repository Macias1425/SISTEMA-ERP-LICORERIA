package com.licoreria.pos.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licoreria.pos.dto.AperturaCajaDTO;
import com.licoreria.pos.dto.CompraRequestDTO;
import com.licoreria.pos.dto.CotizacionRequestDTO;
import com.licoreria.pos.dto.DetalleCompraDTO;
import com.licoreria.pos.dto.DetalleVentaDTO;
import com.licoreria.pos.dto.LoginRequestDTO;
import com.licoreria.pos.dto.PresentacionDTO;
import com.licoreria.pos.dto.ProductoDTO;
import com.licoreria.pos.dto.ProveedorDTO;
import com.licoreria.pos.dto.VentaRequestDTO;
import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.repository.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre el circuito completo del negocio: catálogo → compra recibida → turno de caja →
 * cotización → venta → factura, comprobando que el stock, los lotes y los totales cuadran.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VentaCompletaIntegrationTest {

    private static final int BOTELLAS_POR_CAJA = 12;
    private static final int CAJAS_COMPRADAS = 2;
    private static final BigDecimal COSTO_CAJA = new BigDecimal("1200.00");
    private static final BigDecimal PRECIO_BOTELLA = new BigDecimal("150.00");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProductoRepository productoRepository;

    private String token;
    private String sufijo;

    @BeforeEach
    void autenticar() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = "Bearer " + objectMapper.readTree(cuerpo).get("token").asText();
        sufijo = String.valueOf(System.currentTimeMillis());
    }

    @Test
    void compraVentaYFacturaMantienenElInventarioCuadrado() throws Exception {
        Long proveedorId = crearProveedor();
        JsonNode producto = crearProducto();
        Long productoId = producto.get("id").asLong();
        Long presentacionCajaId = idPresentacion(producto, "Caja");
        Long presentacionBotellaId = idPresentacion(producto, "Botella");

        recibirCompra(proveedorId, productoId, presentacionCajaId);

        int stockEsperado = CAJAS_COMPRADAS * BOTELLAS_POR_CAJA;
        assertEquals(stockEsperado, stockDe(productoId), "La compra debe ingresar en unidad mínima");

        JsonNode lotes = json(get("/api/inventario/lotes/" + productoId)).get("contenido");
        assertEquals(1, lotes.size(), "La recepción crea un lote trazable");
        assertEquals(stockEsperado, lotes.get(0).get("cantidadDisponibleUmm").asInt());

        abrirCaja();

        JsonNode cotizacion = cotizar(productoId, presentacionBotellaId);
        BigDecimal totalCotizado = new BigDecimal(cotizacion.get("total").asText());
        assertTrue(cotizacion.get("cobrable").asBoolean(), "La cotización debe ser cobrable");

        JsonNode venta = vender(productoId, presentacionBotellaId, totalCotizado);
        assertEquals(0, totalCotizado.compareTo(new BigDecimal(venta.get("total").asText())),
                "El total cobrado debe coincidir con el cotizado");
        assertNotNull(venta.get("factura").get("numero").asText());

        assertEquals(stockEsperado - 3, stockDe(productoId), "La venta descuenta 3 botellas");

        JsonNode lotesTrasVenta = json(get("/api/inventario/lotes/" + productoId)).get("contenido");
        assertEquals(stockEsperado - 3, lotesTrasVenta.get(0).get("cantidadDisponibleUmm").asInt(),
                "El consumo FEFO sale del lote más antiguo");
    }

    // ------------------------------------------------------------------ pasos

    private Long crearProveedor() throws Exception {
        ProveedorDTO dto = ProveedorDTO.builder()
                .nombre("Distribuidora Integración " + sufijo)
                .documento("INT-" + sufijo)
                .build();
        return json(post("/api/proveedores").content(objectMapper.writeValueAsString(dto)))
                .get("id").asLong();
    }

    private JsonNode crearProducto() throws Exception {
        ProductoDTO dto = ProductoDTO.builder()
                .codigo("INT-" + sufijo)
                .nombre("Agua Integración " + sufijo)
                .marca("Pruebas")
                .precioCompra(new BigDecimal("100.00"))
                .precioVenta(PRECIO_BOTELLA)
                .stockActual(0)
                .stockMinimo(6)
                .stockCritico(3)
                // Producto sin alcohol: aísla la prueba del horario legal de venta de licor.
                .esAlcoholico(false)
                .activo(true)
                .presentaciones(List.of(
                        PresentacionDTO.builder().nombre("Botella").factorAUnidadMinima(1).activo(true).build(),
                        PresentacionDTO.builder().nombre("Caja").factorAUnidadMinima(BOTELLAS_POR_CAJA).activo(true).build()
                ))
                .build();
        return json(post("/api/productos").content(objectMapper.writeValueAsString(dto)));
    }

    private void recibirCompra(Long proveedorId, Long productoId, Long presentacionCajaId) throws Exception {
        CompraRequestDTO dto = CompraRequestDTO.builder()
                .proveedorId(proveedorId)
                .documentoProveedor("FAC-" + sufijo)
                .actualizarCostos(true)
                .detalles(List.of(DetalleCompraDTO.builder()
                        .productoId(productoId)
                        .presentacionId(presentacionCajaId)
                        .cantidad(CAJAS_COMPRADAS)
                        .costoUnitario(COSTO_CAJA)
                        .fechaVencimiento(LocalDate.now().plusYears(1))
                        .build()))
                .build();
        json(post("/api/compras").content(objectMapper.writeValueAsString(dto)));
    }

    private void abrirCaja() throws Exception {
        JsonNode estado = json(get("/api/caja/estado"));
        if (estado.get("abierta").asBoolean()) {
            return;
        }
        AperturaCajaDTO dto = AperturaCajaDTO.builder().montoInicial(new BigDecimal("500.00")).build();
        json(post("/api/caja/abrir").content(objectMapper.writeValueAsString(dto)));
    }

    private JsonNode cotizar(Long productoId, Long presentacionId) throws Exception {
        CotizacionRequestDTO dto = CotizacionRequestDTO.builder()
                .tipoCliente(TipoCliente.DETAL)
                .detalles(List.of(DetalleVentaDTO.builder()
                        .productoId(productoId)
                        .presentacionId(presentacionId)
                        .cantidad(3)
                        .build()))
                .build();
        return json(post("/api/ventas/cotizar").content(objectMapper.writeValueAsString(dto)));
    }

    private JsonNode vender(Long productoId, Long presentacionId, BigDecimal total) throws Exception {
        VentaRequestDTO dto = VentaRequestDTO.builder()
                .tipoCliente(TipoCliente.DETAL)
                .confirmacionMayoriaEdad(true)
                .formaPago(FormaPago.EFECTIVO)
                .montoRecibido(total.add(new BigDecimal("100.00")))
                .claveIdempotencia("int-" + sufijo)
                .detalles(List.of(DetalleVentaDTO.builder()
                        .productoId(productoId)
                        .presentacionId(presentacionId)
                        .cantidad(3)
                        .build()))
                .build();
        return json(post("/api/ventas").content(objectMapper.writeValueAsString(dto)));
    }

    // ------------------------------------------------------------------ apoyo

    private int stockDe(Long productoId) {
        return productoRepository.findById(productoId)
                .map(producto -> producto.getStockActual() == null ? 0 : producto.getStockActual())
                .orElseThrow();
    }

    private Long idPresentacion(JsonNode producto, String nombre) {
        for (JsonNode presentacion : producto.get("presentaciones")) {
            if (nombre.equals(presentacion.get("nombre").asText())) {
                return presentacion.get("id").asLong();
            }
        }
        throw new IllegalStateException("El producto no tiene la presentación " + nombre);
    }

    private JsonNode json(MockHttpServletRequestBuilder peticion) throws Exception {
        String cuerpo = mockMvc.perform(peticion
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo);
    }
}

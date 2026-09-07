package com.licoreria.pos.service;

import com.licoreria.pos.dto.ConfiguracionDTO;
import com.licoreria.pos.dto.CorteDiaDTO;
import com.licoreria.pos.dto.ReportePeriodoDTO;
import com.licoreria.pos.dto.ReporteProductoVendidoDTO;
import com.licoreria.pos.dto.TurnoCajaDTO;
import com.licoreria.pos.model.EstadoTurnoCaja;
import com.licoreria.pos.model.EstadoVenta;
import com.licoreria.pos.model.FormaPago;
import com.licoreria.pos.model.Venta;
import com.licoreria.pos.repository.VentaRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CorteDiaService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReporteService reporteService;
    private final ConfiguracionService configuracionService;
    private final VentaRepository ventaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CorteDiaDTO corte(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        // tabla=finanzas → sin recortar listas; pedimos top 10 productos aparte
        ReportePeriodoDTO periodo = reporteService.periodo(dia, dia, null, null, null, "finanzas", 0, 20);

        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fin = dia.atTime(LocalTime.MAX);
        List<Venta> ventasDia = ventaRepository.findByFechaBetween(inicio, fin).stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA)
                .toList();

        List<ReporteProductoVendidoDTO> top = periodo.getProductos() != null
                ? periodo.getProductos().stream().limit(10).toList()
                : List.of();

        List<TurnoCajaDTO> turnos = periodo.getTurnos() != null ? periodo.getTurnos() : List.of();
        int abiertos = (int) turnos.stream()
                .filter(t -> t.getEstado() == EstadoTurnoCaja.ABIERTO)
                .count();

        String nombreNegocio = "Licorería POS";
        try {
            ConfiguracionDTO cfg = configuracionService.obtener();
            if (cfg != null && cfg.getNombreNegocio() != null && !cfg.getNombreNegocio().isBlank()) {
                nombreNegocio = cfg.getNombreNegocio();
            }
        } catch (Exception ignored) {
            // Sin config aún.
        }

        return CorteDiaDTO.builder()
                .fecha(dia)
                .nombreNegocio(nombreNegocio)
                .generadoEn(LocalDateTime.now(clock))
                .ventasCantidad(periodo.getVentasCantidad())
                .ventasSubtotal(nz(periodo.getVentasSubtotal()))
                .ventasImpuesto(nz(periodo.getVentasImpuesto()))
                .ventasTotal(nz(periodo.getVentasTotal()))
                .ticketPromedio(nz(periodo.getTicketPromedio()))
                .ventasEfectivo(sumarForma(ventasDia, FormaPago.EFECTIVO))
                .ventasTarjeta(sumarForma(ventasDia, FormaPago.TARJETA))
                .ventasStripe(sumarForma(ventasDia, FormaPago.STRIPE))
                .ventasCredito(sumarForma(ventasDia, FormaPago.CREDITO))
                .comprasCantidad(periodo.getComprasCantidad())
                .comprasTotal(nz(periodo.getComprasTotal()))
                .resultadoDia(nz(periodo.getResultado()))
                .facturasEmitidas(periodo.getFacturasEmitidas())
                .facturasAnuladas(periodo.getFacturasAnuladas())
                .facturasTotalEmitido(nz(periodo.getFacturasTotalEmitido()))
                .turnosCantidad(turnos.size())
                .turnosAbiertos(abiertos)
                .topProductos(top)
                .turnos(turnos)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] pdf(LocalDate fecha) {
        CorteDiaDTO corte = corte(fecha);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            Paragraph head = new Paragraph(corte.getNombreNegocio(), titulo);
            head.setAlignment(Element.ALIGN_CENTER);
            doc.add(head);

            Paragraph subtitulo = new Paragraph(
                    "Corte del día · " + FECHA.format(corte.getFecha()), h2);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(4f);
            doc.add(subtitulo);

            Paragraph meta = new Paragraph(
                    "Generado: " + FECHA_HORA.format(corte.getGeneradoEn()), sub);
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(14f);
            doc.add(meta);

            doc.add(new Paragraph("Resumen de ventas", h2));
            doc.add(kv("Tickets", String.valueOf(corte.getVentasCantidad()), bold, normal));
            doc.add(kv("Subtotal", dinero(corte.getVentasSubtotal()), bold, normal));
            doc.add(kv("IVA", dinero(corte.getVentasImpuesto()), bold, normal));
            doc.add(kv("Total ventas", dinero(corte.getVentasTotal()), bold, normal));
            doc.add(kv("Ticket promedio", dinero(corte.getTicketPromedio()), bold, normal));
            doc.add(espacio());

            doc.add(new Paragraph("Formas de pago", h2));
            doc.add(kv("Efectivo", dinero(corte.getVentasEfectivo()), bold, normal));
            doc.add(kv("Tarjeta", dinero(corte.getVentasTarjeta()), bold, normal));
            doc.add(kv("Stripe", dinero(corte.getVentasStripe()), bold, normal));
            if (corte.getVentasCredito() != null && corte.getVentasCredito().compareTo(BigDecimal.ZERO) > 0) {
                doc.add(kv("Crédito", dinero(corte.getVentasCredito()), bold, normal));
            }
            doc.add(espacio());

            doc.add(new Paragraph("Compras y resultado", h2));
            doc.add(kv("Compras (" + corte.getComprasCantidad() + ")", dinero(corte.getComprasTotal()), bold, normal));
            doc.add(kv("Resultado del día (ventas − compras)", dinero(corte.getResultadoDia()), bold, normal));
            doc.add(kv("Facturas emitidas", String.valueOf(corte.getFacturasEmitidas()), bold, normal));
            doc.add(kv("Facturas anuladas", String.valueOf(corte.getFacturasAnuladas()), bold, normal));
            doc.add(espacio());

            doc.add(new Paragraph("Top productos del día", h2));
            if (corte.getTopProductos() == null || corte.getTopProductos().isEmpty()) {
                doc.add(new Paragraph("Sin ventas de productos en la fecha.", normal));
            } else {
                PdfPTable tabla = new PdfPTable(new float[]{1.2f, 3.5f, 1.2f, 1.5f});
                tabla.setWidthPercentage(100);
                tabla.setSpacingBefore(6f);
                cabecera(tabla, "Código", "Producto", "UMM", "Total");
                for (ReporteProductoVendidoDTO p : corte.getTopProductos()) {
                    fila(tabla, nvl(p.getCodigo()), nvl(p.getNombre()),
                            String.valueOf(p.getCantidadUmm() == null ? 0 : p.getCantidadUmm()),
                            dinero(p.getTotal()));
                }
                doc.add(tabla);
            }
            doc.add(espacio());

            doc.add(new Paragraph("Turnos de caja", h2));
            doc.add(kv("Turnos en el día", String.valueOf(corte.getTurnosCantidad()), bold, normal));
            doc.add(kv("Turnos abiertos", String.valueOf(corte.getTurnosAbiertos()), bold, normal));
            if (corte.getTurnos() != null && !corte.getTurnos().isEmpty()) {
                PdfPTable tabla = new PdfPTable(new float[]{2.2f, 1.5f, 1.5f, 1.5f});
                tabla.setWidthPercentage(100);
                tabla.setSpacingBefore(6f);
                cabecera(tabla, "Cajero", "Estado", "Ventas", "Efectivo");
                for (TurnoCajaDTO t : corte.getTurnos()) {
                    fila(tabla,
                            nvl(t.getUsuarioNombre()),
                            t.getEstado() == null ? "—" : t.getEstado().name(),
                            dinero(t.getTotalVentas()),
                            dinero(t.getVentasEfectivo()));
                }
                doc.add(tabla);
            }

            Paragraph pie = new Paragraph(
                    "Documento interno · No sustituye comprobantes fiscales.", sub);
            pie.setSpacingBefore(18f);
            pie.setAlignment(Element.ALIGN_CENTER);
            doc.add(pie);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del corte: " + e.getMessage(), e);
        }
    }

    private static BigDecimal sumarForma(List<Venta> ventas, FormaPago forma) {
        return ventas.stream()
                .filter(v -> v.getFormaPago() == forma)
                .map(Venta::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String dinero(BigDecimal v) {
        return "C$ " + nz(v).toPlainString();
    }

    private static String nvl(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static Paragraph kv(String k, String v, Font bold, Font normal) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(k + ": ", bold));
        p.add(new Phrase(v, normal));
        p.setSpacingAfter(2f);
        return p;
    }

    private static Paragraph espacio() {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(6f);
        return p;
    }

    private static void cabecera(PdfPTable tabla, String... cols) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, font));
            cell.setBackgroundColor(new Color(40, 55, 70));
            cell.setPadding(5f);
            tabla.addCell(cell);
        }
    }

    private static void fila(PdfPTable tabla, String... cols) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, font));
            cell.setPadding(4f);
            tabla.addCell(cell);
        }
    }
}

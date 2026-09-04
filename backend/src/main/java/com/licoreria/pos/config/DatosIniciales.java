package com.licoreria.pos.config;

import com.licoreria.pos.model.Categoria;
import com.licoreria.pos.model.Cliente;
import com.licoreria.pos.model.HorarioVentaLicor;
import com.licoreria.pos.model.PrecioProveedor;
import com.licoreria.pos.model.Presentacion;
import com.licoreria.pos.model.Producto;
import com.licoreria.pos.model.Proveedor;
import com.licoreria.pos.model.Rol;
import com.licoreria.pos.model.TipoCliente;
import com.licoreria.pos.model.Usuario;
import com.licoreria.pos.repository.CategoriaRepository;
import com.licoreria.pos.repository.ClienteRepository;
import com.licoreria.pos.repository.HorarioVentaLicorRepository;
import com.licoreria.pos.repository.PrecioProveedorRepository;
import com.licoreria.pos.repository.ProductoRepository;
import com.licoreria.pos.repository.ProveedorRepository;
import com.licoreria.pos.repository.UsuarioRepository;
import com.licoreria.pos.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final HorarioVentaLicorRepository horarioRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final PrecioProveedorRepository precioProveedorRepository;
    private final ConfiguracionService configuracionService;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .nombreCompleto("Administrador")
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .debeCambiarPassword(false)
                    .build());
            log.info("Usuario inicial creado: admin / admin123");
        }
        if (usuarioRepository.findByUsername("cajero").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .username("cajero")
                    .password(passwordEncoder.encode("cajero123"))
                    .nombreCompleto("Cajero de turno")
                    .rol(Rol.CAJERO)
                    .activo(true)
                    .debeCambiarPassword(false)
                    .build());
            log.info("Usuario inicial creado: cajero / cajero123");
        }
        if (usuarioRepository.findByUsername("almacenista").isEmpty()) {
            usuarioRepository.save(Usuario.builder()
                    .username("almacenista")
                    .password(passwordEncoder.encode("almacen123"))
                    .nombreCompleto("Encargado de almacén")
                    .rol(Rol.ALMACENISTA)
                    .activo(true)
                    .debeCambiarPassword(false)
                    .build());
            log.info("Usuario inicial creado: almacenista / almacen123");
        }

        if (horarioRepository.count() == 0) {
            for (DayOfWeek dia : DayOfWeek.values()) {
                horarioRepository.save(HorarioVentaLicor.builder()
                        .diaSemana(dia)
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFin(LocalTime.of(22, 0))
                        .activo(true)
                        .build());
            }
            log.info("Horario de venta de licor: 08:00 a 22:00 todos los días");
        }

        if (categoriaRepository.count() == 0) {
            categoriaRepository.saveAll(List.of(
                    Categoria.builder().nombre("Licores").descripcion("Whisky, ron, vodka y destilados").activo(true).build(),
                    Categoria.builder().nombre("Cervezas").descripcion("Cerveza nacional e importada").activo(true).build(),
                    Categoria.builder().nombre("Vinos").descripcion("Vinos y espumantes").activo(true).build(),
                    Categoria.builder().nombre("Sin alcohol").descripcion("Aguas, jugos y energizantes").activo(true).build(),
                    Categoria.builder().nombre("Barras").descripcion("Barras de chocolate, granola y snacks").activo(true).build(),
                    Categoria.builder().nombre("Paletas").descripcion("Paletas de hielo y helados").activo(true).build(),
                    Categoria.builder().nombre("Queques").descripcion("Queques, pasteles y repostería").activo(true).build()
            ));
            log.info("Categorías iniciales creadas: Licores, Cervezas, Vinos, Sin alcohol, Barras, Paletas, Queques");
        } else {
            asegurarCategoria("Barras", "Barras de chocolate, granola y snacks");
            asegurarCategoria("Paletas", "Paletas de hielo y helados");
            asegurarCategoria("Queques", "Queques, pasteles y repostería");
        }

        if (clienteRepository.count() == 0) {
            clienteRepository.save(Cliente.builder()
                    .nombre("Consumidor final")
                    .tipoCliente(TipoCliente.DETAL)
                    .activo(true)
                    .build());
            log.info("Cliente inicial creado: Consumidor final (DETAL)");
        }

        if (proveedorRepository.count() == 0) {
            proveedorRepository.save(Proveedor.builder()
                    .nombre("Distribuidora del Norte")
                    .documento("J0310000123456")
                    .contactoNombre("Ventas mayoristas")
                    .telefono("2222-3344")
                    .email("ventas@distnorte.ni")
                    .activo(true)
                    .build());
            log.info("Proveedor inicial creado: Distribuidora del Norte");
        }

        cargarCatalogoDemo();
        alinearSemillaNicaragua();

        configuracionService.sincronizarAlArranque();
    }

    /** Catálogo de demostración persistido en MySQL vía JPA (no es estático). */
    private void cargarCatalogoDemo() {
        Proveedor proveedor = proveedorRepository.findAll().stream().findFirst().orElse(null);
        Long catLicores = idCategoria("Licores");
        Long catCervezas = idCategoria("Cervezas");
        Long catVinos = idCategoria("Vinos");
        Long catSinAlcohol = idCategoria("Sin alcohol");

        crearProductoSiNoExiste("RON-001", "Ron Añejo 750ml", "Flor de Caña", catLicores,
                new BigDecimal("180.00"), new BigDecimal("250.00"), 24, 6, 3, true, proveedor,
                "https://images.unsplash.com/photo-1618885475625-07e5039336c1?w=400&h=400&fit=crop");
        crearProductoSiNoExiste("CER-001", "Cerveza Toña 355ml", "Toña", catCervezas,
                new BigDecimal("18.00"), new BigDecimal("30.00"), 48, 12, 6, true, proveedor,
                "https://images.unsplash.com/photo-1608270586620-248524c67de9?w=400&h=400&fit=crop");
        crearProductoSiNoExiste("VIN-001", "Vino Tinto 750ml", "Campo Viejo", catVinos,
                new BigDecimal("120.00"), new BigDecimal("185.00"), 12, 4, 2, true, proveedor,
                "https://images.unsplash.com/photo-1510812431400-574042140a09?w=400&h=400&fit=crop");
        crearProductoSiNoExiste("WHI-001", "Whisky 750ml", "Old Parr", catLicores,
                new BigDecimal("320.00"), new BigDecimal("450.00"), 10, 3, 1, true, proveedor,
                "https://images.unsplash.com/photo-1527287840650-4cba8f74b7c0?w=400&h=400&fit=crop");
        crearProductoSiNoExiste("VOD-001", "Vodka 750ml", "Smirnoff", catLicores,
                new BigDecimal("210.00"), new BigDecimal("295.00"), 15, 4, 2, true, proveedor,
                "https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=400&h=400&fit=crop");
        crearProductoSiNoExiste("AGU-001", "Agua purificada 600ml", "Crystal", catSinAlcohol,
                new BigDecimal("8.00"), new BigDecimal("15.00"), 60, 20, 10, false, proveedor,
                "https://images.unsplash.com/photo-1548839140-29a749e1cf4d?w=400&h=400&fit=crop");
        crearProductoSiNoExiste("GIN-001", "Ginebra 750ml", "Beefeater", catLicores,
                new BigDecimal("240.00"), new BigDecimal("340.00"), 8, 2, 1, true, proveedor,
                "https://images.unsplash.com/photo-1598971639059-fab3c3109a00?w=400&h=400&fit=crop");
        crearProductoSiNoExiste("TEQ-001", "Tequila 750ml", "Olmeca", catLicores,
                new BigDecimal("190.00"), new BigDecimal("270.00"), 14, 4, 2, true, proveedor,
                "https://images.unsplash.com/photo-1622484214992-704338d6080a?w=400&h=400&fit=crop");
        backfillImagenesDemo();
    }

    private void backfillImagenesDemo() {
        Map<String, String> imagenes = Map.of(
                "RON-001", "https://images.unsplash.com/photo-1618885475625-07e5039336c1?w=400&h=400&fit=crop",
                "CER-001", "https://images.unsplash.com/photo-1608270586620-248524c67de9?w=400&h=400&fit=crop",
                "VIN-001", "https://images.unsplash.com/photo-1510812431400-574042140a09?w=400&h=400&fit=crop",
                "WHI-001", "https://images.unsplash.com/photo-1527287840650-4cba8f74b7c0?w=400&h=400&fit=crop",
                "VOD-001", "https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=400&h=400&fit=crop",
                "AGU-001", "https://images.unsplash.com/photo-1548839140-29a749e1cf4d?w=400&h=400&fit=crop",
                "GIN-001", "https://images.unsplash.com/photo-1598971639059-fab3c3109a00?w=400&h=400&fit=crop",
                "TEQ-001", "https://images.unsplash.com/photo-1622484214992-704338d6080a?w=400&h=400&fit=crop"
        );
        for (Map.Entry<String, String> entry : imagenes.entrySet()) {
            productoRepository.findByCodigo(entry.getKey()).ifPresent(producto -> {
                if (producto.getUrlImagen() == null || producto.getUrlImagen().isBlank()) {
                    producto.setUrlImagen(entry.getValue());
                    productoRepository.save(producto);
                }
            });
        }
    }

    private Long idCategoria(String nombre) {
        return categoriaRepository.findByNombre(nombre).map(Categoria::getId).orElse(null);
    }

    private void crearProductoSiNoExiste(
            String codigo,
            String nombre,
            String marca,
            Long categoriaId,
            BigDecimal precioCompra,
            BigDecimal precioVenta,
            int stock,
            int stockMinimo,
            int stockCritico,
            boolean esAlcoholico,
            Proveedor proveedor,
            String urlImagen
    ) {
        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            return;
        }
        Producto producto = Producto.builder()
                .codigo(codigo)
                .nombre(nombre)
                .marca(marca)
                .urlImagen(urlImagen)
                .categoriaId(categoriaId)
                .unidadMinima("BOTELLA")
                .precioCompra(precioCompra)
                .precioVenta(precioVenta)
                .stockActual(stock)
                .stockMinimo(stockMinimo)
                .stockCritico(stockCritico)
                .esAlcoholico(esAlcoholico)
                .activo(true)
                .build();
        Presentacion botella = Presentacion.builder()
                .producto(producto)
                .nombre("Botella")
                .factorAUnidadMinima(1)
                .activo(true)
                .build();
        producto.getPresentaciones().add(botella);
        Producto guardado = productoRepository.save(producto);

        if (proveedor != null && !guardado.getPresentaciones().isEmpty()) {
            Presentacion pres = guardado.getPresentaciones().get(0);
            precioProveedorRepository.save(PrecioProveedor.builder()
                    .proveedorId(proveedor.getId())
                    .productoId(guardado.getId())
                    .presentacionId(pres.getId())
                    .precioUnitario(precioCompra)
                    .activo(true)
                    .build());
        }
        log.info("Producto demo creado en BD: {} · stock {} UMM", codigo, stock);
    }

    private void alinearSemillaNicaragua() {
        proveedorRepository.findAll().stream()
                .filter(proveedor -> "Distribuidora del Norte".equals(proveedor.getNombre()))
                .filter(proveedor -> proveedor.getEmail() != null && proveedor.getEmail().endsWith(".hn"))
                .forEach(proveedor -> {
                    proveedor.setDocumento("J0310000123456");
                    proveedor.setTelefono("2222-3344");
                    proveedor.setEmail("ventas@distnorte.ni");
                    proveedorRepository.save(proveedor);
                    log.info("Proveedor demo actualizado a RUC nicaragüense");
                });
        productoRepository.findByCodigo("CER-001").ifPresent(producto -> {
            if ("Salva Vida".equalsIgnoreCase(producto.getMarca())) {
                producto.setMarca("Toña");
                producto.setNombre("Cerveza Toña 355ml");
                productoRepository.save(producto);
                log.info("Cerveza demo actualizada a Toña");
            }
        });
    }

    private void asegurarCategoria(String nombre, String descripcion) {
        if (!categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            categoriaRepository.save(Categoria.builder()
                    .nombre(nombre)
                    .descripcion(descripcion)
                    .activo(true)
                    .build());
            log.info("Categoría agregada: {}", nombre);
        }
    }
}

package com.licoreria.pos.service;

import com.licoreria.pos.config.PosProperties;
import com.licoreria.pos.dto.AlertaMantenimientoDTO;
import com.licoreria.pos.dto.MantenimientoResumenDTO;
import com.licoreria.pos.dto.PaginaDTO;
import com.licoreria.pos.dto.RespaldoDTO;
import com.licoreria.pos.dto.ResultadoTareaDTO;
import com.licoreria.pos.dto.SaludTablaDTO;
import com.licoreria.pos.exception.ReglaNegocioException;
import com.licoreria.pos.model.AccionAuditoria;
import com.licoreria.pos.security.UsuarioActualService;
import com.licoreria.pos.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MantenimientoService {

    static final Pattern PATRON_NOMBRE = Pattern.compile("pos_licoreria_[0-9]{8}_[0-9]{6}\\.sql");
    private static final Pattern PATRON_JDBC = Pattern.compile("jdbc:mysql://([^:/]+)(?::(\\d+))?/([^?]+)");
    private static final Pattern PATRON_IDENT = Pattern.compile("[A-Za-z0-9_]+");
    private static final String MARCA_RESPALDO = "POS Licorería · Respaldo SQL";
    private static final int LOTE_INSERT = 50;
    private static final long DISCO_CRITICO_BYTES = 100L * 1024 * 1024;

    private final DataSource dataSource;
    private final PosProperties posProperties;
    private final AuditoriaService auditoriaService;
    private final UsuarioActualService usuarioActualService;
    private final Clock clock;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Transactional(readOnly = true)
    public MantenimientoResumenDTO resumen() {
        ConexionBd conexion = parsearJdbcUrl(jdbcUrl);
        List<SaludTablaDTO> tablas = listarSaludTablas(conexion.database());
        long registros = tablas.stream().mapToLong(item -> item.getFilas() == null ? 0L : item.getFilas()).sum();
        int tablasAdvertencia = (int) tablas.stream().filter(t -> "ADVERTENCIA".equals(t.getEstado())).count();
        int tablasError = (int) tablas.stream().filter(t -> "ERROR".equals(t.getEstado())).count();
        List<RespaldoDTO> respaldos = listarRespaldos();
        long espacio = respaldos.stream().mapToLong(item -> item.getTamanoBytes() == null ? 0L : item.getTamanoBytes()).sum();
        LocalDateTime ultimo = respaldos.isEmpty() ? null : respaldos.get(0).getFechaCreacion();
        LocalDateTime ahora = LocalDateTime.now(clock);
        Long horas = ultimo == null ? null : Math.max(0, Duration.between(ultimo, ahora).toHours());
        String estadoRespaldo = evaluarEstadoRespaldo(ultimo, ahora);
        EspacioDisco disco = espacioDisco(directorioRespaldos());
        String estadoSalud = peorEstado(estadoRespaldo,
                tablasError > 0 ? "CRITICO" : tablasAdvertencia > 0 ? "ADVERTENCIA" : "OK",
                disco.libreBytes() < DISCO_CRITICO_BYTES ? "CRITICO" : "OK");

        boolean dumpOk = comandoDisponible(posProperties.getRespaldo().getMysqldumpPath());
        boolean mysqlOk = comandoDisponible(posProperties.getRespaldo().getMysqlPath());

        return MantenimientoResumenDTO.builder()
                .baseDatos(conexion.database())
                .motor("MySQL")
                .version(consultarVersion())
                .host(conexion.host() + (conexion.port() != null ? ":" + conexion.port() : ""))
                .tablas(tablas.size())
                .registrosEstimados(registros)
                .respaldosTotales((long) respaldos.size())
                .espacioRespaldosBytes(espacio)
                .espacioRespaldosLegible(formatearTamano(espacio))
                .ultimoRespaldo(ultimo)
                .horasDesdeUltimoRespaldo(horas)
                .directorioRespaldos(directorioRespaldos().toAbsolutePath().toString())
                .mysqldumpDisponible(dumpOk)
                .mysqlDisponible(mysqlOk)
                .restauracionHabilitada(posProperties.getRespaldo().isPermitirRestaurar())
                .maxRespaldos(posProperties.getRespaldo().getMaxRespaldos())
                .tablasConAdvertencia(tablasAdvertencia)
                .tablasConError(tablasError)
                .espacioDiscoLibreBytes(disco.libreBytes())
                .espacioDiscoLibreLegible(formatearTamano(disco.libreBytes()))
                .estadoSalud(estadoSalud)
                .estadoRespaldo(estadoRespaldo)
                .alertas(construirAlertas(estadoRespaldo, horas, tablasAdvertencia, tablasError, dumpOk, mysqlOk,
                        disco.libreBytes(), posProperties.getRespaldo().isPermitirRestaurar(), respaldos.size(),
                        posProperties.getRespaldo().getMaxRespaldos()))
                .tablasSalud(tablas)
                .build();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<RespaldoDTO> listarRespaldos(int pagina, int tamano) {
        return PaginacionUtil.deLista(listarRespaldos(), pagina, tamano);
    }

    @Transactional(readOnly = true)
    public List<RespaldoDTO> listarRespaldos() {
        Path directorio = directorioRespaldos();
        if (!Files.exists(directorio)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directorio)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> PATRON_NOMBRE.matcher(path.getFileName().toString()).matches())
                    .map(this::mapearRespaldo)
                    .sorted(Comparator.comparing(RespaldoDTO::getFechaCreacion, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (IOException ex) {
            throw new ReglaNegocioException("RESPALDO_LISTAR_ERROR", "No se pudo leer el directorio de respaldos");
        }
    }

    public RespaldoDTO crearRespaldo() {
        Path destino = nuevoArchivoRespaldo();
        String metodo;
        try {
            metodo = exportarSql(destino);
        } catch (RuntimeException ex) {
            borrarSilencioso(destino);
            throw ex;
        }

        limpiarRespaldosAntiguos();
        auditoriaService.registrar(
                usuarioActualService.usuario(),
                AccionAuditoria.CONFIGURACION,
                "RespaldoSQL",
                null,
                null,
                destino.getFileName().toString(),
                "Respaldo SQL generado (" + metodo + ")"
        );
        return mapearRespaldo(destino, metodo);
    }

    public Resource descargarRespaldo(String nombre) {
        Path archivo = resolverArchivo(nombre);
        if (!Files.exists(archivo)) {
            throw new ReglaNegocioException("RESPALDO_NO_ENCONTRADO", "El respaldo solicitado no existe");
        }
        return new FileSystemResource(archivo);
    }

    public void eliminarRespaldo(String nombre) {
        Path archivo = resolverArchivo(nombre);
        if (!Files.exists(archivo)) {
            throw new ReglaNegocioException("RESPALDO_NO_ENCONTRADO", "El respaldo solicitado no existe");
        }
        try {
            Files.delete(archivo);
        } catch (IOException ex) {
            throw new ReglaNegocioException("RESPALDO_ELIMINAR_ERROR", "No se pudo eliminar el respaldo");
        }
        auditoriaService.registrar(
                usuarioActualService.usuario(),
                AccionAuditoria.CONFIGURACION,
                "RespaldoSQL",
                null,
                nombre,
                null,
                "Respaldo SQL eliminado"
        );
    }

    public ResultadoTareaDTO restaurar(MultipartFile archivo, String confirmacion) {
        validarRestauracionHabilitada();
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("ARCHIVO_REQUERIDO", "Debe seleccionar un archivo .sql");
        }
        String nombreOriginal = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        if (!nombreOriginal.toLowerCase(Locale.ROOT).endsWith(".sql")) {
            throw new ReglaNegocioException("ARCHIVO_INVALIDO", "Solo se admiten archivos .sql");
        }
        validarConfirmacion(confirmacion);

        Path temporal = null;
        try {
            temporal = Files.createTempFile("restore-pos-", ".sql");
            Files.copy(archivo.getInputStream(), temporal, StandardCopyOption.REPLACE_EXISTING);
            validarArchivoSql(temporal);
            return ejecutarRestauracion(temporal, nombreOriginal, true);
        } catch (IOException ex) {
            throw new ReglaNegocioException("RESTAURAR_ERROR", "No se pudo procesar el archivo SQL");
        } finally {
            if (temporal != null) {
                borrarSilencioso(temporal);
            }
        }
    }

    public ResultadoTareaDTO restaurarExistente(String nombre, String confirmacion) {
        validarRestauracionHabilitada();
        validarConfirmacion(confirmacion);
        Path archivo = resolverArchivo(nombre);
        if (!Files.exists(archivo)) {
            throw new ReglaNegocioException("RESPALDO_NO_ENCONTRADO", "El respaldo solicitado no existe");
        }
        validarArchivoSql(archivo);
        Path temporal = null;
        try {
            temporal = Files.createTempFile("restore-pos-", ".sql");
            Files.copy(archivo, temporal, StandardCopyOption.REPLACE_EXISTING);
            return ejecutarRestauracion(temporal, nombre, true);
        } catch (IOException ex) {
            throw new ReglaNegocioException("RESTAURAR_ERROR", "No se pudo copiar el respaldo para restaurar");
        } finally {
            if (temporal != null) {
                borrarSilencioso(temporal);
            }
        }
    }

    public ResultadoTareaDTO optimizarTablas(boolean soloFragmentadas) {
        long inicio = System.currentTimeMillis();
        List<String> detalles = new ArrayList<>();
        int ok = 0;
        int error = 0;
        ConexionBd conexion = parsearJdbcUrl(jdbcUrl);
        List<SaludTablaDTO> candidatas = listarSaludTablas(conexion.database()).stream()
                .filter(tabla -> !soloFragmentadas || "ADVERTENCIA".equals(tabla.getEstado()))
                .toList();

        if (candidatas.isEmpty()) {
            return ResultadoTareaDTO.builder()
                    .exito(true)
                    .tipo("OPTIMIZE")
                    .mensaje(soloFragmentadas
                            ? "No hay tablas fragmentadas que optimizar"
                            : "No hay tablas para optimizar")
                    .duracionMs(System.currentTimeMillis() - inicio)
                    .build();
        }

        try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
            for (SaludTablaDTO tabla : candidatas) {
                String ident = citarIdentificador(tabla.getNombre());
                try (ResultSet rs = stmt.executeQuery("OPTIMIZE TABLE " + ident)) {
                    String resumen = leerMensajeCheck(rs);
                    detalles.add(tabla.getNombre() + ": " + resumen);
                    if (resumen.toLowerCase(Locale.ROOT).contains("error")) {
                        error++;
                    } else {
                        ok++;
                    }
                } catch (SQLException ex) {
                    error++;
                    detalles.add(tabla.getNombre() + ": ERROR - " + ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            throw new ReglaNegocioException("OPTIMIZE_ERROR", "No se pudo optimizar las tablas: " + ex.getMessage());
        }

        auditoriaService.registrar(
                usuarioActualService.usuario(),
                AccionAuditoria.CONFIGURACION,
                "MantenimientoBD",
                null,
                null,
                soloFragmentadas ? "OPTIMIZE_FRAGMENTADAS" : "OPTIMIZE",
                "Optimización de tablas ejecutada"
        );
        boolean exito = error == 0;
        return ResultadoTareaDTO.builder()
                .exito(exito)
                .tipo("OPTIMIZE")
                .mensaje(exito
                        ? "Optimización completada en " + ok + " tabla(s)"
                        : "Optimización con errores: " + error + " tabla(s) fallaron")
                .detalles(detalles)
                .duracionMs(System.currentTimeMillis() - inicio)
                .tablasOk(ok)
                .tablasError(error)
                .build();
    }

    public ResultadoTareaDTO verificarTablas() {
        long inicio = System.currentTimeMillis();
        List<String> detalles = new ArrayList<>();
        int ok = 0;
        int advertencia = 0;
        int error = 0;
        ConexionBd conexion = parsearJdbcUrl(jdbcUrl);

        try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
            for (SaludTablaDTO tabla : listarSaludTablas(conexion.database())) {
                String ident = citarIdentificador(tabla.getNombre());
                try (ResultSet rs = stmt.executeQuery("CHECK TABLE " + ident)) {
                    String tipo = "status";
                    String texto = "OK";
                    while (rs.next()) {
                        tipo = rs.getString("Msg_type");
                        texto = rs.getString("Msg_text");
                        detalles.add(tabla.getNombre() + ": " + tipo + " - " + texto);
                    }
                    if ("error".equalsIgnoreCase(tipo)) {
                        error++;
                    } else if ("warning".equalsIgnoreCase(tipo)) {
                        advertencia++;
                    } else {
                        ok++;
                    }
                } catch (SQLException ex) {
                    error++;
                    detalles.add(tabla.getNombre() + ": ERROR - " + ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            throw new ReglaNegocioException("CHECK_ERROR", "No se pudo verificar las tablas: " + ex.getMessage());
        }

        auditoriaService.registrar(
                usuarioActualService.usuario(),
                AccionAuditoria.CONFIGURACION,
                "MantenimientoBD",
                null,
                null,
                "CHECK",
                "Verificación de tablas ejecutada"
        );
        boolean exito = error == 0;
        String mensaje;
        if (error > 0) {
            mensaje = "Se detectaron errores en " + error + " tabla(s)";
        } else if (advertencia > 0) {
            mensaje = "Integridad verificada con " + advertencia + " advertencia(s)";
        } else {
            mensaje = "Integridad verificada sin errores en " + ok + " tabla(s)";
        }
        return ResultadoTareaDTO.builder()
                .exito(exito)
                .tipo("CHECK")
                .mensaje(mensaje)
                .detalles(detalles)
                .duracionMs(System.currentTimeMillis() - inicio)
                .tablasOk(ok)
                .tablasAdvertencia(advertencia)
                .tablasError(error)
                .build();
    }

    private ResultadoTareaDTO ejecutarRestauracion(Path archivo, String etiqueta, boolean crearSeguridad) {
        long inicio = System.currentTimeMillis();
        String seguridad = null;
        if (crearSeguridad) {
            try {
                seguridad = crearRespaldo().getNombre();
            } catch (RuntimeException ex) {
                log.warn("No se pudo generar respaldo de seguridad antes de restaurar: {}", ex.getMessage());
            }
        }
        try {
            restaurarDesdeArchivo(archivo);
        } catch (IOException ex) {
            throw new ReglaNegocioException("RESTAURAR_ERROR", "No se pudo leer el archivo SQL");
        } catch (SQLException ex) {
            throw new ReglaNegocioException("RESTAURAR_ERROR", "Error al ejecutar el script SQL: " + ex.getMessage());
        }

        auditoriaService.registrar(
                usuarioActualService.usuario(),
                AccionAuditoria.CONFIGURACION,
                "RespaldoSQL",
                null,
                null,
                etiqueta,
                "Restauración SQL ejecutada" + (seguridad != null ? " (seguridad: " + seguridad + ")" : "")
        );
        String extra = seguridad != null
                ? ". Se guardó un respaldo de seguridad: " + seguridad
                : ". No se pudo generar respaldo de seguridad previo.";
        return ResultadoTareaDTO.builder()
                .exito(true)
                .tipo("RESTORE")
                .mensaje("Base de datos restaurada desde " + etiqueta + extra)
                .duracionMs(System.currentTimeMillis() - inicio)
                .respaldoSeguridad(seguridad)
                .build();
    }

    private Path nuevoArchivoRespaldo() {
        Path directorio = directorioRespaldos();
        try {
            Files.createDirectories(directorio);
        } catch (IOException ex) {
            throw new ReglaNegocioException("RESPALDO_DIR_ERROR", "No se pudo crear el directorio de respaldos");
        }
        String nombre = "pos_licoreria_" + LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        return directorio.resolve(nombre);
    }

    private String exportarSql(Path destino) {
        if (intentarMysqldump(destino)) {
            return "MYSQLDUMP";
        }
        try {
            exportarJdbc(destino);
            return "JDBC";
        } catch (IOException | SQLException ex) {
            borrarSilencioso(destino);
            throw new ReglaNegocioException("RESPALDO_ERROR", "No se pudo generar el respaldo SQL: " + ex.getMessage());
        }
    }

    private boolean intentarMysqldump(Path destino) {
        ConexionBd conexion = parsearJdbcUrl(jdbcUrl);
        PosProperties.Respaldo cfg = posProperties.getRespaldo();
        List<String> comando = new ArrayList<>();
        comando.add(cfg.getMysqldumpPath());
        comando.add("-h");
        comando.add(conexion.host());
        if (conexion.port() != null) {
            comando.add("-P");
            comando.add(conexion.port());
        }
        comando.add("-u");
        comando.add(dbUsername);
        comando.add("--single-transaction");
        comando.add("--routines");
        comando.add("--triggers");
        comando.add("--set-gtid-purged=OFF");
        comando.add("--default-character-set=utf8mb4");
        comando.add(conexion.database());

        Path errorLog = null;
        try {
            errorLog = Files.createTempFile("mysqldump-err-", ".log");
            ProcessBuilder builder = new ProcessBuilder(comando);
            builder.redirectOutput(destino.toFile());
            builder.redirectError(errorLog.toFile());
            if (dbPassword != null && !dbPassword.isBlank()) {
                builder.environment().put("MYSQL_PWD", dbPassword);
            }
            Process proceso = builder.start();
            int codigo = proceso.waitFor();
            if (codigo == 0 && Files.size(destino) > 0) {
                return true;
            }
            String error = leerMuestra(errorLog, 400);
            log.warn("mysqldump finalizó con código {} — se usará respaldo JDBC. {}", codigo, error);
            Files.deleteIfExists(destino);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("mysqldump interrumpido — se usará respaldo JDBC");
            borrarSilencioso(destino);
            return false;
        } catch (IOException ex) {
            log.warn("mysqldump no disponible — se usará respaldo JDBC");
            borrarSilencioso(destino);
            return false;
        } finally {
            if (errorLog != null) {
                borrarSilencioso(errorLog);
            }
        }
    }

    private void exportarJdbc(Path destino) throws IOException, SQLException {
        ConexionBd conexion = parsearJdbcUrl(jdbcUrl);
        try (Connection connection = dataSource.getConnection();
             Writer writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            writer.write("-- " + MARCA_RESPALDO + "\n");
            writer.write("-- Formato: pos-licoreria-sql-v1\n");
            writer.write("-- Método: JDBC\n");
            writer.write("-- Generado: " + LocalDateTime.now(clock) + "\n");
            writer.write("-- Base de datos: " + conexion.database() + "\n\n");
            writer.write("SET NAMES utf8mb4;\n");
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n\n");

            List<String> tablas = nombresTablas(connection);
            for (String tabla : tablas) {
                escribirEstructura(connection, writer, tabla);
                escribirDatos(connection, writer, tabla);
                writer.write("\n");
            }
            writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
        }
    }

    private void escribirEstructura(Connection connection, Writer writer, String tabla) throws IOException, SQLException {
        String ident = citarIdentificador(tabla);
        writer.write("-- Estructura: " + tabla + "\n");
        writer.write("DROP TABLE IF EXISTS " + ident + ";\n");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + ident)) {
            if (rs.next()) {
                writer.write(rs.getString(2));
                writer.write(";\n\n");
            }
        }
    }

    private void escribirDatos(Connection connection, Writer writer, String tabla) throws IOException, SQLException {
        String ident = citarIdentificador(tabla);
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + ident)) {
            int columnas = rs.getMetaData().getColumnCount();
            int enLote = 0;
            while (rs.next()) {
                if (enLote == 0) {
                    writer.write("INSERT INTO " + ident + " VALUES\n");
                } else {
                    writer.write(",\n");
                }
                writer.write("(");
                for (int i = 1; i <= columnas; i++) {
                    if (i > 1) {
                        writer.write(", ");
                    }
                    writer.write(formatearSql(rs.getObject(i)));
                }
                writer.write(")");
                enLote++;
                if (enLote >= LOTE_INSERT) {
                    writer.write(";\n");
                    enLote = 0;
                }
            }
            if (enLote > 0) {
                writer.write(";\n");
            }
        }
    }

    private String formatearSql(Object valor) {
        if (valor == null) {
            return "NULL";
        }
        if (valor instanceof Number || valor instanceof Boolean) {
            return valor.toString();
        }
        if (valor instanceof byte[] bytes) {
            StringBuilder hex = new StringBuilder("0x");
            for (byte b : bytes) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        }
        if (valor instanceof java.sql.Date || valor instanceof java.sql.Time || valor instanceof java.sql.Timestamp) {
            return "'" + valor.toString().replace("'", "''") + "'";
        }
        return "'" + valor.toString().replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private List<String> nombresTablas(Connection connection) throws SQLException {
        List<String> tablas = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) {
                tablas.add(rs.getString(1));
            }
        }
        return tablas;
    }

    private void restaurarDesdeArchivo(Path archivo) throws IOException, SQLException {
        if (intentarMysqlRestore(archivo)) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(archivo.toFile()));
        }
    }

    private boolean intentarMysqlRestore(Path archivo) {
        ConexionBd conexion = parsearJdbcUrl(jdbcUrl);
        PosProperties.Respaldo cfg = posProperties.getRespaldo();
        List<String> comando = new ArrayList<>();
        comando.add(cfg.getMysqlPath());
        comando.add("-h");
        comando.add(conexion.host());
        if (conexion.port() != null) {
            comando.add("-P");
            comando.add(conexion.port());
        }
        comando.add("-u");
        comando.add(dbUsername);
        comando.add("--default-character-set=utf8mb4");
        comando.add(conexion.database());

        try {
            ProcessBuilder builder = new ProcessBuilder(comando);
            builder.redirectInput(archivo.toFile());
            builder.redirectErrorStream(true);
            if (dbPassword != null && !dbPassword.isBlank()) {
                builder.environment().put("MYSQL_PWD", dbPassword);
            }
            Process proceso = builder.start();
            String salida;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder buf = new StringBuilder();
                String linea;
                while ((linea = reader.readLine()) != null && buf.length() < 800) {
                    buf.append(linea).append('\n');
                }
                salida = buf.toString();
            }
            int codigo = proceso.waitFor();
            if (codigo == 0) {
                return true;
            }
            log.warn("mysql restore finalizó con código {}: {}", codigo, salida);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    private List<SaludTablaDTO> listarSaludTablas(String baseDatos) {
        List<SaludTablaDTO> tablas = new ArrayList<>();
        String sql = """
                SELECT TABLE_NAME, ENGINE, TABLE_ROWS,
                       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb,
                       ROUND(DATA_FREE / 1024 / 1024, 2) AS free_mb
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC, TABLE_NAME
                """;
        try (Connection connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, baseDatos);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double tamano = rs.getDouble("size_mb");
                    double libre = rs.getDouble("free_mb");
                    double pct = clasificarFragmentacion(tamano, libre);
                    String estado = clasificarEstadoTabla(tamano, libre);
                    String comentario = "OK".equals(estado)
                            ? "Sin fragmentación relevante"
                            : String.format(Locale.US, "%.0f%% de espacio reclamable", pct);
                    tablas.add(SaludTablaDTO.builder()
                            .nombre(rs.getString("TABLE_NAME"))
                            .motor(rs.getString("ENGINE"))
                            .filas(rs.getLong("TABLE_ROWS"))
                            .tamanoMb(tamano)
                            .espacioLibreMb(libre)
                            .fragmentacionPct(pct)
                            .estado(estado)
                            .comentario(comentario)
                            .build());
                }
            }
        } catch (SQLException ex) {
            throw new ReglaNegocioException("SALUD_BD_ERROR", "No se pudo consultar el estado de las tablas");
        }
        return tablas;
    }

    private String consultarVersion() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {
            return rs.next() ? rs.getString(1) : "MySQL";
        } catch (SQLException ex) {
            return "MySQL";
        }
    }

    private Path resolverArchivo(String nombre) {
        if (!esNombreRespaldoValido(nombre)) {
            throw new ReglaNegocioException("ARCHIVO_INVALIDO", "Nombre de respaldo no válido");
        }
        Path directorio = directorioRespaldos().toAbsolutePath().normalize();
        Path archivo = directorio.resolve(nombre).normalize();
        if (!archivo.startsWith(directorio)) {
            throw new ReglaNegocioException("ARCHIVO_INVALIDO", "Ruta no permitida");
        }
        return archivo;
    }

    private Path directorioRespaldos() {
        return Path.of(posProperties.getRespaldo().getDirectorio());
    }

    private RespaldoDTO mapearRespaldo(Path path) {
        return mapearRespaldo(path, detectarMetodo(path));
    }

    private RespaldoDTO mapearRespaldo(Path path, String metodo) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            long bytes = attrs.size();
            boolean vacio = bytes <= 0;
            return RespaldoDTO.builder()
                    .nombre(path.getFileName().toString())
                    .tamanoBytes(bytes)
                    .tamanoLegible(formatearTamano(bytes))
                    .fechaCreacion(LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), clock.getZone()))
                    .metodo(metodo)
                    .vacio(vacio)
                    .restaurable(!vacio && posProperties.getRespaldo().isPermitirRestaurar())
                    .build();
        } catch (IOException ex) {
            throw new ReglaNegocioException("RESPALDO_LECTURA_ERROR", "No se pudo leer el respaldo");
        }
    }

    private String detectarMetodo(Path path) {
        String muestra = leerMuestra(path, 400).toLowerCase(Locale.ROOT);
        if (muestra.contains("mysqldump") || muestra.contains("mysql dump")) {
            return "MYSQLDUMP";
        }
        if (muestra.contains("método: jdbc") || muestra.contains("pos licorer")) {
            return "JDBC";
        }
        return "SQL";
    }

    private void limpiarRespaldosAntiguos() {
        int maximo = posProperties.getRespaldo().getMaxRespaldos();
        List<RespaldoDTO> respaldos = listarRespaldos();
        if (respaldos.size() <= maximo) {
            return;
        }
        respaldos.stream()
                .skip(maximo)
                .forEach(item -> {
                    try {
                        Files.deleteIfExists(resolverArchivo(item.getNombre()));
                    } catch (Exception ignored) {
                        log.warn("No se pudo eliminar respaldo antiguo {}", item.getNombre());
                    }
                });
    }

    private void validarRestauracionHabilitada() {
        if (!posProperties.getRespaldo().isPermitirRestaurar()) {
            throw new ReglaNegocioException("RESTAURAR_DESHABILITADO", "La restauración está deshabilitada en este entorno");
        }
    }

    private void validarConfirmacion(String confirmacion) {
        ConexionBd conexion = parsearJdbcUrl(jdbcUrl);
        if (!conexion.database().equalsIgnoreCase(String.valueOf(confirmacion).trim())) {
            throw new ReglaNegocioException("CONFIRMACION_INVALIDA",
                    "Escriba exactamente el nombre de la base de datos para confirmar: " + conexion.database());
        }
    }

    private void validarArchivoSql(Path archivo) {
        try {
            if (!Files.exists(archivo) || Files.size(archivo) < 20) {
                throw new ReglaNegocioException("ARCHIVO_INVALIDO", "El archivo SQL está vacío o es demasiado pequeño");
            }
        } catch (IOException ex) {
            throw new ReglaNegocioException("ARCHIVO_INVALIDO", "No se pudo leer el archivo SQL");
        }
        if (!pareceSqlDeRespaldo(leerMuestra(archivo, 8192))) {
            throw new ReglaNegocioException("ARCHIVO_INVALIDO",
                    "El archivo no parece un respaldo SQL válido (CREATE TABLE / INSERT / DROP TABLE)");
        }
    }

    private boolean comandoDisponible(String comando) {
        if (comando == null || comando.isBlank()) {
            return false;
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(comando, "--version");
            builder.redirectErrorStream(true);
            Process proceso = builder.start();
            return proceso.waitFor() == 0;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    private EspacioDisco espacioDisco(Path directorio) {
        try {
            Path existente = Files.exists(directorio) ? directorio : directorio.toAbsolutePath().getParent();
            if (existente == null || !Files.exists(existente)) {
                existente = Path.of(".").toAbsolutePath();
            }
            long libre = Files.getFileStore(existente).getUsableSpace();
            return new EspacioDisco(libre);
        } catch (IOException ex) {
            return new EspacioDisco(0L);
        }
    }

    private static String leerMensajeCheck(ResultSet rs) throws SQLException {
        String ultimo = "OK";
        while (rs.next()) {
            String tipo = rs.getString("Msg_type");
            String texto = rs.getString("Msg_text");
            ultimo = tipo + " - " + texto;
        }
        return ultimo;
    }

    private static String leerMuestra(Path path, int maxChars) {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            char[] buf = new char[maxChars];
            int leidos = reader.read(buf);
            return leidos <= 0 ? "" : new String(buf, 0, leidos);
        } catch (IOException ex) {
            return "";
        }
    }

    private static void borrarSilencioso(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    static List<AlertaMantenimientoDTO> construirAlertas(
            String estadoRespaldo,
            Long horas,
            int tablasAdvertencia,
            int tablasError,
            boolean dumpOk,
            boolean mysqlOk,
            long discoLibre,
            boolean restauracionHabilitada,
            int totalRespaldos,
            int maxRespaldos) {
        List<AlertaMantenimientoDTO> alertas = new ArrayList<>();
        if ("CRITICO".equals(estadoRespaldo) && (horas == null)) {
            alertas.add(alerta("critico", "SIN_RESPALDO", "No hay respaldos. Cree uno ahora antes de cambiar datos."));
        } else if ("CRITICO".equals(estadoRespaldo)) {
            alertas.add(alerta("critico", "RESPALDO_VIEJO",
                    "El último respaldo tiene más de 7 días. Genere uno nuevo."));
        } else if ("ADVERTENCIA".equals(estadoRespaldo)) {
            long dias = horas == null ? 0 : horas / 24;
            alertas.add(alerta("advertencia", "RESPALDO_ATRASADO",
                    "Han pasado " + Math.max(1, dias) + " día(s) desde el último respaldo. Se recomienda uno diario."));
        }
        if (tablasError > 0) {
            alertas.add(alerta("critico", "TABLAS_ERROR",
                    tablasError + " tabla(s) con error. Ejecute verificación de integridad."));
        } else if (tablasAdvertencia > 0) {
            alertas.add(alerta("advertencia", "TABLAS_FRAGMENTADAS",
                    tablasAdvertencia + " tabla(s) fragmentada(s). Optimice en horario de baja actividad."));
        }
        if (!dumpOk) {
            alertas.add(alerta("info", "MYSQLDUMP_AUSENTE",
                    "mysqldump no está en PATH. Los respaldos usarán JDBC (más lentos, sin rutinas)."));
        }
        if (!mysqlOk) {
            alertas.add(alerta("info", "MYSQL_AUSENTE",
                    "El cliente mysql no está en PATH. La restauración usará el motor JDBC."));
        }
        if (!restauracionHabilitada) {
            alertas.add(alerta("info", "RESTORE_OFF", "La restauración está deshabilitada en este entorno."));
        }
        if (discoLibre > 0 && discoLibre < DISCO_CRITICO_BYTES) {
            alertas.add(alerta("critico", "DISCO_BAJO",
                    "Queda poco espacio en disco (" + formatearTamano(discoLibre) + "). Libere espacio antes de respaldar."));
        }
        if (maxRespaldos > 0 && totalRespaldos >= maxRespaldos) {
            alertas.add(alerta("advertencia", "ROTACION",
                    "Hay " + totalRespaldos + " respaldos (máx. " + maxRespaldos + "). El siguiente reemplazará el más antiguo."));
        }
        if (alertas.isEmpty()) {
            alertas.add(alerta("info", "OK", "Base de datos y respaldos en buen estado. Conserve una copia fuera del servidor."));
        }
        return alertas;
    }

    private static AlertaMantenimientoDTO alerta(String nivel, String codigo, String mensaje) {
        return AlertaMantenimientoDTO.builder().nivel(nivel).codigo(codigo).mensaje(mensaje).build();
    }

    static String evaluarEstadoRespaldo(LocalDateTime ultimo, LocalDateTime ahora) {
        if (ultimo == null) {
            return "CRITICO";
        }
        long horas = Duration.between(ultimo, ahora).toHours();
        if (horas >= 24 * 7) {
            return "CRITICO";
        }
        if (horas >= 24) {
            return "ADVERTENCIA";
        }
        return "OK";
    }

    static double clasificarFragmentacion(double tamanoMb, double espacioLibreMb) {
        if (tamanoMb <= 0) {
            return 0;
        }
        return Math.max(0, (espacioLibreMb / tamanoMb) * 100.0);
    }

    static String clasificarEstadoTabla(double tamanoMb, double espacioLibreMb) {
        double pct = clasificarFragmentacion(tamanoMb, espacioLibreMb);
        if (espacioLibreMb >= 1.0 && pct >= 30) {
            return "ADVERTENCIA";
        }
        return "OK";
    }

    static String peorEstado(String... estados) {
        int peor = 0;
        for (String estado : estados) {
            peor = Math.max(peor, nivelEstado(estado));
        }
        if (peor >= 2) {
            return "CRITICO";
        }
        if (peor == 1) {
            return "ADVERTENCIA";
        }
        return "OK";
    }

    private static int nivelEstado(String estado) {
        if ("CRITICO".equals(estado) || "ERROR".equals(estado)) {
            return 2;
        }
        if ("ADVERTENCIA".equals(estado)) {
            return 1;
        }
        return 0;
    }

    static boolean esNombreRespaldoValido(String nombre) {
        return nombre != null && PATRON_NOMBRE.matcher(nombre).matches();
    }

    static boolean pareceSqlDeRespaldo(String muestra) {
        if (muestra == null || muestra.isBlank()) {
            return false;
        }
        String t = muestra.toUpperCase(Locale.ROOT);
        return t.contains("CREATE TABLE")
                || t.contains("INSERT INTO")
                || t.contains("DROP TABLE")
                || t.contains("POS LICORER")
                || t.contains("MYSQLDUMP");
    }

    static String citarIdentificador(String nombre) {
        if (nombre == null || !PATRON_IDENT.matcher(nombre).matches()) {
            throw new ReglaNegocioException("TABLA_INVALIDA", "Nombre de tabla no válido");
        }
        return "`" + nombre + "`";
    }

    static ConexionBd parsearJdbcUrl(String url) {
        Matcher matcher = PATRON_JDBC.matcher(url);
        if (!matcher.find()) {
            throw new ReglaNegocioException("BD_CONFIG_INVALIDA", "URL JDBC no reconocida");
        }
        return new ConexionBd(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    static String formatearTamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    record ConexionBd(String host, String port, String database) {
    }

    record EspacioDisco(long libreBytes) {
    }
}

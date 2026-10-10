package com.sgtg.backend.infraestructure.output.archivo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;
import com.sgtg.backend.application.output.ManageArchivoGatewayIntPort;

/**
 * Almacena los archivos de los trabajos de grado en el sistema de archivos,
 * bajo el directorio configurado en {@code sgtg.archivos.directorio}, con la
 * estructura {@code <trabajoId>/<carpeta del tipo>/v<iteración>/<nombre>}
 * (ej. {@code 15/PP-A/v2/propuesta.pdf}). Las rutas que devuelve son relativas
 * a ese directorio y nunca pueden salir de él.
 */
@Service
public class ManageArchivoGatewayImplAdapter implements ManageArchivoGatewayIntPort {

    /** Carpeta de los archivos que se cargan antes de que el trabajo tenga identificador */
    private static final String CARPETA_TEMPORAL = "temporal";
    private static final Pattern CARPETA_ITERACION = Pattern.compile("v(\\d+)");
    private static final int LONGITUD_MAXIMA_NOMBRE = 100;

    private final Path raiz;

    public ManageArchivoGatewayImplAdapter(@Value("${sgtg.archivos.directorio}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(Long trabajoId, String carpeta, ArchivoAdjunto archivo) {
        try {
            Path iteracion = crearSiguienteIteracion(carpetaDelTipo(trabajoId, carpeta));
            Path destino = iteracion.resolve(nombreSeguro(archivo.nombre()));
            Files.write(destino, archivo.contenido(), StandardOpenOption.CREATE_NEW);
            return relativa(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
    }

    @Override
    public String guardarTemporal(ArchivoAdjunto archivo) {
        try {
            Path carpeta = Files.createDirectories(this.raiz.resolve(CARPETA_TEMPORAL)
                    .resolve(UUID.randomUUID().toString()));
            Path destino = carpeta.resolve(nombreSeguro(archivo.nombre()));
            Files.write(destino, archivo.contenido(), StandardOpenOption.CREATE_NEW);
            return relativa(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo temporal", e);
        }
    }

    @Override
    public String reubicar(String rutaTemporal, Long trabajoId, String carpeta) {
        Path origen = resolver(rutaTemporal);
        if (origen == null || !Files.isRegularFile(origen))
            throw new IllegalArgumentException("El archivo temporal no existe: " + rutaTemporal);
        try {
            Path iteracion = crearSiguienteIteracion(carpetaDelTipo(trabajoId, carpeta));
            Path destino = Files.move(origen, iteracion.resolve(origen.getFileName()));
            eliminarSiVacia(origen.getParent());
            return relativa(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo reubicar el archivo", e);
        }
    }

    @Override
    public ArchivoAdjunto obtener(String ruta) {
        Path archivo = resolver(ruta);
        if (archivo == null || !Files.isRegularFile(archivo))
            return null;
        try {
            String nombre = archivo.getFileName().toString();
            return new ArchivoAdjunto(nombre, tipoContenido(archivo), Files.readAllBytes(archivo));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }

    @Override
    public void eliminar(String ruta) {
        Path archivo = resolver(ruta);
        if (archivo == null)
            return;
        try {
            Files.deleteIfExists(archivo);
            // La carpeta de la iteración queda libre para la siguiente carga
            eliminarSiVacia(archivo.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el archivo", e);
        }
    }

    // ----- Apoyo -----

    private Path carpetaDelTipo(Long trabajoId, String carpeta) {
        if (trabajoId == null)
            throw new IllegalArgumentException("El trabajo de grado no tiene identificador");
        return this.raiz.resolve(trabajoId.toString()).resolve(segmentoSeguro(carpeta));
    }

    /**
     * Crea la carpeta {@code v<n>} siguiente a la mayor existente. La creación
     * de la carpeta es atómica: si otra carga la toma primero se intenta con la
     * siguiente, así nunca se sobrescribe un archivo.
     */
    private Path crearSiguienteIteracion(Path carpetaTipo) throws IOException {
        Files.createDirectories(carpetaTipo);
        int siguiente = mayorIteracion(carpetaTipo) + 1;
        while (true) {
            try {
                return Files.createDirectory(carpetaTipo.resolve("v" + siguiente));
            } catch (FileAlreadyExistsException e) {
                siguiente++;
            }
        }
    }

    private static int mayorIteracion(Path carpetaTipo) throws IOException {
        try (Stream<Path> hijos = Files.list(carpetaTipo)) {
            return hijos.filter(Files::isDirectory)
                    .map(hijo -> CARPETA_ITERACION.matcher(hijo.getFileName().toString()))
                    .filter(Matcher::matches)
                    .mapToInt(coincidencia -> Integer.parseInt(coincidencia.group(1)))
                    .max().orElse(0);
        }
    }

    /** Ruta absoluta dentro del directorio raíz; null si la ruta es vacía o sale de él. */
    private Path resolver(String ruta) {
        if (ruta == null || ruta.isBlank())
            return null;
        Path absoluta = this.raiz.resolve(ruta.replace('\\', '/')).normalize();
        return absoluta.startsWith(this.raiz) && !absoluta.equals(this.raiz) ? absoluta : null;
    }

    private String relativa(Path archivo) {
        return this.raiz.relativize(archivo).toString().replace('\\', '/');
    }

    private void eliminarSiVacia(Path carpeta) throws IOException {
        if (carpeta == null || carpeta.equals(this.raiz) || !Files.isDirectory(carpeta))
            return;
        try (Stream<Path> hijos = Files.list(carpeta)) {
            if (hijos.findAny().isPresent())
                return;
        }
        Files.deleteIfExists(carpeta);
    }

    /** Nombre sin directorios, sin tildes y solo con letras, dígitos, punto, guion y guion bajo. */
    private static String nombreSeguro(String nombreOriginal) {
        String nombre = nombreOriginal == null ? "" : Paths.get(nombreOriginal.replace('\\', '/')).getFileName()
                .toString();
        nombre = segmentoSeguro(nombre);
        if (nombre.isEmpty() || nombre.chars().allMatch(c -> c == '.' || c == '_'))
            nombre = "archivo.pdf";
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE)
            nombre = nombre.substring(nombre.length() - LONGITUD_MAXIMA_NOMBRE);
        return nombre;
    }

    private static String segmentoSeguro(String texto) {
        String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = sinTildes.replaceAll("[^A-Za-z0-9._-]", "_");
        return limpio.equals(".") || limpio.equals("..") ? "_" : limpio;
    }

    private static String tipoContenido(Path archivo) throws IOException {
        String tipo = Files.probeContentType(archivo);
        if (tipo != null)
            return tipo;
        return archivo.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf") ? "application/pdf"
                : "application/octet-stream";
    }
}

package com.facturapp.application;

import com.facturapp.domain.model.EmpresaLogo;
import com.facturapp.domain.repository.EmpresaLogoRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Caso de uso: Gestión de logos de empresa.
 * Copia el fichero seleccionado a ~/facturapp/logos/ y persiste los metadatos.
 */
public class LogoUseCase {

    private static final Logger log = Logger.getLogger(LogoUseCase.class.getName());
    private static final Path DIR_LOGOS = Paths.get(System.getProperty("user.home"), "facturapp", "logos");

    private final EmpresaLogoRepository logoRepository;

    public LogoUseCase(EmpresaLogoRepository logoRepository) {
        this.logoRepository = logoRepository;
        crearDirectorioLogos();
    }

    /**
     * Importa un fichero de imagen, lo copia al directorio de logos
     * y registra los metadatos en la BD.
     */
    public EmpresaLogo importarLogo(String nombre, File ficheroOrigen) throws IOException {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del logo es obligatorio.");
        }
        if (ficheroOrigen == null || !ficheroOrigen.exists()) {
            throw new IllegalArgumentException("El fichero de imagen no existe.");
        }

        // Copiar imagen a ~/facturapp/logos/
        String extension = obtenerExtension(ficheroOrigen.getName());
        String nombreFichero = "logo_" + System.currentTimeMillis() + extension;
        Path destino = DIR_LOGOS.resolve(nombreFichero);
        Files.copy(ficheroOrigen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

        EmpresaLogo logo = new EmpresaLogo();
        logo.setNombre(nombre.trim());
        logo.setRutaArchivo(destino.toString());

        EmpresaLogo guardado = logoRepository.save(logo);
        log.info("Logo importado: " + guardado.getNombre() + " → " + destino);
        return guardado;
    }

    /**
     * Actualiza el nombre de un logo existente.
     */
    public EmpresaLogo renombrar(Long id, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        EmpresaLogo logo = logoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Logo no encontrado: " + id));
        logo.setNombre(nuevoNombre.trim());
        return logoRepository.save(logo);
    }

    /**
     * Marca el logo como el activo para usarlo en las facturas PDF.
     */
    public void activarLogo(Long id) {
        logoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Logo no encontrado: " + id));
        logoRepository.setActivo(id);
        log.info("Logo activado ID: " + id);
    }

    /**
     * Elimina el logo de la BD y su fichero de imagen del disco.
     */
    public void eliminarLogo(Long id) {
        EmpresaLogo logo = logoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Logo no encontrado: " + id));

        // Intentar borrar el fichero físico
        try {
            Path ruta = Paths.get(logo.getRutaArchivo());
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            log.warning("No se pudo borrar el fichero: " + logo.getRutaArchivo());
        }

        logoRepository.delete(id);
        log.info("Logo eliminado ID: " + id);
    }

    /** Lista todos los logos registrados */
    public List<EmpresaLogo> listarLogos() {
        return logoRepository.findAll();
    }

    /** Retorna el logo activo, si lo hay */
    public Optional<EmpresaLogo> obtenerLogoActivo() {
        return logoRepository.findActivo();
    }

    private void crearDirectorioLogos() {
        try {
            Files.createDirectories(DIR_LOGOS);
        } catch (IOException e) {
            log.warning("No se pudo crear el directorio de logos: " + e.getMessage());
        }
    }

    private String obtenerExtension(String nombreFichero) {
        int idx = nombreFichero.lastIndexOf('.');
        return idx >= 0 ? nombreFichero.substring(idx).toLowerCase() : ".png";
    }
}

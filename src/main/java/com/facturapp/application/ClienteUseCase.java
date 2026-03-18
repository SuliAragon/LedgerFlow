package com.facturapp.application;

import com.facturapp.domain.model.Cliente;
import com.facturapp.domain.repository.ClienteRepository;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Caso de uso: Gestión completa de clientes (CRUD).
 */
public class ClienteUseCase {

    private static final Logger log = Logger.getLogger(ClienteUseCase.class.getName());
    private final ClienteRepository clienteRepository;

    public ClienteUseCase(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    /**
     * Crea un nuevo cliente validando que el NIF/CIF no esté duplicado.
     */
    public Cliente crearCliente(String nombre, String nifCif, String email,
                                 String telefono, String direccion) {
        validarCamposObligatorios(nombre, nifCif);

        if (clienteRepository.existsByNifCif(nifCif.trim())) {
            throw new IllegalArgumentException("Ya existe un cliente con el NIF/CIF: " + nifCif);
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre.trim());
        cliente.setNifCif(nifCif.trim().toUpperCase());
        cliente.setEmail(email != null ? email.trim() : null);
        cliente.setTelefono(telefono != null ? telefono.trim() : null);
        cliente.setDireccion(direccion != null ? direccion.trim() : null);

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente creado: " + guardado.getNombre() + " (ID: " + guardado.getId() + ")");
        return guardado;
    }

    /**
     * Actualiza los datos de un cliente existente.
     */
    public Cliente actualizarCliente(Long id, String nombre, String nifCif,
                                      String email, String telefono, String direccion) {
        validarCamposObligatorios(nombre, nifCif);

        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + id));

        // Verificar que el NIF/CIF no esté en uso por otro cliente
        Optional<Cliente> existente = clienteRepository.findAll().stream()
            .filter(c -> c.getNifCif().equalsIgnoreCase(nifCif) && !c.getId().equals(id))
            .findFirst();
        if (existente.isPresent()) {
            throw new IllegalArgumentException("El NIF/CIF ya está registrado para otro cliente.");
        }

        cliente.setNombre(nombre.trim());
        cliente.setNifCif(nifCif.trim().toUpperCase());
        cliente.setEmail(email != null ? email.trim() : null);
        cliente.setTelefono(telefono != null ? telefono.trim() : null);
        cliente.setDireccion(direccion != null ? direccion.trim() : null);

        return clienteRepository.save(cliente);
    }

    /** Elimina un cliente por su ID */
    public void eliminarCliente(Long id) {
        clienteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + id));
        clienteRepository.delete(id);
        log.info("Cliente eliminado ID: " + id);
    }

    /** Lista todos los clientes */
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    /** Busca un cliente por ID */
    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    /** Busca clientes por nombre (búsqueda parcial) */
    public List<Cliente> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombre(nombre);
    }

    /** Total de clientes registrados */
    public long totalClientes() {
        return clienteRepository.count();
    }

    private void validarCamposObligatorios(String nombre, String nifCif) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }
        if (nifCif == null || nifCif.isBlank()) {
            throw new IllegalArgumentException("El NIF/CIF es obligatorio.");
        }
    }
}

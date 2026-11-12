package ing.software.dulcemayor.controller.base;

import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.entity.base.BaseEntity;
import ing.software.dulcemayor.service.base.BaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// Patron Template Method.
// Define el esqueleto estándar de los endpoints CRUD (getAll, getById, create, update, delete)
// delegando toda la lógica de negocio a BaseService, que a su vez define los pasos del
// algoritmo (validate, beforeSave, executeSave, afterSave) para que las subclases solo
// personalicen lo específico de cada entidad.

public abstract class BaseController<E extends BaseEntity, ReqDTO, ResDTO> {

    protected final BaseService<E, ReqDTO, ResDTO> service;

    protected BaseController(BaseService<E, ReqDTO, ResDTO> service) {
        this.service = service;
    }

    // Devuelve todos los registros activos (no eliminados)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll()));
    }

    // Devuelve un registro activo buscándolo por su ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    // Crea un nuevo registro a partir del DTO recibido y responde 201 Created
    @PostMapping
    public ResponseEntity<ApiResponse<ResDTO>> create(@RequestBody ReqDTO request) {
        ResDTO created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Created successfully", created));
    }

    // Actualiza un registro activo existente según su ID
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResDTO>> update(@PathVariable String id, @RequestBody ReqDTO request) {
        ResDTO updated = service.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Updated successfully", updated));
    }

    // Realiza el borrado lógico (soft delete) de un registro por su ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted successfully", null));
    }
}

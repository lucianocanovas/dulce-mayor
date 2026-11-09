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

public abstract class BaseController<E extends BaseEntity, ReqDTO, ResDTO> {

    protected final BaseService<E, ReqDTO, ResDTO> service;

    protected BaseController(BaseService<E, ReqDTO, ResDTO> service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResDTO>> create(@RequestBody ReqDTO request) {
        ResDTO created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResDTO>> update(@PathVariable String id, @RequestBody ReqDTO request) {
        ResDTO updated = service.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted successfully", null));
    }
}

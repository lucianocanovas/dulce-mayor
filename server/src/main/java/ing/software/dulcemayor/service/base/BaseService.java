package ing.software.dulcemayor.service.base;

import ing.software.dulcemayor.entity.base.BaseEntity;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Template Method Pattern (Service Layer).
 * Defines the invariant CRUD lifecycle skeleton (validate -> transform -> before -> execute -> after)
 * while allowing concrete domain services to customize specific steps via protected hooks.
 * Internal template execution methods are declared final to prevent subclasses from bypassing the skeleton,
 * while public entrypoints remain proxyable for Spring's @Transactional AOP interceptor.
 */
public abstract class BaseService<E extends BaseEntity, ReqDTO, ResDTO> {

    protected final BaseRepository<E> repository;

    protected BaseService(BaseRepository<E> repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ResDTO> getAll() {
        return executeGetAllTemplate();
    }

    protected final List<ResDTO> executeGetAllTemplate() {
        return repository.findByIsDeletedFalse().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResDTO getById(String id) {
        return executeGetByIdTemplate(id);
    }

    protected final ResDTO executeGetByIdTemplate(String id) {
        E entity = findEntityById(id);
        return toResponseDto(entity);
    }

    @Transactional
    public ResDTO create(ReqDTO reqDto) {
        return executeCreateTemplate(reqDto);
    }

    protected final ResDTO executeCreateTemplate(ReqDTO reqDto) {
        validate(reqDto, null);
        E entity = toEntity(reqDto);
        beforeSave(entity, reqDto);
        E saved = executeSave(entity);
        afterSave(saved, reqDto);
        return toResponseDto(saved);
    }

    @Transactional
    public ResDTO update(String id, ReqDTO reqDto) {
        return executeUpdateTemplate(id, reqDto);
    }

    protected final ResDTO executeUpdateTemplate(String id, ReqDTO reqDto) {
        validate(reqDto, id);
        E entity = findEntityById(id);
        updateEntityFromDto(entity, reqDto);
        beforeSave(entity, reqDto);
        E saved = executeSave(entity);
        afterSave(saved, reqDto);
        return toResponseDto(saved);
    }

    @Transactional
    public void delete(String id) {
        executeDeleteTemplate(id);
    }

    protected final void executeDeleteTemplate(String id) {
        E entity = findEntityById(id);
        validateDelete(entity);
        beforeDelete(entity);
        E deleted = executeDelete(entity);
        afterDelete(deleted);
    }

    // Save lifecycle hooks
    protected void validate(ReqDTO dto, String id) {
        // default no-op, subclasses can override
    }

    protected void beforeSave(E entity, ReqDTO dto) {
        // default no-op, subclasses can override
    }

    protected E executeSave(E entity) {
        return repository.save(entity);
    }

    protected void afterSave(E savedEntity, ReqDTO dto) {
        // default no-op, subclasses can override
    }

    // Delete lifecycle hooks
    protected void validateDelete(E entity) {
        // default no-op, subclasses can override
    }

    protected void beforeDelete(E entity) {
        // default no-op, subclasses can override
    }

    protected E executeDelete(E entity) {
        entity.setDeleted(true);
        return repository.save(entity);
    }

    protected void afterDelete(E deletedEntity) {
        // default no-op, subclasses can override
    }

    // Mapping hooks
    protected abstract E toEntity(ReqDTO dto);

    protected abstract ResDTO toResponseDto(E entity);

    protected abstract void updateEntityFromDto(E entity, ReqDTO dto);

    protected E findEntityById(String id) {
        return repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Entity not found with ID: " + id));
    }
}

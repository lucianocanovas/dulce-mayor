package ing.software.dulcemayor.service.base;

import ing.software.dulcemayor.entity.base.BaseEntity;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public abstract class BaseService<E extends BaseEntity, ReqDTO, ResDTO> {

    protected final BaseRepository<E> repository;

    protected BaseService(BaseRepository<E> repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ResDTO> getAll() {
        return repository.findByIsDeletedFalse().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResDTO getById(String id) {
        E entity = findEntityById(id);
        return toResponseDto(entity);
    }

    @Transactional
    public ResDTO create(ReqDTO reqDto) {
        validate(reqDto, null);
        E entity = toEntity(reqDto);
        beforeSave(entity, reqDto);
        E saved = executeSave(entity);
        afterSave(saved, reqDto);
        return toResponseDto(saved);
    }

    @Transactional
    public ResDTO update(String id, ReqDTO reqDto) {
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
        E entity = findEntityById(id);
        entity.setDeleted(true);
        repository.save(entity);
    }

    // Template method lifecycle hooks
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

    // Mapping hooks
    protected abstract E toEntity(ReqDTO dto);

    protected abstract ResDTO toResponseDto(E entity);

    protected abstract void updateEntityFromDto(E entity, ReqDTO dto);

    protected E findEntityById(String id) {
        return repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Entity not found with ID: " + id));
    }
}

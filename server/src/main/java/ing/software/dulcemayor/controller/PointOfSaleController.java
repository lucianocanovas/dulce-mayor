package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.controller.base.BaseController;
import ing.software.dulcemayor.dto.order.CreatePointOfSaleRequestDTO;
import ing.software.dulcemayor.dto.order.PointOfSaleResponseDTO;
import ing.software.dulcemayor.service.order.PointOfSaleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points-of-sale")
public class PointOfSaleController extends BaseController<CreatePointOfSaleRequestDTO, PointOfSaleResponseDTO> {

    public PointOfSaleController(PointOfSaleService pointOfSaleService) {
        super(pointOfSaleService);
    }

    @Override
    protected String getCreatedMessage() {
        return "Punto de venta registrado con éxito";
    }

    @Override
    protected String getUpdatedMessage() {
        return "Punto de venta actualizado correctamente";
    }

    @Override
    protected String getDeletedMessage() {
        return "Punto de venta eliminado correctamente";
    }
}

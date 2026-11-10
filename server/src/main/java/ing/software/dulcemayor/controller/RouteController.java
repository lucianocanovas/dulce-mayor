package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.logistics.DailyRouteDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.logistics.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyRouteDTO>> getDailyRoute(@RequestParam(required = false) String role) {
        DailyRouteDTO route = routeService.getDailyRoute(role);
        return ResponseEntity.ok(ApiResponse.ok(route));
    }

    @PatchMapping("/stops/{orderId}/deliver")
    public ResponseEntity<ApiResponse<Void>> markDelivered(@PathVariable String orderId) {
        routeService.markStopDelivered(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Parada y pedido marcados como entregados", null));
    }
}

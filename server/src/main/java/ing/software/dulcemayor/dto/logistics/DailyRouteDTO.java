package ing.software.dulcemayor.dto.logistics;

import ing.software.dulcemayor.enums.RouteStatus;
import ing.software.dulcemayor.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRouteDTO {
    private String id;
    private String routeCode;
    private String date;
    private String assignedAgentName;
    private UserRole role;
    private RouteStatus status;
    private List<RouteStopDTO> stops;
}

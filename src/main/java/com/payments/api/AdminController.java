package com.payments.api;

import com.payments.api.dto.AvailabilityRequest;
import com.payments.api.dto.LoadRequest;
import com.payments.api.dto.RoutingModeRequest;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.RoutingMode;
import com.payments.fee.LoadFeeMultiplier;
import com.payments.provider.ProviderRegistry;
import com.payments.routing.RoutingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProviderRegistry providers;
    private final RoutingService routing;
    private final LoadFeeMultiplier load;

    public AdminController(ProviderRegistry providers, RoutingService routing, LoadFeeMultiplier load) {
        this.providers = providers;
        this.routing = routing;
        this.load = load;
    }

    @PostMapping("/providers/{method}/availability")
    public Map<String, Object> setAvailability(
            @PathVariable PaymentMethod method,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        providers.setAvailable(method, request.available());
        return Map.of("method", method, "available", request.available());
    }

    @PostMapping("/routing-mode")
    public Map<String, RoutingMode> setRoutingMode(@Valid @RequestBody RoutingModeRequest request) {
        routing.setMode(request.mode());
        return Map.of("mode", routing.getMode());
    }

    @PostMapping("/load/{method}")
    public Map<String, Object> setLoad(
            @PathVariable PaymentMethod method,
            @Valid @RequestBody LoadRequest request
    ) {
        load.setLoad(method, request.factor());
        return Map.of("method", method, "factor", request.factor());
    }
}

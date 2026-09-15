package com.payments.api;

import com.payments.api.dto.AddCouponRequest;
import com.payments.api.dto.CouponResponse;
import com.payments.domain.enums.CouponType;
import com.payments.domain.money.Money;
import com.payments.service.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.payments.api.ValidationPatterns.COUPON_CODE;

@Validated
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService coupons;

    public CouponController(CouponService coupons) {
        this.coupons = coupons;
    }

    @PostMapping
    public CouponResponse add(@Valid @RequestBody AddCouponRequest request) {
        int storedValue = request.type() == CouponType.FLAT
                ? (int) Money.rupees(request.value()).paise()
                : request.value().intValue();
        return CouponResponse.from(coupons.add(
                request.code(),
                request.type(),
                storedValue,
                request.minAmount() == null ? Money.ZERO : Money.rupees(request.minAmount()),
                request.maxDiscount() == null ? null : Money.rupees(request.maxDiscount()),
                request.remainingUses(),
                request.expiresAt()
        ));
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @Size(max = 20) @Pattern(regexp = COUPON_CODE, message = "code must be alphanumeric") String code
    ) {
        coupons.delete(code);
    }
}

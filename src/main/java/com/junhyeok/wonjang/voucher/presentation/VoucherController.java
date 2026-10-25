package com.junhyeok.wonjang.voucher.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.junhyeok.wonjang.voucher.application.*;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService){
        this.voucherService = voucherService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoucherResponse create(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid CreateVoucherRequest request) {
        return voucherService.create(request, userId);
    }
}


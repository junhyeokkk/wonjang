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

    // F4 역분개. body 는 선택 — 둘 다 null 가능 (reverseDate=오늘, description 자동).
    @PostMapping("/{id}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public VoucherResponse reverse(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody(required = false) @Valid ReverseVoucherRequest request) {
        ReverseVoucherRequest safe = request != null ? request : new ReverseVoucherRequest(null, null);
        return voucherService.reverse(id, safe, userId);
    }
}


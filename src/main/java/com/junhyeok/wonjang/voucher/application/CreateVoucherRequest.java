package com.junhyeok.wonjang.voucher.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateVoucherRequest(
        @NotNull LocalDate voucherDate,
        @Size(max = 200) String description,
        @NotEmpty @Size(min = 2) @Valid List<EntryLine> entries
) { }


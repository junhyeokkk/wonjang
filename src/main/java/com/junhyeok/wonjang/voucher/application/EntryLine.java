package com.junhyeok.wonjang.voucher.application;

import com.junhyeok.wonjang.voucher.domain.EntrySide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EntryLine(
        @NotBlank String accountCode,
        @NotNull EntrySide side,
        @Positive long amount
) {}


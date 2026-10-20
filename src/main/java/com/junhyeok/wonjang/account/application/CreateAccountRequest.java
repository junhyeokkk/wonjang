package com.junhyeok.wonjang.account.application;

import com.junhyeok.wonjang.account.domain.AccountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;


public record CreateAccountRequest(

        @NotBlank
        @Size(max = 10)
        String code,

        @NotBlank
        @Size(max = 50)
        String name,

        @NotNull
        AccountType type
) {}
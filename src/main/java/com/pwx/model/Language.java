package com.pwx.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author pengweixin
 */
@Data
@Builder
public class Language {

    private Long id;

    private String content;
}
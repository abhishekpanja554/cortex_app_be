package com.abhout.cortex_app_be.jobs.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

public class EmbeddingJobNotFoundException  extends BaseException {
    public EmbeddingJobNotFoundException(String message) {
        super("EMBEDDING_JOB_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}

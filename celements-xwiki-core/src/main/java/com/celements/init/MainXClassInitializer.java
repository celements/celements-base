package com.celements.init;

import java.util.concurrent.ExecutionException;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingRunnable;

public interface MainXClassInitializer extends ThrowingRunnable<ExecutionException> {}

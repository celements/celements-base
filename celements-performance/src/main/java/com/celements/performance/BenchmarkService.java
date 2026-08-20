package com.celements.performance;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;

import com.google.common.base.Strings;

@Component
public class BenchmarkService implements BenchmarkRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkService.class);

  private static final String BENCH_LAST_TIME = "bench_lastTime";

  private static final String BENCH_START_TIME = "bench_startTime";

  private static final String CEL_BENCHMARK_VAL_PREFIX = "CEL_BENCHMARK_";

  private static final String CEL_BENCHMARK_OUT_STRINGS = CEL_BENCHMARK_VAL_PREFIX
      + "benchOutStrings";

  @Requirement
  Execution execution;

  private boolean isBenchStarted() {
    return getContextLongValue(BENCH_START_TIME) != null;
  }

  private Long getContextLongValue(String name) {
    return (Long) execution.getContext().getProperty(CEL_BENCHMARK_VAL_PREFIX + name);
  }

  private void setContextLongValue(String name, long value) {
    execution.getContext().setProperty(CEL_BENCHMARK_VAL_PREFIX + name, value);
  }

  @SuppressWarnings("unchecked")
  private ArrayList<String> getBenchOutStringArray() {
    ArrayList<String> benchOutStringArray;
    if (execution.getContext().getProperty(CEL_BENCHMARK_OUT_STRINGS) == null) {
      benchOutStringArray = new ArrayList<>();
      execution.getContext().setProperty(CEL_BENCHMARK_OUT_STRINGS, benchOutStringArray);
    } else {
      benchOutStringArray = (ArrayList<String>) execution.getContext()
          .getProperty(CEL_BENCHMARK_OUT_STRINGS);
    }
    return benchOutStringArray;
  }

  @Override
  public void bench(String label) {
    checkArgument(!Strings.isNullOrEmpty(label));
    if (isBenchStarted()) {
      long currTime = new Date().getTime();
      double totalTime = (currTime - getContextLongValue(BENCH_START_TIME)) / 1000.0;
      double time = (currTime - getContextLongValue(BENCH_LAST_TIME)) / 1000.0;
      setContextLongValue(BENCH_LAST_TIME, currTime);
      getBenchOutStringArray()
          .add("bench '" + label + "' &mdash; in " + time + "s &mdash; total " + totalTime
              + "s");
    } else {
      LOGGER.info("bench called without startBench. Skipping.");
    }
  }

  @Override
  public void startBench() {
    long benchStartTime = new Date().getTime();
    setContextLongValue(BENCH_START_TIME, benchStartTime);
    setContextLongValue(BENCH_LAST_TIME, benchStartTime);
  }

  @Override
  public String println(boolean visible) {
    String outStr = getBenchOutStringArray().stream()
        .collect(Collectors.joining("\n"));
    getBenchOutStringArray().clear();
    if (!visible) {
      outStr = "<!-- " + outStr + " -->\n";
    }
    return outStr;
  }

}

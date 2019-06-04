package com.smartdengg.plugin

import com.google.common.base.Preconditions
import com.google.common.collect.Sets

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

/**
 * 创建时间:  2019/06/04 18:43 <br>
 * 作者:  SmartDengg <br>
 * 描述: */
@Singleton
class ForkJoinExecutor {

  ForkJoinPool forkJoinPool = new ForkJoinPool()
  private final Set<ForkJoinTask<?>> futureSet = Sets.newConcurrentHashSet()

  void execute(Closure task) {
    ForkJoinTask<?> submitted = forkJoinPool.submit(new Runnable() {
      @Override
      void run() {
        task.call()
      }
    })
    boolean added = futureSet.add(submitted)
    Preconditions.checkState(added, "Failed to add task")
  }

  void waitingForAllTasks() {
    try {
      for (Iterator iterator = futureSet.iterator(); iterator.hasNext();) {
        ForkJoinTask<?> task = iterator.next()
        task.join()
        iterator.remove()
      }
    } finally {
    }
  }
}

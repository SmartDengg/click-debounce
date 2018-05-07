package com.smartdengg.compile;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 创建时间:  2018/05/07 16:34 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
public class WeavedClass {

  private String className;
  private Set<String> debouncedMethods = new LinkedHashSet<>();

  WeavedClass(String className) {
    this.className = className;
  }

  void addDebouncedMethod(String methodSignature) {
    debouncedMethods.add(methodSignature);
  }

  public String getClassName() {
    return className;
  }

  public Set<String> getDebouncedMethods() {
    return debouncedMethods;
  }

  public boolean hasDebouncedMethod() {
    return debouncedMethods.size() > 0;
  }
}

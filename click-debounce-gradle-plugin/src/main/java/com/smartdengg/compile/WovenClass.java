package com.smartdengg.compile;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 创建时间:  2018/05/07 16:34 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
public class WovenClass implements Serializable {
  private static final long serialVersionUID = 211794679952616432L;

  public String className;
  public Set<String> debouncedMethods = new LinkedHashSet<>();

  WovenClass(String className) {
    this.className = className;
  }

  void addDebouncedMethod(String methodSignature) {
    debouncedMethods.add(methodSignature);
  }

  public boolean hasDebouncedMethod() {
    return debouncedMethods.size() > 0;
  }
}

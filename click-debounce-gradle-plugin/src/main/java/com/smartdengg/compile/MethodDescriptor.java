package com.smartdengg.compile;

import java.util.Objects;

/**
 * 创建时间:  2019/01/04 17:16 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
public class MethodDescriptor {

  private int access;
  private String name;
  private String desc;

  MethodDescriptor(int access, String name, String desc) {
    this.access = access;
    this.name = name;
    this.desc = desc;
  }

  boolean match(int access, String name, String desc) {
    return this.access == access &&
        Objects.equals(this.name, name) &&
        Objects.equals(this.desc, desc);
  }
}

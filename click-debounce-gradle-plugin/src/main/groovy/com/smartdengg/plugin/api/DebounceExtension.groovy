package com.smartdengg.plugin.api

/**
 * 创建时间:  2019/06/03 12:13 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 * */
class DebounceExtension {
  static final String NAME = "debounce"
  boolean loggable
  Map<String, List<String>> exclusion = [:]
}

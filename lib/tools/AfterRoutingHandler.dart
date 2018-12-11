import 'dart:async';

import 'package:flutter/material.dart';

//class AfterRoutingHandler {
//  State pageState;
//  bool _animationEnd = false;
//  Map<Function, dynamic> _funcDataMap = {};
//
//  bool get _unmounted => !pageState.mounted;
//
//  AfterRoutingHandler({
//    @required this.pageState,
//    @required Duration duration,
//  }) {
//    Timer(Duration(milliseconds: duration.inMilliseconds + 50), () {
//      if (_unmounted) return;
//      if (_funcDataMap.isEmpty) {
//        _animationEnd = true;
//      } else {
//        _funcDataMap.forEach((func, data) => func(data));
//      }
//    });
//  }
//
//  apiUpdate<DataType>({
//    @required bool fetchData,
//    @required Future<DataType> apiFuture,
//    @required Function(Error) apiErrorCallback,
//    @required Function(DataType) updateDataDelegate,
//  }) {
//    if (!fetchData) return;
//    try {
//      apiFuture.then((data) => _apiEnd(updateDataDelegate, data));
//    } catch (e) {
//      _apiEnd(apiErrorCallback, e);
//    }
//  }
//
//  _apiEnd(Function callback, dynamic data) {
//    if (_unmounted) return;
//    if (_animationEnd) {
//      callback(data);
//    } else {
//      _funcDataMap[callback] = data;
//    }
//  }
//}

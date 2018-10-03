import 'dart:async';

import 'package:flutter/material.dart';

class DataAfterRoutingController<DataType> {
  DataAfterRoutingController({
    @required dynamic initData,
    @required Duration animationDuration,
    @required State pageState,
    @required Future<DataType> apiFuture,
    @required Function(DataType) updateDataDelegate,
    Function(Error) apiErrorCallback,
  }) {
    if (hasInitData(initData)) return;
    Timer(Duration(milliseconds: animationDuration.inMilliseconds + 50), () {
      if (!pageState.mounted) return;
      if (_tempData == null) {
        _animationEnd = true;
      } else {
        updateDataDelegate(_tempData);
      }
    });
    try {
      apiFuture.then((data) {
        if (_animationEnd) {
          updateDataDelegate(data);
        } else {
          this._tempData = data;
        }
      });
    } catch (e) {
      apiErrorCallback(e);
    }
  }

  hasInitData(data) => data != null && !((data is List) && data.length == 0);

  var _tempData;
  var _animationEnd = false;
}

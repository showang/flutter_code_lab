
import 'package:flutter/services.dart';


class Channels {
  static const MethodChannel methodChannel =
      const MethodChannel('com.kube.flutter/player.method');
  static const EventChannel eventChannel =
      const EventChannel('com.kube.flutter/player.event');
}

import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';

typedef void StartPlayCallback(Map<String, dynamic> trackInfo);
typedef void StopPlayCallback();
typedef void PlayStateChangedCallback(
    Map<String, dynamic> trackInfo, bool isPlaying);

/// The KUBE player plug-in.
class KubePlayerPlugin {
  static const _channel = const MethodChannel('kube_player_plugin');
  static const _eventChannel = const EventChannel('kube_player_plugin_event');

  /// Asks the native side to start play a playlist.
  ///
  /// To play a playlist, you need to pass the ID of the playlist in
  /// the [playlistId] parameters, and an optional [startIndex] parameter.
  static Future<bool> startPlay(String playlistId, [int startIndex]) async {
    var argumentMap = {'playlist id': playlistId, 'start index': startIndex};
    return await _channel.invokeMethod('startPlay', argumentMap);
  }

  /// Asks the native side to open the "now playing" view.
  static Future<bool> openNowPlaying() async {
    return await _channel.invokeMethod('openNowPlaying');
  }

  /// Asks the native side to return if there is a current playing song track.
  static Future<bool> currentTrack() async {
    return await _channel.invokeMethod('currentTrack');
  }

  /// Asks the native side to resume playing.
  static Future<bool> resumePlay() async {
    return await _channel.invokeMethod('resumePlay');
  }

  /// Asks the native side to pause playing.
  static Future<bool> pause() async {
    return await _channel.invokeMethod('pause');
  }

  /// Asks the native side to stop playing.
  static Future<bool> stop() async {
    return await _channel.invokeMethod('stop');
  }

  static listenEvents({
    StartPlayCallback startPlay: _defaultStartCallback,
    PlayStateChangedCallback stateChanged: _defaultStateChangedCallback,
    StopPlayCallback stopPlay: _defaultStopCallback,
  }) {
    KubePlayerPlugin._eventChannel.receiveBroadcastStream().listen((event) {
      var _nowPlayingJsonMap = json.decode(event);
      var trackInfo;
      var isPlaying = false;
      print(_nowPlayingJsonMap['action']);
      switch (_nowPlayingJsonMap['action']) {
        case 'startPlay':
          trackInfo = _nowPlayingJsonMap['trackInfo'];
          startPlay(trackInfo);
          break;
        case 'playState':
          trackInfo = _nowPlayingJsonMap['trackInfo'];
          isPlaying = _nowPlayingJsonMap['playing'];
          stateChanged(trackInfo, isPlaying);
          break;
        case 'onNowPlayingClose':
          stop();
          break;
        case 'stopPlay':
        default:
          stopPlay();
      }
    });
  }

  static Future<void> notifyTerritoryDidChange(String terr) async {
    var argumentMap = {'terr': terr};
    return await _channel.invokeMethod('territoryDidChange', argumentMap);
  }

  static _defaultStartCallback(Map<String, dynamic> trackInfo) {}

  static _defaultStateChangedCallback(
      Map<String, dynamic> trackInfo, bool isPlaying) {}

  static _defaultStopCallback() {}
}

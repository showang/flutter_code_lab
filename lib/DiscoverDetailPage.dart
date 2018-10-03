import 'dart:async';

import 'package:easy_listview/easy_listview.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_kube/DiscoverPage.dart';
import 'package:flutter_kube/tools/DataAfterRoutingController.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:kube_player_plugin/kube_player_plugin.dart';

class DiscoverDetailPage extends StatefulWidget {
  DiscoverDetailPage(this.api,
      {this.playlistInfo, this.heroTag, this.navigationDuration});

  final String heroTag;
  final KK.PlaylistInfo playlistInfo;
  final KK.KKBOXOpenAPI api;
  final Duration navigationDuration;

  @override
  State<StatefulWidget> createState() => new DiscoverDetailState(heroTag);
}

class DiscoverDetailState extends State<DiscoverDetailPage> {
  DiscoverDetailState(this.heroTag);

  String heroTag;
  bool hasNextPage = true;
  int nextOffset = 0;
  List<KK.TrackInfo> tracks = [];

  KK.TrackList tempTrackData;

  @override
  void initState() {
    super.initState();
    DataAfterRoutingController(
      initData: tracks,
      animationDuration: widget.navigationDuration,
      pageState: this,
      apiFuture:
          widget.api.fetchTracksInPlaylist(widget.playlistInfo.id, offset: 0),
      updateDataDelegate: _updateTrackList,
      apiErrorCallback: (e) => print("onApiError: $e"),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: buildTrackList(tracks),
    );
  }

  void requestPageData(int offset) {
    print("request offset: $offset");
    widget.api
        .fetchTracksInPlaylist(widget.playlistInfo.id, offset: offset)
        .then(_updateTrackList);
  }

  _updateTrackList(KK.TrackList trackList) => setState(() {
        hasNextPage = trackList.tracks.length > 0;
        tracks.addAll(trackList.tracks);
        nextOffset = tracks.length;
        tempTrackData = null;
      });

  Widget buildCoverImage() {
    var screenWidth = MediaQuery.of(context).size.width;
    return GestureDetector(
      child: Container(
        width: screenWidth,
        height: screenWidth,
        child: Hero(
          tag: heroTag,
          child: Image.network(
            widget.playlistInfo.images[1].url,
            fit: BoxFit.fitWidth,
          ),
        ),
      ),
      onTap: () {
        Navigator.pop(context);
      },
    );
  }

  Widget buildTrackList(List<KK.TrackInfo> tracks) {
    return EasyListView(
      itemCount: tracks.length,
      itemBuilder: (context, index) {
        var track = tracks[index];
        return new ListTile(
          title: new Text(track.name),
          subtitle: new Text(track.album.artist.name),
          leading: Image.network(
            track.album.images[0].url,
            fit: BoxFit.cover,
            width: 60.0,
          ),
          onTap: () {
            print("event tap on item $index");
            KubePlayerPlugin.startPlay(widget.playlistInfo.id, index)
                .then((success) {});
          },
        );
      },
      headerBuilder: _headerViewBuilder(),
      loadMore: hasNextPage,
      onLoadMore: () {
        print("onLoadMore");
        requestPageData(nextOffset);
      },
      dividerSize: 2.0,
    );
  }

  WidgetBuilder _headerViewBuilder() {
    var screenWidth = MediaQuery.of(context).size.width;
    return (context) {
      return new Column(children: <Widget>[
//        new Stack(
//          alignment: new Alignment(0.9, 1.1),
//          children: <Widget>[
//            buildCoverImage(),
//            new MaterialButton(
//                height: 52.0,
//                minWidth: 48.0,
//                color: Colors.pinkAccent,
//                child: new Icon(
//                  Icons.play_arrow,
//                  color: Colors.white,
//                  size: 32.0,
//                ),
//                onPressed: () async {
//                  KubePlayerPlugin.startPlay(widget.playlistInfo.id)
//                      .then((success) {});
//                })
//          ],
//        ),
        CoverWithPlayButtonWidget(
          screenWidth: screenWidth,
          heroTag: heroTag,
          playlistInfo: widget.playlistInfo,
        ),
        new Container(
          padding:
              EdgeInsets.only(left: 8.0, top: 16.0, right: 8.0, bottom: 0.0),
          child: new Text(
            widget.playlistInfo.title,
            style: new TextStyle(fontSize: 32.0, fontWeight: FontWeight.bold),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.start,
          ),
        ),
        new Text(
          "Created by: ${widget.playlistInfo.owner.name}",
          textAlign: TextAlign.start,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: new TextStyle(fontSize: 24.0),
        ),
      ]);
    };
  }

  startPlayingMusic() {}
}

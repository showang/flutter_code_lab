import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_kube/Channels.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:easy_listview/easy_listview.dart';

class DiscoverDetailPage extends StatefulWidget {
  DiscoverDetailPage(this.api, {this.playlistInfo, this.heroTag});

  final String heroTag;
  final KK.PlaylistInfo playlistInfo;
  final KK.KKBOXOpenAPI api;
  final List<KK.TrackInfo> tracks = [];

  @override
  State<StatefulWidget> createState() => new DiscoverDetailState(heroTag);
}

class DiscoverDetailState extends State<DiscoverDetailPage> {
  DiscoverDetailState(this.heroTag);

  String heroTag;
  bool hasNextPage = true;
  int nextOffset = 0;

  @override
  void initState() {
    super.initState();
    requestPageData(0);
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(body: buildTrackList(widget.tracks));
  }

  void requestPageData(int offset) {
    widget.api
        .fetchTracksInPlaylist(widget.playlistInfo.id, offset: offset)
        .then((trackList) {
      setState(() {
        hasNextPage = trackList.tracks.length > 0;
        widget.tracks.addAll(trackList.tracks);
        nextOffset = widget.tracks.length;
      });
    });
  }

  Widget buildCoverImage() {
    return GestureDetector(
      child: Hero(
        tag: heroTag,
        child: new Image.network(
          widget.playlistInfo.images[1].url,
          fit: BoxFit.fitWidth,
        ),
      ),
      onTap: () {
        Navigator.pop(context);
      },
    );
  }

  Widget buildTrackList(List<KK.TrackInfo> tracks) {
    return new EasyListView(
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
            Map<String, dynamic> argumentMap = Map();
            argumentMap["info"] = json.encode(widget.playlistInfo.jsonObject);
            argumentMap["position"] = index;
            Channels.methodChannel
                .invokeMethod('startPlay', argumentMap)
                .then((success) {});
          },
        );
      },
      headerBuilder: _headerViewBuilder(),
      loadMore: hasNextPage,
      onLoadMore: () {
        requestPageData(nextOffset);
      },
      dividerSize: 2.0,
    );
  }

  WidgetBuilder _headerViewBuilder() {
    return (context) {
      return new Column(children: <Widget>[
        new Stack(
          alignment: new Alignment(0.9, 1.1),
          children: <Widget>[
            buildCoverImage(),
            new MaterialButton(
                height: 52.0,
                minWidth: 48.0,
                color: Colors.pinkAccent,
                child: new Icon(
                  Icons.play_arrow,
                  color: Colors.white,
                  size: 32.0,
                ),
                onPressed: () async {
                  Map<String, dynamic> argumentMap = Map();
                  argumentMap["info"] =
                      json.encode(widget.playlistInfo.jsonObject);
                  argumentMap["position"] = 0;
                  await Channels.methodChannel
                      .invokeMethod('startPlay', argumentMap);
                })
          ],
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
          "Create by: ${widget.playlistInfo.owner.name}",
          textAlign: TextAlign.start,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: new TextStyle(fontSize: 24.0),
        ),
      ]);
    };
  }
}
